/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.api.llm.MutantAPI.SubmitMutantResponseDTO.MutantRejectReason;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;

import com.github.javaparser.quality.Nullable;

import static org.codedefenders.util.Constants.MUTANT_COMPILED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_DUPLICATED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_UNCOMPILABLE_MESSAGE;

@WebServlet("/llm-api/battleground/submit-mutant")
public class MutantAPI extends APIServlet {
    @Inject
    protected GameService gameService;

    @Inject
    protected GameRepository gameRepo;

    @Inject
    protected GameClassRepository gameClassRepo;

    @Inject
    protected GameProducer gameProducer;

    @Inject
    protected GameManagingUtils gameManagingUtils;

    @Inject
    protected CodeDefendersAuth login;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");
        final var code = ServletUtils.getStringParameter(request, "code");

        if (gameId.isEmpty() || code.isEmpty()) {
            if (gameId.isEmpty()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            } else {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'code' missing."));
            }
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }

        var canSubmit = gameManagingUtils.canUserSubmitMutant(game, login.getUserId(), false);
        switch (canSubmit) {
            case YES -> {}
            default -> {
                writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        new Common.ErrorResponseDTO("Not allowed to submit mutant: " + canSubmit.name()));
                return;
            }
        }

        GameManagingUtils.CreateBattlegroundMutantResult result;
        try {
            result = gameManagingUtils.createBattlegroundMutant(game, login.getUserId(), code.get());
        } catch (GameManagingUtils.MutantCreationException e) {
            writeResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new Common.ErrorResponseDTO("Server error while creating the mutant."));
            return;
        } catch (UncheckedSQLException e) {
            if (e.isDataTooLong()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO(
                                "Error submitting the mutant: data too long. Maybe you made too many changes?"));
            } else {
                writeResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        new Common.ErrorResponseDTO("Database error while saving the mutant."));
            }
            return;
        }

        var messages = new ArrayList<String>();

        if (result.isSuccess()) {
            Mutant mutant = result.mutant().orElseThrow();

            messages.add(MUTANT_COMPILED_MESSAGE);
            result.mutationTesterMessage().ifPresent(messages::add);

            writeResponse(response, HttpServletResponse.SC_OK,
                    new SubmitMutantResponseDTO(
                            true,
                            messages,
                            Common.MutantDTO.fromMutantDTO(gameService.getMutant(login.getUserId(), mutant.getId())),
                            null
                    ));

        } else {
            var failureReason = result.failureReason().orElseThrow();
            switch (failureReason) {
                case VALIDATION_FAILED -> result.validationErrorMessage().ifPresent(msg -> messages.add(msg.get()));
                case DUPLICATE_MUTANT_FOUND -> {
                    messages.add(MUTANT_DUPLICATED_MESSAGE);
                    result.compilationError().ifPresent(messages::add);
                }
                case COMPILATION_FAILED -> {
                    messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
                    result.compilationError().ifPresent(messages::add);
                }
            }

            writeResponse(response, HttpServletResponse.SC_OK,
                    new SubmitMutantResponseDTO(
                            false,
                            messages,
                            null,
                            switch (failureReason) {
                                case VALIDATION_FAILED -> MutantRejectReason.VALIDATION_FAILED;
                                case DUPLICATE_MUTANT_FOUND -> MutantRejectReason.DUPLICATE_MUTANT_FOUND;
                                case COMPILATION_FAILED -> MutantRejectReason.COMPILATION_FAILED;
                            }));
        }
    }

    public record SubmitMutantResponseDTO(
            boolean success,
            List<String> messages,

            @Nullable
            Common.MutantDTO mutant, // null if request failed
            @Nullable
            MutantRejectReason rejectReason // null if request succeeded
    ) {
        public enum MutantRejectReason {
            VALIDATION_FAILED,
            DUPLICATE_MUTANT_FOUND,
            COMPILATION_FAILED,
        }
    }
}

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
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;

import com.github.javaparser.quality.Nullable;

import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;

@WebServlet("/llm-api/battleground/resolve-equivalence")
public class ResolveEquivalenceAPI extends APIServlet {
    @Inject
    protected GameService gameService;

    @Inject
    protected GameRepository gameRepo;

    @Inject
    protected GameClassRepository gameClassRepo;

    @Inject
    protected MutantRepository mutantRepo;

    @Inject
    protected GameProducer gameProducer;

    @Inject
    protected GameManagingUtils gameManagingUtils;

    @Inject
    protected CodeDefendersAuth login;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");
        final Optional<GameManagingUtils.ResolveBattlegroundEquivalenceAction> action =
            ServletUtils.getStringParameter(request, "action")
                    .map(s -> s.toUpperCase())
                    .map(GameManagingUtils.ResolveBattlegroundEquivalenceAction::valueOf);
        final var equivMutantId = ServletUtils.getIntParameter(request, "equivMutantId");
        final var code = ServletUtils.getStringParameter(request, "code");

        if (gameId.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            return;
        } else if (action.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'action' missing."));
            return;
        } else if (equivMutantId.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'equivMutantId' missing."));
            return;
        } else if (code.isEmpty() && action.get() == GameManagingUtils.ResolveBattlegroundEquivalenceAction.REJECT) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'code' missing."));
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }

        var canResolve = gameManagingUtils.canUserResolveEquivalence(game, login.getUserId(), equivMutantId.get());
        switch (canResolve) {
            case YES -> {}
            default -> {
                writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        new Common.ErrorResponseDTO("Not allowed to resolve equivalence: " + canResolve.name()));
                return;
            }
        }

        var mutant = mutantRepo.getMutantById(equivMutantId.get());
        switch (action.get()) {
            case ACCEPT -> {
                acceptEquivalence(request, response, game, mutant);
            }
            case REJECT -> {
                rejectEquivalence(request, response, game, mutant, code.get());
            }
        }
    }

    private void acceptEquivalence(HttpServletRequest request, HttpServletResponse response,
            MultiplayerGame game, Mutant equivMutant) throws IOException {
        var result = gameManagingUtils.acceptBattlegroundEquivalence(game, login.getUserId(), equivMutant);
        var messages = new ArrayList<String>();

        if (result.mutantKillable()) {
            messages.add(Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE + " However, the mutatnt was killable!");
            writeResponse(response, HttpServletResponse.SC_OK,
                    new AcceptEquivalenceResponseDTO(true, messages));
        } else {
            messages.add(Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE);
            writeResponse(response, HttpServletResponse.SC_OK,
                    new AcceptEquivalenceResponseDTO(false, null));
        }
    }

    private void rejectEquivalence(HttpServletRequest request, HttpServletResponse response,
            MultiplayerGame game, Mutant equivMutant, String code) throws IOException {
        GameManagingUtils.RejectBattlegroundEquivalenceResult result;
        try {
            result = gameManagingUtils.rejectBattlegroundEquivalence(game, login.getUserId(), equivMutant, code);
        } catch (IOException e) {
            writeResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new Common.ErrorResponseDTO("Server error while rejecting equivalence."));
            return;
        }
        var messages = new ArrayList<String>();

        if (!result.testValid()) {
            var failureReason = result.failureReason().orElseThrow();
            switch (failureReason) {
                case VALIDATION_FAILED -> result.validationErrorMessages().ifPresent(messages::addAll);
                case COMPILATION_FAILED -> {
                    messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
                    result.compilationError().ifPresent(messages::add);
                }
                case TEST_DID_NOT_PASS_ON_CUT -> {
                    messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
                    result.testCutError().ifPresent(messages::add);
                }
            }

            writeResponse(response, HttpServletResponse.SC_OK,
                    new RejectEquivalenceResponseDTO(
                            false,
                            false,
                            messages,
                            result.test().map(test -> {
                                return Common.TestDTO.fromTestDTO(
                                    gameService.getTest(login.getUserId(), test.getId()));
                            }).orElse(null),
                            switch (failureReason) {
                                case VALIDATION_FAILED -> TestRejectReason.VALIDATION_FAILED;
                                case COMPILATION_FAILED -> TestRejectReason.COMPILATION_FAILED;
                                case TEST_DID_NOT_PASS_ON_CUT -> TestRejectReason.TEST_DID_NOT_PASS_ON_CUT;
                            },
                            null,
                            null));
        } else {
            if (result.killedPendingMutant().orElseThrow()) {
                messages.add(Constants.TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
            } else {
                if (result.isMutantKillable().orElseThrow()) {
                    messages.add(Constants.TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE + " However, the mutatnt was killable!");
                } else {
                    messages.add(Constants.TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
                }
            }

            int killedOthers = result.numOtherPendingMutantsKilled().orElseThrow();
            if (killedOthers == 1) {
                messages.add("Additionally, your test did kill another claimed mutant!");
            } else if (killedOthers > 1) {
                messages.add(String.format("Additionally, your test killed other %d claimed mutants!", killedOthers));
            }

            writeResponse(response, HttpServletResponse.SC_OK,
                    new RejectEquivalenceResponseDTO(
                            true,
                            result.killedPendingMutant().orElseThrow(),
                            messages,
                            result.test().map(test -> {
                                return Common.TestDTO.fromTestDTO(
                                    gameService.getTest(login.getUserId(), test.getId()));
                            }).orElse(null),
                            null,
                            result.isMutantKillable().orElseThrow(),
                            result.numOtherPendingMutantsKilled().orElseThrow()));
        }
    }

    public record AcceptEquivalenceResponseDTO(
            boolean mutantKillable,
            ArrayList<String> messages
    ) {
    }

    public record RejectEquivalenceResponseDTO(
            boolean testValid,
            boolean killedPendingMutant,
            ArrayList<String> messages,

            @Nullable
            Common.TestDTO test, // null test is invalid
            @Nullable
            TestRejectReason testRejectReason, // null if test is valid

            @Nullable
            Boolean isMutantKillableWithOtherTests, // null if test is invalid
            @Nullable
            Integer numOtherPendingMutantsKilled // null if test is invalid
    ) {
    }

    public enum TestRejectReason {
        VALIDATION_FAILED,
        COMPILATION_FAILED,
        TEST_DID_NOT_PASS_ON_CUT,
    }
}

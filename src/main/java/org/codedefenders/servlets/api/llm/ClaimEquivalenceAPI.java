package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;

@WebServlet("/llm-api/battleground/claim-equivalence")
public class ClaimEquivalenceAPI extends APIServlet {
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
        Optional<String> equivLinesParam = ServletUtils.getStringParameter(request, "equivLines");

        if (gameId.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            return;
        } else if (equivLinesParam.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'equivLines' missing."));
            return;
        }

        List<Integer> equivLines;
        try {
            equivLines = Arrays.stream(equivLinesParam.get().split(","))
                .map(Integer::valueOf)
                .toList();
        } catch (NumberFormatException e) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Invalid value for 'equivLines' parameter."));
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }

        var canResolve = gameManagingUtils.canUserClaimEquivalence(game, login.getUserId());
        switch (canResolve) {
            case YES -> {}
            default -> {
                writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        new Common.ErrorResponseDTO("Not allowed to resolve equivalence: " + canResolve.name()));
                return;
            }
        }

        var result = gameManagingUtils.claimBattlegroundEquivalence(game, login.getUserId(), equivLines);
        var mutantDTOs = result.claimedMutants().stream()
            .map(m -> gameService.getMutant(login.getUserId(), m))
            .map(Common.MutantDTO::fromMutantDTO)
            .toList();
        writeResponse(response, HttpServletResponse.SC_OK,
                new ClaimEquivalenceResponseDTO(mutantDTOs, result.messages()));
        return;
    }

    public record ClaimEquivalenceResponseDTO(
        List<Common.MutantDTO> claimedMutants,
        List<String> messages
    ) {
    }
}

package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.FileUtils;
import org.codedefenders.validation.code.CodeValidatorLevel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet("/llm-api/battleground/game")
public class GameAPI extends HttpServlet {
    @Inject
    protected GameService gameService;

    @Inject
    protected GameRepository gameRepo;

    @Inject
    protected GameClassRepository gameClassRepo;

    @Inject
    protected GameProducer gameProducer;

    @Inject
    protected CodeDefendersAuth login;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");

        if (gameId.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();

        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }

        if (game.getRole(login.getUserId()) == Role.NONE) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    new Common.ErrorResponseDTO("User is not a player in the game."));
            return;
        }

        var gameDTO = prepareGame(game, login.getUserId());
        writeResponse(response, HttpServletResponse.SC_OK, gameDTO);
    }

    protected void writeResponse(HttpServletResponse response, int statusCode, Object responseBody) throws IOException {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .create();
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setStatus(statusCode);
        gson.toJson(responseBody, out);
        out.flush();
    }

    private BattlegroundGameDTO prepareGame(MultiplayerGame game, int userId) {
        List<Common.PlayerDTO> players = gameRepo.getValidPlayersForGame(game.getId()).stream()
                .map(player -> {
                    boolean isSystemPlayer = player.getUser().getId() < 5;
                    return new Common.PlayerDTO(
                            player.getId(),
                            player.getUser().getId(),
                            isSystemPlayer,
                            player.getRole(),
                            player.getPoints()
                    );
                })
                .toList();

        List<Common.EventDTO> events = game.getEvents().stream()
                .map(event -> new Common.EventDTO(
                        event.getId(),
                        event.getUserId(),
                        event.getEventType(),
                        event.getTimestamp()
                ))
                .toList();

        List<Common.MutantDTO> mutants = gameService.getMutants(userId, game.getId()).stream()
                .map(Common.MutantDTO::fromMutantDTO)
                .toList();

        List<Common.TestDTO> tests = gameService.getTests(userId, game.getId()).stream()
                .map(test -> new Common.TestDTO(
                        test.getId(),
                        test.getPlayerId(),
                        test.isCanView(),
                        test.getSource(),
                        test.getPoints(),
                        test.getLinesCovered(),
                        test.getCoveredMutantIds(),
                        test.getKilledMutantIds()

                )).toList();


        List<Common.CutDTO.DependencyDTO> deps = gameClassRepo.getMappedDependenciesForClassId(game.getClassId()).stream()
                .map(dep -> {
                    Path path = Paths.get(dep.getJavaFile());
                    return new Common.CutDTO.DependencyDTO(
                            FileUtils.extractFileNameNoExtension(path),
                            FileUtils.readJavaFileWithDefault(path)
                    );
                }).toList();

        Common.CutDTO cut = gameClassRepo.getClassForId(game.getClassId())
                .map(cut_ -> new Common.CutDTO(
                        cut_.getId(),
                        cut_.getAlias(),
                        cut_.getName(),
                        cut_.getSourceCode(),
                        deps,
                        cut_.getTestingFramework(),
                        cut_.getAssertionLibrary()
                ))
                .orElseThrow();

        return new BattlegroundGameDTO(
                cut,
                players,
                mutants,
                tests,
                events,
                game.getStartTimeUnixSeconds(),
                game.getGameDurationMinutes(),
                game.getLevel(),
                game.getMutantValidatorLevel(),
                game.getMaxAssertionsPerTest(),
                game.getAutomaticMutantEquivalenceThreshold()
        );
    }

    public record BattlegroundGameDTO(
            Common.CutDTO cut,
            List<Common.PlayerDTO> players,
            List<Common.MutantDTO> mutants,
            List<Common.TestDTO> tests,
            List<Common.EventDTO> history,

            long startTime,
            int duration,

            GameLevel level,
            CodeValidatorLevel mutantValidatorLevel,
            int maxAssertionsPerTest,
            int automaticEquivalenceThreshold
    ) {
    }

}

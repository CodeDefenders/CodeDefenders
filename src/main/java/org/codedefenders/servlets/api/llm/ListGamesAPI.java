package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.model.UserMultiplayerGameInfo;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;

@WebServlet("/llm-api/battleground/list")
public class ListGamesAPI extends APIServlet {
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

    @Inject
    private MultiplayerGameRepository multiplayerGameRepo;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        List<UserMultiplayerGameInfo> activeGames =
                multiplayerGameRepo.getActiveMultiplayerGamesWithInfoForUser(login.getUserId());
        List<UserMultiplayerGameInfo> openGames =
                multiplayerGameRepo.getOpenMultiplayerGamesWithInfoForUser(login.getUserId());

        writeResponse(response, HttpStatus.SC_OK,
                new ListGamesResponseDTO(
                        activeGames.stream().map(this::convertGameDTO).toList(),
                        openGames.stream().map(this::convertGameDTO).toList()
                )
        );
    }

    private BattlegroundGameDTO convertGameDTO(UserMultiplayerGameInfo gameInfo) {
        var players = Stream.of(
                gameInfo.game.getAttackerPlayers().stream(),
                gameInfo.game.getDefenderPlayers().stream(),
                gameInfo.game.getObserverPlayers().stream()
        )
                .flatMap(Function.identity())
                .map(p -> new PlayerDTO(
                        p.getId(),
                        p.getUser().getId(),
                        p.getUser().getUsername(),
                        p.getRole(),
                        p.getPoints()
                ))
                .toList();

        return new BattlegroundGameDTO(
                gameInfo.gameId(),
                gameInfo.cutId(),
                gameInfo.cutAlias(),
                gameInfo.creatorId(),
                gameInfo.creatorName(),
                gameInfo.game.getStartTimeUnixSeconds(),
                gameInfo.game.getGameDurationMinutes(),
                gameInfo.gameLevel(),
                players
        );
    }

    public record ListGamesResponseDTO(
            List<BattlegroundGameDTO> activeGames,
            List<BattlegroundGameDTO> openGames
    ) {}

    public record BattlegroundGameDTO(
            int gameId,
            int classId,
            String classAlias,
            int creatorId,
            String creatorName,
            long startTime,
            int durationMinutes,
            GameLevel level,
            List<PlayerDTO> players
    ) {}

    public record PlayerDTO(
            int playerId,
            int userId,
            String userName,
            Role role,
            int points
    ) {}
}

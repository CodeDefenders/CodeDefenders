package org.codedefenders.servlets;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameJoinedEvent;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@WebServlet({Paths.INVITE})
public class InvitePage extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(InvitePage.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private GameProducer gameProducer;

    @Inject
    private PlayerRepository playerRepo;

    @Inject
    private URLUtils url;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private INotificationService notificationService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("Invite page requested");
        MultiplayerGame game = gameProducer.getMultiplayerGame();

        if (game == null) {
            logger.warn("gameProducer returned null");
            messages.add("There is no game with this id.").fadeOut(false);
            resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            return;
        }
        int gameId = game.getId();
        int userId = login.getUserId();

        int playerId = playerRepo.getPlayerIdForUserAndGame(userId, gameId);

        if (playerId == -1 && game.getCreatorId() != login.getUserId()) {
            if (!AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_JOINING).getBoolValue()) {
                logger.warn("User {} tried to join game {}, but game joining is disabled.", userId, gameId);
                messages.add("Joining games is disabled.").fadeOut(false);
            }

            GameJoinedEvent event = new GameJoinedEvent();
            event.setUserId(userId);
            event.setGameId(gameId);
            event.setUserName(login.getSimpleUser().getName());

            game.addPlayer(userId, Role.ATTACKER);
            notificationService.post(event);
            resp.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
        }
    }


}

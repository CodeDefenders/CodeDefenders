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
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameJoinedEvent;
import org.codedefenders.persistence.database.GameRepository;
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
    private GameRepository gameRepository;

    @Inject
    private URLUtils url;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private INotificationService notificationService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("Invite page requested");
        AbstractGame game;

        String inviteId = req.getParameter("inviteId");
        if (inviteId == null) {
            game = gameProducer.getGame();
        } else {
            try {
                game = gameRepository.getGameForInviteId(Integer.parseInt(inviteId));
            } catch (NumberFormatException e) {
                logger.warn("Invalid invite ID: {}", inviteId);
                messages.add("Your link is malformed, you could not join the game.").fadeOut(false);
                return;
            }
        }

        String roleParameter = req.getParameter("role");
        if (roleParameter != null && !roleParameter.equals("attacker") && !roleParameter.equals("defender")) {
            logger.warn("Invalid role parameter in invite link: {}", roleParameter);
            messages.add("Your invite link was malformed: Your role may not be " + roleParameter)
                    .fadeOut(false);
            resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            return;
        }

        if (game == null) {
            logger.warn("User {} tried to join game {}, but the game does not exist.", login.getUserId(),
                    req.getParameter("gameId"));
            messages.add("The game you were invited to does no longer exist.").fadeOut(false);
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

            Role role;
            if (game instanceof MultiplayerGame multiplayerGame) {
                role = multiplayerGame.joinWithInvite(userId, roleParameter);
            } else if (game instanceof MeleeGame) {
                role = Role.PLAYER;
                game.addPlayer(userId, role);
            } else {
                logger.warn("User {} tried to join puzzle game {}.", userId, gameId);
                messages.add("You cannot join puzzle games with an invite link.").fadeOut(false);
                resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return;
            }
            notificationService.post(event);
            logger.info("User {} joined game {} as {}", userId, gameId, role);
            messages.add("You successfully joined the game as " + (role == Role.ATTACKER ? "an " : "a ") + role + ".");
        } else {
            logger.warn("User {} tried to join game {}, but is already in the game.", userId, gameId);
            messages.add("You had already joined this game.");
        }
        String redirectPath;
        if (game instanceof MultiplayerGame) {
            redirectPath = Paths.BATTLEGROUND_GAME;
        } else if (game instanceof MeleeGame) {
            redirectPath = Paths.MELEE_GAME;
        } else {
            throw new IllegalStateException("Game type not supported: " + game.getClass().getName());
        }
        resp.sendRedirect(url.forPath(redirectPath) + "?gameId=" + gameId);
    }


}

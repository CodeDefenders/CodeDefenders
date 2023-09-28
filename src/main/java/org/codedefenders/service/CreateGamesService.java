package org.codedefenders.service;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.creategames.AdminCreateGamesBean;
import org.codedefenders.beans.creategames.ClassroomCreateGamesBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MeleeGameRepository;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.creategames.GameSettings;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.model.creategames.StagedGameList.StagedGame;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;

import static java.text.MessageFormat.format;
import static org.codedefenders.game.GameType.MELEE;
import static org.codedefenders.game.GameType.MULTIPLAYER;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

@ApplicationScoped
public class CreateGamesService {
    @Inject
    private CodeDefendersAuth login;

    @Inject
    private MessagesBean messages;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private UserRepository userRepo;

    @Inject
    private ClassroomService classroomService;

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private GameService gameService;

    @Inject
    private MeleeGameRepository meleeGameRepo;

    /**
     * Maps user ID to their admin staged games list.
     */
    private final Map<Integer, StagedGameList> adminStagedGames;

    /**
     * Maps user ID and classroom ID to its staged games list.
     */
    private final Map<UserIdClassroomId, StagedGameList> classroomStagedGames;
    private record UserIdClassroomId(int userId, int classroomId){}

    public CreateGamesService() {
        adminStagedGames = new HashMap<>();
        classroomStagedGames = new HashMap<>();
    }

    public StagedGameList getStagedGamesForAdmin(int userId) {
        return adminStagedGames.computeIfAbsent(userId, id -> new StagedGameList());
    }

    public StagedGameList getStagedGamesForClassroom(int userId, int classroomId) {
        var key = new UserIdClassroomId(userId, classroomId);
        return classroomStagedGames.computeIfAbsent(key, id -> new StagedGameList());
    }

    public AdminCreateGamesBean getContextForAdmin(int userId) {
        StagedGameList stagedGames = getStagedGamesForAdmin(userId);
        return new AdminCreateGamesBean(stagedGames, messages, eventDAO, userRepo, meleeGameRepo, this);
    }

    public ClassroomCreateGamesBean getContextForClassroom(int userId, int classroomId) {
        StagedGameList stagedGames = getStagedGamesForClassroom(userId, classroomId);
        return new ClassroomCreateGamesBean(classroomId, stagedGames, messages, eventDAO, userRepo, this,
                classroomService, meleeGameRepo);
    }

    /**
     * Creates a stages game as a real game and adds its assigned users to it.
     * @param stagedGame The staged game to create.
     * @return {@code true} if the game was successfully created, {@code false} if not.
     */
    public boolean createGame(StagedGame stagedGame) {
        GameSettings gameSettings = stagedGame.getGameSettings();
        GameClass cut = GameClassDAO.getClassForId(gameSettings.getClassId());

        /* Create the game. */
        AbstractGame game;
        if (gameSettings.getGameType() == MULTIPLAYER) {
            game = new MultiplayerGame.Builder(
                    gameSettings.getClassId(),
                    login.getUserId(),
                    gameSettings.getMaxAssertionsPerTest()
            )
                    .cut(cut)
                    .mutantValidatorLevel(gameSettings.getMutantValidatorLevel())
                    .chatEnabled(gameSettings.isChatEnabled())
                    .capturePlayersIntention(gameSettings.isCaptureIntentions())
                    .automaticMutantEquivalenceThreshold(gameSettings.getEquivalenceThreshold())
                    .level(gameSettings.getLevel())
                    .gameDurationMinutes(gameSettings.getGameDurationMinutes())
                    .classroomId(gameSettings.getClassroomId().orElse(null))
                    .build();
        } else if (gameSettings.getGameType() == MELEE) {
            game = new MeleeGame.Builder(
                    gameSettings.getClassId(),
                    login.getUserId(),
                    gameSettings.getMaxAssertionsPerTest()
            )
                    .cut(cut)
                    .mutantValidatorLevel(gameSettings.getMutantValidatorLevel())
                    .chatEnabled(gameSettings.isChatEnabled())
                    .capturePlayersIntention(gameSettings.isCaptureIntentions())
                    .automaticMutantEquivalenceThreshold(gameSettings.getEquivalenceThreshold())
                    .level(gameSettings.getLevel())
                    .gameDurationMinutes(gameSettings.getGameDurationMinutes())
                    .classroomId(gameSettings.getClassroomId().orElse(null))
                    .build();
        } else {
            messages.add(format("ERROR: Cannot create staged game {0}. Invalid game type: {1}.",
                    stagedGame.getFormattedId(), gameSettings.getGameType().getName()));
            return false;
        }

        /* Insert the game. */
        game.setEventDAO(eventDAO);
        game.setUserRepository(userRepo);
        if (!game.insert()) {
            messages.add(format("ERROR: Could not create game for staged game {0}.",
                    stagedGame.getFormattedId()));
            return false;
        }

        /* Add system users and predefined mutants/tests. */
        if (gameSettings.getGameType() != MELEE) {
            if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER)
                    || !game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER)) {
                messages.add(format("ERROR: Could not add system players to game {0}.",
                        stagedGame.getFormattedId()));
                return false;
            }
        } else {
            if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.PLAYER)
                    || !game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.PLAYER)) {
                messages.add(format("ERROR: Could not add system players to game {0}.",
                        stagedGame.getFormattedId()));
                return false;
            }
        }

        if (gameSettings.isWithMutants() || gameSettings.isWithTests()) {
            gameManagingUtils.addPredefinedMutantsAndTests(game,
                    gameSettings.isWithMutants(), gameSettings.isWithTests());
        }

        /* Add users to the game. */
        if (gameSettings.getGameType() == MULTIPLAYER) {
            if (gameSettings.getCreatorRole() == Role.ATTACKER || gameSettings.getCreatorRole() == Role.DEFENDER) {
                game.addPlayer(login.getUserId(), gameSettings.getCreatorRole());
            }
            for (int userId : stagedGame.getAttackers()) {
                game.addPlayer(userId, Role.ATTACKER);
            }
            for (int userId : stagedGame.getDefenders()) {
                game.addPlayer(userId, Role.DEFENDER);
            }
        } else if (gameSettings.getGameType() == MELEE) {
            if (gameSettings.getCreatorRole() == Role.PLAYER) {
                game.addPlayer(login.getUserId(), gameSettings.getCreatorRole());
            }
            for (int userId : stagedGame.getPlayers()) {
                game.addPlayer(userId, Role.PLAYER);
            }
        }

        /* Start game if configured to. */
        if (gameSettings.isStartGame()) {
            gameService.startGame(game);
        }

        return true;
    }
}

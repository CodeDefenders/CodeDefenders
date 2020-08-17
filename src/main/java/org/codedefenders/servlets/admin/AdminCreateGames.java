/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.servlets.admin;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.User;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

@WebServlet(urlPatterns = {Paths.ADMIN_PAGE, Paths.ADMIN_GAMES})
public class AdminCreateGames extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminCreateGames.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private LoginBean login;
    
    @Inject
    private EventDAO eventDAO;

    public enum RoleAssignmentMethod {
        RANDOM,
        OPPOSITE
    }

    public enum TeamAssignmentMethod {
        RANDOM,
        SCORE_DESCENDING,
        SCORE_SHUFFLED
    }

    public static final String DEFENDER_LISTS_SESSION_ATTRIBUTE = "defenderLists";
    public static final String ATTACKER_LISTS_SESSION_ATTRIBUTE = "attackerLists";
    public static final String CREATED_GAMES_LISTS_SESSION_ATTRIBUTE = "createdGames";
    private static final int NB_CATEGORIES_FOR_SHUFFLING = 3;
    static final String USER_NAME_LIST_DELIMITER = "[\\r\\n]+";

    private List<Integer> selectedUserIds;
    private int cutId;
    private RoleAssignmentMethod roleAssignmentMethod;
    private TeamAssignmentMethod teamAssignmentMethod;
    private int attackersPerGame;
    private int defendersPerGame;
    private GameLevel gamesLevel;
    private GameState gamesState;
    private List<MultiplayerGame> createdGames;
    private List<List<Integer>> attackerIdsList;
    private List<List<Integer>> defenderIdsList;
    private MultiplayerGame mg;
    private boolean chatEnabled;
    private int maxAssertionsPerTest;
    private boolean forceHamcrest;
    private CodeValidatorLevel mutantValidatorLevel;

    private boolean withTests;
    private boolean withMutants;

    private boolean capturePlayersIntention;

    private int automaticEquivalenceTrigger;


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher(Constants.ADMIN_GAMES_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        // Get their user id from the session.

        final String action = request.getParameter("formType");
        switch (action) {
            case "createGame":
                createGame(response, request, session);
                break;
            case "insertGames":
                insertGame(response, request, session);
                break;
            default:
                logger.error("Action not recognised:{}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }

    private void insertGame(HttpServletResponse response, HttpServletRequest request, HttpSession session)
            throws IOException {
        attackerIdsList = (List<List<Integer>>) session.getAttribute(ATTACKER_LISTS_SESSION_ATTRIBUTE);
        defenderIdsList = (List<List<Integer>>) session.getAttribute(DEFENDER_LISTS_SESSION_ATTRIBUTE);
        String gameAndUserRemoveId = request.getParameter("tempGameUserRemoveButton");
        String gameAndUserSwitchId = request.getParameter("tempGameUserSwitchButton");
        String gameAndUserMoveToId = request.getParameter("tempGameUserMoveToButton");

        if (gameAndUserMoveToId != null) {
            int userId = Integer.parseInt(request.getParameter("tempGameUserMoveToButton").split("_")[2]);
            String targetGameIdString = request.getParameter("game_" + userId);
            String currentGameIdString = request.getParameter("tempGameUserMoveToButton").split("_")[5];
            Role role = Role.valueOf(request.getParameter("role_" + userId));
            // If any of this fail state of staged games will be inconsistent
            removePlayerFromGame(session, userId, currentGameIdString);
            associatePlayerToGameWithRole(session, userId, targetGameIdString, role);
        } else if (gameAndUserRemoveId != null || gameAndUserSwitchId != null) {
            // admin is removing user  from temp game or switching their role or
            boolean switchUser = gameAndUserSwitchId != null;
            String gameAndUserId = switchUser ? gameAndUserSwitchId : gameAndUserRemoveId;
            int gameToRemoveFromId = Integer.parseInt(gameAndUserId.split("-")[0]);
            Integer userToRemoveId = Integer.parseInt(gameAndUserId.split("-")[1]);
            List<Integer> attackerIds = attackerIdsList.get(gameToRemoveFromId);
            List<Integer> defenderIds = defenderIdsList.get(gameToRemoveFromId);
            if (attackerIds.contains(userToRemoveId)) {
                attackerIds.remove(userToRemoveId);
                if (switchUser) {
                    defenderIds.add(userToRemoveId);
                }
            } else {
                defenderIds.remove(userToRemoveId);
                if (switchUser) {
                    attackerIds.add(userToRemoveId);
                }
            }
        } else { // admin is inserting or deleting selected temp games
            String[] selectedTempGames;
            selectedTempGames = request.getParameterValues("selectedTempGames");
            createdGames = (List<MultiplayerGame>) session.getAttribute(CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);

            if (selectedTempGames == null) {
                messages.add("Please select at least one game to insert.");
                response.sendRedirect(request.getContextPath() + "/admin");
                return;
            }

            List<Integer> selectedGameIndices = new ArrayList<>();
            for (String u : selectedTempGames) {
                selectedGameIndices.add(Integer.parseInt(u));
            }

            if (request.getParameter("games_btn").equals("insert Games")) {
                for (int i : selectedGameIndices) {
                    insertFilledGame(createdGames.get(i), attackerIdsList.get(i), defenderIdsList.get(i));
                }
            }

            // remove starting from end so indices don't get messed up
            Collections.sort(selectedGameIndices);
            Collections.reverse(selectedGameIndices);

            for (int i : selectedGameIndices) {
                createdGames.remove(i);
                attackerIdsList.remove(i);
                defenderIdsList.remove(i);
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin");
    }

    private void removePlayerFromGame(HttpSession session, int removedUserId, String gidString) {
        int gid;
        List<Integer> userList = new ArrayList<>();
        boolean isTempGame = gidString.startsWith("T");

        // Selecting the target game
        if (isTempGame) {
            createdGames = (List<MultiplayerGame>) session.getAttribute(CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
            gid = Integer.parseInt(gidString.substring(1));
            mg = createdGames.get(gid);
            // My current role matters not the one I will play in the next game !
            // So return the list that contains me...
            userList = (
                    ((List<List<Integer>>) session.getAttribute(ATTACKER_LISTS_SESSION_ATTRIBUTE)).get(gid).contains(removedUserId)
                            ? (List<List<Integer>>) session.getAttribute(ATTACKER_LISTS_SESSION_ATTRIBUTE)
                            : (List<List<Integer>>) session.getAttribute(DEFENDER_LISTS_SESSION_ATTRIBUTE))
                    .get(gid);
        } else {
            gid = Integer.parseInt(gidString);
            mg = MultiplayerGameDAO.getMultiplayerGame(gid);
        }

        // Remove the user. No need to check for creator or wrong user here
        if (isTempGame) {
            if (userList.remove(new Integer(removedUserId))) {
                messages.add("Removed user " + removedUserId + " from game " + gidString);
            } else {
                messages.add("ERROR trying to remove user " + removedUserId + " from game " + gidString);
            }
        } else {
            if (mg.removePlayer(removedUserId)) {
                messages.add("Removed user " + removedUserId + " from game " + gidString);
            } else {
                messages.add("ERROR trying to remove user " + removedUserId + " from game " + gidString);
            }
        }
    }

    private void associatePlayerToGameWithRole(HttpSession session, int addedUserId, String gidString, Role role) {
        int gid;
        List<Integer> userList = new ArrayList<>();
        boolean isTempGame = gidString.startsWith("T");
        // Selecting the target game
        if (isTempGame) {
            createdGames = (List<MultiplayerGame>) session.getAttribute(CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
            gid = Integer.parseInt(gidString.substring(1));
            mg = createdGames.get(gid);
            if (role.equals(Role.ATTACKER)) {
                userList = ((List<List<Integer>>) session.getAttribute(ATTACKER_LISTS_SESSION_ATTRIBUTE)).get(gid);
            } else {
                userList = ((List<List<Integer>>) session.getAttribute(DEFENDER_LISTS_SESSION_ATTRIBUTE)).get(gid);
            }
        } else {
            gid = Integer.parseInt(gidString);
            mg = MultiplayerGameDAO.getMultiplayerGame(gid);
        }

        if (!mg.hasEventDAO()) {
            // Forcefully inject the DAO
            mg.setEventDAO(eventDAO);
        }

        if (mg.getCreatorId() == addedUserId) {
            messages.add("Cannot add user " + addedUserId + " to game " + gid + " because they are it's creator.");
        } else {
            if (isTempGame) {
                userList.add(addedUserId);
                messages.add("Added user " + addedUserId + " to game " + gidString + " as " + role);
            } else {
                if (mg.addPlayer(addedUserId, role)) {
                    messages.add("Added user " + addedUserId + " to game " + gidString + " as " + role);
                } else {
                    messages.add("ERROR trying to add user " + addedUserId + " to game " + gidString + " as " + role);
                }
            }
        }
    }

    // TODO The name of this method might be misleading if we associate a user to an
    // existing game, instead of creating the game...
    private void createGame(HttpServletResponse response, HttpServletRequest request, HttpSession session)
            throws IOException {
        String rowUserId = request.getParameter("userListButton");
        if (rowUserId != null) { // if admin is trying to add a single user to a game
            int addedUserId = Integer.parseInt(rowUserId);
            // Get the identifying information required to create a game from the submitted form.
            String gidString = request.getParameter("game_" + addedUserId);
            Role role = Role.valueOf(request.getParameter("role_" + addedUserId));
            //
            associatePlayerToGameWithRole(session, addedUserId, gidString, role);
        } else { // if admin is batch creating games
            batchCreateGames(request, response, session);
        }
        response.sendRedirect(request.getContextPath() + "/admin");
    }

    private void batchCreateGames(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        attackerIdsList = (List<List<Integer>>) session.getAttribute(ATTACKER_LISTS_SESSION_ATTRIBUTE);
        defenderIdsList = (List<List<Integer>>) session.getAttribute(DEFENDER_LISTS_SESSION_ATTRIBUTE);
        createdGames = (List<MultiplayerGame>) session.getAttribute(CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
        String userIdListString;
        String userNameListString;
        try {
            userIdListString = request.getParameter("hidden_user_id_list");
            userNameListString = request.getParameter("user_name_list");
            cutId = Integer.parseInt(request.getParameter("class"));
            roleAssignmentMethod = request.getParameter("roles").equals(RoleAssignmentMethod.OPPOSITE.name())
                    ? RoleAssignmentMethod.OPPOSITE : RoleAssignmentMethod.RANDOM;
            teamAssignmentMethod = TeamAssignmentMethod.valueOf(request.getParameter("teams"));
            attackersPerGame = Integer.parseInt(request.getParameter("attackers"));
            defendersPerGame = Integer.parseInt(request.getParameter("defenders"));
            gamesLevel = GameLevel.valueOf(request.getParameter("gamesLevel"));
            gamesState = request.getParameter("gamesState")
                    .equals(GameState.ACTIVE.name()) ? GameState.ACTIVE : GameState.CREATED;
            maxAssertionsPerTest = Integer.parseInt(request.getParameter("maxAssertionsPerTest"));
            forceHamcrest = request.getParameter("forceHamcrest") != null;
            mutantValidatorLevel = CodeValidatorLevel.valueOf(request.getParameter("mutantValidatorLevel"));
            chatEnabled = request.getParameter("chatEnabled") != null;

            withTests = request.getParameter("withTests") != null;
            withMutants = request.getParameter("withMutants") != null;

            capturePlayersIntention = request.getParameter("capturePlayersIntention") != null;

            automaticEquivalenceTrigger = Integer.parseInt(request.getParameter("automaticEquivalenceTrigger"));
        } catch (Exception e) {
            messages.add("There was a problem with the form.");
            response.sendRedirect(request.getContextPath() + "/admin");
            return;
        }

        selectedUserIds = new ArrayList<>();
        if (userIdListString != null && !userIdListString.equals("")) {
            String[] userIdList = userIdListString.trim().split(",");
            for (String u : userIdList) {
                String idString = u.replace("<", "").replace(">", "");
                selectedUserIds.add(Integer.parseInt(idString));
            }
        }
        if (userNameListString != null) {
            for (String userName : userNameListString.split(USER_NAME_LIST_DELIMITER)) {
                if (userName.length() > 0) {
                    User u = UserDAO.getUserByName(userName);
                    if (u == null) {
                        messages.add("No user with name or email \'" + userName + "\'!");
                    } else if (!selectedUserIds.contains(u.getId())) {
                        selectedUserIds.add(u.getId());
                    }
                }
            }
        }

        List<Integer> unassignedUserIds = getUnassignedUserIds(attackerIdsList, defenderIdsList);
        for (Integer uid : new ArrayList<>(selectedUserIds)) {
            if (!unassignedUserIds.contains(uid)) {
                messages.add("user " + uid + " is already playing another game!");
                selectedUserIds.remove(uid);
            }
        }


        if (selectedUserIds.size() == 0) {
            messages.add("Please select at least one User.");
        } else {
            messages.add("Creating " + gamesLevel + " games for users " + selectedUserIds + " with CUT " + cutId
                    + ", assigning roles " + roleAssignmentMethod + ", assigning teams " + teamAssignmentMethod
                    + " with " + attackersPerGame + " Attackers and " + defendersPerGame + " Defenders each.");
            createAndFillGames(session, createdGames, attackerIdsList, defenderIdsList);
        }
    }


    private void insertFilledGame(MultiplayerGame multiplayerGame,
                                  List<Integer> attackerIds, List<Integer> defenderIds) {
        // We need to take care of loading and setting system tests and mutants as well for this game
        // multiplayerGame.insert();
        // XXX Code duplication: This is take from {@link
        // MultiplayerGameSelectionManager}

        if (!multiplayerGame.hasEventDAO()) {
            multiplayerGame.setEventDAO(eventDAO);
        }

        final int gameId = multiplayerGame.getId();
        if (multiplayerGame.insert()) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Event event = new Event(-1, gameId, multiplayerGame.getCreatorId(), "Game Created",
                    EventType.GAME_CREATED, EventStatus.GAME, timestamp);
            eventDAO.insert(event);
        } else {
            // TODO What to do if the insert did not work !?
            logger.warn("Cannot create game !");
        }

        // Handle the system tests and mutants
        // Mutants and tests uploaded with the class are already stored in the DB
        int classId = multiplayerGame.getClassId();
        List<Mutant> uploadedMutants = GameClassDAO.getMappedMutantsForClassId(classId);
        List<Test> uploadedTests = GameClassDAO.getMappedTestsForClassId(classId);

        // Always add system player to send mutants and tests at runtime!
        multiplayerGame.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER);
        multiplayerGame.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER);

        // Retrieve the playerId for system users
        int dummyAttackerPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_ATTACKER_USER_ID, gameId);
        int dummyDefenderPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_DEFENDER_USER_ID, gameId);

        // this mutant map links the uploaded mutants and the once generated from them here
        // This implements bookkeeping for killmap
        Map<Mutant, Mutant> mutantMap = new HashMap<>();
        Map<Test, Test> testMap = new HashMap<>();

        boolean withTests = multiplayerGame.hasSystemTests();
        boolean withMutants = multiplayerGame.hasSystemMutants();

        // Register Valid Mutants.
        if (withMutants) {
            // Validate uploaded mutants from the list
            // Link the mutants to the game
            for (Mutant mutant : uploadedMutants) {
                Mutant newMutant = new Mutant(gameId, classId,
                        mutant.getJavaFile(),
                        mutant.getClassFile(),
                        // Alive be default
                        true,
                        //
                        dummyAttackerPlayerId,
                        GameDAO.getCurrentRound(gameId)
                        );
                // insert this into the DB and link the mutant to the game
                newMutant.insert();
                // BookKeeping
                mutantMap.put(mutant, newMutant);
            }
        }
        // Register Valid Tests
        if (withTests) {
            // Validate the tests from the list
            for (Test test : uploadedTests) {
                // At this point we need to fill in all the details
                Test newTest = new Test(-1, classId, gameId, test.getJavaFile(),
                        test.getClassFile(), 0, 0, dummyDefenderPlayerId, test.getLineCoverage().getLinesCovered(),
                        test.getLineCoverage().getLinesUncovered(), 0);
                newTest.insert();
                testMap.put(test, newTest);
            }
        }

        if (withMutants && withTests) {
            List<KillMapEntry> killmap = KillmapDAO.getKillMapEntriesForClass(classId);
            // Filter the killmap and keep only the one created during the upload ...

            for (Mutant uploadedMutant : uploadedMutants) {
                boolean alive = true;
                for (Test uploadedTest : uploadedTests) {
                    // Does the test kill the mutant?
                    for (KillMapEntry entry : killmap) {
                        if (entry.mutant.getId() == uploadedMutant.getId()
                                && entry.test.getId() == uploadedTest.getId()
                                && entry.status.equals(KillMapEntry.Status.KILL)) {
                            // This also update the DB
                            if (mutantMap.get(uploadedMutant).isAlive()) {
                                testMap.get(uploadedTest).killMutant();
                                mutantMap.get(uploadedMutant).kill();
                            }
                            alive = false;
                            break;
                        }
                    }
                    if (!alive) {
                        break;
                    }
                }
            }
        }

        // Finally add the regular users
        for (int aid : attackerIds) {
            multiplayerGame.addPlayerForce(aid, Role.ATTACKER);
        }
        for (int did : defenderIds) {
            multiplayerGame.addPlayerForce(did, Role.DEFENDER);
        }
    }

    private void createAndFillGames(HttpSession session, List<MultiplayerGame> createdGames,
                                    List<List<Integer>> attackerIdsList, List<List<Integer>> defenderIdsList) {
        int nbGames;
        List<Integer> attackerIds;
        List<Integer> defenderIds;
        if (roleAssignmentMethod.equals(RoleAssignmentMethod.OPPOSITE)) {
            attackerIds = getUsersByLastRole(selectedUserIds, Role.DEFENDER);
            defenderIds = getUsersByLastRole(selectedUserIds, Role.ATTACKER);
            distributeRemainingUsers(selectedUserIds, attackerIds, defenderIds);
            nbGames = getNumberOfGames(attackersPerGame, defendersPerGame, attackerIds.size(), defenderIds.size());
        } else {
            nbGames = getNumberOfGames(attackersPerGame, defendersPerGame, selectedUserIds.size());
            int nbAttackers = (int) Math.round((double) attackersPerGame
                    / (attackersPerGame + defendersPerGame) * selectedUserIds.size());
            attackerIds = getRandomUserList(selectedUserIds, nbAttackers);
            defenderIds = getRandomUserList(selectedUserIds, selectedUserIds.size());
        }

        // TODO Why static ?
        List<MultiplayerGame> newlyCreatedGames = createGames(nbGames, attackersPerGame, defendersPerGame, cutId,
                login.getUserId(), gamesLevel, gamesState, maxAssertionsPerTest, forceHamcrest, //
                chatEnabled, mutantValidatorLevel, withTests, withMutants, //
                capturePlayersIntention, automaticEquivalenceTrigger);

        // Forcefully inject the DAO
        for (MultiplayerGame newlyCreatedGame : newlyCreatedGames) {
            if (!newlyCreatedGame.hasEventDAO()) {
                newlyCreatedGame.setEventDAO(eventDAO);
            }
        }

        if (teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_DESCENDING)
                || teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_SHUFFLED)) {
            attackerIds.sort(new ReverseDefenderScoreComparator());
            defenderIds.sort(new ReverseDefenderScoreComparator());
            if (teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_SHUFFLED)) {
                attackerIds = getBlockShuffledList(attackerIds, NB_CATEGORIES_FOR_SHUFFLING);
                defenderIds = getBlockShuffledList(defenderIds, NB_CATEGORIES_FOR_SHUFFLING);
            }
        } else {
            Collections.shuffle(attackerIds);
            Collections.shuffle(defenderIds);
        }
        List<List<Integer>> newAttackerIdsList = getUserLists(newlyCreatedGames, attackerIds, attackersPerGame);
        List<List<Integer>> newDefenderIdsList = getUserLists(newlyCreatedGames, defenderIds, defendersPerGame);

        if (createdGames != null && attackerIdsList != null && defenderIdsList != null) {
            createdGames.addAll(newlyCreatedGames);
            attackerIdsList.addAll(newAttackerIdsList);
            defenderIdsList.addAll(newDefenderIdsList);
        } else {
            createdGames = newlyCreatedGames;
            attackerIdsList = newAttackerIdsList;
            defenderIdsList = newDefenderIdsList;
        }
        session.setAttribute(CREATED_GAMES_LISTS_SESSION_ATTRIBUTE, createdGames);
        session.setAttribute(ATTACKER_LISTS_SESSION_ATTRIBUTE, attackerIdsList);
        session.setAttribute(DEFENDER_LISTS_SESSION_ATTRIBUTE, defenderIdsList);
    }

    static class ReverseDefenderScoreComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            int score1 = AdminDAO.getScore(o1).getDefenderScore();
            int score2 = AdminDAO.getScore(o2).getDefenderScore();
            return (-1) * Integer.compare(score1, score2);
        }
    }

    private static List<Integer> getUsersByLastRole(List<Integer> userIds, Role role) {
        List<Integer> userList = new ArrayList<>();
        for (int uid : userIds) {
            Role lastRole = UserDAO.getLastRoleOfUser(uid);
            if (lastRole != null && lastRole.equals(role)) {
                userList.add(uid);
            }
        }
        return userList;
    }

    private static int getNumberOfGames(int attackersPerGame, int defendersPerGame, int nbAttackers, int nbDefenders) {
        return (int) Math.ceil(Math.max(
                (float) nbAttackers / attackersPerGame,
                (float) nbDefenders / defendersPerGame)
        );
    }

    private static int getNumberOfGames(int attackersPerGame, int defendersPerGame, int nbPlayers) {
        return (int) Math.ceil((float) nbPlayers / (attackersPerGame + defendersPerGame));
    }

    // TODO Why static ?!
    private static List<MultiplayerGame> createGames(int nbGames, int attackersPerGame, int defendersPerGame, int cutId,
            int creatorId, GameLevel level, GameState state, int maxAssertionsPerTest, boolean forceHamcrest,
            boolean chatEnabled, CodeValidatorLevel mutantValidatorLevel, boolean withTests, boolean withMutants, //
            boolean capturePlayersIntention, //
            int automaticEquivalenceTrigger) {
        List<MultiplayerGame> gameList = new ArrayList<>();
        for (int i = 0; i < nbGames; ++i) {
            MultiplayerGame game = new MultiplayerGame.Builder(cutId, creatorId, maxAssertionsPerTest, forceHamcrest)
                    .level(level)
                    .state(state)
                    .chatEnabled(chatEnabled)
                    .mutantValidatorLevel(mutantValidatorLevel)
                    .withTests(withTests)
                    .withMutants(withMutants)
                    .capturePlayersIntention(capturePlayersIntention)
                    .automaticMutantEquivalenceThreshold(automaticEquivalenceTrigger)
                    .build();
            gameList.add(game);
        }
        return gameList;
    }

    private static List<Integer> getRandomUserList(List<Integer> userIds, int nbUsers) {
        List<Integer> randomUserIds = new ArrayList<>();
        for (int i = 0; i < nbUsers; ++i) {
            int randomIndex = new Random().nextInt(userIds.size());
            randomUserIds.add(userIds.get(randomIndex));
            userIds.remove(randomIndex);
        }
        return randomUserIds;
    }

    private static List<Integer> fillGame(List<Integer> userIds, int nbUsersPerGame) {
        int runs = Math.min(userIds.size(), nbUsersPerGame);
        List<Integer> playerList = new ArrayList<>();
        for (int i = 0; i < runs; ++i) {
            playerList.add(userIds.get(0));
            userIds.remove(0);
        }
        return playerList;
    }

    private static List<List<Integer>> getUserLists(List<MultiplayerGame> createdGames, List<Integer> userIds,
                                                    int nbUsersPerGame) {
        List<List<Integer>> userLists = new ArrayList<>();
        for (MultiplayerGame mg : createdGames) {
            userLists.add(fillGame(userIds, nbUsersPerGame));
        }
        return userLists;
    }

    private static List<Integer> getBlockShuffledList(List<Integer> originalList, int numBlocks) {
        if (originalList.size() < numBlocks) {
            return originalList;
        }
        int sublistSize = (int) Math.ceil(originalList.size() / (double) numBlocks);
        List<List<Integer>> blocks = new ArrayList<>();
        for (int i = 0; i < numBlocks; ++i) {
            int startIndex = Math.min(i * sublistSize, originalList.size());
            int endIndex = Math.min((i + 1) * sublistSize, originalList.size());
            blocks.add(originalList.subList(startIndex, endIndex));
        }
        List<Integer> blockShuffledList = new ArrayList<>();
        for (int i = 0; i < numBlocks; ++i) {
            int randomIndex = new Random().nextInt(blocks.size());
            blockShuffledList.addAll(blocks.get(randomIndex));
            blocks.remove(randomIndex);
        }
        return blockShuffledList;
    }

    public static List<List<String>> getUnassignedUsers(List<List<Integer>> attackerIdsLists,
                                                        List<List<Integer>> defenderIdsLists) {
        List<List<String>> unassignedUsersFromDB = AdminDAO.getUnassignedUsersInfo();
        List<Integer> defenderIds = new ArrayList<>();
        List<Integer> attackerIds = new ArrayList<>();
        List<List<String>> unassignedUserIds = new ArrayList<>();
        if (attackerIdsLists != null && defenderIdsLists != null) {
            defenderIds = flattenListOfLists(defenderIdsLists);
            attackerIds = flattenListOfLists(attackerIdsLists);
        }
        for (List<String> userInfo : unassignedUsersFromDB) {
            int uid = Integer.parseInt(userInfo.get(0));
            if (!(attackerIds.contains(uid) || defenderIds.contains(uid))) {
                unassignedUserIds.add(userInfo);
            }
        }
        return unassignedUserIds;
    }

    private static List<Integer> getUnassignedUserIds(List<List<Integer>> attackerIdsLists,
                                                      List<List<Integer>> defenderIdsLists) {
        List<User> unassignedUsersFromDB = UserDAO.getUnassignedUsers();
        List<Integer> defenderIds = new ArrayList<>();
        List<Integer> attackerIds = new ArrayList<>();
        List<Integer> unassignedUserIds = new ArrayList<>();
        if (attackerIdsLists != null && defenderIdsLists != null) {
            defenderIds = flattenListOfLists(defenderIdsLists);
            attackerIds = flattenListOfLists(attackerIdsLists);
        }
        for (User u : unassignedUsersFromDB) {
            int uid = u.getId();
            if (!(attackerIds.contains(uid) || defenderIds.contains(uid))) {
                unassignedUserIds.add(uid);
            }
        }
        return unassignedUserIds;
    }

    private static <T> List<T> flattenListOfLists(List<List<T>> listOfLists) {
        List<T> flatList = new ArrayList<>();
        for (List<T> list : listOfLists) {
            flatList.addAll(list);
        }
        return flatList;
    }

    public static int getPlayerScore(MultiplayerGame mg, int pid) {
        HashMap mutantScores = mg.getMutantScores();
        HashMap testScores = mg.getTestScores();
        if (mutantScores.containsKey(pid) && mutantScores.get(pid) != null) {
            return ((PlayerScore) mutantScores.get(pid)).getTotalScore();
        } else if (testScores.containsKey(pid) && testScores.get(pid) != null) {
            return ((PlayerScore) testScores.get(pid)).getTotalScore();
        }
        return 0;
    }


    private static String zeroPad(long toPad) {
        String s = String.valueOf(toPad);
        return s.length() > 1 ? s : "0" + s;
    }

    public static String formatTimestamp(String lastSubmissionTime) {
        Timestamp currentTS = new Timestamp(System.currentTimeMillis());
        long diff = currentTS.getTime() - Long.parseLong(lastSubmissionTime);
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000);
        long diffDays = (int) (diff / (1000 * 60 * 60 * 24));
        String diffString = "";
        if (diffDays >= 1) {
            return "more than 1 day";
        }
        //if (diffHours >= 1)
        diffString += zeroPad(diffHours) + "h ";
        //if (diffMinutes >= 1)
        diffString += zeroPad(diffMinutes) + "m ";
        //if (diffSeconds >= 1)
        diffString += zeroPad(diffSeconds) + "s ";
        return diffString;
    }

    private static void distributeRemainingUsers(List<Integer> selectedUserIds, List<Integer> attackerIds,
                                                 List<Integer> defenderIds) {
        List<Integer> remainingUsers = new ArrayList<>();
        for (int uid : selectedUserIds) {
            if (!(attackerIds.contains(uid) || defenderIds.contains(uid))) {
                remainingUsers.add(uid);
            }
        }
        Collections.shuffle(remainingUsers);

        int nbDefenders = remainingUsers.size() / 2;
        for (int i = 0; i < remainingUsers.size(); ++i) {
            (i < nbDefenders ? defenderIds : attackerIds).add(remainingUsers.get(i));
        }
    }

}

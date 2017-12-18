package org.codedefenders;

import edu.emory.mathcs.backport.java.util.Collections;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.multiplayer.PlayerScore;
import org.codedefenders.util.AdminDAO;
import org.codedefenders.util.DatabaseAccess;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public class AdminInterface extends HttpServlet {

    public enum RoleAssignmentMethod {RANDOM, OPPOSITE}

    public enum TeamAssignmentMethod {RANDOM, SCORE_DESCENDING, SCORE_SHUFFLED}

    public static final String DEFENDER_LISTS_SESSION_ATTRIBUTE = "defenderLists";
    public static final String ATTACKER_LISTS_SESSION_ATTRIBUTE = "attackerLists";
    public static final String CREATED_GAMES_LISTS_SESSION_ATTRIBUTE = "createdGames";
    private static final int NB_CATEGORIES_FOR_SHUFFLING = 3;

    private int currentUserID;
    private List<Integer> selectedUserIDs;
    private int cutID;
    private RoleAssignmentMethod roleAssignmentMethod;
    private TeamAssignmentMethod teamAssignmentMethod;
    private int attackersPerGame, defendersPerGame, extraAttackersPerGame, extraDefendersPerGame;
    private GameLevel gamesLevel;
    private GameState gamesState;
    private long startTime;
    private long finishTime;
    private List<MultiplayerGame> createdGames;
    private List<List<Integer>> attackerIdsList;
    private List<List<Integer>> defenderIdsList;
    private List<Integer> selectedGameIndices;
    private String errorMessage;
    private MultiplayerGame mg;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.ADMIN_CREATE_JSP);
        dispatcher.forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        // Get their user id from the session.
        currentUserID = (Integer) session.getAttribute("uid");
        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {

            case "startStopGame":
                String playerToRemoveIdGameIdString = request.getParameter("activeGameUserRemoveButton");
                String playerToSwitchIdGameIdString = request.getParameter("activeGameUserSwitchButton");
                boolean switchUser = playerToSwitchIdGameIdString != null;
                if (playerToRemoveIdGameIdString != null || playerToSwitchIdGameIdString != null) { // admin is removing user from temp game
                    int playerToRemoveId = Integer.parseInt((switchUser ? playerToSwitchIdGameIdString : playerToRemoveIdGameIdString).split("-")[0]);
                    int gameToRemoveFromId = Integer.parseInt((switchUser ? playerToSwitchIdGameIdString : playerToRemoveIdGameIdString).split("-")[1]);
                    int userId = DatabaseAccess.getUserFromPlayer(playerToRemoveId).getId();
                    if (!deletePlayer(playerToRemoveId, gameToRemoveFromId))
                        messages.add("Deleting player " + playerToRemoveId + " failed! \n Please check the logs!");
                    else if (switchUser) {
                        Role newRole = Role.valueOf(playerToSwitchIdGameIdString.split("-")[2]).equals(Role.ATTACKER)
                                ? Role.DEFENDER : Role.ATTACKER;
                        mg = DatabaseAccess.getMultiplayerGame(gameToRemoveFromId);
                        if (!mg.addPlayerForce(userId, newRole))
                            messages.add("Inserting user " + userId + " failed! \n Please check the logs!");
                    }

                } else { // admin is inserting or deleting selected temp games
                    int gameId = -1;
                    // Get the identifying information required to create a game from the submitted form.

                    try {
                        gameId = Integer.parseInt(request.getParameter("start_stop_btn"));
                    } catch (Exception e) {
                        messages.add("There was a problem with the form.");
                        response.sendRedirect(request.getContextPath() + "/admin");
                        break;
                    }


                    errorMessage = "ERROR trying to start or stop game " + String.valueOf(gameId)
                            + ".\nIf this problem persists, contact your administrator.";

                    mg = DatabaseAccess.getMultiplayerGame(gameId);

                    if (mg == null) {
                        messages.add(errorMessage);
                    } else {
                        GameState newState = mg.getState() == GameState.ACTIVE ? GameState.FINISHED : GameState.ACTIVE;
                        mg.setState(newState);
                        if (!mg.update()) {
                            messages.add(errorMessage);
                        }
                    }
                }
                response.sendRedirect(request.getContextPath() + "/admin");
                break;
            case "createGame":
                String rowUserId = request.getParameter("userListButton");
                if (rowUserId != null) { // if admin is trying to add a single user to a game
                    int addedUserId = Integer.parseInt(rowUserId);
                    // Get the identifying information required to create a game from the submitted form.
                    String gidString = request.getParameter("game_" + addedUserId);
                    Role role = Role.valueOf(request.getParameter("role_" + addedUserId));
                    int gid, nbUsers;
                    List<Integer> userList = new ArrayList<>();
                    boolean isTempGame = gidString.startsWith("T");
                    if (isTempGame) {
                        createdGames = (List<MultiplayerGame>) session.getAttribute(AdminInterface.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
                        gid = Integer.parseInt(gidString.substring(1));
                        mg = createdGames.get(gid);
                        userList = (role.equals(Role.ATTACKER) ?
                                (List<List<Integer>>) session.getAttribute(AdminInterface.ATTACKER_LISTS_SESSION_ATTRIBUTE) :
                                (List<List<Integer>>) session.getAttribute(AdminInterface.DEFENDER_LISTS_SESSION_ATTRIBUTE))
                                .get(gid);
                    } else {
                        gid = Integer.parseInt(gidString);
                        mg = DatabaseAccess.getMultiplayerGame(gid);
                    }
                    if (mg.getCreatorId() == addedUserId) {
                        messages.add("Cannot add user " + addedUserId + " to game " + String.valueOf(gid) +
                                " because they are it's creator.");
                    } else {
                        if (isTempGame) {
                            userList.add(addedUserId);
                            messages.add("Added user " + addedUserId + " to game " + gidString + " as " + role);
                        } else {
                            if (mg.addPlayer(addedUserId, role)) {
                                messages.add("Added user " + addedUserId + " to game " + gidString + " as " + role);
                            } else {
                                messages.add("ERROR trying to add user " + addedUserId + " to game " +
                                        gidString + " as " + role);
                            }
                        }
                    }
                } else { // if admin is batch creating games
                    String[] selectedUsers;
                    try {
                        selectedUsers = request.getParameterValues("selectedUsers");
                        cutID = Integer.parseInt(request.getParameter("class"));
                        roleAssignmentMethod = request.getParameter("roles").equals(RoleAssignmentMethod.OPPOSITE.name())
                                ? RoleAssignmentMethod.OPPOSITE : RoleAssignmentMethod.RANDOM;
                        teamAssignmentMethod = TeamAssignmentMethod.valueOf(request.getParameter("teams"));
                        attackersPerGame = Integer.parseInt(request.getParameter("attackers"));
                        defendersPerGame = Integer.parseInt(request.getParameter("defenders"));
                        extraAttackersPerGame = Integer.parseInt(request.getParameter("attackers_extra"));
                        extraDefendersPerGame = Integer.parseInt(request.getParameter("defenders_extra"));
                        gamesLevel = request.getParameterValues("level") == null ? GameLevel.HARD : GameLevel.EASY;
                        gamesState = request.getParameter("gamesState").equals(GameState.ACTIVE.name()) ? GameState.ACTIVE : GameState.CREATED;
                        startTime = Long.parseLong(request.getParameter("startTime"));
                        finishTime = Long.parseLong(request.getParameter("finishTime"));
                    } catch (Exception e) {
                        messages.add("There was a problem with the form.");
                        response.sendRedirect(request.getContextPath() + "/admin");
                        break;
                    }

                    if (selectedUsers == null) {
                        messages.add("Please select at least one User.");
                        response.sendRedirect(request.getContextPath() + "/admin");
                        break;
                    }
                    selectedUserIDs = new ArrayList<>();
                    for (String u : selectedUsers) {
                        selectedUserIDs.add(Integer.parseInt(u));
                    }

                    messages.add("Creating " + gamesLevel + " games for users " + selectedUserIDs + " with CUT " + cutID + ", assigning roles " +
                            roleAssignmentMethod + ", assigning teams " + teamAssignmentMethod + " with " + attackersPerGame +
                            " Attackers and " + defendersPerGame + " Defenders each.");
                    createAndFillGames(session);
                }
                response.sendRedirect(request.getContextPath() + "/admin");
                break;
            case "insertGames":
                attackerIdsList = (List<List<Integer>>) session.getAttribute(AdminInterface.ATTACKER_LISTS_SESSION_ATTRIBUTE);
                defenderIdsList = (List<List<Integer>>) session.getAttribute(AdminInterface.DEFENDER_LISTS_SESSION_ATTRIBUTE);
                String gameAndUserRemoveId = request.getParameter("tempGameUserRemoveButton");
                String gameAndUserSwitchId = request.getParameter("tempGameUserSwitchButton");
                if (gameAndUserRemoveId != null || gameAndUserSwitchId != null) { // admin is removing user  from temp game or switching their role
                    switchUser = gameAndUserSwitchId != null;
                    String gameAndUserId = switchUser ? gameAndUserSwitchId : gameAndUserRemoveId;
                    int gameToRemoveFromId = Integer.parseInt(gameAndUserId.split("-")[0]);
                    Integer userToRemoveId = Integer.parseInt(gameAndUserId.split("-")[1]);
                    List<Integer> attackerIds = attackerIdsList.get(gameToRemoveFromId);
                    List<Integer> defenderIds = defenderIdsList.get(gameToRemoveFromId);
                    if (attackerIds.contains(userToRemoveId)) {
                        attackerIds.remove(userToRemoveId);
                        if (switchUser)
                            defenderIds.add(userToRemoveId);
                    }
                    else {
                        defenderIds.remove(userToRemoveId);
                        if (switchUser)
                            attackerIds.add(userToRemoveId);
                    }

                } else { // admin is inserting or deleting selected temp games
                    String[] selectedGames;
                    selectedGames = request.getParameterValues("selectedGames");
                    createdGames = (List<MultiplayerGame>) session.getAttribute(AdminInterface.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);

                    if (selectedGames == null) {
                        messages.add("Please select at least one Game to insert.");
                        response.sendRedirect(request.getContextPath() + "/admin");
                        break;
                    }

                    selectedGameIndices = new ArrayList<>();
                    for (String u : selectedGames) {
                        selectedGameIndices.add(Integer.parseInt(u));
                    }

                    Collections.sort(selectedGameIndices);
                    Collections.reverse(selectedGameIndices);

                    if (request.getParameter("games_btn").equals("insert Games")) {
                        for (int i : selectedGameIndices) {
                            insertFilledGame(createdGames.get(i), attackerIdsList.get(i), defenderIdsList.get(i));
                        }
                    }

                    for (int i : selectedGameIndices) {
                        createdGames.remove(i);
                        attackerIdsList.remove(i);
                        defenderIdsList.remove(i);
                    }
                }

                response.sendRedirect(request.getContextPath() + "/admin");
                break;

            default:
                System.err.println("Action not recognised");
                String redirect = (String) request.getHeader("referer");
                if (!redirect.startsWith(request.getContextPath())) {
                    redirect = request.getContextPath() + "/" + redirect;
                }
                response.sendRedirect(redirect);

                break;
        }
    }

    private void insertFilledGame(MultiplayerGame multiplayerGame, List<Integer> attackerIDs, List<Integer> defenderIDs) {
        multiplayerGame.insert();
        for (int aid : attackerIDs) multiplayerGame.addPlayerForce(aid, Role.ATTACKER);
        for (int did : defenderIDs) multiplayerGame.addPlayerForce(did, Role.DEFENDER);
    }

    private void createAndFillGames(HttpSession session) {
        int nbGames;
        List<Integer> attackerIDs;
        List<Integer> defenderIDs;
        if (roleAssignmentMethod.equals(RoleAssignmentMethod.OPPOSITE)) {
            attackerIDs = getUsersByLastRole(selectedUserIDs, Role.DEFENDER);
            defenderIDs = getUsersByLastRole(selectedUserIDs, Role.ATTACKER);
            distributeRemainingUsers(selectedUserIDs, attackerIDs, defenderIDs);
            nbGames = getNumberOfGames(attackersPerGame, defendersPerGame, attackerIDs.size(), defenderIDs.size());
        } else {
            nbGames = getNumberOfGames(attackersPerGame, defendersPerGame, selectedUserIDs.size());
            int nbAttackers = (int) Math.round((double) attackersPerGame / (attackersPerGame + defendersPerGame) * selectedUserIDs.size());
            attackerIDs = getRandomUserList(selectedUserIDs, nbAttackers);
            defenderIDs = getRandomUserList(selectedUserIDs, selectedUserIDs.size());
        }

        List<MultiplayerGame> createdGames = createGames(nbGames, attackersPerGame, defendersPerGame,
                extraAttackersPerGame, extraDefendersPerGame, cutID, currentUserID, gamesLevel, gamesState,
                startTime, finishTime);
        session.setAttribute(CREATED_GAMES_LISTS_SESSION_ATTRIBUTE, createdGames);

        if (teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_DESCENDING) || teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_SHUFFLED)) {
            Collections.sort(attackerIDs, new ReverseScoreComparator());
            Collections.sort(defenderIDs, new ReverseScoreComparator());
            if (teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_SHUFFLED)) {
                attackerIDs = getBlockShuffledList(attackerIDs, NB_CATEGORIES_FOR_SHUFFLING);
                defenderIDs = getBlockShuffledList(defenderIDs, NB_CATEGORIES_FOR_SHUFFLING);
            }
        } else {
            Collections.shuffle(attackerIDs);
            Collections.shuffle(defenderIDs);
        }

        session.setAttribute(ATTACKER_LISTS_SESSION_ATTRIBUTE, getUserLists(createdGames, attackerIDs,
                attackersPerGame));
        session.setAttribute(DEFENDER_LISTS_SESSION_ATTRIBUTE, getUserLists(createdGames, defenderIDs,
                defendersPerGame));
    }

    class ReverseScoreComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            Integer score1 = AdminDAO.getScore(o1).getTotalPoints();
            Integer score2 = AdminDAO.getScore(o2).getTotalPoints();
            return (-1) * Integer.compare(score1, score2);
        }

    }

    private static List<Integer> getUsersByLastRole(List<Integer> userIDs, Role role) {
        List<Integer> userList = new ArrayList<>();
        for (int uid : userIDs) {
            Role lastRole = AdminDAO.getLastRole(uid);
            if (lastRole != null && lastRole.equals(role)) {
                userList.add(uid);
            }
        }
        return userList;
    }

    private static int getNumberOfGames(int attackersPerGame, int defendersPerGame, int nbAttackers, int nbDefenders) {
        return (int) Math.ceil(Math.max((float) nbAttackers / attackersPerGame, (float) nbDefenders / defendersPerGame));
    }

    private static int getNumberOfGames(int attackersPerGame, int defendersPerGame, int nbPlayers) {
        return (int) Math.ceil((float) nbPlayers / (attackersPerGame + defendersPerGame));
    }

    private static List<MultiplayerGame> createGames(int nbGames, int attackersPerGame, int defendersPerGame,
													 int extraAttackersPerGame, int extraDefendersPerGame,
													 int cutID, int creatorID, GameLevel level, GameState state,
													 long startTime, long finishTime) {
        List<MultiplayerGame> gameList = new ArrayList<>();
        for (int i = 0; i < nbGames; ++i) {
            MultiplayerGame multiplayerGame = new MultiplayerGame(cutID, creatorID, level, (float) 1, (float) 1,
                    (float) 1, 10, 4, defendersPerGame + extraDefendersPerGame,
                    attackersPerGame + extraAttackersPerGame, 0, 0, startTime,
                    finishTime, state.name(), false);
            gameList.add(multiplayerGame);
        }
        return gameList;
    }

    private static List<Integer> getRandomUserList(List<Integer> userIDs, int nbUsers) {
        List<Integer> randomUserIDs = new ArrayList<>();
        for (int i = 0; i < nbUsers; ++i) {
            int randomIndex = new Random().nextInt(userIDs.size());
            randomUserIDs.add(userIDs.get(randomIndex));
            userIDs.remove(randomIndex);
        }
        return randomUserIDs;
    }

    private static List<Integer> fillGame(List<Integer> userIDs, int nbUsersPerGame) {
        int runs = Math.min(userIDs.size(), nbUsersPerGame);
        List<Integer> playerList = new ArrayList<>();
        for (int i = 0; i < runs; ++i) {
            playerList.add(userIDs.get(0));
            userIDs.remove(0);
        }
        return playerList;
    }

    public static List<List<Integer>> getUserLists(List<MultiplayerGame> createdGames, List<Integer> userIds,
												   int nbUsersPerGame) {
        List<List<Integer>> userLists = new ArrayList<>();
        for (MultiplayerGame mg : createdGames) {
            userLists.add(fillGame(userIds, nbUsersPerGame));
        }
        return userLists;
    }

    private static List<Integer> getBlockShuffledList(List<Integer> originalList, int nbBlocks) {
        if (originalList.size() < nbBlocks)
            return originalList;
        int sublistSize = (int) Math.ceil(originalList.size() / (double) nbBlocks);
        List<List<Integer>> blocks = new ArrayList<>();
        for (int i = 0; i < nbBlocks; ++i) {
            int startIndex = Math.min(i * sublistSize, originalList.size());
            int endIndex = Math.min((i + 1) * sublistSize, originalList.size());
            blocks.add(originalList.subList(startIndex, endIndex));
        }
        List<Integer> blockShuffledList = new ArrayList<>();
        for (int i = 0; i < nbBlocks; ++i) {
            int randomIndex = new Random().nextInt(blocks.size());
            blockShuffledList.addAll(blocks.get(randomIndex));
            blocks.remove(randomIndex);
        }
        return blockShuffledList;
    }

    public static List<Integer> getUnassignedUsers(List<List<Integer>> attackerIdsLists, List<List<Integer>> defenderIdsLists) {
        List<User> unassignedUsersFromDB = AdminDAO.getUnassignedUsers();
        List<Integer> defenderIds = new ArrayList<>();
        List<Integer> attackerIds = new ArrayList<>();
        List<Integer> unassignedUserIds = new ArrayList<>();
        if (attackerIdsLists != null && defenderIdsLists != null) {
            defenderIds = flattenListOfLists(defenderIdsLists);
            attackerIds = flattenListOfLists(attackerIdsLists);
        }
        for (User u : unassignedUsersFromDB) {
            int uid = u.getId();
            if (!(attackerIds.contains(uid) || defenderIds.contains(uid)))
                unassignedUserIds.add(uid);
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
        } else if (testScores.containsKey(pid) && testScores.get(pid) != null)
            return ((PlayerScore) testScores.get(pid)).getTotalScore();
        return 0;
    }

    public static int getSubmissionsCount(MultiplayerGame mg, int pid) {
        HashMap mutantScores = mg.getMutantScores();
        HashMap testScores = mg.getTestScores();
        if (testScores.containsKey(pid) && testScores.get(pid) != null) {
            return ((PlayerScore) testScores.get(pid)).getQuantity();
        } else if (mutantScores.containsKey(pid) && mutantScores.get(pid) != null) {
            return ((PlayerScore) mutantScores.get(pid)).getQuantity();
        }
        return 0;
    }

    private static String zeroPad(long toPad) {
        String s = String.valueOf(toPad);
        return s.length() > 1 ? s : "0" + s;
    }

    public static String getTimeSinceLastSubmission(int pid) {
        Timestamp lastSubmissionTS = AdminDAO.getLastSubmissionTS(pid);
        if (lastSubmissionTS == null) {
            return "never";
        } else {
            Timestamp currentTS = new Timestamp(System.currentTimeMillis());
            long diff = currentTS.getTime() - lastSubmissionTS.getTime();
            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000);
            long diffDays = (int) (diff / (1000 * 60 * 60 * 24));
            String diffString = "";
            if (diffDays >= 1)
                return "more than 1 day";
            //if (diffHours >= 1)
            diffString += zeroPad(diffHours) + "h ";
            //if (diffMinutes >= 1)
            diffString += zeroPad(diffMinutes) + "m ";
            //if (diffSeconds >= 1)
            diffString += zeroPad(diffSeconds) + "s ";
            return diffString;
        }
    }

    private static boolean deletePlayer(int pid, int gid) {
        for (Test t : DatabaseAccess.getTestsForGame(gid)){
            if (t.getPlayerId() == pid)
                AdminDAO.deleteTestTargetExecutions(t.getId());
        }
        for (Mutant m : DatabaseAccess.getMutantsForGame(gid)){
            if (m.getPlayerId() == pid)
                AdminDAO.deleteMutantTargetExecutions(m.getId());
        }
        DatabaseAccess.removePlayerEventsForGame(gid, pid);
        AdminDAO.deleteAttackerEquivalences(pid);
        AdminDAO.deleteDefenderEquivalences(pid);
        AdminDAO.deletePlayerTest(pid);
        AdminDAO.deletePlayerMutants(pid);
        return AdminDAO.deletePlayer(pid);
    }

	private static void distributeRemainingUsers(List<Integer> selectedUserIDs, List<Integer> attackerIDs,
												 List<Integer> defenderIDs) {
		List<Integer> remainingUsers = new ArrayList<>();
		for (int uid : selectedUserIDs) {
			if (!(attackerIDs.contains(uid) || defenderIDs.contains(uid))) {
				remainingUsers.add(uid);
			}
		}
		Collections.shuffle(remainingUsers);

		int nbDefenders = remainingUsers.size() / 2;
		for (int i = 0; i < remainingUsers.size(); ++i) {
			(i < nbDefenders ? defenderIDs : attackerIDs).add(remainingUsers.get(i));
		}
	}

}
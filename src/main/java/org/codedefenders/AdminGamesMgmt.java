package org.codedefenders;

import edu.emory.mathcs.backport.java.util.Collections;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.multiplayer.PlayerScore;
import org.codedefenders.util.AdminDAO;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.validation.CodeValidator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public class AdminGamesMgmt extends HttpServlet {

    public enum RoleAssignmentMethod {RANDOM, OPPOSITE}

    public enum TeamAssignmentMethod {RANDOM, SCORE_DESCENDING, SCORE_SHUFFLED}

    public static final String DEFENDER_LISTS_SESSION_ATTRIBUTE = "defenderLists";
    public static final String ATTACKER_LISTS_SESSION_ATTRIBUTE = "attackerLists";
    public static final String CREATED_GAMES_LISTS_SESSION_ATTRIBUTE = "createdGames";
    private static final int NB_CATEGORIES_FOR_SHUFFLING = 3;
    static final String USER_NAME_LIST_DELIMITER = "[\\r\\n]+";

    private int currentUserID;
    private List<Integer> selectedUserIDs;
    private int cutID;
    private RoleAssignmentMethod roleAssignmentMethod;
    private TeamAssignmentMethod teamAssignmentMethod;
    private int attackersPerGame, defendersPerGame;
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
    boolean chatEnabled;
    boolean markUncovered;
    int maxAssertionsPerTest;
    CodeValidator.CodeValidatorLevel mutantValidatorLevel;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect(request.getContextPath() + "/" + Constants.ADMIN_GAMES_JSP);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        // Get their user id from the session.
        currentUserID = (Integer) session.getAttribute("uid");
        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {

            case "startStopGame":
                startStopGame(request, response, messages);
                break;
            case "createGame":
                createGame(response, request, messages, session);
                break;
            case "insertGames":
                insertGame(response, request, messages, session);
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

	private void insertGame(HttpServletResponse response, HttpServletRequest request, ArrayList<String> messages, HttpSession session) throws IOException {
		attackerIdsList = (List<List<Integer>>) session.getAttribute(AdminGamesMgmt.ATTACKER_LISTS_SESSION_ATTRIBUTE);
		defenderIdsList = (List<List<Integer>>) session.getAttribute(AdminGamesMgmt.DEFENDER_LISTS_SESSION_ATTRIBUTE);
		String gameAndUserRemoveId = request.getParameter("tempGameUserRemoveButton");
		String gameAndUserSwitchId = request.getParameter("tempGameUserSwitchButton");
		if (gameAndUserRemoveId != null || gameAndUserSwitchId != null) { // admin is removing user  from temp game or switching their role
			Boolean switchUser = gameAndUserSwitchId != null;
			String gameAndUserId = switchUser ? gameAndUserSwitchId : gameAndUserRemoveId;
			int gameToRemoveFromId = Integer.parseInt(gameAndUserId.split("-")[0]);
			Integer userToRemoveId = Integer.parseInt(gameAndUserId.split("-")[1]);
			List<Integer> attackerIds = attackerIdsList.get(gameToRemoveFromId);
			List<Integer> defenderIds = defenderIdsList.get(gameToRemoveFromId);
			if (attackerIds.contains(userToRemoveId)) {
				attackerIds.remove(userToRemoveId);
				if (switchUser)
					defenderIds.add(userToRemoveId);
			} else {
				defenderIds.remove(userToRemoveId);
				if (switchUser)
					attackerIds.add(userToRemoveId);
			}

		} else { // admin is inserting or deleting selected temp games
			String[] selectedTempGames;
			selectedTempGames = request.getParameterValues("selectedTempGames");
			createdGames = (List<MultiplayerGame>) session.getAttribute(AdminGamesMgmt.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);

			if (selectedTempGames == null) {
				messages.add("Please select at least one Game to insert.");
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

	private void createGame(HttpServletResponse response, HttpServletRequest request, ArrayList<String> messages, HttpSession session) throws IOException {
		String rowUserId = request.getParameter("userListButton");
		if (rowUserId != null) { // if admin is trying to add a single user to a game
			int addedUserId = Integer.parseInt(rowUserId);
			// Get the identifying information required to create a game from the submitted form.
			String gidString = request.getParameter("game_" + addedUserId);
			Role role = Role.valueOf(request.getParameter("role_" + addedUserId));
			int gid;
			List<Integer> userList = new ArrayList<>();
			boolean isTempGame = gidString.startsWith("T");
			if (isTempGame) {
				createdGames = (List<MultiplayerGame>) session.getAttribute(AdminGamesMgmt.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
				gid = Integer.parseInt(gidString.substring(1));
				mg = createdGames.get(gid);
				userList = (role.equals(Role.ATTACKER) ?
						(List<List<Integer>>) session.getAttribute(AdminGamesMgmt.ATTACKER_LISTS_SESSION_ATTRIBUTE) :
						(List<List<Integer>>) session.getAttribute(AdminGamesMgmt.DEFENDER_LISTS_SESSION_ATTRIBUTE))
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
			batchCreateGames(request, response, session, messages);
		}
		response.sendRedirect(request.getContextPath() + "/admin");
	}

	private void batchCreateGames(HttpServletRequest request, HttpServletResponse response, HttpSession session, ArrayList<String> messages) throws IOException {
		attackerIdsList = (List<List<Integer>>) session.getAttribute(AdminGamesMgmt.ATTACKER_LISTS_SESSION_ATTRIBUTE);
		defenderIdsList = (List<List<Integer>>) session.getAttribute(AdminGamesMgmt.DEFENDER_LISTS_SESSION_ATTRIBUTE);
		createdGames = (List<MultiplayerGame>) session.getAttribute(AdminGamesMgmt.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
		String[] selectedUsers;
		String userNameListString;
		try {
			selectedUsers = request.getParameterValues("selectedUsers");
			userNameListString = request.getParameter("user_name_list");
			cutID = Integer.parseInt(request.getParameter("class"));
			roleAssignmentMethod = request.getParameter("roles").equals(RoleAssignmentMethod.OPPOSITE.name())
					? RoleAssignmentMethod.OPPOSITE : RoleAssignmentMethod.RANDOM;
			teamAssignmentMethod = TeamAssignmentMethod.valueOf(request.getParameter("teams"));
			attackersPerGame = Integer.parseInt(request.getParameter("attackers"));
			defendersPerGame = Integer.parseInt(request.getParameter("defenders"));
			gamesLevel = GameLevel.valueOf(request.getParameter("gamesLevel"));
			gamesState = request.getParameter("gamesState").equals(GameState.ACTIVE.name()) ? GameState.ACTIVE : GameState.CREATED;
			startTime = Long.parseLong(request.getParameter("startTime"));
			finishTime = Long.parseLong(request.getParameter("finishTime"));
		} catch (Exception e) {
			messages.add("There was a problem with the form.");
			response.sendRedirect(request.getContextPath() + "/admin");
			return;
		}

		selectedUserIDs = new ArrayList<>();
		if (selectedUsers != null) {
			for (String u : selectedUsers) {
				selectedUserIDs.add(Integer.parseInt(u));
			}
		}
		if (userNameListString != null) {
			for (String uName : userNameListString.split(USER_NAME_LIST_DELIMITER)) {
				if (uName.length() > 0) {
					User u = DatabaseAccess.getUserForNameOrEmail(uName);
					if (u == null)
						messages.add("No user with name or email \'" + uName + "\'!");
					else if (!selectedUserIDs.contains(u.getId()))
						selectedUserIDs.add(u.getId());
				}
			}
		}

		List<Integer> unassignedUserIds = getUnassignedUserIds(attackerIdsList, defenderIdsList);
		for (Integer uid : new ArrayList<>(selectedUserIDs)) {
			if (!unassignedUserIds.contains(uid)) {
				messages.add("user " + uid + " is already playing another game!");
				selectedUserIDs.remove(uid);
			}
		}


		if (selectedUserIDs.size() == 0) {
			messages.add("Please select at least one User.");
		} else {
			messages.add("Creating " + gamesLevel + " games for users " + selectedUserIDs + " with CUT " + cutID + ", assigning roles " +
					roleAssignmentMethod + ", assigning teams " + teamAssignmentMethod + " with " + attackersPerGame +
					" Attackers and " + defendersPerGame + " Defenders each.");
			createAndFillGames(session, createdGames, attackerIdsList, defenderIdsList);
		}
	}

	private void startStopGame(HttpServletRequest request, HttpServletResponse response, ArrayList<String> messages) throws IOException {
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

		} else {  // admin is starting or stopping selected games
			String[] selectedGames = request.getParameterValues("selectedGames");

			if (selectedGames == null) {
				// admin is starting or stopping a single game
				int gameId = -1;
				// Get the identifying information required to create a game from the submitted form.

				try {
					gameId = Integer.parseInt(request.getParameter("start_stop_btn"));
				} catch (Exception e) {
					messages.add("There was a problem with the form.");
					response.sendRedirect(request.getContextPath() + "/admin");
					return;
				}


				String errorMessage = "ERROR trying to start or stop game " + String.valueOf(gameId)
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
			} else {
				GameState newState = request.getParameter("games_btn").equals("Start Games") ? GameState.ACTIVE : GameState.FINISHED;
				for (String gameId : selectedGames) {
					mg = DatabaseAccess.getMultiplayerGame(Integer.parseInt(gameId));
					mg.setState(newState);
					if (!mg.update()) {
						messages.add("ERROR trying to start or stop game " + String.valueOf(gameId));
					}
				}
			}
		}
		response.sendRedirect(request.getContextPath() + "/admin");
	}

	private void insertFilledGame(MultiplayerGame multiplayerGame, List<Integer> attackerIDs, List<Integer> defenderIDs) {
        multiplayerGame.insert();
        for (int aid : attackerIDs) multiplayerGame.addPlayerForce(aid, Role.ATTACKER);
        for (int did : defenderIDs) multiplayerGame.addPlayerForce(did, Role.DEFENDER);
    }

    private void createAndFillGames(HttpSession session, List<MultiplayerGame> createdGames, List<List<Integer>> attackerIdsList, List<List<Integer>> defenderIdsList) {
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

        List<MultiplayerGame> newlyCreatedGames = createGames(nbGames, attackersPerGame, defendersPerGame,
                 cutID, currentUserID, gamesLevel, gamesState,
                startTime, finishTime, maxAssertionsPerTest, chatEnabled,
                mutantValidatorLevel, markUncovered);

        if (teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_DESCENDING) || teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_SHUFFLED)) {
            Collections.sort(attackerIDs, new ReverseDefenderScoreComparator());
            Collections.sort(defenderIDs, new ReverseDefenderScoreComparator());
            if (teamAssignmentMethod.equals(TeamAssignmentMethod.SCORE_SHUFFLED)) {
                attackerIDs = getBlockShuffledList(attackerIDs, NB_CATEGORIES_FOR_SHUFFLING);
                defenderIDs = getBlockShuffledList(defenderIDs, NB_CATEGORIES_FOR_SHUFFLING);
            }
        } else {
            Collections.shuffle(attackerIDs);
            Collections.shuffle(defenderIDs);
        }
        List<List<Integer>> newAttackerIdsList = getUserLists(newlyCreatedGames, attackerIDs, attackersPerGame);
        List<List<Integer>> newDefenderIdsList = getUserLists(newlyCreatedGames, defenderIDs, defendersPerGame);

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

    class ReverseDefenderScoreComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            Integer score1 = AdminDAO.getScore(o1).getDefenderScore();
            Integer score2 = AdminDAO.getScore(o2).getDefenderScore();
            return (-1) * Integer.compare(score1, score2);
        }
    }

    class ReverseTotalScoreComparator implements Comparator<Integer> {
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
                                                     int cutID, int creatorID, GameLevel level, GameState state,
                                                     long startTime, long finishTime, int maxAssertionsPerTest,
                                                     boolean chatEnabled, CodeValidator.CodeValidatorLevel mutantValidatorLevel,
                                                     boolean markUncovered) {
        List<MultiplayerGame> gameList = new ArrayList<>();
        for (int i = 0; i < nbGames; ++i) {
            MultiplayerGame multiplayerGame = new MultiplayerGame(cutID, creatorID, level, (float) 1, (float) 1,
                    (float) 1, 10, 4, 0,
                    0, 0, 0, startTime,
                    finishTime, state.name(), false, maxAssertionsPerTest, chatEnabled, mutantValidatorLevel, markUncovered);
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

    public static List<List<String>> getUnassignedUsers(List<List<Integer>> attackerIdsLists, List<List<Integer>> defenderIdsLists) {
        List<List<String>> unassignedUsersFromDB = AdminDAO.getUnassignedUsersInfo();
        List<Integer> defenderIds = new ArrayList<>();
        List<Integer> attackerIds = new ArrayList<>();
        List<List<String>> unassignedUserIds = new ArrayList<>();
        if (attackerIdsLists != null && defenderIdsLists != null) {
            defenderIds = flattenListOfLists(defenderIdsLists);
            attackerIds = flattenListOfLists(attackerIdsLists);
        }
        for (List<String> userInfo : unassignedUsersFromDB) {
            int uid = Integer.valueOf(userInfo.get(0));
            if (!(attackerIds.contains(uid) || defenderIds.contains(uid)))
                unassignedUserIds.add(userInfo);
        }
        return unassignedUserIds;
    }

    private static List<Integer> getUnassignedUserIds(List<List<Integer>> attackerIdsLists, List<List<Integer>> defenderIdsLists) {
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

    private static boolean deletePlayer(int pid, int gid) {
        for (Test t : DatabaseAccess.getTestsForGame(gid)) {
            if (t.getPlayerId() == pid)
                AdminDAO.deleteTestTargetExecutions(t.getId());
        }
        for (Mutant m : DatabaseAccess.getMutantsForGame(gid)) {
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
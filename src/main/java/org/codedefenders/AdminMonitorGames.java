package org.codedefenders;

import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.util.AdminDAO;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.Redirect;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

public class AdminMonitorGames extends HttpServlet {

    private MultiplayerGame mg;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect(request.getContextPath() + "/" + Constants.ADMIN_MONITOR_JSP);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        // Get their user id from the session.
        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {

            case "startStopGame":
                startStopGame(request, response, messages);
                break;
            default:
                System.err.println("Action not recognised");
                Redirect.redirectBack(request, response);
                break;
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
		response.sendRedirect(request.getContextPath() + "/admin/monitor");
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

}
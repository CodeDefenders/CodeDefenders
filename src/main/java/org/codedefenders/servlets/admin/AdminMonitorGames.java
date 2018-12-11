/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.execution.KillMap.KillMapJob;
import org.codedefenders.execution.KillMap.KillMapJob.Type;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;

public class AdminMonitorGames extends HttpServlet {

    private MultiplayerGame mg;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		request.getRequestDispatcher(Constants.ADMIN_MONITOR_JSP).forward(request, response);
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
			int userId = UserDAO.getUserForPlayer(playerToRemoveId).getId();
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
                    } else {
                        // Schedule the killmap
                        if (GameState.FINISHED.equals(newState)) {
                            KillmapDAO.enqueueJob( new KillMapJob(Type.GAME, gameId));
                        }
                    }
				}
			} else {
				GameState newState = request.getParameter("games_btn").equals("Start Games") ? GameState.ACTIVE : GameState.FINISHED;
				for (String gameId : selectedGames) {
					mg = DatabaseAccess.getMultiplayerGame(Integer.parseInt(gameId));
					mg.setState(newState);
					if (!mg.update()) {
						messages.add("ERROR trying to start or stop game " + String.valueOf(gameId));
                    } else {
                        // Schedule the killmap
                        if (GameState.FINISHED.equals(newState)) {
                            KillmapDAO.enqueueJob( new KillMapJob(Type.GAME, Integer.parseInt(gameId)));
                        }
                    }
				}
			}
		}
		response.sendRedirect(request.getContextPath() + "/admin/monitor");
	}


    private static boolean deletePlayer(int pid, int gid) {
        for (Test t : TestDAO.getTestsForGame(gid)) {
            if (t.getPlayerId() == pid)
                AdminDAO.deleteTestTargetExecutions(t.getId());
        }
        for (Mutant m : MutantDAO.getValidMutantsForGame(gid)) {
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
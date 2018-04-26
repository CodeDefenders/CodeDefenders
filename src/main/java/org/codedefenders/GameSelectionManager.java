package org.codedefenders;

import org.codedefenders.duel.DuelGame;
import org.codedefenders.singleplayer.PrepareAI;
import org.codedefenders.singleplayer.SinglePlayerGame;
import org.codedefenders.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.singleplayer.automated.defender.AiDefender;
import org.codedefenders.util.DatabaseAccess;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

public class GameSelectionManager extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String contextPath = request.getContextPath();
		response.sendRedirect(contextPath + "/games/user");
	}

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String contextPath = request.getContextPath();
        HttpSession session = request.getSession();
        // Get their user id from the session.
        int uid = (Integer) session.getAttribute("uid");
        int gameId;

        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {

            case "createGame":

                // Get the identifying information required to create a game from the submitted form.

                try {
                    int classId = Integer.parseInt(request.getParameter("class"));

                    int rounds = Integer.parseInt(request.getParameter("rounds"));

                    /* Disable mode selection for release. */
                    /* String modeName = request.getParameter("mode"); */
                    Role role = request.getParameter("role") == null ? Role.DEFENDER : Role.ATTACKER;
                    GameLevel level = request.getParameter("level") == null ? GameLevel.HARD : GameLevel.EASY;
                    GameMode mode = GameMode.DUEL;

                    if (rounds < 1 || rounds > 10) {
                        messages.add("Invalid rounds amount");
                        response.sendRedirect(contextPath+"/games");
                        return;
                    }

                    /* Disable mode selection for release. */
                    /*
                    switch (modeName) {
                        case "sing":
                            mode = GameMode.SINGLE;
                            break;
                        case "duel":
                            mode = GameMode.DUEL;
                            break;
                        case "prty":
                            mode = GameMode.PARTY;
                            break;
                        case "utst":
                            mode = GameMode.UTESTING;
                            break;
                        default:
                            mode = GameMode.SINGLE;
                    }
                    */

                    if (classId != 0 && DatabaseAccess.getClassForKey("Class_ID", classId) != null) {
                        //Valid class selected.

                        if (mode.equals(GameMode.SINGLE)) {
                            //Create singleplayer game.
                            if (PrepareAI.isPrepared(DatabaseAccess.getClassForKey("Class_ID", classId))) {
                                SinglePlayerGame nGame = new SinglePlayerGame(classId, uid, rounds, role, level);
                                nGame.insert();
                                if (role.equals(Role.ATTACKER)) {
                                    nGame.addPlayer(uid, Role.ATTACKER);
                                    nGame.addPlayer(AiDefender.ID, Role.DEFENDER);
                                } else {
                                    nGame.addPlayer(uid, Role.DEFENDER);
                                    nGame.addPlayer(AiAttacker.ID, Role.ATTACKER);
                                }
                                nGame.tryFirstTurn();
                            } else {
                                //Not prepared, show a message and redirect.
                                messages.add("AI has not been prepared for class. Please select PREPARE AI on the classes page.");
                                String redirect = (String) request.getHeader("referer");
                                if (!redirect.startsWith(request.getContextPath())) {
                                    redirect = request.getContextPath() + "/" + redirect;
                                }
                                response.sendRedirect(redirect);
                                return;
                            }
                        } else {
                            // Create the game with supplied parameters and insert it in the database.
                            DuelGame nGame = new DuelGame(classId, uid, rounds, role, level);
                            nGame.insert();
                            if (nGame.getAttackerId() != 0)
                                nGame.addPlayer(uid, Role.ATTACKER);
                            else
                                nGame.addPlayer(uid, Role.DEFENDER);
                        }


                        // Redirect to the game selection menu.
                    }
                } catch (Exception e) {
                    messages.add("There was a problem with the form.");
                }

                response.sendRedirect(contextPath+"/games");


                break;

            case "joinGame":

                // Get the identifying information required to create a game from the submitted form.
                try {
                    gameId = Integer.parseInt(request.getParameter("game"));

                    DuelGame jGame = DatabaseAccess.getGameForKey("ID", gameId);

                    if ((jGame.getAttackerId() == uid) || (jGame.getDefenderId() == uid)) {
                        // uid is already in the game
                        if (jGame.getDefenderId() == uid)
                            messages.add("Already a defender in this game!");
                        else
                            messages.add("Already an attacker in this game!");
                        // either way, reload list of open games
                        String redirect = (String) request.getHeader("referer");
                        if( ! redirect.startsWith(request.getContextPath())){
                            redirect = request.getContextPath()+"/" + redirect;
                        }
                        response.sendRedirect(redirect);
                        break;
                    } else {
                        if (jGame.getAttackerId() == 0) {
                            jGame.addPlayer(uid, Role.ATTACKER);
                            messages.add("Joined game as an attacker.");
                        } else if (jGame.getDefenderId() == 0) {
                            messages.add("Joined game as a defender.");
                            jGame.addPlayer(uid, Role.DEFENDER);
                        } else {
                            messages.add("DuelGame is no longer open.");
                            String redirect = (String) request.getHeader("referer");
                            if( ! redirect.startsWith(request.getContextPath())){
                                redirect = request.getContextPath()+"/" + redirect;
                            }
                            response.sendRedirect(redirect);
                            break;
                        }
                        // user joined, update game
                        jGame.setState(GameState.ACTIVE);
                        jGame.setActiveRole(Role.ATTACKER);
                        jGame.update();
                        // go to play view
                        session.setAttribute("gid", gameId);
                        response.sendRedirect(contextPath+"/play");
                    }
                } catch (Exception e) {
                    messages.add("There was a problem joining the game.");
                    String redirect = (String) request.getHeader("referer");
                    if( ! redirect.startsWith(request.getContextPath())){
                        redirect = request.getContextPath()+"/" + redirect;
                    }
                    response.sendRedirect(redirect);
                }

                break;

            case "enterGame":

                try {
                    gameId = Integer.parseInt(request.getParameter("game"));
                    DuelGame eGame = DatabaseAccess.getGameForKey("ID", gameId);

                    if (eGame.isUserInGame(uid)) {
                        session.setAttribute("gid", gameId);
                        if (eGame.getMode().equals(GameMode.UTESTING))
                            response.sendRedirect(contextPath+"/utesting");
                        else
                            response.sendRedirect(contextPath+"/play");
                    } else {
                    	String redirect = (String) request.getHeader("referer");
                    	if( ! redirect.startsWith(request.getContextPath())){
                    		redirect = request.getContextPath()+"/" + redirect;
                    	}
                    	response.sendRedirect(redirect);
                    }
                } catch (Exception e) {
                    messages.add("There was a problem entering the game");
                    String redirect = (String) request.getHeader("referer");
                    if( ! redirect.startsWith(request.getContextPath())){
                        redirect = request.getContextPath()+"/" + redirect;
                    }
                    response.sendRedirect(redirect);
                }
                break;
            default:
                System.err.println("Action not recognised");
                String redirect = (String) request.getHeader("referer");
                if( ! redirect.startsWith(request.getContextPath())){
                    redirect = request.getContextPath()+"/" + redirect;
                }
                response.sendRedirect(redirect);
                break;
        }
    }
}
<% String pageTitle="In Game"; %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%
    // Get their user id from the session.
    int uid = (Integer) session.getAttribute("uid");
    int gameId = 0;
    try {
        gameId = Integer.parseInt(request.getParameter("id"));
        session.setAttribute("mpGameId", gameId);
    } catch (NumberFormatException e){
        gameId = (Integer) session.getAttribute("mpGameId");
    }
    boolean isTests = false;

    MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);
    %>
<%@ include file="/jsp/multiplayer/header_game.jsp" %>
<%
    ArrayList<MultiplayerMutant> mutantsAlive = mg.getAliveMutants();
    ArrayList<MultiplayerMutant> mutantsKilled = mg.getKilledMutants();
    //ArrayList<String> messages = new ArrayList<String>();

    Participance p = mg.getParticipance(uid);

    switch (p){
        case ATTACKER:
            %><%@ include file="/jsp/multiplayer/attacker_view.jsp" %><%
            break;
        case DEFENDER:
            %><%@ include file="/jsp/multiplayer/defender_view.jsp" %><%
            break;
        case CREATOR:
            %><%@ include file="/jsp/multiplayer/creator_view.jsp" %><%
            break;
        default:
            System.out.println("Entered Page Display");
            if (request.getParameter("defender") != null){
                mg.addUserAsDefender(uid);
            } else if (request.getParameter("attacker") != null){
                mg.addUserAsAttacker(uid);
            } else {
                response.sendRedirect("/games/user");
                break;
            }
            %>
            <p>Joining Game...</p>
<%
            response.setIntHeader("Refresh", 1);
            break;
    }
%>
<%@ include file="/jsp/multiplayer/footer_game.jsp" %>
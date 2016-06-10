<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.codedefenders.DatabaseAccess" %>
<%@ page import="org.codedefenders.multiplayer.Participance" %>
<% String pageTitle="In Game"; %>
<%@ include file="/jsp/header_game.jsp" %>
<%
    // Get their user id from the session.
    int uid = (Integer) session.getAttribute("uid");
    int gameId = Integer.parseInt(request.getParameter("id"));

    MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);

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
            response.sendRedirect("/games/user");
            break;
    }
%>
<%@ include file="/jsp/footer_game.jsp" %>
<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<% String pageTitle="In Game"; %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.servlets.util.ServletUtils" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    int userId = ServletUtils.userId(request); // required for playerFeedback, too
	Role role = game.getRole(userId); // required for header_game, too
%>
<%-- Set request attributes for the components. --%>
<%
    /* playerFeedback & game_notifications */
    request.setAttribute("gameId", game.getId());
    // playerId already set in servlet.
    // request.setAttribute("playerId", playerId);
%>
<%@ include file="/jsp/battleground/header_game.jsp" %>

<%-- Push notifications using WebSocket --%>
<%--<%@ include file="/jsp/push_notifications.jsp"%>--%>
<%-- Show the bell icon with counts of unread notifications: requires push_notifications.jsp --%>
<%--<%@ include file="/jsp/push_game_notifications.jsp"%>--%>
<%-- Show the mail icon with counts of unread notifications: requires push_notifications.jsp --%>
<%--<%@ include file="/jsp/push_chat_notifications.jsp"%>--%>

<jsp:include page="/jsp/scoring_tooltip.jsp"/>
<jsp:include page="/jsp/playerFeedback.jsp"/>
<jsp:include page="/jsp/battleground/game_scoreboard.jsp"/>
<jsp:include page="/jsp/game_components/editor_help_config_modal.jsp"/>

<div class="crow fly no-gutter up">
<%
    messages = new ArrayList<>();
    session.setAttribute("messages", messages);
    boolean openEquivalenceDuel = request.getAttribute("openEquivalenceDuel") != null;

    switch (role){
        case ATTACKER:
            if (openEquivalenceDuel) { %>
                <%@ include file="/jsp/battleground/equivalence_view.jsp" %>
            <% } else { %>
                <%@ include file="/jsp/battleground/attacker_view.jsp" %>
            <% }
            break;
        case DEFENDER:
            %><%@ include file="/jsp/battleground/defender_view.jsp" %>
            <%
            break;
        case OBSERVER:
            %><%@ include file="/jsp/battleground/creator_view.jsp" %><%
            break;
        default:
            response.sendRedirect(request.getContextPath()+ Paths.GAMES_OVERVIEW);
            return;
    }
%>
    </div>
<%
if (game.isCapturePlayersIntention()) {
    if (role == Role.DEFENDER) {
%>
<%@ include file="/jsp/game_components/defender_intention_collector.jsp" %>
<%
	} else if (role == Role.ATTACKER) {
%>
<%@ include file="/jsp/game_components/attacker_intention_collector.jsp" %>
<%
	}
}
%>
<!-- This corresponds to dispatcher.Dispatch -->
<jsp:include page="/jsp/game_notifications.jsp"/>
<%@ include file="/jsp/battleground/footer_game.jsp" %>

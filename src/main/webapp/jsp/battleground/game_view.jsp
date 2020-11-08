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
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
	Role role = game.getRole(login.getUserId()); // required for header_game, too
%>

<jsp:useBean id="playerFeedback" class="org.codedefenders.beans.game.PlayerFeedbackBean" scope="request"/>
<%
    playerFeedback.setGameInfo(game.getId(), game.getCreatorId());
    playerFeedback.setPlayerInfo(login.getUser(), role);
%>

<jsp:useBean id="scoreboard" class="org.codedefenders.beans.game.ScoreboardBean" scope="request"/>
<%
    scoreboard.setGameId(game.getId());
    scoreboard.setScores(game.getMutantScores(), game.getTestScores());
    scoreboard.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());
%>

<jsp:useBean id="history" class="org.codedefenders.beans.game.HistoryBean" scope="request"/>
<%
    history.setLogin(login);
    history.setGameId(game.getId());
    history.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());
%>

<jsp:include page="/jsp/battleground/header_game.jsp"/>

<%-- Push notifications using WebSocket --%>
<jsp:include page="/jsp/push_notifications.jsp"/>
<t:game_chat/>

<%-- Show the bell icon with counts of unread notifications: requires push_notifications.jsp --%>
<%--<%@ include file="/jsp/push_game_notifications.jsp"%>--%>

<jsp:include page="/jsp/scoring_tooltip.jsp"/>
<jsp:include page="/jsp/player_feedback.jsp"/>
<jsp:include page="/jsp/battleground/game_scoreboard.jsp"/>
<jsp:include page="/jsp/battleground/game_history.jsp"/>
<jsp:include page="/jsp/game_components/editor_help_config_modal.jsp"/>

<div class="crow fly no-gutter up">
<%
    boolean openEquivalenceDuel = request.getAttribute("openEquivalenceDuel") != null;

    switch (role){
        case ATTACKER:
            if (openEquivalenceDuel) { %>
                <jsp:include page="/jsp/battleground/equivalence_view.jsp"/>
            <% } else { %>
                <jsp:include page="/jsp/battleground/attacker_view.jsp"/>
            <% }
            break;
        case DEFENDER:
            %><jsp:include page="/jsp/battleground/defender_view.jsp"/>
            <%
            break;
        case OBSERVER:
            %><jsp:include page="/jsp/battleground/creator_view.jsp"/><%
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

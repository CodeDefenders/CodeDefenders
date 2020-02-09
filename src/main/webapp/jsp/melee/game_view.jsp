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
<%@ page import="org.codedefenders.game.Role"%>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame"%>
<%@ page import="org.codedefenders.util.Paths"%>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
    Role role = game.getRole(login.getUserId());
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
%>

<jsp:include page="/jsp/melee/header_game.jsp"/>

<%-- Push notifications using WebSocket --%>
<jsp:include page="/jsp/push_notifications.jsp"/>
<%-- Show the bell icon with counts of unread notifications: requires push_notifications.jsp --%>
<%--<%@ include file="/jsp/push_game_notifications.jsp"%>--%>
<%-- Show the mail icon with counts of unread notifications: requires push_notifications.jsp --%>
<%--<%@ include file="/jsp/push_chat_notifications.jsp"%>--%>

<jsp:include page="/jsp/scoring_tooltip.jsp"/>
<%-- <jsp:include page="/jsp/player_feedback.jsp"/> --%>
<%-- <jsp:include page="/jsp/battleground/game_scoreboard.jsp"/> --%>
<jsp:include page="/jsp/game_components/editor_help_config_modal.jsp"/>

<div class="crow fly no-gutter up">
	<%
	    if (role.equals(Role.OBSERVER)) {
	%>
			<jsp:include page="/jsp/melee/creator_view.jsp" />
	<%
	    } else {
	%>
			<jsp:include page="/jsp/melee/player_view.jsp" />
	<%
	    }
	%>
</div>

<!-- This corresponds to dispatcher.Dispatch -->
<jsp:include page="/jsp/game_notifications.jsp"/>
<%@ include file="/jsp/melee/footer_game.jsp" %>

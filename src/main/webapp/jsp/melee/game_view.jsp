<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.ArrayList" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
    Role role = game.getRole(login.getUserId());

    pageContext.setAttribute("game", game);
    pageContext.setAttribute("role", role);
%>

<jsp:useBean id="playerFeedback" class="org.codedefenders.beans.game.PlayerFeedbackBean" scope="request"/>
<%
    playerFeedback.setGameInfo(game.getId(), game.getCreatorId());
    playerFeedback.setPlayerInfo(login.getUserId(), role);
%>


<jsp:useBean id="history" class="org.codedefenders.beans.game.HistoryBean" scope="request"/>
<%
    history.setLogin(login);
    history.setGameId(game.getId());

    Player userPlayer = null;
    List<Player> otherPlayers = new ArrayList<>();
    for (Player player : game.getPlayers()) {
        if (player.getUser().getId() == login.getUserId()) {
            userPlayer = player;
        } else {
            otherPlayers.add(player);
        }
    }

    // We simply need two distinct sets, to determine which events to display on the left/right side of the timeline
    history.setPlayers(Collections.singletonList(userPlayer), otherPlayers);
%>

<jsp:useBean id="previousSubmission" class="org.codedefenders.beans.game.PreviousSubmissionBean" scope="request"/>

<!-- We set the  meeleScoreboardBean from the servlet not the jsp -->

<p:game_page>
    <jsp:include page="/jsp/player_feedback.jsp"/>
    <jsp:include page="/jsp/melee/game_scoreboard.jsp"/>
    <jsp:include page="/jsp/battleground/game_history.jsp"/>

    <c:choose>
        <c:when test="${role == Role.OBSERVER}">
            <jsp:include page="/jsp/melee/creator_view.jsp" />
        </c:when>
        <c:otherwise>
            <jsp:include page="/jsp/melee/player_view.jsp" />
        </c:otherwise>
    </c:choose>
    <c:if test="${game.capturePlayersIntention}">
        <jsp:include page="/jsp/game_components/defender_intention_collector.jsp"/>
        <jsp:include page="/jsp/game_components/attacker_intention_collector.jsp"/>
    </c:if>
</p:game_page>


<% previousSubmission.clear(); %>

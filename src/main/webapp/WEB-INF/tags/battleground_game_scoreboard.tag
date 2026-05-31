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
<%@ tag import="org.codedefenders.model.UserEntity" %>
<%@ tag import="org.codedefenders.util.Constants" %>
<%@ tag import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%@ tag import="org.codedefenders.model.Player" %>
<%@ tag import="java.util.Map" %>
<%@ tag import="java.util.List" %>
<%@ tag import="org.codedefenders.model.UserEntity" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%@ attribute name="playerId" required="false" type="java.lang.Integer" %>
<%@ attribute name="gameFinished" required="false" type="java.lang.Boolean" %>

<jsp:useBean id="scoreboard" class="org.codedefenders.beans.game.ScoreboardBean" scope="request"/>
<%
    Map<Integer, PlayerScore> mutantScores = scoreboard.getMutantsScores();
    Map<Integer, PlayerScore> testScores = scoreboard.getTestScores();

    // Those return the PlayerID not the UserID
    final List<Player> attackers = scoreboard.getAttackers();
    final List<Player> defenders = scoreboard.getDefenders();

    PlayerScore zeroDummyScore = new PlayerScore(-1);
    zeroDummyScore.setMutantKillInformation("0 / 0 / 0");
    zeroDummyScore.setDuelInformation("0 / 0 / 0");
%>

<%-- default values for the attributes --%>
<c:set var="playerStatus" value="${empty playerId ? '' : scoreboard.getStatusForPlayer(playerId).toString()}"/>
<c:set var="gameFinished" value="${empty gameFinished ? false : gameFinished}"/>

<div class="w-100 d-flex justify-content-center align-content-center gap-3 mb-3">
    <span class="fg-attacker fs-1 text-end total-attacker-score">
        <c:if test="${gameFinished}">
            <span class="player-message">
                <c:choose>
                    <c:when test="${playerStatus == 'WINNING_ATTACKER'}">${i18n.tr('Your team won!')}</c:when>
                    <c:when test="${playerStatus == 'LOSING_ATTACKER'}">${i18n.tr('Your team lost!')}</c:when>
                    <c:when test="${playerStatus == 'TIE_ATTACKER'}">${i18n.tr('Tie!')}</c:when>
                </c:choose>
            </span>
        </c:if>
        ${scoreboard.totalAttackerScore}
    </span>
    <img alt="${i18n.tr('Code Defenders Logo')}" style="width: 4rem;" src="${pageContext.request.contextPath}/images/logo.png"/>
    <span class="fg-defender fs-1 text-start total-defender-score">
        ${scoreboard.totalDefenderScore}
        <c:if test="${gameFinished}">
            <span class="player-message">
                <c:choose>
                    <c:when test="${playerStatus == 'WINNING_DEFENDER'}">${i18n.tr('Your team won!')}</c:when>
                    <c:when test="${playerStatus == 'LOSING_DEFENDER'}">${i18n.tr('Your team lost!')}</c:when>
                    <c:when test="${playerStatus == 'TIE_DEFENDER'}">${i18n.tr('Tie!')}</c:when>
                </c:choose>
            </span>
        </c:if>
    </span>
</div>
<table class="scoreboard table m-0 text-white">

    <tr class="attacker header">
        <th>${i18n.tr('Attackers')}</th>
        <th>${i18n.tr('Mutants')}</th>
        <th>${i18n.tr('Alive / Killed / Equivalent')}</th>
        <th>${i18n.tr('Duels Won / Lost / Ongoing')}</th>
        <th>${i18n.tr('Total Points')}</th>
    </tr>
    <%
        for (Player attacker : attackers) {
            UserEntity attackerUser = attacker.getUser();

            // TODO Phil 09/08/19: Isn't this fixed by now? Why is this hack still in place?
            // Does system attacker submitted any mutant?
            // TODO #418: we use UserId instead of PlayerID because there's a bug in the logic which initialize the game.
            // For system generated mutants,  mutant.playerID == userID, which is wrong...
            if (attackerUser.getId() == Constants.DUMMY_ATTACKER_USER_ID && !scoreboard.gameHasPredefinedMutants()) {
                continue;
            }

            PlayerScore mutantsScore = mutantScores.getOrDefault(attacker.getId(), zeroDummyScore);
            PlayerScore testsScore = testScores.getOrDefault(attacker.getId(), zeroDummyScore);
    %>
    <tr class="attacker">
        <td><%=attackerUser.getUsername()%>
        </td>
        <td><%=mutantsScore.getQuantity()%>
        </td>
        <td><%=mutantsScore.getMutantKillInformation()%>
        </td>
        <!-- Equivalence duels -->
        <td><%=mutantsScore.getDuelInformation()%>
        </td>
        <!-- Total Points -->
        <td><%=mutantsScore.getTotalScore() + testsScore.getTotalScore()%>
        </td>
    </tr>
    <%
        }
    %>
    <tr class="attacker total">
        <td>${i18n.tr('Attacking Team')}</td>
        <td><%=mutantScores.getOrDefault(-1, zeroDummyScore).getQuantity()%>
        </td>
        <td><%=mutantScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation()%>
        </td>
        <!-- Equivalence duels -->
        <td><%=mutantScores.getOrDefault(-1, zeroDummyScore).getDuelInformation()%>
        </td>
        <!-- Total points -->
        <td>
            <%=mutantScores.getOrDefault(-1, zeroDummyScore).getTotalScore() +
                    testScores.getOrDefault(-2, zeroDummyScore).getTotalScore()%>
        </td>
    </tr>

    <tr class="defender header">
        <th>${i18n.tr('Defenders')}</th>
        <th>${i18n.tr('Tests')}</th>
        <th>${i18n.tr('Mutants Killed')}</th>
        <th>${i18n.tr('Duels Won / Lost / Ongoing')}</th>
        <th>${i18n.tr('Total Points')}</th>
    </tr>
    <%
        for (Player defender : defenders) {
            UserEntity defenderUser = defender.getUser();

            if (defenderUser.getId() == Constants.DUMMY_DEFENDER_USER_ID && !scoreboard.gameHasPredefinedTests()) {
                continue;
            }

            PlayerScore testsScore = testScores.getOrDefault(defender.getId(), zeroDummyScore);
    %>
    <tr class="defender">
        <td><%=defenderUser.getUsername()%>
        </td>
        <td><%=testsScore.getQuantity()%>
        </td>
        <td><%=testsScore.getMutantKillInformation()%>
        </td>
        <!-- Equivalence duels -->
        <td><%=testsScore.getDuelInformation()%>
        </td>
        <td><%=testsScore.getTotalScore()%>
        </td>
    </tr>
    <%
        }
    %>
    <tr class="defender total">
        <td>${i18n.tr('Defending Team')}</td>
        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getQuantity()%>
        </td>
        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation()%>
        </td>
        <!-- Equivalence duels -->
        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getDuelInformation()%>
        </td>
        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getTotalScore()%>
        </td>
    </tr>
</table>

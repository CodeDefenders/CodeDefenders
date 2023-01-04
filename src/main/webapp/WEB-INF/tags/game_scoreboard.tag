<%--

    Copyright (C) 2016-2023 Code Defenders contributors

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
<%@ tag import="org.codedefenders.database.TestDAO" %>
<%@ tag import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%@ tag import="org.codedefenders.model.Player" %>
<%@ tag import="org.codedefenders.database.MutantDAO" %>
<%@ tag import="java.util.Map" %>
<%@ tag import="java.util.List" %>
<%@ tag import="org.codedefenders.model.UserEntity" %>

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

<div class="w-100 d-flex justify-content-center align-content-center gap-3 mb-3">
    <span class="fg-attacker fs-1 text-end">
        <%=
        mutantScores.getOrDefault(-1, zeroDummyScore).getTotalScore() +
                mutantScores.getOrDefault(-2, zeroDummyScore).getTotalScore()
        %>
    </span>
    <img alt="Code Defenders Logo" style="width: 4rem;" src="${pageContext.request.contextPath}/images/logo.png"/>
    <span class="fg-defender fs-1 text-start">
        <%=testScores.getOrDefault(-1, zeroDummyScore).getTotalScore()%>
    </span>
</div>
<table class="scoreboard table m-0 text-white">

    <tr class="attacker header">
        <th>Attackers</th>
        <th>Mutants</th>
        <th>Alive / Killed / Equivalent</th>
        <th>Duels Won / Lost / Ongoing</th>
        <th>Total Points</th>
    </tr>
    <%
        for (Player attacker : attackers) {
            int playerId = attacker.getId();
            UserEntity attackerUser = attacker.getUser();
            // TODO Phil 09/08/19: Isn't this fixed by now? Why is this hack still in place?
            // Does system attacker submitted any mutant?
            // TODO #418: we use UserId instead of PlayerID because there's a bug in the logic which initialize the game.
            // For system generated mutants,  mutant.playerID == userID, which is wrong...
            if (attackerUser.getId() == Constants.DUMMY_ATTACKER_USER_ID &&
                    MutantDAO.getMutantsByGameAndUser(scoreboard.getGameId(), attackerUser.getId()).isEmpty()) {
                continue;
            }

            PlayerScore mutantsScore = mutantScores.getOrDefault(playerId, zeroDummyScore);
            PlayerScore testsScore = testScores.getOrDefault(playerId, zeroDummyScore);
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
        <td>Attacking Team</td>
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
        <th>Defenders</th>
        <th>Tests</th>
        <th>Mutants Killed</th>
        <th>Duels Won / Lost / Ongoing</th>
        <th>Total Points</th>
    </tr>
    <%
        for (Player defender : defenders) {
            int playerId = defender.getId();
            UserEntity defenderUser = defender.getUser();

            if (defenderUser.getId() == Constants.DUMMY_DEFENDER_USER_ID
                    && TestDAO.getTestsForGameAndUser(scoreboard.getGameId(), defenderUser.getId()).isEmpty()) {
                continue;
            }

            PlayerScore testsScore = testScores.getOrDefault(playerId, zeroDummyScore);
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
        <td>Defending Team</td>
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
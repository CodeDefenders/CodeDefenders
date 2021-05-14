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
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.database.TestDAO" %>
<%@ page import="org.codedefenders.database.MutantDAO" %>
<%@ page import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.Map" %>

<jsp:useBean id="scoreboard" class="org.codedefenders.beans.game.ScoreboardBean" scope="request"/>
<%
    Map<Integer, PlayerScore> mutantScores = scoreboard.getMutantsScores();
    Map<Integer, PlayerScore> testScores = scoreboard.getTestScores();

    // Those return the PlayerID not the UserID
    final List<Player> attackers = scoreboard.getAttackers();
    final List<Player> defenders = scoreboard.getDefenders();

    PlayerScore zeroDummyScore = new PlayerScore(-1);
    zeroDummyScore.setMutantKillInformation("0/0/0");
    zeroDummyScore.setDuelInformation("0/0/0");
%>

<link href="${pageContext.request.contextPath}/css/game_scoreboard.css" rel="stylesheet">

<div id="scoreboard" class="modal fade" tabindex="-1">
    <div class="modal-dialog" style="max-width: 60rem;">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Scoreboard</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="w-100 d-flex justify-content-center align-content-center gap-3 mb-3">
                    <span class="fg-attacker fs-1 text-end">
                        <%=mutantScores.getOrDefault(-1, zeroDummyScore).getTotalScore() +
                           mutantScores.getOrDefault(-2, zeroDummyScore).getTotalScore()%>
                    </span>
                    <img alt="Code Defenders Logo" style="width: 4rem;" src="${pageContext.request.contextPath}/images/logo.png"/>
                    <span class="fg-defender fs-1 text-start">
                        <%=testScores.getOrDefault(-1, zeroDummyScore).getTotalScore()%>
                    </span>
                </div>
                <table class="scoreboard table table-responsive m-0 text-white">

                    <tr class="attacker header">
                        <th>Attackers</th>
                        <th>Mutants</th>
                        <th>Alive / Killed / Equivalent</th>
                        <th>Duels Won / Lost / Ongoing</th>
                        <th>Total Points</th>
                    </tr>
                    <% if (attackers.isEmpty()) { %>
                        <tr class="attacker">
                            <td colspan="4"></td>
                        </tr>
                    <% } %>
                    <%
                        for (Player attacker : attackers) {
                            int playerId = attacker.getId();
                            User attackerUser = attacker.getUser();

                            if (attackerUser.getId() == Constants.DUMMY_ATTACKER_USER_ID
                                    && MutantDAO.getMutantsByGameAndUser(scoreboard.getGameId(), attackerUser.getId()).isEmpty()) {
                               continue;
                            }
                    %>
                        <tr class="attacker">
                            <td><%=attackerUser.getUsername()%></td>
                            <td><%=mutantScores.getOrDefault(playerId, zeroDummyScore).getQuantity()%></td>
                            <td><%=mutantScores.getOrDefault(playerId, zeroDummyScore).getMutantKillInformation()%></td>
                            <!-- Equivalence duels -->
                            <td><%=mutantScores.getOrDefault(playerId, zeroDummyScore).getDuelInformation()%></td>
                            <!-- Total Points -->
                            <td>
                                <%=mutantScores.getOrDefault(playerId, zeroDummyScore).getTotalScore() +
                                   testScores.getOrDefault(playerId, zeroDummyScore).getTotalScore()%>
                            </td>
                        </tr>
                    <%
                        }
                    %>
                    <tr class="attacker total">
                        <td>Attacking Team</td>
                        <td><%=mutantScores.getOrDefault(-1, zeroDummyScore).getQuantity()%></td>
                        <td><%=mutantScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation()%></td>
                        <!-- Equivalence duels -->
                        <td><%=mutantScores.getOrDefault(-1, zeroDummyScore).getDuelInformation()%></td>
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
                    <% if (defenders.isEmpty()) { %>
                        <tr class="defender">
                            <td colspan="5"></td>
                        </tr>
                    <% } %>
                    <%
                        for (Player defender : defenders) {
                            int playerId = defender.getId();
                            User defenderUser = defender.getUser();

                            if (defenderUser.getId() == Constants.DUMMY_DEFENDER_USER_ID
                                    && TestDAO.getTestsForGameAndUser(scoreboard.getGameId(), defenderUser.getId()).isEmpty()) {
                                continue;
                            }
                    %>
                        <tr class="defender">
                            <td><%=defenderUser.getUsername()%></td>
                            <td><%=testScores.getOrDefault(playerId, zeroDummyScore).getQuantity()%></td>
                            <td><%=testScores.getOrDefault(playerId, zeroDummyScore).getMutantKillInformation()%></td>
                            <!-- Equivalence duels -->
                            <td><%=testScores.getOrDefault(playerId, zeroDummyScore).getDuelInformation()%></td>
                            <td><%=testScores.getOrDefault(playerId, zeroDummyScore).getTotalScore()%></td>
                        </tr>
                    <%
                        }
                    %>
                    <tr class="defender total">
                        <td>Defending Team</td>
                        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getQuantity()%></td>
                        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation()%></td>
                        <!-- Equivalence duels -->
                        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getDuelInformation()%></td>
                        <td><%=testScores.getOrDefault(-1, zeroDummyScore).getTotalScore()%></td>
                    </tr>
                </table>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>

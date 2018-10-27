<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<%@ page import="java.util.HashMap" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%

    HashMap mutantScores = game.getMutantScores();


    HashMap testScores = game.getTestScores();

    int[] attackers = game.getAttackerIds();

    int[] defenders = game.getDefenderIds();
%>
<div id="scoreboard" class="modal fade" role="dialog" style="z-index: 10000; position: absolute;">
    <div class="modal-dialog">
        <!-- Modal content-->
        <div class="modal-content" style="z-index: 10000; position: absolute; width: 100%; left:0%;">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Scoreboard</h4>
            </div>
            <div class="modal-body">
                <div class="scoreBanner">
                    <span class="attackerTotal"><%
                        int ts = 0;
                        if (mutantScores.containsKey(-1) && mutantScores.get(-1) != null){
                            ts += ((PlayerScore)mutantScores.get(-1)).getTotalScore();
                        }
                        if (testScores.containsKey(-2) && testScores.get(-2) != null){
                            ts += ((PlayerScore)testScores.get(-2)).getTotalScore();
                        } %>
                        <%= ts %>
                    </span><img class="logo" href="<%=request.getContextPath() %>/" src="images/logo.png"/><span class="defenderTotal">
                    <% ts = 0;
                        if (testScores.containsKey(-1) && testScores.get(-1) != null){
                                ts += ((PlayerScore)testScores.get(-1)).getTotalScore(); %>
                        <% } %>
                        <%= ts %>
                </span>
                </div>
                <table class="scoreboard">
                    <tr class="attacker header"><th>Attackers</th><th>Mutants</th><th>Alive / Killed / Equivalent</th><th>Total Points</th></tr>
                    <%
                    int total = 0;
                    for (int index = 0; index < attackers.length; index++){
                        int i = attackers[index];
                        if (i == -1){
                            continue;
                        }
                        User aUser = DatabaseAccess.getUserFromPlayer(i);
                        total = 0;
                        int counter = 0;
                        %>
                        <tr class="attacker"><td>
                                <%=aUser.getUsername()%>
                            </td>
                            <td><%
                                if (mutantScores.containsKey(i) && mutantScores.get(i) != null){ %>
                                <%= ((PlayerScore)mutantScores.get(i)).getQuantity() %>
                                <% } else { %>
                                0
                                <% } %></td>
                            <td>
                                <%
                                    if (mutantScores.containsKey(i) && mutantScores.get(i) != null){%>
                                <%= ((PlayerScore)mutantScores.get(i)).getAdditionalInformation() %>
                                <% } else { %>
                                    0 / 0 / 0
                                <% } %>
                            </td>
                            <td>
                                <%
                                if (mutantScores.containsKey(i) && mutantScores.get(i) != null){
                                    total += ((PlayerScore)mutantScores.get(i)).getTotalScore(); %>
                            <% }
                                if (testScores.containsKey(i) && testScores.get(i) != null){
                                    total += ((PlayerScore)testScores.get(i)).getTotalScore(); %>
                            <% } %>
                                <%= total %>
                            </td>
                        </tr>
                <%
                    }
                    total = 0;

                    if (attackers.length == 0){
                %><tr class="attacker"><td colspan="4"></td></tr><%
                    }
                %>
                    <tr class="attacker header"><td>
                        Attacking Team
                    </td>
                        <td><%
                            if (mutantScores.containsKey(-1) && mutantScores.get(-1) != null){ %>
                            <%= ((PlayerScore)mutantScores.get(-1)).getQuantity() %>
                            <% } else { %>
                            0
                            <% } %></td>
                        <td>
                            <%
                                if (mutantScores.containsKey(-1) && mutantScores.get(-1) != null){%>
                            <%= ((PlayerScore)mutantScores.get(-1)).getAdditionalInformation() %>
                            <% } else { %>
                            0 / 0 / 0
                            <% } %>
                        </td>
                        <td>
                            <%
                                if (mutantScores.containsKey(-1) && mutantScores.get(-1) != null){
                                    total += ((PlayerScore)mutantScores.get(-1)).getTotalScore(); %>
                            <% } else { %>
                            0
                            <% }
                                if (testScores.containsKey(-2) && testScores.get(-2) != null){
                                    total += ((PlayerScore)testScores.get(-2)).getTotalScore(); %>
                            <% } %>
                            <%= total %>
                        </td>
                    </tr>
                    <tr class="defender header"><th>Defenders</th><th>Tests</th><th>Mutants Killed</th><th>Total Points</th></tr>
                    <%
                        for (int index = 0; index < defenders.length; index++){
                            int i = defenders[index];
                            if (i == -1){
                                continue;
                            }
                            User dUser = DatabaseAccess.getUserFromPlayer(i);
                            total = 0;
                    %>
                    <tr class="defender"><td>
                        <%=dUser.getUsername()%>
                    </td>
                        <td><%
                            if (testScores.containsKey(i) && testScores.get(i) != null){ %>
                                <%= ((PlayerScore)testScores.get(i)).getQuantity() %>

                            <% } else { %>
                                0 <% } %></td>
                        <td><%
                            if (testScores.containsKey(i) && testScores.get(i) != null){
                                    total += ((PlayerScore)testScores.get(i)).getTotalScore(); %>
                            <%= ((PlayerScore)testScores.get(i)).getAdditionalInformation()%>
                            <% } else { %>
                            0 <% } %></td>
                        <td>
                            <%= total %>
                        </td>
                    </tr>
                    <%
                        }
                        total = 0;

                        if (defenders.length == 0){
                            %><tr class="defender"><td colspan="4"></td></tr><%
                        }
                    %>
                    <tr class="defender header"><td>
                        Defending Team
                    </td>
                        <td><%
                            if (testScores.containsKey(-1) && testScores.get(-1) != null){ %>
                            <%= ((PlayerScore)testScores.get(-1)).getQuantity() %>

                            <% } else { %>
                            0 <% } %></td>
                        <td><%
                            if (testScores.containsKey(-1) && testScores.get(-1) != null){
                                total += ((PlayerScore)testScores.get(-1)).getTotalScore(); %>
                            <%= ((PlayerScore)testScores.get(-1)).getAdditionalInformation()%>
                            <% } else { %>
                            0 <% } %></td>
                        <td>
                            <%= total %>
                        </td>
                    </tr>
                    </table>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
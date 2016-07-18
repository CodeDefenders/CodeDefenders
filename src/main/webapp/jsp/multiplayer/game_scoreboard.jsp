<%@ page import="java.util.HashMap" %>
<%@ page import="org.codedefenders.DatabaseAccess" %>
<%@ page import="org.codedefenders.User" %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%

    HashMap mutantScores = mg.getMutantScores();


    HashMap testScores = mg.getTestScores();

    int[] attackers = mg.getAttackerIds();

    int[] defenders = mg.getDefenderIds();
%>
<div id="scoreboard" class="modal fade" role="dialog">
    <div class="modal-dialog">
        <!-- Modal content-->
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Scoreboard</h4>
            </div>
            <div class="modal-body">
                <div class="scoreBanner">
                    <span class="attackerTotal"><%
                        int ts = 0;
                        if (mutantScores.containsKey(-1) && mutantScores.get(-1) != null){
                            ts += ((MultiplayerGame.PlayerScore)mutantScores.get(-1)).getTotalScore();
                        }
                        if (testScores.containsKey(-2) && testScores.get(-2) != null){
                            ts += ((MultiplayerGame.PlayerScore)testScores.get(-2)).getTotalScore();
                        } %>
                        <%= ts %>
                    </span><img class="logo" href="/" src="images/logo.png"/><span class="defenderTotal">
                    <% ts = 0;
                        if (testScores.containsKey(-1) && testScores.get(-1) != null){
                                ts += ((MultiplayerGame.PlayerScore)testScores.get(-1)).getTotalScore(); %>
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
                                <%=aUser.username%>
                            </td>
                            <td><%
                                if (mutantScores.containsKey(i) && mutantScores.get(i) != null){ %>
                                <%= ((MultiplayerGame.PlayerScore)mutantScores.get(i)).getQuantity() %>
                                <% } else { %>
                                0
                                <% } %></td>
                            <td>
                                <%
                                    if (mutantScores.containsKey(i) && mutantScores.get(i) != null){%>
                                <%= ((MultiplayerGame.PlayerScore)mutantScores.get(i)).getAdditionalInformation() %>
                                <% } else { %>
                                    0, 0, 0
                                <% } %>
                            </td>
                            <td>
                                <%
                                if (mutantScores.containsKey(i) && mutantScores.get(i) != null){
                                    total += ((MultiplayerGame.PlayerScore)mutantScores.get(i)).getTotalScore(); %>
                            <% } else { %>
                                  0
                            <% }
                                if (testScores.containsKey(i) && testScores.get(i) != null){
                                    total += ((MultiplayerGame.PlayerScore)testScores.get(i)).getTotalScore(); %>
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
                            <%= ((MultiplayerGame.PlayerScore)mutantScores.get(-1)).getQuantity() %>
                            <% } else { %>
                            0
                            <% } %></td>
                        <td>
                            <%
                                if (mutantScores.containsKey(-1) && mutantScores.get(-1) != null){%>
                            <%= ((MultiplayerGame.PlayerScore)mutantScores.get(-1)).getAdditionalInformation() %>
                            <% } else { %>
                            0, 0, 0
                            <% } %>
                        </td>
                        <td>
                            <%
                                if (mutantScores.containsKey(-1) && mutantScores.get(-1) != null){
                                    total += ((MultiplayerGame.PlayerScore)mutantScores.get(-1)).getTotalScore(); %>
                            <% } else { %>
                            0
                            <% }
                                if (testScores.containsKey(-2) && testScores.get(-2) != null){
                                    total += ((MultiplayerGame.PlayerScore)testScores.get(-2)).getTotalScore(); %>
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
                        <%=dUser.username%>
                    </td>
                        <td><%
                            if (testScores.containsKey(i) && testScores.get(i) != null){ %>
                                <%= ((MultiplayerGame.PlayerScore)testScores.get(i)).getQuantity() %>

                            <% } %></td>
                        <td><%
                            if (testScores.containsKey(i) && testScores.get(i) != null){
                                    total += ((MultiplayerGame.PlayerScore)testScores.get(i)).getTotalScore(); %>
                            <%= ((MultiplayerGame.PlayerScore)testScores.get(i)).getAdditionalInformation()%>
                            <% } %></td>
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
                            <%= ((MultiplayerGame.PlayerScore)testScores.get(-1)).getQuantity() %>

                            <% } %></td>
                        <td><%
                            if (testScores.containsKey(-1) && testScores.get(-1) != null){
                                total += ((MultiplayerGame.PlayerScore)testScores.get(-1)).getTotalScore(); %>
                            <%= ((MultiplayerGame.PlayerScore)testScores.get(-1)).getAdditionalInformation()%>
                            <% } %></td>
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
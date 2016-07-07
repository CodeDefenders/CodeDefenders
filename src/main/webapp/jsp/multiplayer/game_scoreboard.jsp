<%@ page import="java.util.HashMap" %><%

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
                <table class="scoreboard">
                    <tr class="attacker header"><th>Attackers</th><th>Mutation+Equivalence Points</th><th>Total Points</th></tr>
                    <%
                    for (int index = 0; index < attackers.length; index++){
                        int i = attackers[index];
                        int total = 0;
                        %>
                        <tr class="attacker"><td>
                                <%=i%>
                            </td>
                            <td><%
                                if (mutantScores.containsKey(i) && mutantScores.get(i) != null){
                                    total += ((Integer)mutantScores.get(i)).intValue(); %>
                                <%= mutantScores.get(i)%>
                                <% } else { %>
                                    0
                                <% }

                                  if (testScores.containsKey(i) && testScores.get(i) != null){
                                    total += ((Integer)testScores.get(i)).intValue(); %>
                                    +<%=testScores.get(i) %>
                                    <% } %></td>
                            <td>
                                <%= total %>
                            </td>
                        </tr>
                <%
                    }
                %>
                    <%
                        for (int index = 0; index < defenders.length; index++){
                            int i = defenders[index];
                            int total = 0;
                    %>
                    <tr class="defender header"><th>Defenders</th><th></th><th>Total Points</th></tr>
                    <tr class="defender"><td>
                        <%=i%>
                    </td>
                        <td><%
                            if (testScores.containsKey(i) && testScores.get(i) != null){
                                    total += ((Integer)testScores.get(i)).intValue(); %>

                            <% } %></td>
                        <td>
                            <%= total %>
                        </td>
                    </tr>
                    <%
                        }
                    %>
                    </table>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
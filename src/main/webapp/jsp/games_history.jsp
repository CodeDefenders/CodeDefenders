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
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.UserMultiplayerGameInfo" %>
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.model.UserMeleeGameInfo" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Game History"); %>

<%
    List<UserMultiplayerGameInfo> games = ((List<UserMultiplayerGameInfo>) request.getAttribute("finishedBattlegroundGames"));
    List<UserMeleeGameInfo> meleeGames = ((List<UserMeleeGameInfo>) request.getAttribute("finishedMeleeGames"));
%>

<%!
    /* Quick fix to get striped tables to display properly without DataTables.
       Later, the tables on this page should be converted to DataTables. */
    int row = 1;
    String oddEven() {
        return row++ % 2 == 0 ? "even" : "odd";
    }
    void resetOddEven() {
        row = 1;
    }
%>

<jsp:include page="/jsp/header_main.jsp"/>

<div class="container">

<h3>Battlegrounds</h3>
<table class="table table-striped table-v-align-middle">
    <thead>
        <tr>
            <th style="width: 3rem;"></th>
            <th style="width: 4rem;">ID</th>
            <th>Owner</th>
            <th>Class</th>
            <th>Attackers</th>
            <th>Defenders</th>
            <th>Level</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
        <% resetOddEven(); %>

        <%
            if (games.isEmpty()) {
        %>

            <tr class="<%=oddEven()%>">
                <td colspan="8">Empty multi-player games history.</td>
            </tr>

        <%
            } else {
                for (UserMultiplayerGameInfo g : games) {
                    int gameId = g.gameId();
                    List<Player> attackers = g.attackers();
                    List<Player> defenders = g.defenders();
                    Map<Integer, PlayerScore> attackerScores = g.getMutantScores();
                    Map<Integer, PlayerScore> defenderScores = g.getTestScores();
        %>

            <tr id="game-<%=gameId%>" class="<%=oddEven()%>">
                <td id="toggle-game-<%=gameId%>" class="toggle-details">
                    <i class="toggle-details-icon fa fa-chevron-right"></i>
                </td>
                <td><%=gameId%></td>
                <td><%= g.creatorName()%></td>
                <td>
                    <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=gameId%>"><%=g.cutAlias()%></a>
                    <div id="modalCUTFor<%=gameId%>" class="modal fade" role="dialog">
                        <div class="modal-dialog">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                    <h4 class="modal-title"><%=g.cutAlias()%></h4>
                                </div>
                                <div class="modal-body">
                                <pre class="readonly-pre"><textarea class="readonly-textarea classPreview"
                                                                    id="sut<%=gameId%>"
                                                                    name="cut<%=g.cutId()%>" cols="80"
                                                                    rows="30"></textarea></pre>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </td>
                <td><%=attackers.size()%></td>
                <td><%=defenders.size()%></td>
                <td><%=g.gameLevel().getFormattedString()%></td>
                <td>
                    <a class="btn btn-sm btn-secondary" id="<%="results_"+gameId%>"
                       href="<%=request.getContextPath() + Paths.BATTLEGROUND_HISTORY%>?gameId=<%=gameId%>">
                        View Results
                    </a>
                </td>
            </tr>

            <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                <td colspan="8">
                    <div class="child-row-wrapper">
                        <table class="child-row-details">
                            <thead>
                                <tr>
                                    <th class="text-end">Attacker</th>
                                    <th class="text-end">Mutants</th>
                                    <th class="text-end">Alive Mutants</th>
                                    <th>Points</th>
                                </tr>
                            </thead>
                            <tbody>
                                <%
                                    if(attackers.isEmpty()){
                                %>
                                    <tr>
                                        <td colspan="4" class="text-center">There are no Attackers.</td>
                                    </tr>
                                <%
                                    } else {
                                        for (Player attacker : attackers) {
                                            int playerId = attacker.getId();
                                            PlayerScore playerScore = attackerScores.get(playerId);
                                            boolean scoresExists = attackerScores.containsKey(playerId) && attackerScores.get(playerId) != null;
                                %>
                                    <tr>
                                        <td>
                                            <%=attacker.getUser().getUsername()%>
                                        </td>
                                        <td class="text-end">
                                            <% if (scoresExists) { %>
                                                <%=playerScore.getQuantity() %>
                                            <% } else { %>
                                                0
                                            <% } %>
                                        </td>
                                        <td class="text-end">
                                            <% if (scoresExists) { %>
                                                <%=playerScore.getMutantKillInformation().split("/")[0]%>
                                            <% } else { %>
                                                0
                                            <% } %>
                                        </td>
                                        <td class="text-end">
                                            <% if (scoresExists) { %>
                                                <%=playerScore.getTotalScore()%>
                                            <% } else { %>
                                                0
                                            <% } %>
                                        </td>
                                    </tr>
                                <%
                                        }
                                    }
                                %>
                            </tbody>
                            <thead>
                                <tr>
                                    <th>Defender</th>
                                    <th class="text-end">Tests</th>
                                    <th class="text-end">Mutants Killed</th>
                                    <th class="text-end">Points</th>
                                </tr>
                            </thead>
                            <tbody>
                                <%
                                    if(defenders.isEmpty()){
                                %>
                                    <tr>
                                        <td colspan="4" class="text-center">There are no Defenders.</td>
                                    </tr>
                                <%
                                    } else {
                                        for (Player defender : defenders) {
                                            int playerId = defender.getId();
                                            PlayerScore playerScore = defenderScores.get(playerId);
                                            boolean scoresExists = defenderScores.containsKey(playerId) && defenderScores.get(playerId) != null;
                                %>
                                    <tr>
                                        <td><%=defender.getUser().getUsername()%></td>
                                        <td class="text-end">
                                            <% if (scoresExists) { %>
                                                <%=playerScore.getQuantity() %>
                                            <% } else { %>
                                                0
                                            <% } %>
                                        </td>
                                        <td class="text-end">
                                            <% if (scoresExists) { %>
                                                <%=playerScore.getMutantKillInformation()%>
                                            <% } else { %>
                                                0
                                            <% } %>
                                        </td>
                                        <td class="text-end">
                                            <% if (scoresExists) { %>
                                                <%=playerScore.getTotalScore()%>
                                            <% } else { %>
                                                0
                                            <% } %>
                                        </td>
                                    </tr>

                                <%
                                        }
                                    }
                                %>
                            </tbody>
                        </table>
                    </div>
                </td>
            </tr>
        <%
                }
            }
        %>
    </tbody>
</table>

<h3 class="mt-4">Melee games</h3>
<table class="table table-striped table-v-align-middle">
    <thead>
        <tr>
            <th style="width: 3rem;"></th>
            <th style="width: 4rem;">ID</th>
            <th>Owner</th>
            <th>Class</th>
            <th>Players</th>
            <th>Level</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
        <% resetOddEven(); %>

            <%
                if (meleeGames.isEmpty()) {
            %>

                <tr class="<%=oddEven()%>">
                    <td colspan="7">Empty multi-player games history.</td>
                </tr>

            <%
                } else {
                    for (UserMeleeGameInfo g : meleeGames) {
                        int gameId = g.gameId();
                        List<Player> players = g.players();
            %>

                <tr id="game-<%=gameId%>" class="<%=oddEven()%>">
                    <td id="toggle-game-<%=gameId%>" class="toggle-details">
                        <i class="toggle-details-icon fa fa-chevron-right"></i>
                    </td>
                    <td><%=gameId%></td>
                    <td><%=g.creatorName()%></td>
                    <td>
                        <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=gameId%>"><%=g.cutAlias()%></a>
                        <div id="modalCUTFor<%=gameId%>" class="modal fade" role="dialog">
                            <div class="modal-dialog">
                                <!-- Modal content-->
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <button type="button" class="close" data-dismiss="modal">&times;</button>
                                        <h4 class="modal-title"><%=g.cutAlias()%></h4>
                                    </div>
                                    <div class="modal-body">
                                    <pre class="readonly-pre"><textarea class="readonly-textarea classPreview"
                                                                        id="sut<%=gameId%>"
                                                                        name="cut<%=g.cutId()%>" cols="80"
                                                                        rows="30"></textarea></pre>
                                    </div>
                                    <div class="modal-footer">
                                        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </td>
                    <td><%=players.size()%></td>
                    <td><%=g.gameLevel().getFormattedString()%></td>
                    <td>
                        <a class="btn btn-sm btn-secondary" id="<%="results_"+gameId%>" href="<%=request.getContextPath() + Paths.MELEE_HISTORY%>?gameId=<%= gameId %>">View Results</a>
                    </td>
                </tr>

                <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                    <td colspan="7">
                        <div class="child-row-wrapper">
                            <table class="child-row-details">
                                <thead>
                                    <tr>
                                        <th>Player</th>
                                        <th class="text-end">Points</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <%
                                        if (players.isEmpty()) {
                                    %>
                                        <tr>
                                            <td colspan="2" class="text-center">There are no Players.</td>
                                        </tr>
                                    <%
                                        } else {
                                            for (Player player : players) {
                                    %>
                                        <tr>
                                            <td><%=player.getUser().getUsername()%></td>
                                            <td class="text-end"><%=player.getPoints()%></td>
                                        </tr>
                                    <%
                                            }
                                        }
                                    %>
                                </tbody>
                            </table>
                        </div>
                    </td>
                </tr>
            <%
                    }
                }
            %>
        </tbody>
    </table>

    <script>
(function () {

    $('.modal').on('shown.bs.modal', function () {
        let codeMirrorContainer = $(this).find(".CodeMirror")[0];
        if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
            codeMirrorContainer.CodeMirror.refresh();
        } else {
            let textarea = $(this).find('textarea')[0];
            let editor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: false,
                readOnly: true,
                mode: "text/x-java",
                autoRefresh: true
            });
            editor.setSize("100%", 500);
            ClassAPI.getAndSetEditorValue(textarea, editor);
        }
    });

    $('table td.toggle-details').on('click', function () {
        let id = '.' + $(this).attr('id');
        if ($(id).is(':visible')) {
            $(this).find('.toggle-details-icon').removeClass('fa-chevron-down');
            $(this).find('.toggle-details-icon').addClass('fa-chevron-right');
            $(id).hide()
        } else {
            $(this).find('.toggle-details-icon').removeClass('fa-chevron-right');
            $(this).find('.toggle-details-icon').addClass('fa-chevron-down');
            $(id).show()
        }
    });

})();
</script>

</div>

<%@ include file="/jsp/footer.jsp" %>

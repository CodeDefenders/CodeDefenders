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

<jsp:include page="/jsp/header_main.jsp"/>

<div class="container">

<h3>Battlegrounds</h3>
<table class="table table-striped table-hover table-responsive table-center">
    <thead>
        <tr>
            <th>ID</th>
            <th>Owner</th>
            <th>Class</th>
            <th>Attackers</th>
            <th>Defenders</th>
            <th>Level</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>

        <%
            if (games.isEmpty()) {
        %>

        <tr><td colspan="100%"> Empty multi-player games history. </td></tr>

        <%
            } else {
                for (UserMultiplayerGameInfo g : games) {
                    int gameId = g.gameId();
                    List<Player> attackers = g.attackers();
                    List<Player> defenders = g.defenders();
                    Map<Integer, PlayerScore> attackerScores = g.getMutantScores();
                    Map<Integer, PlayerScore> defenderScores = g.getTestScores();
        %>

		<tr id="game-<%=gameId%>">
            <td id="toggle-game-<%=gameId%>" class="col-sm-1 toggle-details">
                <span style="margin-right: 5px" class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"> </span><%=gameId%></td>
            <td class="col-sm-2"><%= g.creatorName()%></td>
            <td class="col-sm-2">
                <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=gameId%>"><%=g.cutAlias()%></a>
                <div id="modalCUTFor<%=gameId%>" class="modal fade" role="dialog" style="text-align: left;" >
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
            <td class="col-sm-2"><%= attackers.size()%></td>
            <td class="col-sm-2"><%= defenders.size()%></td>
            <td class="col-sm-2"><%= g.gameLevel().getFormattedString() %></td>
            <td class="col-sm-2">
                <a class="btn btn-sm btn-default" id="<%="results_"+gameId%>" href="<%=request.getContextPath() + Paths.BATTLEGROUND_HISTORY%>?gameId=<%= gameId %>">View Results</a>
            </td>
        <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
            <td colspan="6">
                <table class="table-child-details" style="display: inline; margin-right: 15px">
                    <thead>
                    <tr>
                        <th>
                            Attacker
                        </th>
                        <th>
                            Mutants
                        </th>
                        <th>
                            Alive
                        </th>
                        <th>
                            Points
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if(attackers.isEmpty()){ %>
                    <tr>
                        <td colspan="4">There are no Attackers.</td>
                    </tr>
                    <% } else {
                        for (Player attacker : attackers) {
                            int playerId = attacker.getId();
                            PlayerScore playerScores = attackerScores.get(playerId);
                            boolean scoresExists = attackerScores.containsKey(playerId) && attackerScores.get(playerId) != null;
                    %>
                    <tr>
                        <td>
                            <%=attacker.getUser().getUsername()%>
                        </td>
                        <td>
                            <% if (scoresExists) { %>
                            <%=playerScores.getQuantity() %>
                            <% } else { %>
                            0
                            <% } %>
                        </td>
                        <td>
                            <% if (scoresExists) { %>
                            <%-- Well it is a string ... So split it to get the alive Mutants--%>
                            <%=playerScores.getMutantKillInformation().split("/")[0]%>
                            <% } else { %>
                            0
                            <% } %>
                        </td>
                        <td>
                            <% if (scoresExists) { %>
                            <%=playerScores.getTotalScore()%>
                            <% } else { %>
                            0
                            <% } %>
                        </td>
                    </tr>
                    <% }
                    } %>
                    </tbody>
                </table>
                <table class="table-child-details" style="display: inline; margin-left: 15px">
                    <thead>
                    <tr>
                        <th>
                            Defender
                        </th>
                        <th>
                            Tests
                        </th>
                        <th>
                            Mutants killed
                        </th>
                        <th>
                            Points
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if(defenders.isEmpty()){ %>
                    <tr>
                        <td colspan="4">There are no Defenders.</td>
                    </tr>
                    <% } else { for (Player defender : defenders) {
                        int playerId = defender.getId();
                        PlayerScore playerScores = defenderScores.get(playerId);
                        boolean scoresExists = defenderScores.containsKey(playerId) && defenderScores.get(playerId) != null;
                    %>
                    <tr>
                        <td>
                            <%=defender.getUser().getUsername()%>
                        </td>
                        <td>
                            <% if (scoresExists) { %>
                            <%=playerScores.getQuantity() %>
                            <% } else { %>
                            0
                            <% } %>
                        </td>
                        <td>
                            <% if (scoresExists) { %>
                            <%=playerScores.getMutantKillInformation()%>
                            <% } else { %>
                            0
                            <% } %>
                        </td>
                        <td>
                            <% if (scoresExists) { %>
                            <%=playerScores.getTotalScore()%>
                            <% } else { %>
                            0
                            <% } %>
                        </td>
                    </tr>
                    <% }
                    } %>
                    </tbody>
                </table>
            </td>
        </tr>
        <%
                }
            }
        %>
    </tbody>
</table>

<h3>Melee games</h3>
<table class="table table-striped table-hover table-responsive table-center">
        <thead>
        <tr>
            <th>ID</th>
            <th>Owner</th>
            <th>Class</th>
            <th>Players</th>
            <th>Level</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>

        <%
            if (meleeGames.isEmpty()) {
        %>

        <tr><td colspan="100%"> Empty multi-player games history. </td></tr>

        <%
        } else {
            for (UserMeleeGameInfo g : meleeGames) {
                int gameId = g.gameId();
                List<Player> players = g.players();
        %>

        <tr id="game-<%=gameId%>">
            <td id="toggle-game-<%=gameId%>" class="col-sm-1 toggle-details">
                <span style="margin-right: 5px" class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"> </span><%=gameId%></td>
            <td class="col-sm-2"><%= g.creatorName()%></td>
            <td class="col-sm-2">
                <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=gameId%>"><%=g.cutAlias()%></a>
                <div id="modalCUTFor<%=gameId%>" class="modal fade" role="dialog" style="text-align: left;" >
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
            <td class="col-sm-2"><%= players.size()%></td>
            <td class="col-sm-2"><%= g.gameLevel().getFormattedString() %></td>
            <td class="col-sm-2">
                <a class="btn btn-sm btn-default" id="<%="results_"+gameId%>" href="<%=request.getContextPath() + Paths.MELEE_HISTORY%>?gameId=<%= gameId %>">View Results</a>
            </td>
        <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
            <td colspan="6">
                <table class="table-child-details" style="display: inline; margin-left: 15px">
                    <thead>
                    <tr>
                        <th>
                            Player
                        </th>
                        <th>
                            Points
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if(players.isEmpty()){ %>
                    <tr>
                        <td colspan="2">There are no Players.</td>
                    </tr>
                    <% } else { for (Player player : players) {
                    %>
                    <tr>
                        <td>
                            <%=player.getUser().getUsername()%>
                        </td>
                        <td>
                            <%=player.getPoints()%>
                        </td>
                    </tr>
                    <% }
                    } %>
                    </tbody>
                </table>
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
            $(this).find('span').removeClass('glyphicon-chevron-down');
            $(this).find('span').addClass('glyphicon-chevron-right');
            $(id).hide()
        } else {
            $(this).find('span').removeClass('glyphicon-chevron-right');
            $(this).find('span').addClass('glyphicon-chevron-down');
            $(id).show()
        }
    });

})();
</script>

</div>

<%@ include file="/jsp/footer.jsp" %>

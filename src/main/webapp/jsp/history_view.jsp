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
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.duel.DuelGame" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.List" %>
<% String pageTitle="Game History"; %>
<%@ include file="/jsp/header_main.jsp" %>
<div>
<h3>Duels</h3>
<table class="table table-striped table-hover table-responsive table-paragraphs games-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Class</th>
            <th>Attacker</th>
            <th>Defender</th>
            <th>Level</th>
            <th></th>
        </tr>
    </thead>
    <tbody>

        <%
            String atkName;
            String defName;
            int uid = (Integer)request.getSession().getAttribute("uid");
            int atkId;
            int defId;
            List<DuelGame> games = DatabaseAccess.getHistoryForUser(uid);

            if (!games.isEmpty()) {
                for (DuelGame g : games) {
                    atkId = g.getAttackerId();
                    defId = g.getDefenderId();
                    User attacker = DatabaseAccess.getUserForKey("User_ID", atkId);
                    User defender = DatabaseAccess.getUserForKey("User_ID", defId);
                    atkName = attacker == null ? "-" : attacker.getUsername();
                    defName = defender == null ? "-" : defender.getUsername();
        %>

        <tr id="<%="game_"+g.getId()%>">
            <td class="col-sm-2"><%= g.getId() %></td>
            <td class="col-sm-2"><%= g.getCUT().getAlias() %></td>
            <td class="col-sm-2"><%= atkName %></td>
            <td class="col-sm-2"><%= defName %></td>
            <td class="col-sm-2"><%= g.getLevel().name() %></td>
            <td class="col-sm-2">
                <form id="view" action="<%=request.getContextPath() %>/games" method="post">
                    <input type="hidden" name="formType" value="enterGame">
                    <input type="hidden" name="game" value=<%=g.getId()%>>
                    <button id="<%="results_"+g.getId()%>" type="submit" class="btn btn-sm btn-default">View Scores</button>
                </form>
            </td>
        </tr>

        <%
                }
            } else {
        %>

        <tr><td colspan="100%"> Empty duels history. </td></tr>

        <%  } %>

    </tbody>
</table>

<h3>Battlegrounds</h3>
<table class="table table-striped table-hover table-responsive table-paragraphs games-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Class</th>
            <th>Owner</th>
            <th>Attackers</th>
            <th>Defenders</th>
            <th>Level</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>

        <%
            List<MultiplayerGame> mgames = DatabaseAccess.getFinishedMultiplayerGamesForUser(uid);
            if (!mgames.isEmpty()) {
                for (MultiplayerGame g : mgames) {
                    Role role = g.getRole(uid);
        %>

        <tr id="<%="game_"+g.getId()%>">
            <td class="col-sm-2"><%= g.getId() %></td>
            <td class="col-sm-2"><%= g.getCUT().getAlias() %></td>
            <td class="col-sm-2"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).getUsername() %></td>
            <td class="col-sm-1"><%= g.getAttackerIds().length %></td>
            <td class="col-sm-1"><%= g.getDefenderIds().length %></td>
            <td class="col-sm-2"><%= g.getLevel().name() %></td>
            <td class="col-sm-2">
                <a class="btn btn-sm btn-default" id="<%="results_"+g.getId()%>" href="multiplayer/history?id=<%= g.getId() %>">View Results</a>
            </td>
        </tr>

        <%
                }
            } else {
        %>

        <tr><td colspan="100%"> Empty multi-player games history. </td></tr>

        <%  } %>

    </tbody>
</table>

</div>
</div>

<%@ include file="/jsp/footer.jsp" %>

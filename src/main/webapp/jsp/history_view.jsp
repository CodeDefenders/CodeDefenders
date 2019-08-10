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
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.database.UserDAO" %>
<%@ page import="org.codedefenders.database.MultiplayerGameDAO" %>
<% String pageTitle="Game History"; %>
<%@ include file="/jsp/header_main.jsp" %>
<div>

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
            int userId = (Integer)request.getSession().getAttribute("uid");
            List<MultiplayerGame> mgames = MultiplayerGameDAO.getFinishedMultiplayerGamesForUser(userId);
            if (!mgames.isEmpty()) {
                for (MultiplayerGame g : mgames) {
        %>

        <tr id="<%="game_"+g.getId()%>">
            <td class="col-sm-2"><%= g.getId() %></td>
            <td class="col-sm-2"><%= g.getCUT().getAlias() %></td>
            <td class="col-sm-2"><%= UserDAO.getUserById(g.getCreatorId()).getUsername() %></td>
            <td class="col-sm-1"><%= g.getAttackerIds().length %></td>
            <td class="col-sm-1"><%= g.getDefenderIds().length %></td>
            <td class="col-sm-2"><%= g.getLevel().name() %></td>
            <td class="col-sm-2">
                <a class="btn btn-sm btn-default" id="<%="results_"+g.getId()%>" href="<%=request.getContextPath() + Paths.BATTLEGROUND_HISTORY%>?gameId=<%= g.getId() %>">View Results</a>
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

<%@ include file="/jsp/footer.jsp" %>

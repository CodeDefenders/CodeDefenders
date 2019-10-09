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
<% pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>
</div></div></div></div></div>

<%
    {
        int gameId = (int) request.getAttribute("gameId");
%>

<div class="game-container">
    <nav class="nest" style="width: 100%; margin-left: 0; margin-right: auto;">
        <div class="crow fly">
            <% String userRole = role.getFormattedString(); %>
            <div><h2 style="margin-top: 7px"><%= "Game " + game.getId() + " (" + userRole + ")" %>
            </h2></div>

            <%  int userIdCurrent = ((Integer) session.getAttribute("uid"));
                if (game.getCreatorId() == userIdCurrent) { %>
            <div class="admin-panel col-md-12">
                <% if (game.getState() == GameState.ACTIVE) { %>
                <form id="adminEndBtn" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post" style="display: inline-block;">
                    <button type="submit" class="btn btn-primary btn-game btn-left" id="endGame" form="adminEndBtn">End Game</button>
                    <input type="hidden" name="formType" value="endGame">
                    <input type="hidden" name="gameId" value="<%= game.getId() %>" />
                </form>
                <% } else if (game.getState() == GameState.CREATED) { %>
                <form id="adminStartBtn" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post" style="display: inline-block;">
                    <button type="submit" class="btn btn-primary btn-game" id="startGame" form="adminStartBtn">Start Game</button>
                    <input type="hidden" name="formType" value="startGame">
                    <input type="hidden" name="gameId" value="<%= game.getId() %>" />
                </form>
                <% } %>
            </div>

            <% } %>

            <%-- This bar shows the possible interactions at game level --%>
            <div class="container">

                <%-- Make those interactive later ! For the moment they are just place holders ! --%>
                <%-- Probably use some DISABLE CSS or something  --%>
                <%--<a id="notification-chat" class="notification-icon glyphicon glyphicon-envelope"></a>--%>
                <%--<a id="notification-game" class="notification-icon glyphicon glyphicon-bell"></a>--%>

                <a href="#" class="btn btn-diff" id="btnScoringTooltip" data-toggle="modal"
                   data-target="#scoringTooltip"
                   style="color: black; font-size: 18px; padding: 5px;">
                    <span class="glyphicon glyphicon-question-sign"></span>
                </a>
                <a href="<%=request.getContextPath() + Paths.PROJECT_EXPORT%>?gameId=<%=gameId%>"
                   title="Export as a Gradle project to import into an IDE."
                   class="btn btn-default btn-diff" id="btnProjectExport">
                    Export
                </a>
                <a href="#" class="btn btn-default btn-diff" id="btnScoreboard" data-toggle="modal"
                   data-target="#scoreboard">Scoreboard</a>
                <a href="#" class="btn btn-default btn-diff" id="btnFeedback" data-toggle="modal"
                   data-target="#playerFeedback">
                    Feedback
                </a>
            </div>
        </div>
    </nav>
    <div class="clear"></div>
<%
    }
%>

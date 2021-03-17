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
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.util.Paths" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    Role role = game.getRole(login.getUserId());
    int gameId = game.getId();
%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Game " + game.getId() + " (" + role.getFormattedString() + ")"); %>

<jsp:include page="/jsp/header_main.jsp"/>

<div id="game-container" class="container"> <%-- closed in footer --%>
    <div class="row">
            <% if (game.getCreatorId() == login.getUserId()) { %>
            <div class="admin-panel col-md-6">
                <% if (game.getState() == GameState.ACTIVE) { %>
                <form id="adminEndBtn" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>"
                      method="post" style="display: inline-block;">
                    <button type="submit" class="btn btn-primary btn-bold" id="endGame" form="adminEndBtn">End
                        Game
                    </button>
                    <input type="hidden" name="formType" value="endGame">
                    <input type="hidden" name="gameId" value="<%= game.getId() %>"/>
                </form>
                <% } else if (game.getState() == GameState.CREATED) { %>
                <form id="adminStartBtn" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>"
                      method="post" style="display: inline-block;">
                    <button type="submit" class="btn btn-primary btn-bold" id="startGame" form="adminStartBtn">Start
                        Game
                    </button>
                    <input type="hidden" name="formType" value="startGame">
                    <input type="hidden" name="gameId" value="<%= game.getId() %>"/>
                </form>
                <% } %>
            </div>

            <% } %>

            <%-- This bar shows the possible interactions at game level --%>
            <div class="col-md-5" style="float: right;">

                <a href="#" class="btn pull-right" id="btnScoringTooltip" data-toggle="modal"
                   data-target="#scoringTooltip"
                   style="color: black; font-size: 18px; padding: 5px;">
                    <span class="glyphicon glyphicon-question-sign"></span>
                </a>


                <a href="#" class="btn btn-default pull-right" id="btnScoreboard" data-toggle="modal"
                   data-target="#scoreboard">Scoreboard
                </a>
                <a href="#" class="btn btn-default pull-right" id="btnHistory" data-toggle="modal"
                   data-target="#history">History
                </a>


                <a href="<%=request.getContextPath() + Paths.PROJECT_EXPORT%>?gameId=<%=gameId%>"
                   title="Export as a Gradle project to import into an IDE."
                   class="btn btn-default pull-right" id="btnProjectExport">
                    Export
                </a>
                <a href="#" class="btn btn-default pull-right" id="btnFeedback" data-toggle="modal"
                   data-target="#playerFeedback">
                    Feedback
                </a>
            </div>
    </div>

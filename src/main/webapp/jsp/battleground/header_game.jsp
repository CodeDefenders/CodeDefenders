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
<% pageTitle = null;
%>
<%@ include file="/jsp/header_main.jsp" %>
</div></div></div></div></div>
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.duel.DuelGame" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="static org.codedefenders.game.GameState.ACTIVE" %>

<div class="game-container">
    <nav class="nest" style="width: 90%; margin-left: auto; margin-right: auto;">
        <div class="crow fly">
            <div class="col-md-3"><h2><%= "Game " + game.getId() %>
            </h2></div>
            <div class="col-md-6" style="text-align: center">
                <h2>
                    <%
                        switch (role.toString()) {
                            case "CREATOR": %>
                    You're the creator of this game.
                    <%
                            break;
                        case "ATTACKER": %>
                    Attack the class!
                    <%
                            break;
                        case "DEFENDER": %>
                    Defend your class!
                    <%
                                break;
                        }
                    %>
                </h2>
            </div>
            <div>
                <a href="#" class="btn btn-diff" id="btnScoringTooltip" data-toggle="modal"
                   data-target="#scoringTooltip"
                   style="color: black; font-size: 18px; padding: 5px;">
                    <span class="glyphicon glyphicon-question-sign"></span>
                </a>
                <a href="#" class="btn btn-default btn-diff" id="btnScoreboard" data-toggle="modal"
                   data-target="#scoreboard">Show Scoreboard</a>
                <a href="#" class="btn btn-default btn-diff" id="btnFeedback" data-toggle="modal"
                   data-target="#playerFeedback">
                    Feedback
                </a>
            </div>
        </div>
    </nav>
    <div class="clear"></div>
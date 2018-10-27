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
<% String pTitle = pageTitle;
pageTitle = null;
%>
<%@ include file="/jsp/header_main.jsp" %>
</div></div></div></div></div>
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.*" %>
<%@ page import="static org.codedefenders.game.GameState.ACTIVE" %>
<%@ page import="static org.codedefenders.game.GameState.FINISHED" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<% DuelGame game = (DuelGame) session.getAttribute("game"); %>

<div class="game-container"><h2 class="full-width page-title" style="text-align: center"><%= pTitle %></h2>
<nav>
    <div class="container-fluid">
        <%
            int uid = (Integer) session.getAttribute("uid");
            String turnMsg = "Waiting";
            String labelType = "label-default";
            if (game.getState().equals(ACTIVE)) {
                if ((uid==game.getAttackerId() && game.getActiveRole().equals(Role.ATTACKER)) ||
                        (uid==game.getDefenderId() && game.getActiveRole().equals(Role.DEFENDER))) {
                    turnMsg = "Your turn";
                    labelType = "label-primary";
                }
            } else if (game.getState().equals(FINISHED)) {
                turnMsg = "Finished";
            }

            List<Mutant> mutants = game.getMutants();

            for (Mutant m : mutants){
                m.getLines();
            }

            String loggedName = (String) request.getSession().getAttribute("username");
            String atkName = DatabaseAccess.getUserForKey("User_ID", game.getAttackerId()).getUsername();
            String defName = DatabaseAccess.getUserForKey("User_ID", game.getDefenderId()).getUsername();
            if (atkName.equals(loggedName))
                atkName = "you";
            else
                defName = "you";
        %>

    <div class="row">
            <ul class="nav navbar-nav" aria-label="Vehicle Models Available:">
                <li class="navbar-text">Game ID<br><%= game.getId() %></li>
                <li class="navbar-text">Score<br>ATK (<%= atkName %>): <%= game.getAttackerScore() %> | DEF (<%= defName %>): <%= game.getDefenderScore() %></li>
                <li class="navbar-text">Round<br><%= game.getCurrentRound() %> of <%= game.getFinalRound() %></li>
                <li class="navbar-text">Mutants Alive<br><%= game.getAliveMutants().size() %></li>
                <li class="navbar-text">Status<br>
                    <span class="label <%=labelType%> turn-badge"><%=turnMsg%></span>
                </li>
            </ul>
    </div>
</nav>
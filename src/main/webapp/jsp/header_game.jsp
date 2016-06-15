<% String pTitle = pageTitle;
pageTitle = null;
%>
<%@ include file="/jsp/header.jsp" %>
</div></div></div></div></div>
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.Test" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.Constants" %>
<%@ page import="org.codedefenders.DatabaseAccess" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.GameClass" %>
<%@ page import="static org.codedefenders.Game.State.ACTIVE" %>
<div class="game-container"><h2 class="full-width page-title" style="text-align: center"><%= pTitle %></h2>
<nav>
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1" aria-expanded="false">
            </button>
            <a class="navbar-brand" href="/">
                Game Stats:
            </a>
        </div>
        <div class= "collapse navbar-collapse" id="navbar-collapse-1">
            <ul class="nav navbar-nav navbar-left">
                <li class="navbar-text">Game ID: <%= game.getId() %></li>
                <li class="navbar-text">ATK: <%= game.getAttackerScore() %> | DEF: <%= game.getDefenderScore() %></li>
                <li class="navbar-text">Round <%= game.getCurrentRound() %> of <%= game.getFinalRound() %></li>
                <% if (game.getAliveMutants().size() == 1) {%>
                <li class="navbar-text">1 Mutant Alive</li>
                <% } else {%>
                <li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants Alive</li>
                <% }%>
                <li class="navbar-text">
                    <% if (game.getState().equals(ACTIVE) && game.getActiveRole().equals(Game.Role.ATTACKER)) {%>
                    <span class="label label-primary turn-badge">Your turn</span>
                    <% } else { %>
                    <span class="label label-default turn-badge">Waiting</span>
                    <% } %>
                </li>
            </ul>
        </div>
    </div>
</nav>
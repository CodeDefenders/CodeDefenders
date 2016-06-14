<% String pTitle = pageTitle;
pageTitle = "In Game";
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
<%@ page import="org.codedefenders.multiplayer.MultiplayerMutant" %>
<%@ page import="static org.codedefenders.Game.State.ACTIVE" %>
<h2 class="full-width page-title"><%= pTitle %></h2>
<div class="game-container">
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
                <li class="navbar-text">Game ID: <%= mg.getId() %></li>
                <% if (mg.getAliveMutants().size() == 1) {%>
                <li class="navbar-text">1 Mutant Alive</li>
                <% } else {%>
                <li class="navbar-text"><%= mg.getAliveMutants().size() %> Mutants Alive</li>
                <% }%>
                <li class="navbar-text">
                    <span class="label label-primary turn-badge">Your turn</span>
                </li>
            </ul>
        </div>
    </div>
</nav>
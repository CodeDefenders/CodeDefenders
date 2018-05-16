<% pageTitle = "Game " + mg.getId();
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
        <div style="text-align: left">
            <h3><%= role %>::<%= mg.getState().toString() %></h3>
        </div>
        <div style="text-align: center"><h1><%= mg.getCUT().getName() %></h1></div>
        <div>
            <a href="#" class="btn btn-diff" id="btnScoringTooltip" data-toggle="modal" data-target="#scoringTooltip"
               style="color: black; font-size: 18px; padding: 5px;">
            <span class="glyphicon glyphicon-question-sign"></span>
            </a>
            <a href="#" class="btn btn-default btn-diff" id="btnScoreboard" data-toggle="modal" data-target="#scoreboard">Show Scoreboard</a>
            <a href="#" class="btn btn-default btn-diff" id="btnFeedback" data-toggle="modal" data-target="#playerFeedback">
                Feedback
            </a>
        </div>
    </div>
</nav>
<div class="clear"></div>

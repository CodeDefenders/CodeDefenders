<% String pTitle = pageTitle;
pageTitle = null;
%>
<%@ include file="/jsp/header.jsp" %>
</div></div></div></div></div>
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.GameClass" %>
<%@ page import="static org.codedefenders.AbstractGame.State.ACTIVE" %>
<%@ page import="org.codedefenders.*" %>
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
                        }
                    %>
                    <span class="label <%=labelType%> turn-badge"><%=turnMsg%></span>
                </li>
            </ul>
        </div>
    </div>
</nav>
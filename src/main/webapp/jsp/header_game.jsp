<% String pTitle = pageTitle;
pageTitle = null;
%>
<%@ include file="/jsp/header.jsp" %>
</div></div></div></div></div>
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.*" %>
<%@ page import="static org.codedefenders.GameState.ACTIVE" %>
<%@ page import="static org.codedefenders.GameState.FINISHED" %>
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
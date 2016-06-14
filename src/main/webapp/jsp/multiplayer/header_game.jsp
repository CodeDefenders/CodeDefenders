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
<%@ page import="org.codedefenders.multiplayer.MultiplayerMutant" %>
<%@ page import="static org.codedefenders.Game.State.ACTIVE" %>
<div class="game-container">
<nav class="nest" style="width: 80%; margin-left: auto; margin-right: auto;">
    <div class="crow fly">
        <div style="text-align: left">
            <h3><%= p %></h3>
        </div>
        <div style="text-align: center"><h1><%= mg.getCUT().getName() %></h1></div>
        <div style="text-align: right"><h3>
                <% if (mg.getAliveMutants().size() == 1) {%>
                    1 Mutant Alive
                <% } else {%>
                    <%= mg.getAliveMutants().size() %> Mutants Alive
                <% }%></h3>
        </div>
    </div>
</nav>
<div class="clear"></div>

<% String pTitle = pageTitle;
pageTitle = null;
%>
<%@ include file="/jsp/header.jsp" %>
</div></div></div></div></div>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<div class="game-container"><h2 class="full-width page-title" style="text-align: center"><%= pTitle %></h2>
<nav>
    <div class="container-fluid">
        <%
            int uid = (Integer) session.getAttribute("uid");
            String storyMode;
            if (puzzle.getStoryMode().equals(PuzzleMode.ATTACKER)) {
                storyMode = "Attacker";
            } else if (puzzle.getStoryMode().equals(PuzzleMode.DEFENDER)){
                storyMode = "Defender";
            } else {
                storyMode = "ERROR";
            }
        %>
    <div class="row">
        <ul class="nav navbar-nav">
            <li class="navbar-text"><b>Puzzle</b><br><%=puzzle.getLevelNum()%> - <%=puzzle.getPuzzle()%></li>
            <li class="navbar-text"><b>Puzzle Name</b><br><%=puzzle.getPuzzleName()%></li>
            <li class="navbar-text"><b>Points</b><br><%=puzzle.getPoints()%></li>
            <li class="navbar-text"><b>Mode</b><br><%=storyMode%></li>
        </ul>
    </div>
</nav>
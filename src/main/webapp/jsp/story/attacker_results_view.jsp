<% String pageTitle="Results page"; %>
<%@ include file="/jsp/header.jsp" %>

<%
    StoryGame sg = (StoryGame) request.getSession().getAttribute("puzzle");
    int testsPassed = (Integer) request.getSession().getAttribute("testsPassed");
    int score = (Integer) request.getSession().getAttribute("score");
%>

<div class="w-50">
    <a href="story/view">Return to main menu</a>
    <p></p>
    <table class="table table-hover table-responsive table-paragraphs games-table w-50">
        <tr>
            <th class="col-lg-1">Puzzle</th>
            <td><%=sg.getLevelNum()%> - <%=sg.getPuzzle()%></td>
        </tr>
        <tr>
            <th class="col-lg-3">Name</th>
            <td><%=sg.getPuzzleName()%></td>
        </tr>
        <tr>
            <th class="col-lg-3">Mode</th>
            <td>Attacker</td>
        </tr>
        <tr>
            <th class="col-lg-4">Test passed</th>
            <td><%= testsPassed %></td>
        </tr>
        <tr>
            <th class="col-lg-4">Points obtained</th>
            <td><%= score %></td>
        </tr>
    </table>
    <form id="retry" action="puzzle" method="post" class="w-50">
        <input type="hidden" name="formType" value="enterPuzzle">
        <input type="hidden" name="puzzleId" value="<%= sg.getPuzzleId() %>">
        <input type="hidden" name="puzzleName" value="<%= sg.getPuzzleName()%>">
        <input class="btn btn-primary btn-right" type="submit" value="Retry Puzzle">
    </form>
</div>
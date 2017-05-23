<%@ page import="org.codedefenders.story.StoryGame" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle="Puzzle Editor"; %>
<%@ include file="/jsp/header.jsp" %>

<%
    int uid = (Integer)request.getSession().getAttribute("uid");
    List<StoryGame> classes = DatabaseAccess.getStoryClassesForUser(uid);
%>
<div class="w-50">
<a href="story/view">Back to Puzzles</a>
<p></p>
<table class="table table-hover table-responsive table-paragraphs games-table">
    <tr>
        <th class="col-sm-1">ID</th>
        <th class="col-sm-1">Puzzle</th>
        <th class="col-sm-5">Classes (Puzzle name)</th>
        <th class="col-sm-2"></th>
        <th class="col-sm-2"></th>
        <th class="col-sm-2"></th>
    </tr>
    <%
        if (classes.isEmpty()) {
    %>
    <tr><td colspan="100%">You haven't created any puzzles!</td></tr>
    <%
        } else {
            for (StoryGame c : classes) {
    %>
    <tr>
        <td class="col-sm-1"><%= c.getClassId() %></td>
        <% if (c.getPuzzle() == 0) {%>
            <td class="col-sm-1">None</td>
        <% } else { %>
            <td class="col-sm-1"><%= c.getLevelId() %> - <%= c.getPuzzle() %></td>
        <% } %>
        <td class="col-sm-5"><%= c.getClassName() %> (<%= c.getPuzzleName() %>)</td>
        <%
            if (c.getPuzzleName() == null || c.getDesc() == null) {
        %>
        <td class="col-sm-2" style="color:red;">Needs editing!</td>
        <% } else { %>
        <td class="col-sm-2"></td>
        <% } %>
        <td class="col-sm-2">
            <form id="view" action="editpuzzles" method="post">
                <input type="hidden" name="formType" value="reqEdit">
                <input type="hidden" name="editClassId" value="<%= c.getClassId() %>">
                <input type="hidden" name="classAlias" value="<%= c.getPuzzleName() %>">
                <input class="btn btn-primary" type="submit" value="Edit">
            </form>
        </td>
        <td class="col-sm-2">
            <form id="delete" action="editpuzzles" method="post">
                <input type="hidden" name="formType" value="deletePuzzle">
                <input type="hidden" name="delClassId" value="<%= c.getClassId() %>">
                <input class="btn btn-danger" type="submit" value="Delete" onclick="return confirm('Are you sure you want to delete this file?')">
            </form>
        </td>
    </tr>
    <%
            }
        }
    %>
</table>
</div>
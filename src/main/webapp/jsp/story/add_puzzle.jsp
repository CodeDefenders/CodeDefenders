<%@ page import="org.codedefenders.PuzzleClass" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle = null; %>
<%@ include file ="/jsp/header.jsp" %>

<%
    List<PuzzleClass> levels = DatabaseAccess.getLevels();
%>
<div class="container-fluid">
    <h2>Edit ${classAlias}</h2>
    <form id="formEdit" action="editpuzzles" class="form-upload form-horizontal" method="post">
        <input type="hidden" name="formType" value="addPuzzle">
        <input type="hidden" name="editClassId" value="${editClassId}">
        <div class="form-group">
            <label class="control-label" for="levelNumber">Level:</label>
            <div>
                <select id="levelNumber" name="levelNumber">
                    <%
                        if (levels.isEmpty()) {
                    %>
                    <option>ERROR</option>
                    <% } else {

                        for (PuzzleClass l : levels) { %>
                    <option><%= l.getLevelNum() %></option>
                    <%      }
                    }
                    %>
                </select>
            </div>
        </div>
        <div class="form-group">
            <label class="control-label" for="puzzleNumber">Puzzle number:</label>
            <div>
                <input id="puzzleNumber" name="puzzleNumber" type="text" class="form-control">
            </div>
        </div>
        <div class="form-group">
            <label class="control-label" for="classAlias">Puzzle name:</label>
            <div>
                <input id="classAlias" name="classAlias" type="text" class="form-control">
            </div>
        </div>
        <div class="form-group">
            <label class="control-label" for="puzzleHint">Hint:</label>
            <div>
                <textarea id="puzzleHint" name="puzzleHint" rows="3" class="form-control"></textarea>
            </div>
        </div>
        <div class="form-group">
            <label class="control-label" for="puzzleDesc">Description:</label>
            <div>
                <textarea id="puzzleDesc" name="puzzleDesc" rows="4" class="form-control"></textarea>
            </div>
        </div>
        <div class="form-group">
            <input class="btn btn-primary" type="submit" value="Submit changes">
        </div>
    </form>
</div>

<%@ include file="/jsp/footer.jsp" %>
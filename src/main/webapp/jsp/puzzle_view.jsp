<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle="Story Mode"; %>
<%@ include file="/jsp/header_story.jsp" %>
<%
    int puzzleId = (Integer) request.getAttribute("puzzleId");
    List<PuzzleClass> puzzleDetails = DatabaseAccess.getDetailsForPuzzle(puzzleId);
%>
<div class="row-fluid">
    <div class="col-md-6" id="cut-div">
        <h2>Class Under Test</h2>
        <% for (PuzzleClass p : puzzleDetails) {%>
        <pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" cols="80" rows="30"><%= story.getCUT().getAsString() %></textarea></pre>
    </div>
    <div class="col-md-6" id="utest-div">
        <h2>Write a new JUnit test here
            <button type="submit" class="btn btn-primary btn-game btn-right" onClick="this.form.submit(); this.disabled=true; this.value='Submitting...';">Submit!</button>
        </h2>
        <form id="story" action="story" method="post">
            <pre><textarea id="code" name="test" cols="80" rows="30"></textarea></pre>
            <input type="hidden" name="formType" value="createTest">
        </form>
    </div>
    <% } %>
</div>
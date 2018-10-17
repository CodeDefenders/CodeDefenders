<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="java.util.Set" %>

<%--
    Displays a list of tests in a one-item slider.

    @param List<Test> tests
        The tests to display.
--%>

<% { %>

<%
    List<Test> testsTODORENAME = (List<Test>) request.getAttribute("tests");
%>

<div class="slider single-item">

    <% if (testsTODORENAME.isEmpty()) { %>
    <div><p> There are currently no tests </p></div>
    <% } %>
    <%
        for (Test test : testsTODORENAME) {
            User creator = DatabaseAccess.getUserFromPlayer(test.getPlayerId());
            final Set<Mutant> coveredMutants = test.getCoveredMutants();
            final Set<Mutant> killedMutants = test.getKilledMutants();
            final String coveredMutantsIdString = coveredMutants.stream().map(mutant -> String.valueOf(mutant.getId())).collect(Collectors.joining(", "));
            final String killedMuantsIdString = killedMutants.stream().map(mutant -> String.valueOf(mutant.getId())).collect(Collectors.joining(", "));

    %>

    <div class="container nowrap" style="overflow:hidden;white-space:nowrap;">
        <ul class="list-inline white-space:nowrap">
            <li style=" display: inline-block;">
                Test <%= test.getId() %>
                &nbsp |
            </li>
            <li style=" display: inline-block;">
                Creator: <%= creator.getUsername() %> (uid <%= creator.getId() %>)
                &nbsp |
            </li>
            <li style=" display: inline-block;">
                <% if(!coveredMutants.isEmpty()) { %>
                    <a href="javascript:void(0);" data-toggle="tooltip" title="<%=coveredMutantsIdString%>">Covered: <%=coveredMutants.size() %></a>
                <% } else { %>
                    Covered: 0
                <% } %>
                &nbsp |
            </li>
            <li style=" display: inline-block;">
                <% if(!killedMutants.isEmpty()) {
                %>
                    <a href="javascript:void(0);" data-toggle="tooltip" title="<%= killedMuantsIdString %>">Killed: <%= killedMutants.size() %></a>
                <% } else { %>
                    Killed: 0
                <% } %>
                &nbsp |
            </li>
            <li style=" display: inline-block;">
                Points: <%= test.getScore() %>
            </li>

        </ul>
        <pre class="readonly-pre"><textarea class="utest" title="utest" cols="20" rows="10"><%=StringEscapeUtils.escapeHtml(test.getAsString())%></textarea></pre>
    </div>
    <%  } %>
</div>

<script>
    var x = document.getElementsByClassName("utest");
    for (var i = 0; i < x.length; i++) {
        CodeMirror.fromTextArea(x[i], {
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-java",
            readOnly: true
        });
    }
</script>

<% } %>

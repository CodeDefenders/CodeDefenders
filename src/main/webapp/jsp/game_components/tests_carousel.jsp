<%--

    Copyright (C) 2016-2018 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.codedefenders.database.TestSmellsDAO"%>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.stream.Collectors" %>

<%--
    Displays a list of tests in a one-item slider.

    @param List<Test> tests
        The tests to display.
--%>

<% { %>

<%
    List<Test> testsTODORENAME = (List<Test>) request.getAttribute("tests");
    List<Mutant> mutantsTODORENAME = (List<Mutant>) request.getAttribute("mutants");
%>

<div class="slider single-item">

    <% if (testsTODORENAME.isEmpty()) { %>
    <div><p> There are currently no tests </p></div>
    <% } %>
    <%
        for (Test test : testsTODORENAME) {
            User creator = UserDAO.getUserForPlayer(test.getPlayerId());
            final Set<Mutant> coveredMutants = test.getCoveredMutants(mutantsTODORENAME);
            final Set<Mutant> killedMutants = test.getKilledMutants();
            final String coveredMutantsIdString = coveredMutants.stream().map(mutant -> String.valueOf(mutant.getId())).collect(Collectors.joining(", "));
            final String killedMuantsIdString = killedMutants.stream().map(mutant -> String.valueOf(mutant.getId())).collect(Collectors.joining(", "));

            // Get the smells for this test
            final List<String> smellList = TestSmellsDAO.getDetectedTestSmellsForTest( test );
            final String smellHtmlList = StringUtils.join(smellList, "</br>");
            // Compute the smell level
            String smellLevel = "Good";
            String smellColor = "btn-success";

            if(smellList.size() >= 1 ){
            	smellLevel = "Fishy";
                smellColor = "btn-warning";
            }
			if(smellList.size() >= 3 ){
				smellLevel = "Bad";
                smellColor = "btn-danger";
            }
			if(smellList.size() >= 5 ){
				smellLevel = "A lot";
                smellColor = "btn-dark";

            }
    %>

    <div class="container nowrap" style="overflow:hidden;white-space:nowrap;">
        <ul class="list-inline white-space:nowrap">
            <li style=" display: inline-block;">
                Test <%= test.getId() %>
                &nbsp |
            </li>
            <li style=" display: inline-block;">
                <%= creator.getUsername() %> (uid <%= creator.getId() %>)
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
                &nbsp |
            </li>
            <li style=" display: inline-block;">
                Smells:
                <a class="validatorLevelTag btn <%=smellColor %>" data-container="body" data-html="true"
                    <%if( ! smellList.isEmpty() ){ %>
                       data-toggle="popover"
                    <% } %>
                   data-trigger="focus" data-placement="top" title="This test smells of:"  data-content="<%=smellHtmlList %>" data-original-title=""><%=smellLevel %></a>
            </li>

        </ul>
        <pre class="readonly-pre"><textarea class="utest" title="utest" cols="20"
                                            rows="10"><%=test.getAsHTMLEscapedString()%></textarea></pre>
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
<%-- Activate the popover thingy --%>
<script>
$(function () {
	  $('[data-toggle="popover"]').popover({
		  trigger: 'focus'
	  })
})

</script>
<% } %>


<%--

    Copyright (C) 2016-2019 Code Defenders contributors

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
<%@page import="org.codedefenders.database.TestSmellsDAO"%>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.TestCarousel" %>
<%@ page import="org.codedefenders.game.TestCarousel.TCMethodDescription" %>

<%--
    Displays a list of tests in a one-item slider.

    @param List<Test> tests
        The tests to display.
    // TODO: new parameters
--%>

<% { %>

<%
    List<Test> testsTODORENAME = (List<Test>) request.getAttribute("tests");
    List<Mutant> mutantsTODORENAME = (List<Mutant>) request.getAttribute("mutants");
    /*
     *   Ideally TestSmellsDAO should be injected using @Inject as it is @ManagedBean, but that proven to be not straigthforward.
     *   Since TestSmellsDAO does not have additional dependencies we can instantiate the object here. This will work as long as no @Inject are used
     */
    TestSmellsDAO testSmellsDAO = new TestSmellsDAO();
%>

<%--
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
            final List<String> smellList = testSmellsDAO.getDetectedTestSmellsForTest(test);
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

<!-- Activate the popover thingy -->
<script>
$(function () {
	  $('[data-toggle="popover"]').popover({
		  trigger: 'focus'
	  })
})
</script>

--%>

<%
    TestCarousel testCarousel = new TestCarousel(cut, testsTODORENAME, mutantsTODORENAME);
    List<TCMethodDescription> infos = testCarousel.getInfos();

    Gson gson = new GsonBuilder().registerTypeAdapter(Map.class, new JSONUtils.MapSerializer()).create();
    String tcString = gson.toJson(testCarousel);
%>

<style type="text/css">
    <%-- Prefix all classes with "tc-" to avoid conflicts.
    We probably want to extract some common CSS when we finally tackle the CSS issue. --%>

    #tests-accordion {
        margin-bottom: 0;
    }

    #tests-accordion .panel-heading {
        padding-top: .75ex;
        padding-bottom: .75ex;
    }

    #tests-accordion .panel-body {
        padding: 0;
    }

    #tests-accordion thead {
        display: none;
    }

    #tests-accordion .dataTables_scrollHead {
        display: none;
    }

    #tests-accordion .panel-title.ta-covered {
        color: black;
    }

    #tests-accordion .panel-title:not(.ta-covered) {
        color: grey;
    }

    #tests-accordion .tc-column-name {
        color: #B0B0B0;
    }
</style>

<div class="panel panel-default">
    <%--
    <div class="panel-heading">
        Tests
    </div>
    --%>
    <div class="panel-body" id="tests">
        <div class="panel-group" id="tests-accordion">
            <%
                int index = -1;
                for (TCMethodDescription info : infos) {
                    index++;
            %>
                <div class="panel panel-default">
                    <div class="panel-heading" id="heading-<%=index%>">
                        <a role="button" data-toggle="collapse" aria-expanded="false"
                                href="#collapse-<%=index%>"
                                aria-controls="collapse-<%=index%>"
                                class="panel-title <%=info.getCoveringTests().isEmpty() ? "" : "ta-covered"%>">
                            <%=info.getDescription()%>
                        </a>
                    </div>
                    <div class="panel-collapse collapse" data-parent="#tests-accordion"
                            id="collapse-<%=index%>"
                            aria-labelledby="heading-<%=index%>">
                        <div class="panel-body">
                            <table id="table-<%=index%>" class="table table-sm"></table>
                        </div>
                    </div>
                </div>
            <%
                }
            %>
        </div>
    </div>
</div>

<script>
    // TODO: only generate the table when panel is opened?

    /* Wrap in a function so it has it's own scope. */
    (function () {

        /* Game highlighting data. */
        const tc_data = JSON.parse(`<%=tcString%>`);
        const methodDescriptions = tc_data.methodDescriptions;
        const tests = new Map(tc_data.tests);

        const genId = function (row) {
            return '<span class="tc-column-name">ID:</span> ' + row.id;
        };

        const genCreator = function (row) {
            return row.creatorName + ' (' + row.creatorId + ')';
        };

        const genCoveredMutants = function (row) {
            return '<span class="tc-column-name">Covered:</span> ' + row.numCoveredMutants;
            // TODO: hoverabe link
        };

        const genKilledMutants = function (row) {
            return '<span class="tc-column-name">Killed:</span> ' + row.numKilledMutants;
            // TODO: hoverabe link
        };

        const genPoints = function (row) {
            return '<span class="tc-column-name">Points:</span> ' + row.points;
        };

        const genSmellButton = function (row) {
            return ''; // TODO
        };

        const genShowButton = function (row) {
            const btn = document.createElement('button');
            btn.classList.add('tc-btn');
            btn.classList.add('btn');
            btn.classList.add('btn-primary');
            btn.innerText = 'Show';
            btn.onclick = () => showTest(row.id);
            return btn;
        };

        const showTest = function (id) {
            console.log('this would show test ' + id); // TODO
        };

        let index = -1;
        for (const methodDescription of methodDescriptions) {
            index++;

            const coveringTests = methodDescription.coveringTestIds.map(id => tests.get(id));

            $('#table-' + index).DataTable({
                data: coveringTests,
                columns: [
                    { data: genId, title: '' },
                    { data: genCreator, title: '' },
                    { data: genCoveredMutants, title: '' },
                    { data: genKilledMutants, title: '' },
                    { data: genPoints, title: '' },
                    { data: genSmellButton, title: '' },
                    { data: genShowButton, title: '' }
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'No tests cover this method.'}
            });
            <%-- TODO
            if (coveringTests.length > 0) {
                $(`#heading-${i} a`).append(`<y>(${coveringTests.length})</y>`);
            }
            --%>
        }
    }());
</script>

<% } %>


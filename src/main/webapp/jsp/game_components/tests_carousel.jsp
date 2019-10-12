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
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.TestCarousel" %>
<%@ page import="org.codedefenders.game.TestCarousel.TestCarouselCategory" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.gson.GsonBuilder" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.util.JSONUtils" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="org.codedefenders.game.GameClass" %>

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
    GameClass cut = (GameClass) request.getAttribute("cut");

    TestCarousel testCarousel = new TestCarousel(cut, testsTODORENAME, mutantsTODORENAME);
    List<TestCarouselCategory> categories = testCarousel.getInfos();

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

    #tests-accordion td {
        vertical-align: middle;
    }

    #tests-accordion .panel-title.tc-covered {
        color: black;
    }

    #tests-accordion .panel-title:not(.tc-covered) {
        color: #B0B0B0;
    }

    #tests-accordion .tc-test-count {
        color: #909090;
    }

    #tests-accordion .tc-column-name {
        color: #B0B0B0;
    }

    #tests-accordion .tc-covered-link {
        color: inherit;
    }

    #tests-accordion .tc-covered-link:hover {
        text-decoration: none;
    }

    #tests-accordion .tc-killed-link {
        color: inherit;
    }

    #tests-accordion .tc-killed-link:hover {
        text-decoration: none;
    }

    /* #tests-accordion .btn {
        margin-top: -.75ex;
        margin-bottom: -.5ex;
    } */
</style>

<div class="panel panel-default">
    <div class="panel-body" id="tests">
        <div class="panel-group" id="tests-accordion">
            <%
                int index = -1;
                for (TestCarouselCategory category : categories) {
                    index++;
            %>
                <div class="panel panel-default">
                    <div class="panel-heading" id="tc-heading-<%=index%>">
                        <a role="button" data-toggle="collapse" aria-expanded="false"
                                href="#tc-collapse-<%=index%>"
                                aria-controls="tc-collapse-<%=index%>"
                                class="panel-title <%=category.getTestIds().isEmpty() ? "" : "tc-covered"%>">
                            <%=category.getDescription()%>
                            <span class="tc-test-count"></span>
                        </a>
                    </div>
                    <div class="panel-collapse collapse" data-parent="#tests-accordion"
                            id="tc-collapse-<%=index%>"
                            aria-labelledby="tc-heading-<%=index%>">
                        <div class="panel-body">
                            <table id="tc-table-<%=index%>" class="table table-sm"></table>
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
    for (let textarea of document.getElementsByClassName("utest")) {
        let editor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-java",
            readOnly: true
        });
        // Potential ajax calls, but the carousel seems to require all DOM nodes to be present to be rendered correctly.
        // TestAPI.getAndSetEditorValue(textarea, editor);
    }

    /* Wrap in a function so it has it's own scope. */
    (function () {

        /* Test carousel data. */
        const tc_data = JSON.parse(`<%=tcString%>`);
        const categories = tc_data.categories;
        const tests = new Map(tc_data.tests);

        const genId             = row => 'Test ' + row.id;
        const genCreator        = row => <%-- '<span class="tc-column-name">Creator:</span> ' + --%> row.creatorName;
        const genPoints         = row => '<span class="tc-column-name">Points:</span> '  + row.points;
        const genCoveredMutants = row => '<a class="tc-covered-link"><span class="tc-column-name">Covered:</span> ' + row.coveredMutantIds.length + '</a>';;
        const genKilledMutants  = row => '<a class="tc-killed-link"><span class="tc-column-name">Killed:</span> ' + row.killedMutantIds.length + '</a>';
        const genViewButton     = row => '<button class="tc-view-button btn btn-ssm btn-primary">View</button>';
        const genSmells         = row => {
            const numSmells = row.smells.length;
            let smellLevel;
            let smellColor;
            if (numSmells >= 3) {
                smellLevel = 'Bad';
                smellColor = 'btn-danger';
            } else if (numSmells >= 1) {
                smellLevel = 'Fishy';
                smellColor = 'btn-warning';
            } else {
                smellLevel = 'Good';
                smellColor = 'btn-success';
            }
            return <%-- '<span class="tc-column-name">Smells:</span> '
                + --%> '<a class="tc-smells-link btn btn-ssm ' + smellColor + '">' + smellLevel + '</a>';
        };

        const rowData = function (element, dataTable) {
            const row = $(element).closest('tr');
            return dataTable.row(row).data();
        };

        const setupPopovers = function (jqElements, getData, emptyHeading, nonEmptyHeading, emptyBody, nonEmptyBody) {
            jqElements.popover({
                container: document.body,
                template:
                    `<div class="popover" role="tooltip">
                        <div class="arrow"></div>
                        <h3 class="popover-title"></h3>
                        <div class="popover-content" style="max-width: 250px;"></div>
                    </div>`,
                placement: 'top',
                trigger: 'hover',
                html: true,
                title: function () {
                    const data = getData(this);
                    return data.length > 0
                        ? emptyHeading(data)
                        : nonEmptyHeading(data);
                },
                content: function () {
                    const data = getData(this);
                    return data.length > 0
                        ? emptyBody(data)
                        : nonEmptyBody(data);
                }
            });
        };

        const viewTestModal = function (testId) {
            // TODO
        };

        let index = -1;
        for (const category of categories) {
            index++;

            const rows = category.testIds
                .sort()
                .map(tests.get.bind(tests));

            if (rows.length > 0) {
                $('#tc-heading-' + index + ' .tc-test-count').text('(' + rows.length + ')');
            }

            const tableElement = $('#tc-table-' + index);
            const dataTable = tableElement.DataTable({
                data: rows,
                columns: [
                    { data: null, title: '', defaultContent: '' },
                    { data: genId, title: '' },
                    { data: genCreator, title: '' },
                    { data: genCoveredMutants, title: '' },
                    { data: genKilledMutants, title: '' },
                    { data: genPoints, title: '' },
                    { data: genSmells, title: '' },
                    { data: genViewButton, title: '' }
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'No tests cover this method.'}
            });

            tableElement.on('click', '.tc-view-button', function () {
                const testId = rowData(this, dataTable).id;
                <%--
                                        <div class="modal-dialog">
                                            <!-- Modal content-->
                                            <div class="modal-content">
                                                <div class="modal-header">
                                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                                    <h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
                                                </div>
                                                <div class="modal-body">
                                                    <pre class="readonly-pre"><textarea
                                                            class="mutdiff" title="mutdiff"
                                                            id="diff<%=m.getId()%>"><%=m.getHTMLEscapedPatchString()%></textarea></pre>
                                                </div>
                                                <div class="modal-footer">
                                                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                                </div>
                                            </div>
                                        </div>
                --%>
            });

            setupPopovers(
                tableElement.find('.tc-covered-link'),
                that => rowData(that, dataTable).coveredMutantIds,
                data => 'Covered Mutants',
                data => null,
                data => data.join(', '),
                data => null
            );

            setupPopovers(
                tableElement.find('.tc-killed-link'),
                that => rowData(that, dataTable).killedMutantIds,
                data => 'Killed Mutants',
                data => null,
                data => data.join(', '),
                data => null
            );

            setupPopovers(
                tableElement.find('.tc-smells-link'),
                that => rowData(that, dataTable).smells,
                data => 'This test smells of',
                data => null,
                data => data.join('<br>'),
                data => 'This test does not have any smells.'
            );
        }
    }());
</script>
<% } %>


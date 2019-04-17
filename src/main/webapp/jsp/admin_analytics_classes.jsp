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
<%@ page import="org.codedefenders.servlets.FeedbackManager" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <h3>Classes</h3>

    <table id="tableClasses"
           class="table table-striped table-hover table-responsive">
        <thead>
            <tr>
                <th id="toggle-all-details"><span class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"></span></th>
                <th>ID</th>
                <th>Name (Alias)</th>
                <th>Games Played</th>
                <th>Test Submitted</th>
                <th>Tests per Game</th>
                <th>Mutants Submitted</th>
                <th>Mutants per Game</th>
            </tr>
        </thead>
    </table>

    <div class="btn-group">
        <a download="classes-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_CLASSES%>?filetype=csv"
            type="button" class="btn btn-default" id="download-csv">Download as CSV</a>
        <a download="classes-analytics.json" href="<%=request.getContextPath()+Paths.API_ANALYTICS_CLASSES%>?filetype=json"
           type="button" class="btn btn-default" id="download-json">Download as JSON</a>
    </div>

    <script src="js/datatables-utils.js" type="text/javascript" ></script>

    <script>
        var table;

        function format(data) {
            var rating1 = data.ratings.cutMutationDifficulty;
            var rating2 = data.ratings.cutTestDifficulty;
            var rating3 = data.ratings.gameEngaging;

            return '' +
                '<table class="table-child-details indented">'+
                    '<thead>'+
                        '<tr>'+
                            '<th>Win Rates</td>'+
                        '</tr>'+
                    '</thead>'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Attacker Wins:</td>'+
                            '<td>'+dtValAndPerc(data.attackerWins, data.nrGames)+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Defender Wins:</td>'+
                            '<td>'+dtValAndPerc(data.defenderWins, data.nrGames)+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                    '<thead>'+
                        '<tr>'+
                            '<th>Feedback</td>'+
                        '</tr>'+
                    '</thead>'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Mutation Difficulty:</td>'+
                            '<td>'+dtDiv(rating1.sum, rating1.count, 'NA')+'</td>'+
                            '<td>Number of votes:</td>'+
                            '<td>'+rating1.count+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Test Difficulty:</td>'+
                            '<td>'+dtDiv(rating2.sum, rating2.count, 'NA')+'</td>'+
                            '<td>Number of votes:</td>'+
                            '<td>'+rating2.count+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Game is engaging:</td>'+
                            '<td>'+dtDiv(rating3.sum, rating3.count, 'NA')+'</td>'+
                            '<td>Number of votes:</td>'+
                            '<td>'+rating3.count+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                    '<thead>'+
                        '<tr>'+
                            '<th>Mutants</td>'+
                        '</tr>'+
                    '</thead>'+
                    '<tbody>'+
                        '<tr>'+
                            '<td>Mutants Alive:</td>'+
                            '<td>'+dtValAndPerc(data.mutantsAlive, data.mutantsSubmitted)+'</td>'+
                            '<td>Per Game:</td>'+
                            '<td>'+dtDiv(data.mutantsAlive, data.nrGames)+'</td>'+
                        '</tr>'+
                        '<tr>'+
                            '<td>Mutants Equivalent:</td>'+
                            '<td>'+dtValAndPerc(data.mutantsEquivalent, data.mutantsSubmitted)+'</td>'+
                            '<td>Per Game:</td>'+
                            '<td>'+dtDiv(data.mutantsEquivalent, data.nrGames)+'</td>'+
                        '</tr>'+
                    '</tbody>'+
                '</table>';
        }

        $(document).ready(function() {
            table = $('#tableClasses').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Paths.API_ANALYTICS_CLASSES + "?fileType=json"%>",
                    "dataSrc": "data"
                },
                "columns": [
                    {
                        "className":      'toggle-details',
                        "orderable":      false,
                        "data":           null,
                        "defaultContent": '<span class="toggle-details-icon glyphicon glyphicon-chevron-right text-muted"></span>'
                    },
                    { "data": "id" },
                    { "data":
                            function(row, type, val, meta) {
                                return row.classname + ' (' + row.classalias + ')';
                            }
                    },
                    { "data": "nrGames" },
                    { "data": "testsSubmitted" },
                    { "data":
                            function(row, type, val, meta) {
                                return dtDiv(row.testsSubmitted, row.nrGames);
                            }
                    },
                    { "data": "mutantsSubmitted" },
                    { "data":
                            function(row, type, val, meta) {
                                return dtDiv(row.mutantsSubmitted, row.nrGames);
                            }
                    }
                ],
                "pageLength": 50,
                "order": [[ 1, "asc" ]]
            });

            setupChildRows("#tableClasses", table, format);
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>

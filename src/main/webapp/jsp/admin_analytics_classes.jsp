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
<jsp:include page="/jsp/header_main.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <h3>Classes</h3>

    <table id="tableClasses" class="table table-striped">
        <thead>
            <tr>
                <th class="toggle-all-details"><i class="toggle-details-icon fa fa-chevron-right"></i></th>
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

    <div class="row mt-4">
        <div class="col-auto">
            <div class="btn-group">
                <a download="classes-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_CLASSES%>?fileType=csv"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download">
                    <i class="fa fa-download me-1"></i>
                    Download table
                </a>
                <a download="classes-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_CLASSES%>?fileType=csv"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-csv">
                    as CSV
                </a>
                <a download="classes-analytics.json" href="<%=request.getContextPath()+Paths.API_ANALYTICS_CLASSES%>?fileType=json"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-json">
                    as JSON
                </a>
            </div>
        </div>
    </div>
</div>

<script src="js/datatables-utils.js" type="text/javascript" ></script>

<script>
(function () {

    function format(data) {
        var rating1 = data.ratings.cutMutationDifficulty;
        var rating2 = data.ratings.cutTestDifficulty;
        var rating3 = data.ratings.gameEngaging;

        return '' +
            `<div class="child-row-wrapper">
                <table class="child-row-details">
                    <thead>
                        <tr>
                            <th>Win Rates</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Attacker Wins:</td>
                            <td>\${dtValAndPercent(data.attackerWins, data.nrGames)}</td>
                        </tr>
                        <tr>
                            <td>Defender Wins:</td>
                            <td>\${dtValAndPercent(data.defenderWins, data.nrGames)}</td>
                        </tr>
                    </tbody>
                    <thead>
                        <tr>
                            <th>Feedback</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Mutation Difficulty:</td>
                            <td>\${dtDiv(rating1.sum, rating1.count, 'NA')}</td>
                            <td>Number of votes:</td>
                            <td>\${rating1.count}</td>
                        </tr>
                        <tr>
                            <td>Test Difficulty:</td>
                            <td>\${dtDiv(rating2.sum, rating2.count, 'NA')}</td>
                            <td>Number of votes:</td>
                            <td>\${rating2.count}</td>
                        </tr>
                        <tr>
                            <td>Game is engaging:</td>
                            <td>\${dtDiv(rating3.sum, rating3.count, 'NA')}</td>
                            <td>Number of votes:</td>
                            <td>\${rating3.count}</td>
                        </tr>
                    </tbody>
                    <thead>
                        <tr>
                            <th>Mutants</td>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Mutants Alive:</td>
                            <td>\${dtValAndPercent(data.mutantsAlive, data.mutantsSubmitted)}</td>
                            <td>Per Game:</td>
                            <td>\${dtDiv(data.mutantsAlive, data.nrGames)}</td>
                        </tr>
                        <tr>
                            <td>Mutants Equivalent:</td>
                            <td>\${dtValAndPercent(data.mutantsEquivalent, data.mutantsSubmitted)}</td>
                            <td>Per Game:</td>
                            <td>\${dtDiv(data.mutantsEquivalent, data.nrGames)}</td>
                        </tr>
                    </tbody>
                </table>
            </div>`;
    }

    $(document).ready(function() {
        const table = $('#tableClasses').DataTable({
            "ajax": {
                "url": "<%=request.getContextPath() + Paths.API_ANALYTICS_CLASSES + "?fileType=json"%>",
                "dataSrc": "data"
            },
            "columns": [
                {
                    "className":      'toggle-details',
                    "orderable":      false,
                    "data":           null,
                    "defaultContent": '<i class="toggle-details-icon fa fa-chevron-right"></i>'
                },
                { "data": "id" },
                { "data":
                        function (row, type, val, meta) {
                            return row.classname + ' (' + row.classalias + ')';
                        }
                },
                { "data": "nrGames" },
                { "data": "testsSubmitted" },
                { "data":
                        function (row, type, val, meta) {
                            return dtDiv(row.testsSubmitted, row.nrGames);
                        }
                },
                { "data": "mutantsSubmitted" },
                { "data":
                        function (row, type, val, meta) {
                            return dtDiv(row.mutantsSubmitted, row.nrGames);
                        }
                }
            ],
            "pageLength": 50,
            "order": [[ 1, "asc" ]],
            "scrollY": '600px',
            "scrollCollapse": true,
            "paging": false,
            "language": {"info": "Showing _TOTAL_ entries"}
        });

        setupChildRows(table, format);
    });

})();
</script>

<%@ include file="/jsp/footer.jsp" %>

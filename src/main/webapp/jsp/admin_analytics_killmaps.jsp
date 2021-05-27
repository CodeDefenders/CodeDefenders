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

    <h3>Useful Actions</h3>

    <table id="tableKillmaps" class="table table-striped">
        <thead>
            <tr>
                <th>User ID</th>
                <th>User Name</th>
                <th>Class ID</th>
                <th>Class Name</th>
                <th>Role</th>
                <th>Useful Mutants</th>
                <th>Useful Tests</th>
                <th>Useful Actions</th>
            </tr>
        </thead>
    </table>

    <div class="row mb-3 mt-4">
        <label class="form-label col-sm-12">Download Table</label>
        <div class="col-auto">
            <div class="btn-group">
                <a download="killmap-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_KILLMAP%>?fileType=csv"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-csv">Download as CSV</a>
                <a download="killmap-analytics.json" href="<%=request.getContextPath()+Paths.API_ANALYTICS_KILLMAP%>?fileType=json"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-json">Download as JSON</a>
            </div>
        </div>
    </div>

    <div class="accordion mt-4">
        <div class="accordion-item">
            <h2 class="accordion-header" id="explanation-heading">
                <button class="accordion-button collapsed" type="button"
                        data-bs-toggle="collapse" data-bs-target="#explanation-collapse"
                        aria-expanded="false" aria-controls="explanation-collapse">
                    Explanation
                    <i class="fa fa-question-circle ms-1"></i>
                </button>
            </h2>
            <div id="explanation-collapse" class="accordion-collapse collapse" aria-labelledby="explanation-heading">
                <div class="accordion-body">
                    <p>
                        This table uses data from the class killmaps to determine the number of useful tests and mutants per
                        player, class and role.
                    </p>
                    <table>
                        <tbody>
                            <tr>
                                <td class="pe-2"><b>Useful Tests:</b></td>
                                <td>Number of tests, which killed at least one mutant.</td>
                            </tr>
                            <tr>
                                <td class="pe-2"><b>Useful Mutants:</b></td>
                                <td>Number of mutants, which were killed by at least one test,
                                    but were covered and not killed by at least one other test.</td>
                            </tr>
                            <tr>
                                <td class="pe-2"><b>Useful Actions:</b></td>
                                <td>Sum of useful tests and useful mutants.</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
(function () {

    $(document).ready(function () {
        const table = $('#tableKillmaps').DataTable({
            "ajax": {
                "url": "<%=request.getContextPath() + Paths.API_ANALYTICS_KILLMAP + "?fileType=json"%>",
                "dataSrc": "data"
            },
            "columns": [
                {"data": "userId"},
                {"data": "userName"},
                {"data": "classId"},
                {"data": "className"},
                {"data": "role"},
                {"data": "usefulMutants"},
                {"data": "usefulTests"},
                {
                    "data":
                        function (row, type, val, meta) {
                            return row.usefulMutants + row.usefulTests;
                        }
                }
            ],
            /* "columnDefs": [
                { className: "text-right", "targets": [0,2,5,6,7] },
            ], */
            "pageLength": 50,
            "order": [[1, "asc"]],
            "scrollY": '600px',
            "scrollCollapse": true,
            "paging": false,
            "language": {"info": "Showing _TOTAL_ entries"}
        });
    });

})();
</script>

<%@ include file="/jsp/footer.jsp" %>

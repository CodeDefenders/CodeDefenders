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

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <h3>Useful Actions</h3>

    <table id="tableKillmaps"
           class="table table-striped table-hover table-responsive">
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

    <div class="btn-group">
        <a download="killmap-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_KILLMAP%>?filetype=csv"
            type="button" class="btn btn-default" id="download-csv">Download as CSV</a>
        <a download="killmap-analytics.json" href="<%=request.getContextPath()+Paths.API_ANALYTICS_KILLMAP%>?filetype=json"
           type="button" class="btn btn-default" id="download-json">Download as JSON</a>
    </div>
    <div style="display: inline-block; margin-left: 10px;">
        <a data-toggle="collapse" href="#explanation" style="color: black">
            <span class="glyphicon glyphicon-question-sign"></span>
        </a>
    </div>

    <div id="explanation" class="collapse panel panel-default" style="margin-top: 10px;">
        <div class="panel-body" style="padding: 10px;">
            This table uses data from the class killmaps to determine the number of useful tests and mutants per
            player, class and role.
            <p></p>
            <table>
                <tr>
                    <td><b>Useful Tests:</b></td>
                    <td>Number of tests, which killed at least one mutant.</td>
                </tr>
                <tr>
                    <td><b>Useful Mutants:</b></td>
                    <td>Number of mutants, which were killed by at least one test,
                        but were covered and not killed by at least one other test.</td>
                </tr>
                <tr>
                    <td><b>Useful Actions:</b></td>
                    <td>Sum of useful tests and useful mutants.</td>
                </tr>
            </table>
        </div>
    </div>

    <script>
        var table;

        $(document).ready(function() {
            table = $('#tableKillmaps').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Paths.API_ANALYTICS_KILLMAP + "?fileType=json"%>",
                    "dataSrc": "data"
                },
                "columns": [
                    { "data": "userId" },
                    { "data": "userName" },
                    { "data": "classId" },
                    { "data": "className" },
                    { "data": "role" },
                    { "data": "usefulMutants" },
                    { "data": "usefulTests" },
                    { "data":
                            function(row, type, val, meta) {
                                return row.usefulMutants + row.usefulTests;
                            }
                    }
                ],
                /* "columnDefs": [
                    { className: "text-right", "targets": [0,2,5,6,7] },
                ], */
                "pageLength": 50,
                "order": [[ 1, "asc" ]]
            });
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>

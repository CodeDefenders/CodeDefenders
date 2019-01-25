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
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminAnalytics"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

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
        <a download="killmap-analytics.csv" href="<%=request.getContextPath()+Paths.API_ANALYTICS_KILLMAP%>?type=csv"
            type="button" class="btn btn-default" id="download-csv">Download as CSV</a>
        <a download="killmap-analytics.json" href="<%=request.getContextPath()+Paths.API_ANALYTICS_KILLMAP%>?type=json"
           type="button" class="btn btn-default" id="download-json">Download as JSON</a>
    </div>

    <script>
        var table;

        $(document).ready(function() {
            table = $('#tableKillmaps').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Paths.API_ANALYTICS_KILLMAP%>",
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
                "pageLength": 50,
                "order": [[ 1, "asc" ]]
            });
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>

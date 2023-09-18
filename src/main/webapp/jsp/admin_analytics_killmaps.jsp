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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("KillMap Analytics"); %>

<jsp:include page="/jsp/header.jsp"/>

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

    <div class="row g-3 mt-4">
        <div class="col-12">
            <a data-bs-toggle="modal" data-bs-target="#useful-actions-explanation" class="btn btn-outline-secondary btn-sm">
                What are useful actions?
            </a>
        </div>
        <div class="col-12">
            <div class="btn-group">
                <a download="killmap-analytics.csv" href="${url.forPath(Paths.API_ANALYTICS_KILLMAP)}?fileType=csv"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download">
                    <i class="fa fa-download me-1"></i>
                    Download table
                </a>
                <a download="killmap-analytics.csv" href="${url.forPath(Paths.API_ANALYTICS_KILLMAP)}?fileType=csv"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-csv">
                    as CSV
                </a>
                <a download="killmap-analytics.json" href="${url.forPath(Paths.API_ANALYTICS_KILLMAP)}?fileType=json"
                   type="button" class="btn btn-sm btn-outline-secondary" id="download-json">
                    as JSON
                </a>
            </div>
        </div>
    </div>

    <t:modal title="Useful Actions Explanation" id="useful-actions-explanation">
        <jsp:attribute name="content">
            <p>
                This table uses data from the class killmaps to determine the number of useful tests and mutants per
                player, class and role.
            </p>
            <table class="table table-no-last-border mb-0">
                <tbody>
                <tr>
                    <td class="text-nowrap"><b>Useful Tests:</b></td>
                    <td>Number of tests, which killed at least one mutant.</td>
                </tr>
                <tr>
                    <td class="text-nowrap"><b>Useful Mutants:</b></td>
                    <td>Number of mutants, which were killed by at least one test,
                        but were covered and not killed by at least one other test.</td>
                </tr>
                <tr>
                    <td class="text-nowrap"><b>Useful Actions:</b></td>
                    <td>Sum of useful tests and useful mutants.</td>
                </tr>
                </tbody>
            </table>
        </jsp:attribute>
    </t:modal>
</div>

<script type="module">
    import DataTable from '${url.forPath("/js/datatables.mjs")}';
    import $ from '${url.forPath("/js/jquery.mjs")}';


    $(document).ready(function () {
        const table = new DataTable('#tableKillmaps', {
            "ajax": {
                "url": "${url.forPath(Paths.API_ANALYTICS_KILLMAP)}?fileType=json",
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
</script>

<%@ include file="/jsp/footer.jsp" %>

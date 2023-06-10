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
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Classrooms"); %>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "classrooms"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div class="loading loading-height-200 loading-border-card">
        <div class="d-flex justify-content-between flex-wrap align-items-baseline">
            <h3 class="mb-3">Classrooms</h3>
            <input type="search" id="search-classrooms" placeholder="Search"
                   class="form-control form-control-sm" style="width: 15em;">
        </div>
        <table id="classrooms-table" class="table"></table>
    </div>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import {LoadingAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';

        const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';

        const getClassrooms = async function() {
            const params = new URLSearchParams({
                type: 'classrooms'
            });
            const response = await fetch(`\${API_URL}?\${params}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            if (!response.ok) {
                // TODO: Show toast message here? But we don't have toasts :(
                return null;
            }
            return await response.json();
        };

        const classrooms = await getClassrooms();

        const classroomsTable = new DataTable('#classrooms-table', {
            data: classrooms,
            columns: [
                {
                    data: 'id',
                    type: 'number',
                    title: 'ID'
                },
                {
                    data: 'name',
                    type: 'string',
                    title: 'Name'
                },
                {
                    data: 'roomCode',
                    type: 'string',
                    title: 'Room Code'
                },
                {
                    data: 'open',
                    type: 'boolean',
                    title: 'Open'
                }
            ],
            order: [[1, 'asc']],
            scrollY: '600px',
            scrollCollapse: true,
            paging: false,
            dom: 't',
            language: {emptyTable: "There aren't any classrooms... yet."}
        });
        LoadingAnimation.hideAnimation(classroomsTable.table().container());

        /* Search bar. */
        document.getElementById('search-classrooms').addEventListener('input', function(event) {
            classroomsTable.search(this.value).draw();
        });
    </script>
</div>

<%@ include file="/jsp/footer.jsp" %>

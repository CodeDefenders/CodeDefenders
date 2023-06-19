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

<jsp:include page="/jsp/header.jsp"/>

<div class="container">

    <div class="d-flex justify-content-between flex-wrap align-items-baseline">
        <h2 class="mb-3">My Classrooms</h2>
        <input type="search" id="search-user" placeholder="Search"
               class="form-control form-control-sm" style="width: 15em;">
    </div>
    <div class="loading loading-border-card loading-height-200">
        <table id="user-classrooms" class="table"></table>
    </div>

    <div class="d-flex justify-content-between flex-wrap align-items-baseline">
        <h2 class="mt-5 mb-3">Public Classrooms</h2>
        <input type="search" id="search-public" placeholder="Search"
               class="form-control form-control-sm" style="width: 15em;">
    </div>
    <div class="loading loading-border-card loading-height-200">
        <table id="public-classrooms" class="table"></table>
    </div>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import {LoadingAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';

        const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';
        const CLASSROOM_URL = '${url.forPath(Paths.CLASSROOM)}';

        const getClassrooms = async function(which) {
            const params = new URLSearchParams({
                type: 'classrooms',
                which
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

        const renderClassroomLink = function(data, type, row, meta) {
            switch (type) {
                case 'type':
                case 'sort':
                case 'filter':
                    return null;
                case 'display':
                    const params = new URLSearchParams({
                        classroomUid: data.uuid
                    });
                    return `
                        <a href="\${CLASSROOM_URL}?\${params}" class="cursor-pointer float-end px-2">
                            <i class="fa fa-external-link text-primary"></i>
                        </a>
                    `;
            }
        };

        const renderOpenClassroomLink = function(data, type, row, meta) {
            switch (type) {
                case 'type':
                case 'sort':
                case 'filter':
                    return null;
                case 'display':
                    if (!data.open) {
                        return `
                            <span class="float-end px-2" title="Not open for joining">
                                <i class="fa fa-external-link text-muted"></i>
                            </a>
                        `;
                    }
                    return renderClassroomLink(data, type, row, meta);
            }
        };

        const userClassrooms = await getClassrooms("user");
        const publicClassrooms = await getClassrooms("visible");

        const classroomIds = new Set();
        for (const classroom of userClassrooms) {
            classroomIds.add(classroom.id);
        }
        const filteredPublicClassrooms = publicClassrooms.filter(classroom => !classroomIds.has(classroom.id));

        const userTable = new DataTable('#user-classrooms', {
            data: userClassrooms,
            columns: [
                {
                    data: 'name',
                    type: 'string',
                    title: 'Name'
                },
                {
                    data: null,
                    title: 'Link',
                    render: renderClassroomLink,
                    width: '2em'
                },
            ],
            order: [[0, 'asc']],
            scrollY: '600px',
            scrollCollapse: true,
            paging: false,
            dom: 't',
            language: {emptyTable: "You're not part of any classrooms."}
        });
        LoadingAnimation.hideAnimation(userTable.table().container());

        const publicTable = new DataTable('#public-classrooms', {
            data: filteredPublicClassrooms,
            columns: [
                {
                    data: 'name',
                    type: 'string',
                    title: 'Name'
                },
                {
                    data: null,
                    title: 'Link',
                    render: renderOpenClassroomLink,
                    width: '2em'
                },
            ],
            order: [[0, 'asc']],
            scrollY: '600px',
            scrollCollapse: true,
            paging: false,
            dom: 't',
            language: {emptyTable: "There are currently no public classrooms."}
        });
        LoadingAnimation.hideAnimation(publicTable.table().container());

        document.getElementById('search-user').addEventListener('keyup', function(event) {
            setTimeout(() => userTable.search(this.value).draw(), 0);
        });
        document.getElementById('search-public').addEventListener('keyup', function(event) {
            setTimeout(() => publicTable.search(this.value).draw(), 0);
        });
    </script>


</div>
<%@ include file="/jsp/footer.jsp" %>

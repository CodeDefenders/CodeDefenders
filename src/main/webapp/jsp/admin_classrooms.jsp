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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Classrooms"); %>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "classrooms"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div class="loading loading-height-200 loading-border-card mb-4">
        <div class="d-flex justify-content-between flex-wrap align-items-baseline">
            <div class="mb-3 d-flex gap-3">
                <h2 class="mb-0">Classrooms</h2>

                <button id="create-classroom" type="button" class="btn btn-primary rounded-pill"
                        data-bs-toggle="modal" data-bs-target="#create-classroom-modal">
                    New Classroom
                    <i class="fa fa-plus ms-1" aria-hidden="true"></i>
                </button>
            </div>

            <input type="search" id="search-classrooms" placeholder="Search"
                   class="form-control form-control-sm" style="width: 15em;">
        </div>
        <table id="classrooms-table" class="table"></table>
    </div>

    <form action="${url.forPath(Paths.ADMIN_CLASSROOMS)}" method="post" class="needs-validation">
        <input type="hidden" name="action" value="create-classroom"/>

        <t:modal title="Create classroom" id="create-classroom-modal" closeButtonText="Cancel">
            <jsp:attribute name="content">
                <label for="name-input" class="form-label">Name</label>
                <input type="text" class="form-control" id="name-input" name="name"
                       required maxlength="100" placeholder="Name">
                <div class="invalid-feedback">
                    Please enter a valid name.
                </div>
                <div class="form-text">
                    Maximum length: 100 characters.
                </div>
            </jsp:attribute>
            <jsp:attribute name="footer">
                <button type="submit" class="btn btn-primary">Create Classroom</button>
            </jsp:attribute>
        </t:modal>
    </form>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import {LoadingAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';

        const API_URL = '${url.forPath(Paths.API_CLASSROOM)}';
        const CLASSROOM_URL = '${url.forPath(Paths.CLASSROOM)}';

        const getClassrooms = async function() {
            const params = new URLSearchParams({
                type: 'classrooms',
                which: 'all'
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

        const renderMemberCount = function(memberCount, type, row, meta) {
            switch (type) {
                case 'type':
                case 'sort':
                case 'filter':
                    return memberCount;
                case 'display':
                    return `
                        <span class="text-muted">\${memberCount} Members</span>
                    `;
            }
        };

        const renderLinkButton = function(data, type, row, meta) {
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
                    title: 'Name',
                    width: '25em',
                    className: 'truncate'
                },
                {
                    data: 'memberCount',
                    type: 'html',
                    title: 'Members',
                    render: renderMemberCount
                },
                {
                    data: 'uuid',
                    type: 'string',
                    title: 'UID'
                },
                {
                    data: 'open',
                    type: 'boolean',
                    title: 'Open'
                },
                {
                    data: 'visible',
                    type: 'boolean',
                    title: 'Visible'
                },
                {
                    data: null,
                    title: 'Link',
                    render: renderLinkButton,
                    width: '2em'
                },
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

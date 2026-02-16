<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="shiro" uri="http://shiro.apache.org/tags" %>

<%@ page import="org.codedefenders.auth.permissions.CreateClassroomPermission" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<p:main_page title="${i18n.tr('My Classrooms')}">
    <div class="container">

        <div class="d-flex justify-content-between flex-wrap align-items-baseline">

            <div class="mb-3 d-flex gap-3">
                <h2 class="mb-0">${i18n.tr('My Classrooms')}</h2>

                <shiro:hasPermission name="${CreateClassroomPermission.name}">
                    <button id="create-classroom" type="button" class="btn btn-primary rounded-pill"
                            data-bs-toggle="modal" data-bs-target="#create-classroom-modal">
                            ${i18n.tr('New Classroom')}
                        <i class="fa fa-plus ms-1" aria-hidden="true"></i>
                    </button>
                </shiro:hasPermission>
            </div>

            <div class="d-flex gap-3">
                <div>
                    <input type="radio" class="btn-check" name="classroom-type" id="radio-active" autocomplete="off" checked>
                    <label class="btn btn-sm btn-outline-secondary rounded-pill"
                           for="radio-active">${i18n.tr('Active')}</label>

                    <input type="radio" class="btn-check" name="classroom-type" id="radio-archived" autocomplete="off">
                    <label class="btn btn-sm btn-outline-secondary rounded-pill"
                           for="radio-archived">${i18n.tr('Archived')}</label>
                </div>
                <input type="search" id="search-user" placeholder="${i18n.tr('Search')}"
                       class="form-control form-control-sm" style="width: 15em;">
            </div>
        </div>
        <div class="loading loading-border-card loading-height-200">
            <table id="user-classrooms" class="table"></table>
        </div>

        <div class="d-flex justify-content-between flex-wrap align-items-baseline">
            <h2 class="mt-5 mb-3">${i18n.tr('Public Classrooms')}</h2>
            <input type="search" id="search-public" placeholder="${i18n.tr('Search')}"
                   class="form-control form-control-sm" style="width: 15em;">
        </div>
        <div class="loading loading-border-card loading-height-200">
            <table id="public-classrooms" class="table"></table>
        </div>

        <form action="${url.forPath(Paths.CLASSROOM)}" method="post" class="needs-validation">
            <input type="hidden" name="action" value="create-classroom"/>

            <t:modal title="${i18n.tr('Create classroom')}" id="create-classroom-modal"
                     closeButtonText="${i18n.tr('Cancel')}">
                <jsp:attribute name="content">
                    <label for="name-input" class="form-label">${i18n.tr('Name')}</label>
                    <input type="text" class="form-control" id="name-input" name="name"
                           required maxlength="100" placeholder="${i18n.tr('Name')}">
                    <div class="invalid-feedback">
                            ${i18n.tr('Please enter a valid name.')}
                    </div>
                    <div class="form-text">
                            ${i18n.tr('Maximum length: 100 characters.')}
                    </div>
                </jsp:attribute>
                <jsp:attribute name="footer">
                    <button type="submit" class="btn btn-primary">${i18n.tr('Create Classroom')}</button>
                </jsp:attribute>
            </t:modal>
        </form>

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

            const renderMemberCount = function(memberCount, type, row, meta) {
                switch (type) {
                    case 'type':
                    case 'sort':
                    case 'filter':
                        return memberCount;
                    case 'display':
                        const members = memberCount > 1 ? '${i18n.tr('Members')}' : '${i18n.tr('Member')}';
                        return `
                            <span class="text-muted">\${memberCount} \${members}</span>
                        `;
                }
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

            const renderPublicClassroomLink = function(data, type, row, meta) {
                switch (type) {
                    case 'type':
                    case 'sort':
                    case 'filter':
                        return null;
                    case 'display':
                        if (!data.open) {
                            return `
                                <span class="float-end px-2" title="${i18n.tr('Not open for joining')}">
                                    <i class="fa fa-external-link text-muted"></i>
                                </a>
                            `;
                        }
                        return renderClassroomLink(data, type, row, meta);
                }
            };

            const initUserTable = function(data) {
                const userTable = new DataTable('#user-classrooms', {
                    data,
                    columns: [
                        {
                            data: 'name',
                            type: 'string',
                            title: '${i18n.tr('Name')}',
                            width: '25em',
                            className: 'truncate'
                        },
                        {
                            data: 'memberCount',
                            type: 'html',
                            title: '${i18n.tr('Members')}',
                            render: renderMemberCount
                        },
                        {
                            data: null,
                            title: '${i18n.tr('Link')}',
                            render: renderClassroomLink,
                            width: '2em'
                        },
                    ],
                    order: [[0, 'asc']],
                    scrollY: '600px',
                    scrollCollapse: true,
                    paging: false,
                    dom: 't',
                    language: {
                        emptyTable: '${i18n.tr('You\'re not part of any classrooms.')}',
                        zeroRecords: '${i18n.tr('No classrooms found.')}'
                    }
                });

                LoadingAnimation.hideAnimation(userTable.table().container());

                document.getElementById('search-user').addEventListener('keyup', function(event) {
                    setTimeout(() => userTable.search(this.value).draw(), 0);
                });

                const activeRadio = document.getElementById("radio-active");
                const archivedRadio = document.getElementById("radio-archived");

                const searchFunction = (settings, renderedData, index, data, counter) => {
                    /* Let this only affect the "My Classrooms" table. */
                    if (settings.nTable.id !== 'user-classrooms') {
                        return true;
                    }

                    if (activeRadio.checked) {
                        return !data.archived;
                    } else {
                        return data.archived;
                    }
                };
                DataTable.ext.search.push(searchFunction);

                activeRadio.addEventListener('change', e => userTable.draw());
                archivedRadio.addEventListener('change', e => userTable.draw());

                return userTable;
            };

            const initPublicTable = function(data) {
                const publicTable = new DataTable('#public-classrooms', {
                    data,
                    columns: [
                        {
                            data: 'name',
                            type: 'string',
                            title: '${i18n.tr('Name')}',
                            width: '25em',
                            className: 'truncate'
                        },
                        {
                            data: 'memberCount',
                            type: 'html',
                            title: '${i18n.tr('Members')}',
                            render: renderMemberCount
                        },
                        {
                            data: null,
                            title: '${i18n.tr('Link')}',
                            render: renderPublicClassroomLink,
                            width: '2em'
                        },
                    ],
                    order: [[0, 'asc']],
                    scrollY: '600px',
                    scrollCollapse: true,
                    paging: false,
                    dom: 't',
                    language: {
                        emptyTable: '${i18n.tr('There are currently no public classrooms.')}',
                        zeroRecords: '${i18n.tr('No Classrooms found.')}'
                    }
                });

                LoadingAnimation.hideAnimation(publicTable.table().container());

                document.getElementById('search-public').addEventListener('keyup', function(event) {
                    setTimeout(() => publicTable.search(this.value).draw(), 0);
                });

                return publicTable;
            };

            getClassrooms('user').then(userClassrooms => initUserTable(userClassrooms));
            getClassrooms('visible').then(publicClassrooms => initPublicTable(publicClassrooms));
        </script>

    </div>
</p:main_page>

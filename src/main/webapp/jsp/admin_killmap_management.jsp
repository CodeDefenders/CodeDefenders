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
<%@ page import="org.codedefenders.cron.KillMapCronJob" %>
<%@ page import="org.codedefenders.execution.KillMap.KillMapJob" %>
<%@ page import="org.codedefenders.database.KillmapDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME" %>
<%@ page import="org.codedefenders.servlets.admin.AdminKillmapManagement.KillmapPage" %>
<%@ page import="org.codedefenders.util.CDIUtil" %>

<%@ page import="static org.codedefenders.util.MessageUtils.pluralize" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="killMapCronJob" type="org.codedefenders.cron.KillMapCronJob"--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("KillMap Management"); %>

<jsp:include page="/jsp/header.jsp"/>

<%
    /* The current page. There are three pages:
     * manual:    enter ids manually to queue or delete killmaps
     * available: choose killmaps to queue or delete from a table of available killmaps
     * queue:     choose killmap jobs to cancel from a table of current killmap jobs
     */
    KillmapPage currentPage = (KillmapPage) request.getAttribute("page");

    String processorExplanation = SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION.toString();

    KillMapJob currentJob = CDIUtil.getBeanFromCDI(KillMapCronJob.class).getCurrentJob();

    int numClassesQueued = KillmapDAO.getNumClassKillmapJobsQueued();
    int numGamesQueued = KillmapDAO.getNumGameKillmapJobsQueued();
%>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminKillMaps"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div class="card">
        <div class="card-body">
            <h5 class="card-title">Killmap Status</h5>

            <p class="m-0">
                <%= numClassesQueued %> <%= pluralize(numClassesQueued, "Class", "Classes") %> and
                <%= numGamesQueued %> <%= pluralize(numGamesQueued, "Game", "Games") %> currently queued.
            </p>

            <p class="m-0">
                <c:choose>
                    <c:when test="${killMapCronJob.enabled}">
                        Killmap Processing is <span class="text-success">enabled</span>.
                    </c:when>
                    <c:otherwise>
                        Killmap Processing is <span class="text-danger">disabled</span>.
                    </c:otherwise>
                </c:choose>
            </p>

            <%
                if (currentJob != null) {
                    String jobType;
                    switch (currentJob.getType()) {
                        case CLASS: jobType = "Class"; break;
                        case GAME: jobType = "Game"; break;
                        default: jobType = "[Unknown]";
                    }
            %>
                <p>Currently processing: <%=jobType%> with ID <%=currentJob.getId()%></p>
            <%
                }
            %>

            <form id="killmap-processor-settings" class="mt-3" name="killmap-processor-settings" title="<%=processorExplanation%>"
                  method="post">
                <input type="hidden" name="formType" value="toggleKillMapProcessing">
                <c:choose>
                    <c:when test="${killMapCronJob.enabled}">
                        <button type="submit" name="enable" value="false" id="toggle-killmap-processing" class="btn btn-danger">
                            Disable KillMap Processing
                        </button>
                    </c:when>
                    <c:otherwise>
                        <button type="submit" name="enable" value="true" id="toggle-killmap-processing" class="btn btn-success">
                            Enable KillMap Processing
                        </button>
                    </c:otherwise>
                </c:choose>
            </form>
        </div>
    </div>

    <ul class="nav nav-tabs my-4">
        <li class="nav-item">
            <a class="nav-link <%=currentPage == KillmapPage.MANUAL ? "active" : ""%>"
               href="${url.forPath(Paths.ADMIN_KILLMAPS)}/manual">
                Enter IDs
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link <%=currentPage == KillmapPage.AVAILABLE ? "active" : ""%>"
               href="${url.forPath(Paths.ADMIN_KILLMAPS)}/available">
                Select Classes / Games / Classrooms
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link <%=currentPage == KillmapPage.QUEUE ? "active" : ""%>"
               href="${url.forPath(Paths.ADMIN_KILLMAPS)}/queue">
                Queued Killmap Jobs
            </a>
        </li>
    </ul>

    <% if (currentPage == KillmapPage.MANUAL) { %>

        <div class="card mb-4">
            <div class="card-header">
                Classes
            </div>
            <div class="card-body">
                <div class="row mb-3">
                    <div class="col-12">
                        <label for="class-ids" class="form-label">
                            <a data-bs-toggle="modal" data-bs-target="#class-ids-explanation" class="text-decoration-none text-reset cursor-pointer">
                                Class IDs
                                <span class="fa fa-question-circle ms-1"></span>
                            </a>
                        </label>
                        <textarea name="ids" id="class-ids" class="form-control" placeholder="Class IDs" rows="3"></textarea>
                    </div>
                </div>
                <div class="row g-2">
                    <div class="col-auto">
                        <button id="queue-ids-classes" class="btn btn-primary">Queue</button>
                    </div>
                    <div class="col-auto">
                        <button id="delete-ids-classes" class="btn btn-danger">Delete</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                Games
            </div>
            <div class="card-body">
                <div class="row mb-3">
                    <div class="col-12">
                        <label for="game-ids" class="form-label">
                            <a data-bs-toggle="modal" data-bs-target="#game-ids-explanation" class="text-decoration-none text-reset cursor-pointer">
                                Game IDs
                                <span class="fa fa-question-circle ms-1"></span>
                            </a>
                        </label>
                        <textarea name="ids" id="game-ids" class="form-control" placeholder="Game IDs" rows="3"></textarea>
                    </div>
                </div>
                <div class="row g-2">
                    <div class="col-auto">
                        <button id="queue-ids-games" class="btn btn-primary">Queue</button>
                    </div>
                    <div class="col-auto">
                        <button id="delete-ids-games" class="btn btn-danger">Delete</button>
                    </div>
                </div>
            </div>
        </div>

        <t:modal title="Class IDs Explanation" id="class-ids-explanation">
            <jsp:attribute name="content">
                Comma separated list of class IDs to generate killmaps for.
                Newlines and whitespaces are allowed.
            </jsp:attribute>
        </t:modal>
        <t:modal title="Game IDs Explanation" id="game-ids-explanation">
            <jsp:attribute name="content">
                Comma separated list of game IDs to generate killmaps for.
                Newlines and whitespaces are allowed.
            </jsp:attribute>
        </t:modal>

    <% } else if (currentPage == KillmapPage.AVAILABLE || currentPage == KillmapPage.QUEUE) { %>

        <div class="card mb-4">
            <div class="card-header d-flex justify-content-between flex-wrap gap-1">
                Classes
                <div class="d-flex flex-wrap gap-2">
                    <div>
                        <input type="checkbox" id="toggle-progress-classes" class="btn-check" autocomplete="off">
                        <label for="toggle-progress-classes" class="btn btn-xs btn-outline-secondary">
                            Show progress
                            <i class="fa fa-check btn-check-active"></i>
                        </label>
                    </div>
                    <button id="invert-selection-classes" class="btn btn-xs btn-secondary">Invert Selection</button>
                    <% if (currentPage == KillmapPage.AVAILABLE) { %>
                        <div class="text-nowrap">
                            <button id="queue-selection-classes" class="btn btn-xs btn-primary">Queue Selected</button>
                            <button id="delete-selection-classes" class="btn btn-xs btn-danger">Delete Selected Killmaps</button>
                        </div>
                    <% } else { %>
                        <button id="cancel-selection-classes" class="btn btn-xs btn-primary">Cancel Selected</button>
                    <% } %>
                    <input type="search" id="search-classes" class="form-control input-xs" placeholder="Search">
                </div>
            </div>
            <div class="card-body">
                <table id="table-classes" class="table"></table>
            </div>
        </div>

        <div class="card mb-4">
            <div class="card-header d-flex justify-content-between flex-wrap gap-1">
                Games
                <div class="d-flex flex-wrap gap-2">
                    <div>
                        <input type="checkbox" id="toggle-progress-games" class="btn-check" autocomplete="off">
                        <label for="toggle-progress-games" class="btn btn-xs btn-outline-secondary">
                            Show progress
                            <i class="fa fa-check btn-check-active"></i>
                        </label>
                    </div>
                    <button id="invert-selection-games" class="btn btn-xs btn-secondary">Invert Selection</button>
                    <% if (currentPage == KillmapPage.AVAILABLE) { %>
                        <div class="text-nowrap">
                            <button id="queue-selection-games" class="btn btn-xs btn-primary">Queue Selected</button>
                            <button id="delete-selection-games" class="btn btn-xs btn-danger">Delete Selected Killmaps</button>
                        </div>
                    <% } else { %>
                        <button id="cancel-selection-games" class="btn btn-xs btn-primary">Cancel Selected</button>
                    <% } %>
                    <input type="search" id="search-games" class="form-control input-xs" placeholder="Search">
                </div>
            </div>
            <div class="card-body">
                <table id="table-games" class="table"></table>
            </div>
        </div>

        <div class="card">
            <div class="card-header d-flex justify-content-between flex-wrap gap-1">
                Classrooms
                <div class="d-flex flex-wrap gap-2">
                    <div>
                        <input type="checkbox" id="toggle-progress-classrooms" class="btn-check" autocomplete="off">
                        <label for="toggle-progress-classrooms" class="btn btn-xs btn-outline-secondary">
                            Show progress
                            <i class="fa fa-check btn-check-active"></i>
                        </label>
                    </div>
                    <button id="invert-selection-classrooms" class="btn btn-xs btn-secondary">Invert Selection</button>
                    <% if (currentPage == KillmapPage.AVAILABLE) { %>
                    <div class="text-nowrap">
                        <button id="queue-selection-classrooms" class="btn btn-xs btn-primary">Queue Selected</button>
                        <button id="delete-selection-classrooms" class="btn btn-xs btn-danger">Delete Selected Killmaps</button>
                    </div>
                    <% } else { %>
                    <button id="cancel-selection-classrooms" class="btn btn-xs btn-primary">Cancel Selected</button>
                    <% } %>
                    <input type="search" id="search-classrooms" class="form-control input-xs" placeholder="Search">
                </div>
            </div>
            <div class="card-body">
                <table id="table-classrooms" class="table"></table>
            </div>
        </div>

        <div class="row g-2 mt-4">
            <div class="col-12">
                <div class="btn-group">
                    <a download="classes.csv"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=class&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-classes">
                        <i class="fa fa-download me-1"></i>
                        Download classes table
                    </a>
                    <a download="classes.csv"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=class&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-classes-csv">
                        as CSV
                    </a>
                    <a download="classes.json"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=class&fileType=json"
                       class="btn btn-sm btn-outline-secondary" id="download-classes-json">
                        as JSON
                    </a>
                </div>
            </div>
            <div class="col-12">
                <div class="btn-group">
                    <a download="games.csv"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=game&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-games">
                        <i class="fa fa-download me-1"></i>
                        Download games table
                    </a>
                    <a download="games.csv"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=game&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-games-csv">
                        as CSV
                    </a>
                    <a download="games.json"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=game&fileType=json"
                       class="btn btn-sm btn-outline-secondary" id="download-games-json">
                        as JSON
                    </a>
                </div>
            </div>
            <div class="col-12">
                <div class="btn-group">
                    <a download="classrooms.csv"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=classroom&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-classrooms">
                        <i class="fa fa-download me-1"></i>
                        Download classrooms table
                    </a>
                    <a download="classrooms.csv"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=classroom&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-classrooms-csv">
                        as CSV
                    </a>
                    <a download="classrooms.json"
                       href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%=currentPage%>&killmapType=classroom&fileType=json"
                       class="btn btn-sm btn-outline-secondary" id="download-classrooms-json">
                        as JSON
                    </a>
                </div>
            </div>
        </div>
    <% } %>
</div>

<script type="module">
    import DataTable from '${url.forPath("/js/datatables.mjs")}';
    import {parseHTML, postForm, DataTablesUtils} from '${url.forPath("/js/codedefenders_main.mjs")}';


<% if (currentPage == KillmapPage.MANUAL) { %>

    document.getElementById('queue-ids-classes').addEventListener('click', event => {
        const idsField = document.getElementById('class-ids');
        postForm(null, {
            ids: idsField.value,
            formType: 'submitKillMapJobs',
            killmapType: 'class'
        });
    });
    document.getElementById('queue-ids-games').addEventListener('click', event => {
        const idsField = document.getElementById('game-ids');
        postForm(null, {
            ids: idsField.value,
            formType: 'submitKillMapJobs',
            killmapType: 'game'
        });
    });

    document.getElementById('delete-ids-classes').addEventListener('click', event => {
        const idsField = document.getElementById('class-ids');
        if (confirm('Are you sure you want to delete the specified killmaps?')) {
            postForm(null, {
                ids: idsField.value,
                formType: 'deleteKillmaps',
                killmapType: 'class'
            });
        }
    });
    document.getElementById('delete-ids-games').addEventListener('click', event => {
        const idsField = document.getElementById('class-ids');
        if (confirm('Are you sure you want to delete the specified killmaps?')) {
            postForm(null, {
                ids: idsField.value,
                formType: 'deleteKillmaps',
                killmapType: 'game'
            });
        }
    });

<% } else if (currentPage == KillmapPage.AVAILABLE || currentPage == KillmapPage.QUEUE) { %>

    DataTablesUtils.registerSortBySelected();

    const getId = function (rowData) {
        return rowData.classId ?? rowData.gameId ?? rowData.classroomId;
    }
    const getSelectedIds = function (table) {
        return DataTablesUtils.getSelected(table)
                .map(getId);
    };

    const colorRow = function () {
        const data = this.data();
        const node = this.node();
        let percentage;

        if (data.nrExpectedEntries !== 0) {
            percentage = ((data.nrEntries * 100) / data.nrExpectedEntries).toFixed(0);
        } else {
            percentage = 0;
        }

        node.style.background = 'linear-gradient(to right, '
            + 'rgba(41, 182, 246, 0.15) ' + percentage + '%, '
            + 'transparent ' + percentage + '%)';
    };

    const uncolorRow = function () {
        const node = this.node();
        node.style.background = null;
    };

    const invertSelection = function (table) {
        const selectedIds = new Set(getSelectedIds(table));
        table.rows().every(function () {
            const id = getId(this.data());
            if (selectedIds.has(id)) {
                this.deselect();
            } else {
                this.select();
            }
        });
    };

    const renderProgress = function(data, type, row, meta) {
        switch (type) {
            case 'sort':
            case 'type':
                if (data.nrExpectedEntries !== 0) {
                    return (data.nrEntries * 100) / data.nrExpectedEntries;
                } else {
                    return -1;
                }
            case 'filter':
            case 'display':
                if (data.nrExpectedEntries !== 0) {
                    return ((data.nrEntries * 100) / data.nrExpectedEntries).toFixed(0) + '%';
                } else {
                    return 'NA';
                }
            default:
                return data;
        }
    };

    const renderClassName = function(data, type, row, meta) {
        if (data.className === data.classAlias) {
            return data.className;
        } else {
            return data.className + ' (alias ' + data.classAlias + ')';
        }
    };

    let emptyClassTableMessage;
    let emptyGameTableMessage;
    let emptyClassroomTableMessage;

    <% if (currentPage == KillmapPage.AVAILABLE) { %>
        emptyClassTableMessage = 'No classes available.';
        emptyGameTableMessage = 'No games available.';
        emptyClassroomTableMessage = 'No classrooms available.';
    <% } else { %>
        emptyClassTableMessage = 'No classes queued for killmap computation.';
        emptyGameTableMessage = 'No games queued for killmap computation.';
        emptyClassroomTableMessage = 'No classrooms queued for killmap computation.';
    <% } %>

    const classTable = new DataTable(document.getElementById('table-classes'), {
        ajax: {
            url: '${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%= currentPage %>&killmapType=class&fileType=json',
            dataSrc: 'data'
        },
        columns: [
            {
                data: null,
                title: 'Select',
                defaultContent: '',
                className: 'select-checkbox align-middle',
                orderDataType: 'select-extension',
                width: '3em'
            },
            {
                data: 'classId',
                title: 'Class',
                type: 'num'
            },
            {
                data: null,
                render: renderClassName,
                title: 'Name',
                type: 'string'
            },
            {
                data: 'nrMutants',
                title: 'Mutants',
                type: 'num'
            },
            {
                data: 'nrTests',
                title: 'Tests',
                type: 'num'
            },
            {
                data: null,
                render: renderProgress,
                title: 'Computed',
                type: 'num'
            }
        ],
        select: {
            style: 'multi',
            className: 'selected'
        },
        scrollY: '400px',
        scrollCollapse: true,
        paging: false,
        dom: 't',
        language: {emptyTable: emptyClassTableMessage}
    });

    const gameTable = new DataTable(document.getElementById('table-games'), {
        ajax: {
            url: '${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%= currentPage %>&killmapType=game&fileType=json',
            dataSrc: 'data'
        },
        columns: [
            {
                data: null,
                title: 'Select',
                defaultContent: '',
                className: 'select-checkbox align-middle',
                orderDataType: 'select-extension',
                width: '3em'
            },
            {
                data: 'gameId',
                title: 'Game',
                type: 'num'
            },
            {
                data: 'gameMode',
                title: 'Mode',
                type: 'string'
            },
            {
                data: 'nrMutants',
                title: 'Mutants',
                type: 'num'
            },
            {
                data: 'nrTests',
                title: 'Tests',
                type: 'num'
            },
            {
                data: null,
                render: renderProgress,
                title: 'Computed',
                type: 'num'
            },
        ],
        select: {
            style: 'multi',
            className: 'selected'
        },
        scrollY: '400px',
        scrollCollapse: true,
        paging: false,
        dom: 't',
        language: {emptyTable: emptyGameTableMessage}
    });

    const classroomTable = new DataTable(document.getElementById('table-classrooms'), {
        ajax: {
            url: '${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=<%= currentPage %>&killmapType=classroom&fileType=json',
            dataSrc: 'data'
        },
        columns: [
            {
                data: null,
                title: 'Select',
                defaultContent: '',
                className: 'select-checkbox align-middle',
                orderDataType: 'select-extension',
                width: '3em'
            },
            {
                data: 'classroomName',
                type: 'string',
                title: 'Name',
                width: '25em',
                className: 'truncate'
            },
            {
                data: 'nrMutants',
                title: 'Mutants',
                type: 'num'
            },
            {
                data: 'nrTests',
                title: 'Tests',
                type: 'num'
            },
            {
                data: null,
                render: renderProgress,
                title: 'Computed',
                type: 'string'
            }
        ],
        select: {
            style: 'multi',
            className: 'selected'
        },
        scrollY: '400px',
        scrollCollapse: true,
        paging: false,
        dom: 't',
        language: {emptyTable: emptyClassroomTableMessage}
    });

    document.getElementById('toggle-progress-classes').addEventListener('change', event => {
        const colorFun = event.currentTarget.checked ? colorRow : uncolorRow;
        classTable.rows().every(colorFun);
    });
    document.getElementById('toggle-progress-games').addEventListener('change', event => {
        const colorFun = event.currentTarget.checked ? colorRow : uncolorRow;
        gameTable.rows().every(colorFun);
    });
    document.getElementById('toggle-progress-classrooms').addEventListener('change', event => {
        const colorFun = event.currentTarget.checked ? colorRow : uncolorRow;
        classroomTable.rows().every(colorFun);
    });

    document.getElementById('search-classes').addEventListener('keyup', event => {
        classTable.search(this.value).draw()
    });
    document.getElementById('search-games').addEventListener('keyup', event => {
        gameTable.search(this.value).draw()
    });
    document.getElementById('search-classrooms').addEventListener('keyup', event => {
        classroomTable.search(this.value).draw()
    });

    document.getElementById('invert-selection-classes').addEventListener('click', event => {
        invertSelection(classTable)
    });
    document.getElementById('invert-selection-games').addEventListener('click', event => {
        invertSelection(gameTable)
    });
    document.getElementById('invert-selection-classrooms').addEventListener('click', event => {
        invertSelection(classroomTable)
    });

    <% if (currentPage == KillmapPage.AVAILABLE) { %>

        document.getElementById('queue-selection-classes').addEventListener('click', event => {
            postForm(null, {
                ids: getSelectedIds(classTable),
                formType: 'submitKillMapJobs',
                killmapType: 'class'
            });
        });
        document.getElementById('queue-selection-games').addEventListener('click', event => {
            postForm(null, {
                ids: getSelectedIds(gameTable),
                formType: 'submitKillMapJobs',
                killmapType: 'game'
            });
        });
        document.getElementById('queue-selection-classrooms').addEventListener('click', event => {
            postForm(null, {
                ids: getSelectedIds(classroomTable),
                formType: 'submitKillMapJobs',
                killmapType: 'classroom'
            });
        });

        document.getElementById('delete-selection-classes').addEventListener('click', event => {
            if (confirm('Are you sure you want to delete the selected killmaps?')) {
                postForm(null, {
                    ids: getSelectedIds(classTable),
                    formType: 'deleteKillMaps',
                    killmapType: 'class'
                });
            }
        });
        document.getElementById('delete-selection-games').addEventListener('click', event => {
            if (confirm('Are you sure you want to delete the selected killmaps?')) {
                postForm(null, {
                    ids: getSelectedIds(gameTable),
                    formType: 'deleteKillMaps',
                    killmapType: 'game'
                });
            }
        });
        document.getElementById('delete-selection-classrooms').addEventListener('click', event => {
            if (confirm('Are you sure you want to delete the selected killmaps?')) {
                postForm(null, {
                    ids: getSelectedIds(classroomTable),
                    formType: 'deleteKillMaps',
                    killmapType: 'classroom'
                });
            }
        });

    <% } %>

    <% if (currentPage == KillmapPage.QUEUE) { %>

        document.getElementById('cancel-selection-classes').addEventListener('click', event => {
            postForm(null, {
                ids: getSelectedIds(classTable),
                formType: 'cancelKillMapJobs',
                killmapType: 'class'
            });
        });
        document.getElementById('cancel-selection-games').addEventListener('click', event => {
            postForm(null, {
                ids: getSelectedIds(gameTable),
                formType: 'cancelKillMapJobs',
                killmapType: 'game'
            });
        });
        document.getElementById('cancel-selection-classrooms').addEventListener('click', event => {
            postForm(null, {
                ids: getSelectedIds(classroomTable),
                formType: 'cancelKillMapJobs',
                killmapType: 'classroom'
            });
        });

    <% } %>
<% } %>
</script>

<%@ include file="/jsp/footer.jsp" %>

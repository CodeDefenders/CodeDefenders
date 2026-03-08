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
<%@ page import="org.codedefenders.cron.KillMapCronJob" %>
<%@ page import="org.codedefenders.execution.KillMap.KillMapJob" %>
<%@ page import="org.codedefenders.database.KillmapDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME" %>
<%@ page import="org.codedefenders.servlets.admin.AdminKillmapManagement.KillmapPage" %>
<%@ page import="org.codedefenders.util.CDIUtil" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.xnap.commons.i18n.I18n" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="fn" uri="org.codedefenders.functions" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="killMapCronJob" type="org.codedefenders.cron.KillMapCronJob"--%>

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

    pageContext.setAttribute("currentPage", currentPage);
    pageContext.setAttribute("processorExplanation", processorExplanation);
    pageContext.setAttribute("currentJob", currentJob);
    pageContext.setAttribute("numClassesQueued", numClassesQueued);
    pageContext.setAttribute("numGamesQueued", numGamesQueued);

    I18n i18n = (I18n) request.getAttribute("i18n");

    if (currentJob != null) {
        String jobType;
        switch (currentJob.getType()) {
            case CLASS: jobType = "Class";
                break;
            case GAME:
                jobType = i18n.tr("Game");
                break;
            case CLASSROOM:
                jobType = i18n.tr("Classroom");
                break;
            default:
                jobType = i18n.tr("[Unknown]");
        }
        pageContext.setAttribute("jobType", jobType);
    }

    // Get the enum constants here, since EL doesn't seem to work with inner-class enums.
    pageContext.setAttribute("MANUAL", KillmapPage.MANUAL);
    pageContext.setAttribute("AVAILABLE", KillmapPage.AVAILABLE);
    pageContext.setAttribute("QUEUE", KillmapPage.QUEUE);
%>

<p:main_page title="${i18n.tr('KillMap Management')}">
    <div class="container">
        <t:admin_navigation activePage="adminKillMaps"/>

        <div class="card">
            <div class="card-body">
                <h5 class="card-title">${i18n.tr('Killmap Status')}</h5>

                <p class="m-0">
                        ${numClassesQueued} ${i18n.trn('Class', 'Classes', numClassesQueued)} ${i18n.tr('and')}
                        ${numGamesQueued} ${i18n.trn('Game', 'Games', numGamesQueued)} ${i18n.tr('currently queued.')}
                </p>

                <p class="m-0">
                    <c:choose>
                        <c:when test="${killMapCronJob.enabled}">
                            ${i18n.tr('Killmap Processing is')} <span class="text-success">${i18n.tr('enabled')}</span>.
                        </c:when>
                        <c:otherwise>
                            ${i18n.tr('Killmap Processing is')} <span class="text-danger">${i18n.tr('disabled')}</span>.
                        </c:otherwise>
                    </c:choose>
                </p>

                <c:if test="${currentJob != null}">
                    <p>${i18n.tr('Currently processing: {0} with ID {1}', jobType, currentJob.id)}</p>
                </c:if>

                <form id="killmap-processor-settings" class="mt-3" name="killmap-processor-settings" title="${processorExplanation}"
                      method="post">
                    <input type="hidden" name="formType" value="toggleKillMapProcessing">
                    <c:choose>
                        <c:when test="${killMapCronJob.enabled}">
                            <button type="submit" name="enable" value="false" id="toggle-killmap-processing" class="btn btn-danger">
                                    ${i18n.tr('Disable KillMap Processing')}
                            </button>
                        </c:when>
                        <c:otherwise>
                            <button type="submit" name="enable" value="true" id="toggle-killmap-processing" class="btn btn-success">
                                    ${i18n.tr('Enable KillMap Processing')}
                            </button>
                        </c:otherwise>
                    </c:choose>
                </form>
            </div>
        </div>

        <ul class="nav nav-tabs my-4">
            <li class="nav-item">
                <a class="nav-link ${currentPage == MANUAL ? 'active' : ''}"
                   href="${url.forPath(Paths.ADMIN_KILLMAPS)}/manual">
                        ${i18n.tr('Enter IDs')}
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${currentPage == AVAILABLE ? 'active' : ''}"
                   href="${url.forPath(Paths.ADMIN_KILLMAPS)}/available">
                        ${i18n.tr('Select Classes / Games / Classrooms')}
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${currentPage == QUEUE ? 'active' : ''}"
                   href="${url.forPath(Paths.ADMIN_KILLMAPS)}/queue">
                        ${i18n.tr('Queued Killmap Jobs')}
                </a>
            </li>
        </ul>

        <c:if test="${currentPage == MANUAL}">
            <div class="card mb-4">
                <div class="card-header">
                        ${i18n.tr('Classes')}
                </div>
                <div class="card-body">
                    <div class="row mb-3">
                        <div class="col-12">
                            <label for="class-ids" class="form-label">
                                <a data-bs-toggle="modal" data-bs-target="#class-ids-explanation" class="text-decoration-none text-reset cursor-pointer">
                                        ${i18n.tr('Class IDs')}
                                    <span class="fa fa-question-circle ms-1"></span>
                                </a>
                            </label>
                            <textarea name="ids" id="class-ids" class="form-control"
                                      placeholder="${i18n.tr('Class IDs')}" rows="3"></textarea>
                        </div>
                    </div>
                    <div class="row g-2">
                        <div class="col-auto">
                            <button id="queue-ids-classes" class="btn btn-primary">${i18n.tr('Queue')}</button>
                        </div>
                        <div class="col-auto">
                            <button id="delete-ids-classes" class="btn btn-danger">${i18n.tr('Delete')}</button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                        ${i18n.tr('Games')}
                </div>
                <div class="card-body">
                    <div class="row mb-3">
                        <div class="col-12">
                            <label for="game-ids" class="form-label">
                                <a data-bs-toggle="modal" data-bs-target="#game-ids-explanation" class="text-decoration-none text-reset cursor-pointer">
                                        ${i18n.tr('Game IDs')}
                                    <span class="fa fa-question-circle ms-1"></span>
                                </a>
                            </label>
                            <textarea name="ids" id="game-ids" class="form-control" placeholder="${i18n.tr('Game IDs')}"
                                      rows="3"></textarea>
                        </div>
                    </div>
                    <div class="row g-2">
                        <div class="col-auto">
                            <button id="queue-ids-games" class="btn btn-primary">${i18n.tr('Queue')}</button>
                        </div>
                        <div class="col-auto">
                            <button id="delete-ids-games" class="btn btn-danger">${i18n.tr('Delete')}</button>
                        </div>
                    </div>
                </div>
            </div>

            <t:modal title="${i18n.tr('Class IDs Explanation')}" id="class-ids-explanation">
                <jsp:attribute name="content">
                    ${i18n.tr('Comma separated list of class IDs to generate killmaps for. Newlines and whitespaces are allowed.')}
                </jsp:attribute>
            </t:modal>
            <t:modal title="${i18n.tr('Game IDs Explanation')}" id="game-ids-explanation">
                <jsp:attribute name="content">
                    ${i18n.tr('Comma separated list of game IDs to generate killmaps for. Newlines and whitespaces are allowed.')}
                </jsp:attribute>
            </t:modal>
        </c:if>

        <c:if test="${currentPage == AVAILABLE || currentPage == QUEUE}">
            <div class="card mb-4">
                <div class="card-header d-flex justify-content-between flex-wrap gap-1">
                        ${i18n.tr('Classes')}
                    <div class="d-flex flex-wrap gap-2">
                        <div>
                            <input type="checkbox" id="toggle-progress-classes" class="btn-check" autocomplete="off">
                            <label for="toggle-progress-classes" class="btn btn-xs btn-outline-secondary">
                                    ${i18n.tr('Show progress')}
                                <i class="fa fa-check btn-check-active"></i>
                            </label>
                        </div>
                        <button id="invert-selection-classes"
                                class="btn btn-xs btn-secondary">${i18n.tr('Invert Selection')}</button>
                        <c:choose>
                            <c:when test="${currentPage == AVAILABLE}">
                                <div class="text-nowrap">
                                    <button id="queue-selection-classes"
                                            class="btn btn-xs btn-primary">${i18n.tr('Queue Selected')}</button>
                                    <button id="delete-selection-classes"
                                            class="btn btn-xs btn-danger">${i18n.tr('Delete Selected Killmaps')}</button>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <button id="cancel-selection-classes"
                                        class="btn btn-xs btn-primary">${i18n.tr('Cancel Selected')}</button>
                            </c:otherwise>
                        </c:choose>
                        <input type="search" id="search-classes" class="form-control input-xs"
                               placeholder="${i18n.tr('Search')}">
                    </div>
                </div>
                <div class="card-body">
                    <table id="table-classes" class="table"></table>
                </div>
            </div>

            <div class="card mb-4">
                <div class="card-header d-flex justify-content-between flex-wrap gap-1">
                        ${i18n.tr('Games')}
                    <div class="d-flex flex-wrap gap-2">
                        <div>
                            <input type="checkbox" id="toggle-progress-games" class="btn-check" autocomplete="off">
                            <label for="toggle-progress-games" class="btn btn-xs btn-outline-secondary">
                                    ${i18n.tr('Show progress')}
                                <i class="fa fa-check btn-check-active"></i>
                            </label>
                        </div>
                        <button id="invert-selection-games"
                                class="btn btn-xs btn-secondary">${i18n.tr('Invert Selection')}</button>
                        <c:choose>
                            <c:when test="${currentPage == AVAILABLE}">
                                <div class="text-nowrap">
                                    <button id="queue-selection-games"
                                            class="btn btn-xs btn-primary">${i18n.tr('Queue Selected')}</button>
                                    <button id="delete-selection-games"
                                            class="btn btn-xs btn-danger">${i18n.tr('Delete Selected Killmaps')}</button>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <button id="cancel-selection-games"
                                        class="btn btn-xs btn-primary">${i18n.tr('Cancel Selected')}</button>
                            </c:otherwise>
                        </c:choose>
                        <input type="search" id="search-games" class="form-control input-xs"
                               placeholder="${i18n.tr('Search')}">
                    </div>
                </div>
                <div class="card-body">
                    <table id="table-games" class="table"></table>
                </div>
            </div>

            <div class="card mb-4">
                <div class="card-header d-flex justify-content-between flex-wrap gap-1">
                        ${i18n.tr('Classrooms')}
                    <div class="d-flex flex-wrap gap-2">
                        <div>
                            <input type="checkbox" id="toggle-progress-classrooms" class="btn-check" autocomplete="off">
                            <label for="toggle-progress-classrooms" class="btn btn-xs btn-outline-secondary">
                                    ${i18n.tr('Show progress')}
                                <i class="fa fa-check btn-check-active"></i>
                            </label>
                        </div>
                        <button id="invert-selection-classrooms"
                                class="btn btn-xs btn-secondary">${i18n.tr('Invert Selection')}</button>
                        <c:choose>
                            <c:when test="${currentPage == AVAILABLE}">
                                <div class="text-nowrap">
                                    <button id="queue-selection-classrooms"
                                            class="btn btn-xs btn-primary">${i18n.tr('Queue Selected')}</button>
                                    <button id="delete-selection-classrooms"
                                            class="btn btn-xs btn-danger">${i18n.tr('Delete Selected Killmaps')}</button>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <button id="cancel-selection-classrooms"
                                        class="btn btn-xs btn-primary">${i18n.tr('Cancel Selected')}</button>
                            </c:otherwise>
                        </c:choose>
                        <input type="search" id="search-classrooms" class="form-control input-xs"
                               placeholder="${i18n.tr('Search')}">
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
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=class&fileType=csv"
                           class="btn btn-sm btn-outline-secondary" id="download-classes">
                            <i class="fa fa-download me-1"></i>
                                ${i18n.tr('Download classes table')}
                        </a>
                        <a download="classes.csv"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=class&fileType=csv"
                           class="btn btn-sm btn-outline-secondary" id="download-classes-csv">
                                ${i18n.tr('as CSV')}
                        </a>
                        <a download="classes.json"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=class&fileType=json"
                           class="btn btn-sm btn-outline-secondary" id="download-classes-json">
                                ${i18n.tr('as JSON')}
                        </a>
                    </div>
                </div>
                <div class="col-12">
                    <div class="btn-group">
                        <a download="games.csv"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=game&fileType=csv"
                           class="btn btn-sm btn-outline-secondary" id="download-games">
                            <i class="fa fa-download me-1"></i>
                                ${i18n.tr('Download games table')}
                        </a>
                        <a download="games.csv"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=game&fileType=csv"
                           class="btn btn-sm btn-outline-secondary" id="download-games-csv">
                                ${i18n.tr('as CSV')}
                        </a>
                        <a download="games.json"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=game&fileType=json"
                           class="btn btn-sm btn-outline-secondary" id="download-games-json">
                                ${i18n.tr('as JSON')}
                        </a>
                    </div>
                </div>
                <div class="col-12">
                    <div class="btn-group">
                        <a download="classrooms.csv"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=classroom&fileType=csv"
                           class="btn btn-sm btn-outline-secondary" id="download-classrooms">
                            <i class="fa fa-download me-1"></i>
                                ${i18n.tr('Download classrooms table')}
                        </a>
                        <a download="classrooms.csv"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=classroom&fileType=csv"
                           class="btn btn-sm btn-outline-secondary" id="download-classrooms-csv">
                                ${i18n.tr('as CSV')}
                        </a>
                        <a download="classrooms.json"
                           href="${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=classroom&fileType=json"
                           class="btn btn-sm btn-outline-secondary" id="download-classrooms-json">
                                ${i18n.tr('as JSON')}
                        </a>
                    </div>
                </div>
            </div>
        </c:if>
    </div>

    <script type="module">
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import {postForm, DataTablesUtils} from '${url.forPath("/js/codedefenders_main.mjs")}';


        <c:if test="${currentPage == MANUAL}">
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
                if (confirm('${i18n.tr('Are you sure you want to delete the specified killmaps?')}')) {
                    postForm(null, {
                        ids: idsField.value,
                        formType: 'deleteKillmaps',
                        killmapType: 'class'
                    });
                }
            });
            document.getElementById('delete-ids-games').addEventListener('click', event => {
                const idsField = document.getElementById('class-ids');
                if (confirm('${i18n.tr('Are you sure you want to delete the specified killmaps?')}')) {
                    postForm(null, {
                        ids: idsField.value,
                        formType: 'deleteKillmaps',
                        killmapType: 'game'
                    });
                }
            });
        </c:if>

        <c:if test="${currentPage == AVAILABLE || currentPage == QUEUE}">
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

            <c:choose>
                <c:when test="${currentPage == AVAILABLE}">
        emptyClassTableMessage = '${i18n.tr('No classes available.')}';
        emptyGameTableMessage = '${i18n.tr('No games available.')}';
        emptyClassroomTableMessage = '${i18n.tr('No classrooms available.')}';
                </c:when>
                <c:otherwise>
        emptyClassTableMessage = '${i18n.tr('No classes queued for killmap computation.')}';
        emptyGameTableMessage = '${i18n.tr('No games queued for killmap computation.')}';
        emptyClassroomTableMessage = '${i18n.tr('No classrooms queued for killmap computation.')}';
                </c:otherwise>
            </c:choose>

            const classTable = new DataTable(document.getElementById('table-classes'), {
                ajax: {
                    url: '${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=class&fileType=json',
                    dataSrc: 'data'
                },
                columns: [
                    {
                        data: null,
                        title: '${i18n.tr('Select')}',
                        defaultContent: '',
                        className: 'select-checkbox align-middle',
                        orderDataType: 'select-extension',
                        width: '3em'
                    },
                    {
                        data: 'classId',
                        title: '${i18n.tr('Class')}',
                        type: 'num'
                    },
                    {
                        data: null,
                        render: renderClassName,
                        title: '${i18n.tr('Name')}',
                        type: 'string'
                    },
                    {
                        data: 'nrMutants',
                        title: '${i18n.tr('Mutants')}',
                        type: 'num'
                    },
                    {
                        data: 'nrTests',
                        title: '${i18n.tr('Tests')}',
                        type: 'num'
                    },
                    {
                        data: null,
                        render: renderProgress,
                        title: '${i18n.tr('Computed')}',
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
                language: DataTablesUtils.language({emptyTable: emptyClassTableMessage})
            });

            const gameTable = new DataTable(document.getElementById('table-games'), {
                ajax: {
                    url: '${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=game&fileType=json',
                    dataSrc: 'data'
                },
                columns: [
                    {
                        data: null,
                        title: '${i18n.tr('Select')}',
                        defaultContent: '',
                        className: 'select-checkbox align-middle',
                        orderDataType: 'select-extension',
                        width: '3em'
                    },
                    {
                        data: 'gameId',
                        title: '${i18n.tr('Game')}',
                        type: 'num'
                    },
                    {
                        data: 'gameMode',
                        title: '${i18n.tr('Mode')}',
                        type: 'string'
                    },
                    {
                        data: 'nrMutants',
                        title: '${i18n.tr('Mutants')}',
                        type: 'num'
                    },
                    {
                        data: 'nrTests',
                        title: '${i18n.tr('Tests')}',
                        type: 'num'
                    },
                    {
                        data: null,
                        render: renderProgress,
                        title: '${i18n.tr('Computed')}',
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
                language: DataTablesUtils.language({emptyTable: emptyGameTableMessage})
            });

            const classroomTable = new DataTable(document.getElementById('table-classrooms'), {
                ajax: {
                    url: '${url.forPath(Paths.API_KILLMAP_MANAGEMENT)}?dataType=${currentPage}&killmapType=classroom&fileType=json',
                    dataSrc: 'data'
                },
                columns: [
                    {
                        data: null,
                        title: '${i18n.tr('Select')}',
                        defaultContent: '',
                        className: 'select-checkbox align-middle',
                        orderDataType: 'select-extension',
                        width: '3em'
                    },
                    {
                        data: 'classroomName',
                        type: 'string',
                        title: '${i18n.tr('Name')}',
                        width: '25em',
                        className: 'truncate'
                    },
                    {
                        data: 'nrMutants',
                        title: '${i18n.tr('Mutants')}',
                        type: 'num'
                    },
                    {
                        data: 'nrTests',
                        title: '${i18n.tr('Tests')}',
                        type: 'num'
                    },
                    {
                        data: null,
                        render: renderProgress,
                        title: '${i18n.tr('Computed')}',
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
                language: DataTablesUtils.language({emptyTable: emptyClassroomTableMessage})
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

            <c:if test="${currentPage == AVAILABLE}">
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
                    if (confirm('${i18n.tr('Are you sure you want to delete the selected killmaps?')}')) {
                        postForm(null, {
                            ids: getSelectedIds(classTable),
                            formType: 'deleteKillMaps',
                            killmapType: 'class'
                        });
                    }
                });
                document.getElementById('delete-selection-games').addEventListener('click', event => {
                    if (confirm('${i18n.tr('Are you sure you want to delete the selected killmaps?')}')) {
                        postForm(null, {
                            ids: getSelectedIds(gameTable),
                            formType: 'deleteKillMaps',
                            killmapType: 'game'
                        });
                    }
                });
                document.getElementById('delete-selection-classrooms').addEventListener('click', event => {
                    if (confirm('${i18n.tr('Are you sure you want to delete the selected killmaps?')}')) {
                        postForm(null, {
                            ids: getSelectedIds(classroomTable),
                            formType: 'deleteKillMaps',
                            killmapType: 'classroom'
                        });
                    }
                });
            </c:if>

            <c:if test="${currentPage == QUEUE}">
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
            </c:if>
        </c:if>
    </script>
</p:main_page>

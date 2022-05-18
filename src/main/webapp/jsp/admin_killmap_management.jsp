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
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME" %>
<%@ page import="org.codedefenders.database.KillmapDAO" %>
<%@ page import="org.codedefenders.execution.KillMapProcessor" %>
<%@ page import="org.codedefenders.execution.KillMapProcessor.KillMapJob" %>
<%@ page import="static org.codedefenders.util.MessageUtils.pluralize" %>
<%@ page import="org.codedefenders.servlets.admin.AdminKillmapManagement.KillmapPage" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

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
    ServletContext context = pageContext.getServletContext();
    KillMapProcessor processor = (KillMapProcessor) context.getAttribute(KillMapProcessor.NAME);

    boolean processorEnabled = processor.isEnabled();
    KillMapJob currentJob = processor.getCurrentJob();

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
                <% if (processorEnabled) { %>
                    Killmap Processing is <span class="text-success">enabled</span>.
                <% } else { %>
                    Killmap Processing is <span class="text-danger">disabled</span>.
                <% } %>
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
                <% if (processorEnabled) { %>
                    <button type="submit" name="enable" value="false" id="toggle-killmap-processing" class="btn btn-danger">
                        Disable KillMap Processing
                    </button>
                <% } else { %>
                    <button type="submit" name="enable" value="true" id="toggle-killmap-processing" class="btn btn-success">
                        Enable KillMap Processing
                    </button>
                <% } %>
            </form>
        </div>
    </div>

    <ul class="nav nav-tabs my-4">
        <li class="nav-item">
            <a class="nav-link <%=currentPage == KillmapPage.MANUAL ? "active" : ""%>"
               href="<%=request.getContextPath() + Paths.ADMIN_KILLMAPS + "/manual"%>">
                Enter IDs
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link <%=currentPage == KillmapPage.AVAILABLE ? "active" : ""%>"
               href="<%=request.getContextPath() + Paths.ADMIN_KILLMAPS + "/available"%>">
                Select Classes / Games
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link <%=currentPage == KillmapPage.QUEUE ? "active" : ""%>"
               href="<%=request.getContextPath() + Paths.ADMIN_KILLMAPS + "/queue"%>">
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
                <table id="table-classes" class="table table-striped"></table>
            </div>
        </div>

        <div class="card">
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
                <table id="table-games" class="table table-striped"></table>
            </div>
        </div>

        <div class="row g-2 mt-4">
            <div class="col-12">
                <div class="btn-group">
                    <a download="classes.csv"
                       href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%=currentPage%>&killmapType=class&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-classes">
                        <i class="fa fa-download me-1"></i>
                        Download classes table
                    </a>
                    <a download="classes.csv"
                       href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%=currentPage%>&killmapType=class&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-classes-csv">
                        as CSV
                    </a>
                    <a download="classes.json"
                       href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%=currentPage%>&killmapType=class&fileType=json"
                       class="btn btn-sm btn-outline-secondary" id="download-classes-json">
                        as JSON
                    </a>
                </div>
            </div>
            <div class="col-12">
                <div class="btn-group">
                    <a download="games.csv"
                       href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%=currentPage%>&killmapType=game&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-games">
                        <i class="fa fa-download me-1"></i>
                        Download games table
                    </a>
                    <a download="games.csv"
                       href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%=currentPage%>&killmapType=game&fileType=csv"
                       class="btn btn-sm btn-outline-secondary" id="download-games-csv">
                        as CSV
                    </a>
                    <a download="games.json"
                       href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%=currentPage%>&killmapType=game&fileType=json"
                       class="btn btn-sm btn-outline-secondary" id="download-games-json">
                        as JSON
                    </a>
                </div>
            </div>
        </div>
    <% } %>
</div>

<script type="module">
    import DataTable from './js/datatables.mjs';
    import $ from './js/jquery.mjs';


    const postIds = function (idsString, formType, killmapType) {
        const form = $(
              '<form method="post">'
            +     '<input type="hidden" name="formType" value="' + formType + '">'
            +     '<input type="hidden" name="killmapType" value="' + killmapType + '">'
            + '</form>'
        );

        /* Construct form field with ids like this so we dont have to sanitize the ids. */
        const idsField = $('<input type="hidden" name="ids" value="">');
        idsField.val(idsString);
        form.append(idsField);

        $('body').append(form);
        form.submit();
    };

<% if (currentPage == KillmapPage.MANUAL) { %>

    $(document).ready(function() {
        $('#queue-ids-classes').on('click',  () => postIds($("#class-ids").val(), 'submitKillMapJobs', 'class'));
        $('#queue-ids-games').on('click',    () => postIds($("#game-ids").val(), 'submitKillMapJobs', 'game'));

        $('#delete-ids-classes').on('click', () => {
            if (confirm('Are you sure you want to delete the specified killmaps?'))
                postIds($("#class-ids").val(), 'deleteKillMaps', 'class');
        });
        $('#delete-ids-games').on('click', () => {
            if (confirm('Are you sure you want to delete the specified killmaps?'))
                postIds($("#game-ids").val(), 'deleteKillMaps', 'game');
        });
    });

<% } else if (currentPage == KillmapPage.AVAILABLE || currentPage == KillmapPage.QUEUE) { %>

    const postTable = function (table, formType, killmapType) {
        const data = table.data();
        const ids = [];

        for (let i = 0; i < data.length; i++) {
            const node = table.row(i).node();
            const checkbox = $(node).find('input');

            if (checkbox.is(':checked')) {
                ids.push(data[i][killmapType + 'Id']);
            }
        }

        postIds(JSON.stringify(ids), formType, killmapType);
    };

    const colorRow = function () {
        const data = this.data();
        const node = this.node();
        let percentage;

        const expectedNrEntries = (data.nrTests * data.nrMutants);
        if (expectedNrEntries !== 0) {
            percentage = ((data.nrEntries * 100) / expectedNrEntries).toFixed(0);
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
        const data = table.data();

        for (let i = 0; i < data.length; i++) {
            const node = table.row(i).node();
            const checkbox = $(node).find('input');
            checkbox.prop('checked', (_, checked) => !checked);
        }
    };

    const progressFromRow = function (row) {
        const expectedNrEntries = (row.nrTests * row.nrMutants);
        if (expectedNrEntries !== 0) {
            return ((row.nrEntries * 100) / expectedNrEntries).toFixed(0) + '%';
        } else {
            return 'NA';
        }
    };

    const classNameFromRow = function (row) {
        if (row.className === row.classAlias) {
            return row.className;
        } else {
            return row.className + ' (alias ' + row.classAlias + ')';
        }
    };

    let emptyClassTableMessage;
    let emptyGameTableMessage;

    <% if (currentPage == KillmapPage.AVAILABLE) { %>
        emptyClassTableMessage = 'No classes available.';
        emptyGameTableMessage = 'No games available.';
    <% } else { %>
        emptyClassTableMessage = 'No classes queued for killmap computation.';
        emptyGameTableMessage = 'No games queued for killmap computation.';
    <% } %>

    $(document).ready(function() {
        const classTable = new DataTable('#table-classes', {
            ajax: {
                url: '<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=class&fileType=json',
                dataSrc: 'data'
            },
            columns: [
                { data: null,             defaultContent: '<div class="form-check"><input type="checkbox" class="form-check-input select-for-queue"></div>' },
                { data: 'classId',        title: 'Class' },
                { data: classNameFromRow, title: 'Name' },
                { data: 'nrMutants',      title: 'Mutants' },
                { data: 'nrTests',        title: 'Tests' },
                { data: progressFromRow,  title: 'Computed' },
            ],
            scrollY: '400px',
            scrollCollapse: true,
            paging: false,
            dom: 't',
            language: {emptyTable: emptyClassTableMessage}
        });

        const gameTable = new DataTable('#table-games', {
            ajax: {
                url: '<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=game&fileType=json',
                dataSrc: 'data'
            },
            columns: [
                { data: null,            defaultContent: '<div class="form-check"><input type="checkbox" class="form-check-input select-for-queue"></div>' },
                { data: 'gameId',        title: 'Game' },
                { data: 'gameMode',      title: 'Mode' },
                { data: 'nrMutants',     title: 'Mutants' },
                { data: 'nrTests',       title: 'Tests' },
                { data: progressFromRow, title: 'Computed' },
            ],
            scrollY: '400px',
            scrollCollapse: true,
            paging: false,
            dom: 't',
            language: {emptyTable: emptyGameTableMessage}
        });

        $('#toggle-progress-classes').on('change', function () {
            const colorFun = $(this).is(':checked') ? colorRow : uncolorRow;
            classTable.rows().every(colorFun);
        });
        $('#toggle-progress-games').on('change', function () {
            const colorFun = $(this).is(':checked') ? colorRow : uncolorRow;
            gameTable.rows().every(colorFun);
        });

        $('#search-classes').on('keyup', function () { classTable.search(this.value).draw(); });
        $('#search-games').on('keyup',   function () { gameTable.search(this.value).draw(); });

        $('#invert-selection-classes').on('click', () => invertSelection(classTable));
        $('#invert-selection-games').on('click',   () => invertSelection(gameTable));

        $('#queue-selection-classes').on('click',  () => postTable(classTable, 'submitKillMapJobs', 'class'));
        $('#queue-selection-games').on('click',    () => postTable(gameTable, 'submitKillMapJobs', 'game'));
        $('#cancel-selection-classes').on('click', () => postTable(classTable, 'cancelKillMapJobs', 'class'));
        $('#cancel-selection-games').on('click',   () => postTable(gameTable, 'cancelKillMapJobs', 'game'));

        $('#delete-selection-classes').on('click', () => {
            if (confirm('Are you sure you want to delete the selected killmaps?'))
                postTable(classTable, 'deleteKillMaps', 'class')
        });
        $('#delete-selection-games').on('click', () => {
            if (confirm('Are you sure you want to delete the selected killmaps?'))
                postTable(gameTable, 'deleteKillMaps', 'game')
        });
    });

<% } %>
</script>

<%@ include file="/jsp/footer.jsp" %>

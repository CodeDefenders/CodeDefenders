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

<jsp:include page="/jsp/header_main.jsp"/>

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

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminKillMaps"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div class="panel panel-default" style="margin-top: 25px;">
        <div class="panel-body">
            <%= numClassesQueued %> <%= pluralize(numClassesQueued, "Class", "Classes") %> and
            <%= numGamesQueued %> <%= pluralize(numGamesQueued, "Game", "Games") %> currently queued.
            <br>

            <% if (processorEnabled) { %>
                Killmap Processing is <span class="text-success">enabled</span>
            <% } else { %>
                Killmap Processing is <span class="text-danger">disabled</span>
            <% } %>

            <% if (currentJob != null) {
                    String jobType;
                    switch (currentJob.getType()) {
                        case CLASS: jobType = "Class"; break;
                        case GAME: jobType = "Game"; break;
                        default: jobType = "[Unknown]";
                    } %>
                <br> Currently processing: <%= jobType %> with ID <%= currentJob.getId() %>
            <% } %>
            <p></p>

            <form id="killmap-processor-settings" name="killmap-processor-settings" title="<%= processorExplanation %>"
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


    <ul class="nav nav-tabs" style="margin-top: 25px; margin-bottom: 25px;">
        <li <%= currentPage == KillmapPage.MANUAL ? "class=\"active\"" : "" %>>
            <a href="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS + "/manual" %>">
                Enter IDs
            </a>
        </li>
        <li <%= currentPage == KillmapPage.AVAILABLE ? "class=\"active\"" : "" %>>
            <a href="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS + "/available" %>">
                Select Classes / Games
            </a>
        </li>
        <li <%= currentPage == KillmapPage.QUEUE ? "class=\"active\"" : "" %>>
            <a href="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS + "/queue" %>">
                Queued Killmap Jobs
            </a>
        </li>
    </ul>

    <% if (currentPage == KillmapPage.MANUAL) { %>

        <div class="panel panel-default">
            <div class="panel-heading">
                Classes
            </div>
            <div class="panel-body">
                <div class="form-group">
                    <label for="class-ids">Class IDs</label>
                    <a data-toggle="collapse" href="#class-ids-explanation" style="color: black;">
                        <span class="glyphicon glyphicon-question-sign"></span>
                    </a>
                    <div id="class-ids-explanation" class="collapse panel panel-default" style="margin-top: 10px;">
                        <div class="panel-body" style="padding: 10px;">
                            Comma separated list of class IDs to generate killmaps for.
                            Newlines and whitespaces are allowed.
                        </div>
                    </div>
                    <textarea name="ids" id="class-ids" class="form-control" placeholder="Class IDs" rows="3"></textarea>
                </div>

                <button id="queue-ids-classes" class="btn btn-primary">Queue</button>
                <button id="delete-ids-classes" class="btn btn-danger">Delete</button>
            </div>
        </div>
        <div class="panel panel-default">
            <div class="panel-heading">
                Games
            </div>
            <div class="panel-body">
                <div class="form-group">
                    <label for="game-ids">Game IDs</label>
                    <a data-toggle="collapse" href="#game-ids-explanation" style="color: black;">
                        <span class="glyphicon glyphicon-question-sign"></span>
                    </a>
                    <div id="game-ids-explanation" class="collapse panel panel-default" style="margin-top: 10px;">
                        <div class="panel-body" style="padding: 10px;">
                            Comma separated list of game IDs to generate killmaps for.
                            Newlines and whitespaces are allowed.
                        </div>
                    </div>
                    <textarea name="ids" id="game-ids" class="form-control" placeholder="Game IDs" rows="3"></textarea>
                </div>

                <button id="queue-ids-games" class="btn btn-primary">Queue</button>
                <button id="delete-ids-games" class="btn btn-danger">Delete</button>
            </div>
        </div>

    <% } else if (currentPage == KillmapPage.AVAILABLE || currentPage == KillmapPage.QUEUE) { %>

        <div class="panel panel-default">
            <div class="panel-heading">
                Classes
                <div style="float: right;">
                    <input type="search" id="search-classes" class="form-control" placeholder="Search" style="height: .65em; width: 10em; display: inline;">
                    <div class="btn-group" data-toggle="buttons" style="margin-left: 1em;">
                        <label class="btn btn-xs btn-default">
                            <input id="toggle-progress-classes" type="checkbox"> Show progress
                        </label>
                    </div>
                    <button id="invert-selection-classes" class="btn btn-xs btn-default" style="margin-left: 1em;">Invert Selection</button>
                    <% if (currentPage == KillmapPage.AVAILABLE) { %>
                        <button id="queue-selection-classes" class="btn btn-xs btn-primary" style="margin-left: 1em;">Queue Selected</button>
                        <button id="delete-selection-classes" class="btn btn-xs btn-danger">Delete Selected</button>
                    <% } else { %>
                        <button id="cancel-selection-classes" class="btn btn-xs btn-primary" style="margin-left: 1em;">Cancel Selected</button>
                    <% } %>
                </div>
            </div>
            <div class="panel-body">
                <table id="table-classes" class="table table-striped table-responsive"></table>
            </div>
        </div>
        <div class="panel panel-default">
            <div class="panel-heading">
                Games
                <div style="float: right;">
                    <input type="search" id="search-games" class="form-control" placeholder="Search" style="height: .65em; width: 10em; display: inline;">
                    <div class="btn-group" data-toggle="buttons" style="margin-left: 1em;">
                        <label class="btn btn-xs btn-default">
                            <input id="toggle-progress-games" type="checkbox"> Show progress
                        </label>
                    </div>
                    <button id="invert-selection-games" class="btn btn-xs btn-default" style="margin-left: 1em;">Invert Selection</button>
                    <% if (currentPage == KillmapPage.AVAILABLE) { %>
                        <button id="queue-selection-games" class="btn btn-xs btn-primary" style="margin-left: 1em;">Queue Selected</button>
                        <button id="delete-selection-games" class="btn btn-xs btn-danger">Delete Selected</button>
                    <% } else { %>
                        <button id="cancel-selection-games" class="btn btn-xs btn-primary" style="margin-left: 1em;">Cancel Selected</button>
                    <% } %>
                </div>
            </div>
            <div class="panel-body">
                <table id="table-games" class="table table-striped table-responsive"></table>
            </div>
        </div>

        <a data-toggle="collapse" href="#downloads" style="color: black;">
            <span class="glyphicon glyphicon-download"></span> Download Tables
        </a>
        <div id="downloads" class="collapse panel panel-default" style="margin-top: 10px;">
            <div class="panel-body">
                <div style="display: inline-block; margin-right: 25px;">
                    <label>Classes</label>
                    <br>
                    <div class="btn-group">
                        <a download="classes.csv"
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=class&fileType=csv"
                           type="button" class="btn btn-default" id="download-classes-csv">Download as CSV</a>
                        <a download="classes.json"
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=class&fileType=json"
                           type="button" class="btn btn-default" id="download-classes-json">Download as JSON</a>
                    </div>
                </div>

                <div style="display: inline-block;">
                    <label>Games</label>
                    <br>
                    <div class="btn-group">
                        <a download="games.csv"
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=game&fileType=csv"
                           type="button" class="btn btn-default" id="download-games-csv">Download as CSV</a>
                        <a download="games.json"
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=game&fileType=json"
                           type="button" class="btn btn-default" id="download-games-json">Download as JSON</a>
                    </div>
                </div>
            </div>
        </div>
    <% } %>
</div>

<script>
(function () {

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
        const classTable = $('#table-classes').DataTable({
            ajax: {
                url: '<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=class&fileType=json',
                dataSrc: 'data'
            },
            columns: [
                { data: null,             defaultContent: '<input type="checkbox" class="select-for-queue">' },
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

        const gameTable = $('#table-games').DataTable({
            ajax: {
                url: '<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?dataType=<%= currentPage %>&killmapType=game&fileType=json',
                dataSrc: 'data'
            },
            columns: [
                { data: null,            defaultContent: '<input type="checkbox" class="select-for-queue">' },
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

})();
</script>

<%@ include file="/jsp/footer.jsp" %>

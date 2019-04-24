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
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SettingsDTO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME" %>
<%@ page import="org.codedefenders.database.KillmapDAO" %>

<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<%
    String currentPage = request.getParameter("page");
    if (currentPage == null) {
        currentPage = "manual";
    }

    SettingsDTO processorSetting = AdminDAO.getSystemSetting(SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION);
    boolean processorEnabled = processorSetting.getBoolValue();
    String processorExplanation = SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION.toString();

    int numClassesQueued = KillmapDAO.getNumClassKillmapJobsQueued();
    int numGamesQueued = KillmapDAO.getNumGameKillmapJobsQueued();
%>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminKillMaps"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <%-- TODO check if the database settings value matches the processor state? --%>
    <div class="panel panel-default" style="margin-top: 25px;">
        <div class="panel-body">
            <%= numClassesQueued %> Class<%= (numClassesQueued == 0 || numClassesQueued > 1) ? "es" : "" %> and
            <%= numGamesQueued %> Game<%= (numGamesQueued == 0 || numGamesQueued > 1) ? "s" : "" %>  currently queued.
            <br>
            <% if (processorEnabled) { %>
                Currently processing: TODO
            <% } %>
            <p></p>

            <form id="killmap-processor-settings" name="killmap-processor-settings" title="<%= processorExplanation %>"
                  action="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS %>" method="post">
                <input type="hidden" name="formType" value="toggleKillMapProcessing">
                <input type="hidden" name="page" value="<%= currentPage %>">
                <% if (processorEnabled) { %>
                    <label for="toggle-killmap-processing">
                        Killmap Processing is <span class="text-success">enabled</span>
                    </label>
                    <br>
                    <button type="submit" name="enable" value="false" id="toggle-killmap-processing" class="btn btn-danger">
                        Disable KillMap Processing
                    </button>
                <% } else { %>
                    <label for="toggle-killmap-processing">
                        Killmap Processing is <span class="text-danger">disabled</span>
                    </label>
                    <br>
                    <button type="submit" name="enable" value="true" id="toggle-killmap-processing" class="btn btn-success">
                        Enable KillMap Processing
                    </button>
                <% } %>
            </form>
        </div>
    </div>


    <ul class="nav nav-tabs" style="margin-top: 25px; margin-bottom: 25px;">
        <li <%= currentPage.equals("manual") ? "class=\"active\"" : "" %>>
            <a href="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS + "?page=manual" %>">
                Enter IDs
            </a>
        </li>
        <li <%= currentPage.equals("available") ? "class=\"active\"" : "" %>>
            <a href="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS + "?page=available" %>">
                Available Killmaps
            </a>
        </li>
        <li <%= currentPage.equals("queue") ? "class=\"active\"" : "" %>>
            <a href="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS + "?page=queue" %>">
                Queued Killmaps
            </a>
        </li>
    </ul>

    <% if (currentPage.equals("manual")) { %>

        <div class="panel panel-default">
            <div class="panel-heading">
                Classes
            </div>
            <div class="panel-body">
                <form id="enter-class-ids" name="enter-class-ids"
                      action="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS %>" method="post">
                    <input type="hidden" name="formType" value="submitKillMapJobs">
                    <input type="hidden" name="killmapType" value="class">
                    <input type="hidden" name="page" value="<%= currentPage %>">

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

                    <button type="submit" id="submit-class-ids" class="btn btn-primary">
                        Submit
                    </button>
                </form>
            </div>
        </div>
        <div class="panel panel-default">
            <div class="panel-heading">
                Games
            </div>
            <div class="panel-body">
                <form id="enter-game-ids" name="enter-game-ids"
                      action="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS %>" method="post">
                    <input type="hidden" name="formType" value="submitKillMapJobs">
                    <input type="hidden" name="killmapType" value="game">
                    <input type="hidden" name="page" value="<%= currentPage %>">

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

                    <button type="submit" id="submit-game-ids" class="btn btn-primary">
                        Submit
                    </button>
                </form>
            </div>
        </div>

    <% } else if (currentPage.equals("available") || currentPage.equals("queue")) { %>

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
                    <% if (currentPage.equals("available")) { %>
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
                    <% if (currentPage.equals("available")) { %>
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
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?pageType=<%= currentPage %>&killmapType=class&fileType=csv"
                           type="button" class="btn btn-default" id="download-classes-csv">Download as CSV</a>
                        <a download="classes.json"
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?pageType=<%= currentPage %>&killmapType=class&fileType=json"
                           type="button" class="btn btn-default" id="download-classes-json">Download as JSON</a>
                    </div>
                </div>

                <div style="display: inline-block;">
                    <label>Games</label>
                    <br>
                    <div class="btn-group">
                        <a download="games.csv"
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?pageType=<%= currentPage %>&killmapType=game&fileType=csv"
                           type="button" class="btn btn-default" id="download-games-csv">Download as CSV</a>
                        <a download="games.json"
                           href="<%= request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?pageType=<%= currentPage %>&killmapType=game&fileType=json"
                           type="button" class="btn btn-default" id="download-games-json">Download as JSON</a>
                    </div>
                </div>
            </div>
        </div>
    <% } %>

    <script>
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

        const postIds = function (table, formType, killmapType) {
            const data = table.data();
            const ids = [];

            for (let i = 0; i < data.length; i++) {
                const node = table.row(i).node();
                const checkbox = $(node).find('input');

                if (checkbox.is(':checked')) {
                    ids.push(data[i][killmapType + 'Id']);
                }
            }

            const form = $(
                  '<form action="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS %>" method="post">'
                +     '<input type="hidden" name="page" value="<%= currentPage %>">'
                +     '<input type="hidden" name="formType" value="' + formType + '">'
                +     '<input type="hidden" name="killmapType" value="' + killmapType + '">'
                +     '<input type="hidden" name="ids" value="' + JSON.stringify(ids) + '">'
                + '</form>'
            );
            $('body').append(form);
            form.submit();
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

        let emptyClassTableMessage = '';
        let emptyGameTableMessage = '';

        <% if (currentPage.equals("available")) { %>
            emptyClassTableMessage = 'No classes available.';
            emptyGameTableMessage = 'No games available.';
        <% } else if (currentPage.equals("queue")) { %>
            emptyClassTableMessage = 'No classes queued for killmap computation.';
            emptyGameTableMessage = 'No games queued for killmap computation.';
        <% } %>

        $(document).ready(function() {
            const classTable = $('#table-classes').DataTable({
                ajax: {
                    url: '<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?pageType=<%= currentPage %>&killmapType=class&fileType=json',
                    dataSrc: 'data'
                },
                columns: [
                    { data: null,
                      defaultContent: '<input type="checkbox" class="select-for-queue">' },
                    { data:  'classId',
                      title: 'Class' },
                    { data:  classNameFromRow,
                      title: 'Name' },
                    { data:  'nrMutants',
                      title: 'Mutants' },
                    { data:  'nrTests',
                      title: 'Tests' },
                    { data:  progressFromRow,
                      title: 'Computed' },
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: { emptyTable: emptyClassTableMessage }
            });

            const gameTable = $('#table-games').DataTable({
                ajax: {
                    url: '<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT %>?pageType=<%= currentPage %>&killmapType=game&fileType=json',
                    dataSrc: 'data'
                },
                columns: [
                    { data: null,
                      defaultContent: '<input type="checkbox" class="select-for-queue">' },
                    { data:  'gameId',
                      title: 'Game' },
                    { data:  'gameMode',
                      title: 'Mode' },
                    { data:  'nrMutants',
                      title: 'Mutants' },
                    { data:  'nrTests',
                      title: 'Tests' },
                    { data:  progressFromRow,
                      title: 'Computed' },
                ],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: { emptyTable: emptyGameTableMessage }
            });

            $('#toggle-progress-classes').on('change', function () {
                if ($(this).is(':checked')) {
                    classTable.rows().every(colorRow);
                } else {
                    classTable.rows().every(uncolorRow);
                }
            });

            $('#toggle-progress-games').on('change', function () {
                if ($(this).is(':checked')) {
                    gameTable.rows().every(colorRow);
                } else {
                    gameTable.rows().every(uncolorRow);
                }
            });

            $('#invert-selection-classes').on('click', () => invertSelection(classTable));
            $('#invert-selection-games').on('click',   () => invertSelection(gameTable));

            $('#queue-selection-classes').on('click',  () => postIds(classTable, 'submitKillMapJobs', 'class'));
            $('#queue-selection-games').on('click',    () => postIds(gameTable, 'submitKillMapJobs', 'game'));
            $('#cancel-selection-classes').on('click', () => postIds(classTable, 'cancelKillMapJobs', 'class'));
            $('#cancel-selection-games').on('click',   () => postIds(gameTable, 'cancelKillMapJobs', 'game'));

            $('#delete-selection-classes').on('click', () => {
                if (confirm('Are you sure you want to delete the selected killmaps?'))
                    postIds(classTable, 'deleteKillMaps', 'class')
            });
            $('#delete-selection-games').on('click', () => {
                if (confirm('Are you sure you want to delete the selected killmaps?'))
                    postIds(gameTable, 'deleteKillMaps', 'game')
            });

            $('#search-classes').on('keyup', function () {
                classTable.search(this.value).draw();
            });
            $('#search-games').on('keyup', function () {
                gameTable.search(this.value).draw();
            });
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>

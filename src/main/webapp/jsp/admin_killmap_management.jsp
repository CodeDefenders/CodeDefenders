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
    SettingsDTO processorSetting = AdminDAO.getSystemSetting(SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION);
    boolean processorEnabled = processorSetting.getBoolValue();

    String processorExplanation = SETTING_NAME.AUTOMATIC_KILLMAP_COMPUTATION.toString();

    int numClassesQueued = KillmapDAO.getNumClassKillmapJobsQueued();
    int numGamesQueued = KillmapDAO.getNumGameKillmapJobsQueued();
%>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminKillMaps"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <%-- TODO check if the database settings value mathes the processor state? --%>
    <div class="panel panel-default" style="margin-top: 25px;">
        <div class="panel-body">
            <%= numClassesQueued %> Class<%= numClassesQueued > 0 ? "es" : "" %> and
            <%= numGamesQueued %> Game<%= numGamesQueued > 0 ? "s" : "" %>  currently queued.<br>
            <% if (processorEnabled) { %>
                Currently computing: TODO
            <% } %>
            <p></p>

            <form id="killmap-processor-settings" name="killmap-processor-settings" title="<%= processorExplanation %>"
                  action="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS %>" method="post">
                <input type="hidden" name="formType" value="toggleKillMapProcessing">
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

    <ul class="nav nav-tabs" style="margin-top: 25px;">
        <li role="presentation" class="active"><a href="#manual" aria-controls="manual" role="tab" data-toggle="tab">Enter IDs</a></li>
        <li role="presentation"><a href="#availabe" aria-controls="availabe" role="tab" data-toggle="tab">Choose Available KillMaps</a></li>
        <li role="presentation"><a href="#queue" aria-controls="queue" role="tab" data-toggle="tab">Show Queued</a></li>
    </ul>

    <div class="tab-content" style="margin-top: 25px;">
        <div role="tabpanel" class="tab-pane active" id="manual" data-toggle="tab">
            <div class="panel panel-default">
                <div class="panel-heading">
                    Classes
                </div>
                <div class="panel-body">
                    <form id="enter-class-ids" name="enter-class-ids"
                          action="<%= request.getContextPath() + Paths.ADMIN_KILLMAPS %>" method="post">
                        <input type="hidden" name="formType" value="submitKillMapJob">
                        <input type="hidden" name="jobType" value="class">

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
                            <textarea name="ids" id="class-ids" class="form-control" placeholder="Class IDs" rows="1"></textarea>
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
                        <input type="hidden" name="formType" value="submitKillMapJob">
                        <input type="hidden" name="jobType" value="game">

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
                            <textarea name="ids" id="game-ids" class="form-control" placeholder="Game IDs" rows="1"></textarea>
                        </div>

                        <button type="submit" id="submit-game-ids" class="btn btn-primary">
                            Submit
                        </button>
                    </form>
                </div>
            </div>
        </div>
        <div role="tabpanel" class="tab-pane" id="availabe" data-toggle="tab">
            <div class="panel panel-default">
                <div class="panel-heading">
                    Classes
                    <div style="float: right;">
                        <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                            <label class="btn btn-xs btn-default">
                                <input id="toggle-progress-classes" type="checkbox"> Show progress
                            </label>
                        </div>
                        <button class="btn btn-xs btn-default">Invert Selection</button>
                        <button class="btn btn-xs btn-default" style="margin-right: 1em;">Clear Selection</button>
                        <button class="btn btn-xs btn-primary">Queue Selected</button>
                        <button class="btn btn-xs btn-danger">Delete Selected</button>
                    </div>
                </div>
                <div class="panel-body">
                    <table id="tableClasses" class="table table-striped table-responsive"></table>
                </div>
            </div>
            <div class="panel panel-default">
                <div class="panel-heading">
                    Games
                    <div style="float: right;">
                        <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                            <label class="btn btn-xs btn-default">
                                <input id="toggle-progress-games" type="checkbox"> Show progress
                            </label>
                        </div>
                        <button class="btn btn-xs btn-default">Invert Selection</button>
                        <button class="btn btn-xs btn-default" style="margin-right: 1em;">Clear Selection</button>
                        <button class="btn btn-xs btn-primary">Queue Selected</button>
                        <button class="btn btn-xs btn-danger">Delete Selected</button>
                    </div>
                </div>
                <div class="panel-body">
                    <table id="tableGames" class="table table-striped table-responsive"></table>
                </div>
            </div>
        </div>
        <div role="tabpanel" class="tab-pane" id="queue" data-toggle="tab">
            <div class="panel panel-default">
                <div class="panel-heading">
                    Classes
                    <div style="float: right;">
                        <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                            <label class="btn btn-xs btn-default">
                                <input id="toggle-progress-classes-queue" type="checkbox"> Show progress
                            </label>
                        </div>
                        <button class="btn btn-xs btn-default">Invert Selection</button>
                        <button class="btn btn-xs btn-default" style="margin-right: 1em;">Clear Selection</button>
                        <button class="btn btn-xs btn-default">Cancel Selected</button>
                    </div>
                </div>
                <div class="panel-body">
                    <table id="tableClassesQueue" class="table table-striped table-responsive"></table>
                </div>
            </div>
            <div class="panel panel-default">
                <div class="panel-heading">
                    Games
                    <div style="float: right;">
                        <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                            <label class="btn btn-xs btn-default">
                                <input id="toggle-progress-games-queue" type="checkbox"> Show progress
                            </label>
                        </div>
                        <button class="btn btn-xs btn-default">Invert Selection</button>
                        <button class="btn btn-xs btn-default" style="margin-right: 1em;">Clear Selection</button>
                        <button class="btn btn-xs btn-default">Cancel Selected</button>
                    </div>
                </div>
                <div class="panel-body">
                    <table id="tableGamesQueue" class="table table-striped table-responsive"></table>
                </div>
            </div>
        </div>
    </div>

    <script>
        // TODO: constant string formatting (single quotes?)
        // TODO: constant object key formatting (no quotes?)

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

            node.style.background = "linear-gradient(to right, rgba(41, 182, 246, 0.15) " + percentage + "%, transparent " + percentage + "%)";
        };

        const uncolorRow = function () {
            const node = this.node();
            node.style.background = null;
        };

        const progressFromRow = function (row) {
            const expectedNrEntries = (row.nrTests * row.nrMutants);
            if (expectedNrEntries !== 0) {
                return ((row.nrEntries * 100) / expectedNrEntries).toFixed(2) + '%';
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

        let classTable;
        let gameTable;
        let classQueueTable;
        let gameQueueTable;

        $(document).ready(function() {
            classTable = $('#tableClasses').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT + "?pageType=available&killmapType=class&fileType=json"%>",
                    "dataSrc": "data"
                },
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "classId",
                      "title": "Class" },
                    { "data":  classNameFromRow,
                      "title": "Name" },
                    { "data":  "nrMutants",
                      "title": "Mutants" },
                    { "data":  "nrTests",
                      "title": "Tests" },
                    { "data":  progressFromRow,
                      "title": "Computed" },
                ],
                "scrollY": "400px",
                "scrollCollapse": true,
                "paging": false,
                "dom": 't',
                "language": { emptyTable: 'No classes' }
            });

            gameTable = $('#tableGames').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT + "?pageType=available&killmapType=game&fileType=json"%>",
                    "dataSrc": "data"
                },
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "gameId",
                      "title": "Game" },
                    { "data":  "gameMode",
                      "title": "Mode" },
                    { "data":  "nrMutants",
                      "title": "Mutants" },
                    { "data":  "nrTests",
                      "title": "Tests" },
                    { "data":  progressFromRow,
                      "title": "Computed" },
                ],
                "scrollY": "400px",
                "scrollCollapse": true,
                "paging": false,
                "dom": 't',
                "language": { emptyTable: 'No games' }
            });

            classQueueTable = $('#tableClassesQueue').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT + "?pageType=queue&killmapType=class&fileType=json"%>",
                    "dataSrc": "data"
                },
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "classId",
                      "title": "Class" },
                    { "data":  classNameFromRow,
                      "title": "Name" },
                    { "data":  "nrMutants",
                      "title": "Mutants" },
                    { "data":  "nrTests",
                      "title": "Tests" },
                    { "data":  progressFromRow,
                      "title": "Computed" },
                ],
                "scrollY": "400px",
                "scrollCollapse": true,
                "paging": false,
                "dom": 't',
                "language": { emptyTable: 'No classes queued for killmap computation' }
            });

            gameQueueTable = $('#tableGamesQueue').DataTable({
                "ajax": {
                    "url": "<%=request.getContextPath() + Paths.API_KILLMAP_MANAGEMENT + "?pageType=queue&killmapType=game&fileType=json"%>",
                    "dataSrc": "data"
                },
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "gameId",
                      "title": "Game" },
                    { "data":  "gameMode",
                      "title": "Mode" },
                    { "data":  "nrMutants",
                      "title": "Mutants" },
                    { "data":  "nrTests",
                      "title": "Tests" },
                    { "data":  progressFromRow,
                      "title": "Computed" },
                ],
                "scrollY": "400px",
                "scrollCollapse": true,
                "paging": false,
                "dom": 't',
                "language": { emptyTable: 'No games queued for killmap computation' }
            });

            $("#toggle-progress-classes").on("change", function () {
                if ($(this).is(':checked')) {
                    classTable.rows().every(colorRow);
                } else {
                    classTable.rows().every(uncolorRow);
                }
            });

            $("#toggle-progress-games").on("change", function () {
                if ($(this).is(':checked')) {
                    gameTable.rows().every(colorRow);
                } else {
                    gameTable.rows().every(uncolorRow);
                }
            });

            $("#toggle-progress-classes-queue").on("change", function () {
                if ($(this).is(':checked')) {
                    classQueueTable.rows().every(colorRow);
                } else {
                    classQueueTable.rows().every(uncolorRow);
                }
            });

            $("#toggle-progress-games-queue").on("change", function () {
                if ($(this).is(':checked')) {
                    gameQueueTable.rows().every(colorRow);
                } else {
                    gameQueueTable.rows().every(uncolorRow);
                }
            });
        });
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>

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
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminKillMaps"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <div class="panel panel-default" style="margin-top: 25px;">
        <div class="panel-body">
            10 Games and 13 Classes currently queued.<br>
            Killmap computation is enabled, currently computing: Class ID 123.
            <p></p>
            <form class="form-inline">
                <button class="btn btn-danger">Stop killmap computation</button>
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
                </div>
                <div class="panel-body">
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

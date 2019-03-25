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
        <li role="presentation" class="active"><a href="#availabe" aria-controls="availabe" role="tab" data-toggle="tab">Available</a></li>
        <li role="presentation"><a href="#queued" aria-controls="queued" role="tab" data-toggle="tab">Queued</a></li>
    </ul>

    <div class="tab-content" style="margin-top: 25px;">
        <div role="tabpanel" class="tab-pane active" id="availabe" data-toggle="tab">
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
        <div role="tabpanel" class="tab-pane" id="queued" data-toggle="tab">
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
            var percentage = data.percentage * 100;
            node.style.background = "linear-gradient(to right, rgba(41, 182, 246, 0.15) " + percentage + "%, transparent " + percentage + "%)";
        };

        const uncolorRow = function () {
            const node = this.node();
            node.style.background = null;
        };

        var classTable;
        var gameTable;
        var classQueueTable;
        var gameQueueTable;

        $(document).ready(function() {
            classTable = $('#tableClasses').DataTable({
                "data": [
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                ],
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "class",
                      "title": "Class" },
                    { "data":  "name",
                      "title": "Name" },
                    { "data":  "mutants",
                      "title": "Mutants" },
                    { "data":  "tests",
                      "title": "Tests" },
                    { "data":  "percentage",
                      "title": "Computed" },
                ],
                "scrollY": "400px",
                "scrollCollapse": true,
                "paging": false,
                "dom": 't',
                "language": { emptyTable: 'No classes' }
            });

            gameTable = $('#tableGames').DataTable({
                "data": [
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                ],
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "game",
                      "title": "Game" },
                    { "data":  "mode",
                      "title": "Mode" },
                    { "data":  "mutants",
                      "title": "Mutants" },
                    { "data":  "tests",
                      "title": "Tests" },
                    { "data":  "percentage",
                      "title": "Computed" },
                ],
                "scrollY": "400px",
                "scrollCollapse": true,
                "paging": false,
                "dom": 't',
                "language": { emptyTable: 'No games' }
            });

            classQueueTable = $('#tableClassesQueue').DataTable({
                "data": [
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                    { class: 101, name: 'Test', mutants: 7, tests: 8, percentage: 0.7},
                    { class: 102, name: 'Test', mutants: 5, tests: 3, percentage: 0.0},
                    { class: 103, name: 'Test', mutants: 25, tests: 7, percentage: 1.0},
                    { class: 104, name: 'Test', mutants: 40, tests: 65, percentage: 0.3},
                    { class: 105, name: 'Test', mutants: 3, tests: 2, percentage: 0.7},
                ],
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "class",
                      "title": "Class" },
                    { "data":  "name",
                      "title": "Name" },
                    { "data":  "mutants",
                      "title": "Mutants" },
                    { "data":  "tests",
                      "title": "Tests" },
                    { "data":  "percentage",
                      "title": "Computed" },
                ],
                "scrollY": "400px",
                "scrollCollapse": true,
                "paging": false,
                "dom": 't',
                "language": { emptyTable: 'No classes queued for killmap computation' }
            });

            gameQueueTable = $('#tableGamesQueue').DataTable({
                "data": [
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                    { game: 101, mode: "PARTY", mutants: 7, tests: 8, percentage: 0.7},
                    { game: 102, mode: "DUEL", mutants: 5, tests: 3, percentage: 0.0},
                    { game: 103, mode: "PARTY", mutants: 25, tests: 7, percentage: 1.0},
                    { game: 104, mode: "PARTY", mutants: 40, tests: 65, percentage: 0.3},
                    { game: 105, mode: "DUEL", mutants: 3, tests: 2, percentage: 0.7},
                ],
                "columns": [
                    { "data": null,
                      "defaultContent": '<input type="checkbox" class="select-for-queue">' },
                    { "data":  "game",
                      "title": "Game" },
                    { "data":  "mode",
                      "title": "Mode" },
                    { "data":  "mutants",
                      "title": "Mutants" },
                    { "data":  "tests",
                      "title": "Tests" },
                    { "data":  "percentage",
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

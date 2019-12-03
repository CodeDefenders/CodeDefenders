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
<%
    String pageTitle = "Admin Puzzles";
%>
<%@ include file="/jsp/header_main.jsp"%>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminPuzzles"); %>
    <%@ include file="/jsp/admin_navigation.jsp"%>

    <h2>Puzzle Management</h2>

    <h3>Puzzle Chapters</h3>
    <table id="tableChapters"
           class="table table-striped table-hover table-responsive">
        <thead>
        <tr>
            <th>ID</th>
            <th>Position</th>
            <th>Title</th>
            <th>Description</th>
            <th>Delete</th>
        </tr>
        </thead>
    </table>

    <h3>Puzzle</h3>
    <table id="tablePuzzles"
           class="table table-striped table-hover table-responsive">
        <thead>
        <tr>
            <th>ID</th>
            <th>Chapter</th>
            <th>Position</th>
            <th>Title</th>
            <th>Description</th>
            <th>Class</th>
            <th>Delete</th>
        </tr>
        </thead>
    </table>

    <jsp:include page="/jsp/api/puzzle-api.jsp"/>
    <script type="text/javascript">
        let puzzleTable;
        let chapterTable;

        $(document).ready(async function() {
            const puzzleData = await PuzzleAPI.fetchPuzzleData();

            const chapters = puzzleData.puzzleChapters;
            if (chapters) {
                chapterTable = $('#tableChapters').DataTable({
                    "data": chapters,
                    "columns": [
                        { "data": "id" },
                        { "data": "position" },
                        { "data": "title" },
                        { "data": "description" },
                        { "data": null,
                            "defaultContent": "",
                            "render": function(data, type, row, meta) {
                                return `<button class="btn btn-sm btn-danger" ` +
                                    `onclick="` +
                                    `removePuzzleChapter(` + data.id + `,` + meta.row + `,` + meta.col + `);">` +
                                    `<span class="glyphicon glyphicon-trash"></span>` +
                                    `</button>`;
                            }
                        },
                    ],
                    "pageLength": 10,
                    "order": [[ 1, "asc" ]]
                });
            }

            const puzzles = puzzleData.puzzles;
            if (puzzles) {
                puzzleTable = $('#tablePuzzles').DataTable({
                    "data": puzzles,
                    "columns": [
                        { "data": "id" },
                        {
                            "data": "chapterId",
                            "defaultContent": "none"
                        },
                        { "data": "position" },
                        { "data": "title" },
                        { "data": "description" },
                        { "data": "classId" },
                        {
                            "data": null,
                            "defaultContent": "",
                            "render": function(data, type, row, meta) {
                                return `<button class="btn btn-sm btn-danger" ` +
                                    `onclick="` +
                                    `removePuzzle(` + data.id + `,` + meta.row + `,` + meta.col + `);">` +
                                        `<span class="glyphicon glyphicon-trash"></span>` +
                                    `</button>`;
                            }
                        },
                    ],
                    "pageLength": 10,
                    "order": [[ 1, "asc" ], [ 2, "asc" ]]
                });
            }
        });

        function removePuzzleChapter(puzzleChapterId, row, column) {
            let confirmResult = confirm('Are you sure you want to delete puzzle chapter ' + puzzleChapterId + '?'
                + ' This will not remove the puzzles of the chapter.');
            if (confirmResult) {
                PuzzleAPI.deletePuzzleChapter(puzzleChapterId)
                    .then(responseJSON => {
                        chapterTable.row(row).remove();

                        // Forces re-rendering to update row index
                        chapterTable.rows().invalidate('data').draw();
                    }).catch(error => {
                    chapterTable.cell(row, column).node().firstChild.disabled = true;
                    alert('Puzzle chapter ' + puzzleChapterId + ' could not be removed.');
                });
            }
        }

        function removePuzzle(puzzleId, row, column) {
            let confirmResult = confirm('Are you sure you want to delete puzzle ' + puzzleId + '?');
            if (confirmResult) {
                PuzzleAPI.deletePuzzle(puzzleId)
                    .then(responseJSON => {
                        puzzleTable.row(row).remove();

                        // Forces re-rendering to update row index
                        puzzleTable.rows().invalidate('data').draw();
                    }).catch(error => {
                    puzzleTable.cell(row, column).node().firstChild.disabled = true;
                    alert('Puzzle ' + puzzleId + ' could not be removed.');
                });
            }
        }
    </script>



</div>
<%@ include file="/jsp/footer.jsp"%>

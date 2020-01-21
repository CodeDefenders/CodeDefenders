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
<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Admin Puzzles"); %>

<jsp:include page="/jsp/header_main.jsp"/>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminPuzzles"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

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
            <th>Update</th>
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
            <th>Update</th>
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
                        {
                            "data": "position",
                            "defaultContent": "none"
                        },
                        { "data": "title" },
                        { "data": "description" },
                        {
                            "data": null,
                            "defaultContent": "",
                            "render": function(chapter, type, row, meta) {
                                return `
<button type="button" class="btn btn-sm btn-danger"
        data-toggle="modal" data-target="#updatePuzzleChapter-` + chapter.id +`">
    <span class="glyphicon glyphicon-pencil"></span>
</button>
<div class="modal fade" id="updatePuzzleChapter-` + chapter.id + `" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
                <h5 class="modal-title">Update Puzzle Chapter ` + chapter.id + `</h5>
            </div>
            <div class="modal-body">
                <form>
                    <div class="form-group">
                        <label style="width:100px" for="positionForPuzzleChapter` + chapter.id + `">Position</label>
                        <input style="width:250px" type="number" class="form-control"
                            id="positionForPuzzleChapter` + chapter.id + `" value="` + chapter.position + `"
                            placeholder="Number or empty.">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="titleForPuzzleChapter` + chapter.id + `">Title</label>
                        <input style="width:250px" type="text" class="form-control"
                            id="titleForPuzzleChapter` + chapter.id + `" value="` + chapter.title + `">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="descriptionForPuzzleChapter` + chapter.id + `">Description</label>
                        <input style="width:250px" type="text" class="form-control"
                            id="descriptionForPuzzleChapter` + chapter.id + `" value="` + chapter.description + `">
                    </div>
                    <br>
                    <button type="button" class="btn btn-primary"
                            onclick="updatePuzzleChapter(` + chapter.id + `,` + meta.row + `)">
                        Update
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>
                                `;
                            },
                            "orderable": false
                        },
                        {
                            "data": null,
                            "defaultContent": "",
                            "render": function(data, type, row, meta) {
                                return `<button class="btn btn-sm btn-danger" ` +
                                    `onclick="` +
                                    `removePuzzleChapter(` + data.id + `,` + meta.row + `,` + meta.col + `);">` +
                                    `<span class="glyphicon glyphicon-trash"></span>` +
                                    `</button>`;
                            },
                            "orderable": false
                        }
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
                        {
                            "data": "position",
                            "defaultContent": "none"
                        },
                        { "data": "title" },
                        { "data": "description" },
                        { "data": "classId" },
                        {
                            "data": null,
                            "defaultContent": "",
                            "render": function(puzzle, type, row, meta) {
                                return `
<button type="button" class="btn btn-sm btn-danger"
        data-toggle="modal" data-target="#updatePuzzle-` + puzzle.id +`">
    <span class="glyphicon glyphicon-pencil"></span>
</button>
<div class="modal fade" id="updatePuzzle-` + puzzle.id + `" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
                <h5 class="modal-title">Update Puzzle ` + puzzle.id + `</h5>
            </div>
            <div class="modal-body">
                <form>
                    <div class="form-group">
                        <label style="width:100px" for="positionForPuzzle` + puzzle.id + `">Position</label>
                        <input style="width:250px" type="number" class="form-control"
                            id="positionForPuzzle` + puzzle.id + `" value="` + puzzle.position + `"
                            placeholder="Number or empty.">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="chapterIdForPuzzle` + puzzle.id + `">Chapter Id</label>
                        <input style="width:250px" type="number" class="form-control"
                            id="chapterIdForPuzzle` + puzzle.id + `"
                            value="` + puzzle.chapterId + `" placeholder="Identifier of a chapter or empty.">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="titleForPuzzle` + puzzle.id + `">Title</label>
                        <input style="width:250px" type="text" class="form-control"
                            id="titleForPuzzle` + puzzle.id + `" value="` + puzzle.title + `">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="descriptionForPuzzle` + puzzle.id + `">Description</label>
                        <input style="width:250px" type="text" class="form-control"
                            id="descriptionForPuzzle` + puzzle.id + `" value="` + puzzle.description + `">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="maxAssertionsPerTestForPuzzle` + puzzle.id + `">Max. Assertions</label>
                        <input style="width:250px" type="number" class="form-control"
                            id="maxAssertionsPerTestForPuzzle` + puzzle.id + `" value="` + puzzle.maxAssertionsPerTest + `">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="editableLinesStartForPuzzle` + puzzle.id + `">First Edit Line</label>
                        <input style="width:250px" type="number" class="form-control"
                            id="editableLinesStartForPuzzle` + puzzle.id + `" value="` + puzzle.editableLinesStart + `">
                    </div>
                    <br>
                    <div class="form-group">
                        <label style="width:100px" for="editableLinesEndForPuzzle` + puzzle.id + `">Last Edit Line</label>
                        <input style="width:250px" type="number" class="form-control"
                            id="editableLinesEndForPuzzle` + puzzle.id + `" value="` + puzzle.editableLinesEnd + `">
                    </div>
                    <br>
                    <button type="button" class="btn btn-primary"
                            onclick="updatePuzzle(` + puzzle.id + `,` + meta.row + `)">
                        Update
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>
                                `;
                            },
                            "orderable": false
                        },
                        {
                            "data": null,
                            "defaultContent": "",
                            "render": function(data, type, row, meta) {
                                return `<button class="btn btn-sm btn-danger" ` +
                                    `onclick="` +
                                    `removePuzzle(` + data.id + `,` + meta.row + `,` + meta.col + `);">` +
                                        `<span class="glyphicon glyphicon-trash"></span>` +
                                    `</button>`;
                            },
                            "orderable": false
                        }
                    ],
                    "pageLength": 10,
                    "order": [[ 1, "asc" ], [ 2, "asc" ]]
                });
            }
        });

        function parseIntOrNull(value) {
            let newVal = parseInt(value);
            if (isNaN(newVal)) {
                return null;
            }
            return newVal;
        }

        function updatePuzzleChapter(puzzleChapterId, row) {
            let updateChapterData = {
                id: puzzleChapterId,
                title: String(document.getElementById(`titleForPuzzleChapter` + puzzleChapterId).value),
                description: String(document.getElementById(`descriptionForPuzzleChapter` + puzzleChapterId).value),
                position: parseIntOrNull(document.getElementById(`positionForPuzzleChapter` + puzzleChapterId).value)
            };

            PuzzleAPI.updatePuzzleChapter(puzzleChapterId, updateChapterData)
                .then(responseJSON => {
                    chapterTable.row(row).data(updateChapterData);
                    $('.modal-backdrop').fadeOut();
                    document.querySelector("#updatePuzzleChapter-" + puzzleChapterId +  " .close").click();

                    // Redraw, so ordering is restored, too.
                    chapterTable.rows().invalidate('data').draw();
                }).catch(error => {
                    alert('Puzzle chapter' + puzzleChapterId + ' could not be updated.');
            });
        }

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

        function updatePuzzle(puzzleId, row) {
            let puzzleData = puzzleTable.row(row).data();

            let updatedPuzzleData = {
                id: puzzleId,
                title: String(document.getElementById(`titleForPuzzle` + puzzleId).value),
                description: String(document.getElementById(`descriptionForPuzzle` + puzzleId).value),
                chapterId: parseIntOrNull(document.getElementById(`chapterIdForPuzzle` + puzzleId).value),
                position: parseIntOrNull(document.getElementById(`positionForPuzzle` + puzzleId).value),
                classId: puzzleData.classId,
                maxAssertionsPerTest: parseIntOrNull(document.getElementById(`maxAssertionsPerTestForPuzzle` + puzzleId).value),
                forceHamcrest: puzzleData.forceHamcrest,
                editableLinesStart: parseIntOrNull(document.getElementById(`editableLinesStartForPuzzle` + puzzleId).value),
                editableLinesEnd: parseIntOrNull(document.getElementById(`editableLinesEndForPuzzle` + puzzleId).value)
            };

            PuzzleAPI.updatePuzzle(puzzleId, updatedPuzzleData)
                .then(responseJSON => {
                    puzzleTable.row(row).data(updatedPuzzleData);
                    $('.modal-backdrop').fadeOut();
                    document.querySelector("#updatePuzzle-" + puzzleId +  " .close").click();

                    // Redraw, so ordering is restored, too.
                    puzzleTable.rows().invalidate('data').draw();
                }).catch(error => {
                    alert('Puzzle ' + puzzleId + ' could not be updated.');
                });
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

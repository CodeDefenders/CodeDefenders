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
    String pageTitle = null;
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
        </tr>
        </thead>
    </table>

    <script type="text/javascript">
        let puzzleTable;
        let chapterTable;

        async function fetchPuzzleData () {
            return await fetch('<%=request.getContextPath() + Paths.API_ADMIN_PUZZLES_ALL%>', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            }).then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw Error("Response failed")
                }
            });
        }

        $(document).ready(async function() {
            const puzzleData = await fetchPuzzleData();

            const chapters = puzzleData.puzzleChapters;
            if (chapters) {
                chapterTable = $('#tableChapters').DataTable({
                    "data": chapters,
                    "columns": [
                        { "data": "id" },
                        { "data": "position" },
                        { "data": "title" },
                        { "data": "description" },
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
                        { "data": "chapterId" },
                        { "data": "position" },
                        { "data": "title" },
                        { "data": "description" },
                        { "data": "classId" },
                        // { "data": "maxAssertionsPerTest" },
                        // { "data": "forceHamcrest" },
                        // { "data": "editableLinesStart" },
                        // { "data": "editableLinesEnd" },
                    ],
                    "pageLength": 10,
                    "order": [[ 1, "asc" ], [ 2, "asc" ]]
                });
            }
        });

    </script>



</div>
<%@ include file="/jsp/footer.jsp"%>

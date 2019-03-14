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
<%@ page import="org.codedefenders.game.puzzle.PuzzleChapter" %>
<%@ page import="org.codedefenders.model.PuzzleChapterEntry" %>
<%@ page import="org.codedefenders.model.PuzzleEntry" %>
<% String pageTitle = null; %>

<%--
    Displays all puzzles for a user. Puzzles may link to active puzzle games
    or are locked for the logged in user.

    @param SortedSet<PuzzleChapterEntry> puzzleChapterEntries
        A set of puzzle chapters, which contains chapter information and all
        associated puzzles. The set is sorted ascendingly on puzzle chapters position.
        Associated puzzles are sorted on the puzzle identifier.

--%>
<!doctype html>
<%@ include file="/jsp/header_main.jsp"
%>
<%
{
    SortedSet<PuzzleChapterEntry> puzzleChapterEntries = (SortedSet<PuzzleChapterEntry>) request.getAttribute("puzzleChapterEntries");
%>

<div class="w-100">
    <h2 class="full-width page-title">Puzzles</h2>
    <table id="puzzles" class="table table-striped table-hover table-responsive table-paragraphs games-table">
        <tr>
            <%
                if (puzzleChapterEntries.isEmpty()) {
            %>
            <th colspan="100%"> Currently there are no puzzles available. </th>
            <%
            } else { //
            %>
            <th>Lecture</th>
            <th>Levels</th>
        </tr>
        <tr>
            <%
                for (PuzzleChapterEntry puzzleChapterEntry : puzzleChapterEntries) {
                    final PuzzleChapter chapter = puzzleChapterEntry.getChapter();

            %>
            <td><div data-toggle="popover" data-placement="top" data-content="<%=chapter.getDescription()%>"><%=chapter.getTitle()%></div></td>
            <td>
                <%
                    for (PuzzleEntry puzzleEntry : puzzleChapterEntry.getPuzzleEntries()) {
                        if (puzzleEntry.getType() == PuzzleEntry.Type.GAME || !puzzleEntry.isLocked()) {
                            final int puzzleId = puzzleEntry.getPuzzleId();
                            final String title = puzzleEntry.getPuzzle().getTitle();
                            final String description = puzzleEntry.getPuzzle().getDescription();
                %>
                <a class="btn btn-xs"
                   href="<%=request.getContextPath() + Paths.PUZZLE_GAME%>?puzzleId=<%=puzzleId%>"
                    data-toggle="popover" data-placement="top" data-content="<%=description%>"><%=title%></a>
                <%
                        } else { // Locked puzzle
                %>
                <p class="glyphicon glyphicon-lock"></p>
                <%
                        }
                    }
                %>
            </td>
        </tr>
        <%
            } }
        %>
    </table>
    <script>
        $(function () {
            $('[data-toggle="popover"]').popover({
                trigger: 'hover'
            })
        })
    </script>
</div>

<%
}
%>
<%@include file="../footer.jsp" %>

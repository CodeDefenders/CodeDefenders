<%--
    Copyright (C) 2016-2018 Code Defenders contributors

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

<%@ include file="/jsp/header_main.jsp"
%>
<%
{
    SortedSet<PuzzleChapterEntry> puzzleChapterEntries = (SortedSet<PuzzleChapterEntry>) request.getAttribute("puzzleChapterEntries");
%>

<%
    if (puzzleChapterEntries.isEmpty()) {
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
            <td><%=chapter.getTitle()%></td>
            <%-- TODO add chapter description, maybe as a modal --%>
            <%--<td><%=chapter.getDescription()%></td>--%>
            <td>
                <%
                    for (PuzzleEntry puzzleEntry : puzzleChapterEntry.getPuzzleEntries()) {
                        if (puzzleEntry.getType() == PuzzleEntry.Type.GAME || !puzzleEntry.isLocked()) {
                            final int puzzleId = puzzleEntry.getPuzzleId();
                            final String title = puzzleEntry.getPuzzle().getTitle();
                %>
                <a class="btn btn-xs"
                   href="<%=request.getContextPath() + Paths.PUZZLE_GAME%>?puzzleId=<%=puzzleId%>"><%=title%></a>
                <%--TODO add puzzle description, maybe as a modal--%>
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
</div>

<%
    }
}
%>
<%@include file="../footer.jsp" %>

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
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleChapter" %>
<%@ page import="org.codedefenders.model.PuzzleChapterEntry" %>
<%@ page import="org.codedefenders.model.PuzzleEntry" %>
<%@ page import="java.util.SortedSet" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%--
    Displays all puzzles for a user. Puzzles may link to active puzzle games
    or are locked for the logged in user.

    @param SortedSet<PuzzleChapterEntry> puzzleChapterEntries
        A set of puzzle chapters, which contains chapter information and all
        associated puzzles. The set is sorted ascendingly on puzzle chapters position.
        Associated puzzles are sorted on the puzzle identifier.

--%>
<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Puzzles"); %>

<jsp:include page="/jsp/header.jsp"/>

<%
    SortedSet<PuzzleChapterEntry> puzzleChapterEntries = (SortedSet<PuzzleChapterEntry>) request.getAttribute("puzzleChapterEntries");
    Set<Integer> solvedPuzzles = (Set<Integer>) request.getAttribute("solvedPuzzles");
%>

<div class="container">

    <h2 class="mb-3">${pageInfo.pageTitle}</h2>
    <table id="puzzles" class="table table-striped table-v-align-middle">
        <thead>
            <tr>
                <th>Lecture</th>
                <th>Levels</th>
            </tr>
        </thead>
        <%
            if (puzzleChapterEntries.isEmpty()) {
        %>
            <tbody>
                <tr><td colspan="100" class="text-center">There are currently no puzzles available.</td></tr>
            </tbody>
        <%
            } else {
        %>
            <tbody>
                <%
                    for (PuzzleChapterEntry puzzleChapterEntry : puzzleChapterEntries) {
                        final PuzzleChapter chapter = puzzleChapterEntry.getChapter();

                %>
                    <tr>
                        <td>
                            <div data-bs-toggle="tooltip"
                                 title="<%=chapter.getDescription()%>">
                                <%=chapter.getTitle()%>
                            </div>
                        </td>
                        <td>
                            <div class="d-flex flex-wrap gap-1">
                                <%
                                    for (PuzzleEntry puzzleEntry : puzzleChapterEntry.getPuzzleEntries()) {
                                        final String title = puzzleEntry.getPuzzle().getTitle();
                                        final String description = puzzleEntry.getPuzzle().getDescription();

                                        if (puzzleEntry.getType() == PuzzleEntry.Type.GAME) {
                                            final String color = puzzleEntry.getState().equals(GameState.ACTIVE)
                                                    ? "btn-info"
                                                    : "btn-primary";
                                            final int puzzleId = puzzleEntry.getPuzzleId();
                                %>
                                                <a class="btn btn-sm <%=color%>"
                                                   href="${url.forPath(Paths.PUZZLE_GAME)}?puzzleId=<%=puzzleId%>"
                                                   data-bs-toggle="tooltip"
                                                   title="<%=description%>">
                                                    <%=title%>
                                                </a>
                                <%
                                        } else if (!puzzleEntry.isLocked()) {
                                            final int puzzleId = puzzleEntry.getPuzzleId();
                                            boolean puzzleSolved = solvedPuzzles.contains(puzzleId);
                                            String color = puzzleSolved
                                                    ? "btn-success"
                                                    : "btn-primary";
                                %>
                                            <a class="btn btn-sm <%=color%>"
                                               href="${url.forPath(Paths.PUZZLE_GAME)}?puzzleId=<%=puzzleId%>"
                                               data-bs-toggle="tooltip"
                                               title="<%=description%>">
                                                <%=title%>
                                            </a>
                                <%
                                        } else {
                                %>
                                            <a disabled class="btn btn-sm btn-secondary"
                                               data-bs-toggle="tooltip"
                                               title="<%=description%>">
                                                <%=title%>
                                            </a>
                                <%
                                        }
                                    }
                                %>
                            </div>
                        </td>
                    </tr>
                <%
                    }
                %>
            </tbody>
        <%
            }
        %>
    </table>

</div>

<%@include file="/jsp/footer.jsp" %>

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
<%--@elvariable id="puzzleChapterEntries" type="java.util.SortedSet"--%>
<%--@elvariable id="nextPuzzle" type="java.util.Optional"--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="org.codedefenders.database.PuzzleDAO" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleChapter" %>
<%@ page import="org.codedefenders.model.PuzzleChapterEntry" %>
<%@ page import="org.codedefenders.model.PuzzleEntry" %>
<%@ page import="java.util.SortedSet" %>

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
%>

<div class="container">

    <h1 class="mb-3">${pageInfo.pageTitle}</h1>

    <c:if test="${nextPuzzle.present}">
        <%--@elvariable id="nextPuzzleObj" type="org.codedefenders.model.PuzzleEntry"--%>
        <c:set var="nextPuzzleObj" value="${nextPuzzle.get()}"/>
        <a class="next-puzzle" href="${url.forPath(Paths.PUZZLE_GAME)}?puzzleId=${nextPuzzleObj.puzzleId}">
            <div class="next-puzzle__image">
                <img src="${url.forPath("/images/defender_puzzle.png")}" alt="Preview Image Puzzle Game">
                <div class="next-puzzle__play-btn">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 384 512">
                        <path d="M73 39c-14.8-9.1-33.4-9.4-48.5-.9S0 62.6 0 80V432c0 17.4 9.4 33.4 24.5 41.9s33.7 8.1 48.5-.9L361 297c14.3-8.7 23-24.2 23-41s-8.7-32.2-23-41L73 39z"></path>
                    </svg>
                </div>
            </div>
            <div class="next-puzzle__title">
                <h2>
                    Next puzzle:
                    <span class="chapter">${nextPuzzleObj.puzzle.chapter.title}</span>,
                    <span class="title">${nextPuzzleObj.puzzle.title}</span>
                </h2>
                <p>${nextPuzzleObj.puzzle.description}</p>
            </div>
        </a>
    </c:if>
    <c:if test="${puzzleChapterEntries.size() == 0}">
        <div class="no-puzzles">There are currently no puzzles available.</div>
    </c:if>
    <c:forEach items="${puzzleChapterEntries}" var="ChapterEntry">
        <%--@elvariable id="ChapterEntry" type="org.codedefenders.model.PuzzleChapterEntry"--%>
        <c:set var="chapter" value="${ChapterEntry.chapter}"/>
        <div class="chapter">
            <div class="chapter__title">
                <h2>${chapter.title}</h2>
            </div>
            <a class="chapter__levels">
                <c:forEach var="puzzleEntry" items="${ChapterEntry.puzzleEntries}">
                    <c:set var="puzzle" value="${puzzleEntry.puzzle}"/>
                    <a class="chapter__level puzzle-${puzzleEntry.solved ? 'solved' : 'unsolved'}"
                            <c:choose>
                                <c:when test="${puzzleEntry.type == 'GAME' || !puzzleEntry.locked}">
                                    href="${url.forPath(Paths.PUZZLE_GAME)}?puzzleId=${puzzle.puzzleId}"
                                </c:when>
                                <c:otherwise>
                                    disabled="disabled"
                                </c:otherwise>
                            </c:choose>
                    >
                        <div class="chapter__level__image">

                        </div>
                        <div class="chapter__level__title">
                            <h3>${puzzle.title}</h3>
                            <p>${puzzle.description}</p>
                        </div>
                    </a>
                </c:forEach>
            </a>
        </div>
    </c:forEach>

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
                                            PuzzleGame playedGame = PuzzleDAO.getLatestPuzzleGameForPuzzleAndUser(puzzleId, login.getUserId());
                                            String color = playedGame != null && playedGame.getState().equals(GameState.SOLVED)
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

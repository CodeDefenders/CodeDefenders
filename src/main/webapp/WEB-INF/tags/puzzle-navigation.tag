<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ tag import="org.codedefenders.util.Paths" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="auth" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="puzzleRepo" type="org.codedefenders.persistence.database.PuzzleRepository"--%>
<%--@elvariable id="puzzleNavigation" type="org.codedefenders.beans.page.PuzzleNavigationBean"--%>

<%--
    Provides the navigation bar part for puzzles.
    Only available when the user is logged in and puzzles are enabled and exist.
--%>

<c:if test="${auth.loggedIn && puzzleRepo.checkPuzzlesEnabled() && puzzleRepo.checkActivePuzzlesExist()}">
    <li class="nav-item nav-item-highlight dropdown me-3">
        <a class="nav-link dropdown-toggle" id="header-puzzle" role="button"
           data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Puzzles</a>
        <ul class="dropdown-menu header-puzzle-menu" aria-labelledby="header-puzzle">
            <li><a class="dropdown-item" id="header-puzzle-overview"
                   title="Puzzle Overview: See all puzzles and your progress."
                   href="${url.forPath(Paths.PUZZLE_OVERVIEW)}">Overview</a></li>
            <c:if test="${puzzleNavigation.hasNextPuzzle()}">
                <%--@elvariable id="nextPuzzleObj" type="org.codedefenders.model.PuzzleEntry"--%>
                <c:set var="nextPuzzleObj" value="${puzzleNavigation.nextPuzzle.get()}"/>
                <li><a class="dropdown-item" id="header-puzzle-next"
                       href="${url.forPath(Paths.PUZZLE_GAME)}?puzzleId=${nextPuzzleObj.puzzleId}"
                       title="Next puzzle: ${nextPuzzleObj.puzzle.chapter.title}, ${nextPuzzleObj.puzzle.title}"
                       data-bs-toggle="tooltip"
                >Play next puzzle</a></li>
            </c:if>
            <li class="dropdown-divider"></li>
            <c:if test="${puzzleNavigation.puzzleChapters.size() == 0}">
                <%-- shouldn't happen --%>
                <li class="dropdown-item">There are currently no puzzles available.</li>
            </c:if>
            <c:forEach items="${puzzleNavigation.puzzleChapters}" var="ChapterEntry">
                <%--@elvariable id="ChapterEntry" type="org.codedefenders.model.PuzzleChapterEntry"--%>
                <c:set var="chapter" value="${ChapterEntry.chapter}"/>
                <li class="dropdown dropend">
                    <a class="dropdown-item dropdown-toggle" href="#"
                       id="header-puzzle-chapter-${chapter.position}"
                       data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false"
                    >${chapter.title}</a>

                    <ul class="dropdown-menu">
                        <c:forEach var="puzzleEntry" items="${ChapterEntry.puzzleEntries}">
                            <c:set var="puzzle" value="${puzzleEntry.puzzle}"/>
                            <c:set var="status"
                                   value="${puzzleEntry.equals(nextPuzzleObj) ? 'next' : (puzzleEntry.solved ? 'solved' : 'locked')}"/>
                            <c:set var="icon"
                                   value="${puzzleEntry.equals(nextPuzzleObj) ? 'fa-chevron-right' : (puzzleEntry.solved ? 'fa-check' : 'fa-lock')}"/>
                            <li><a class="dropdown-item header-puzzle-${status}"
                                   id="header-puzzle-chapter-${chapter.position}-${puzzle.position}"
                                    <c:choose>
                                        <c:when test="${puzzleEntry.type == 'GAME' || !puzzleEntry.locked}">
                                            href="${url.forPath(Paths.PUZZLE_GAME)}?puzzleId=${puzzle.puzzleId}"
                                        </c:when>
                                        <c:otherwise>
                                            disabled="disabled"
                                            data-bs-toggle="tooltip"
                                            title="This puzzle is locked."
                                        </c:otherwise>
                                    </c:choose>
                            ><i class="fa ${icon}"></i> ${puzzle.title}</a></li>
                        </c:forEach>
                    </ul>
                </li>
            </c:forEach>
        </ul>
    </li>
</c:if>

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
<%@ taglib prefix="fn" uri="org.codedefenders.functions" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ page import="org.codedefenders.util.Paths" %>

<%--
Displays all puzzles for a user. Puzzles may link to active puzzle games
or are locked for the logged in user.
--%>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="puzzleNavigation" type="org.codedefenders.beans.page.PuzzleNavigationBean"--%>

<c:set var="title" value="Puzzles"/>

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/puzzle_overview.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <div class="container">
            <h1 class="mb-3">${title}</h1>

            <c:if test="${puzzleNavigation.hasNextPuzzle()}">
                <%--@elvariable id="nextPuzzleObj" type="org.codedefenders.model.PuzzleEntry"--%>
                <c:set var="nextPuzzleObj" value="${puzzleNavigation.nextPuzzle.get()}"/>
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
                        <img class="next-puzzle__watermark" alt="${nextPuzzleObj.puzzle.type}"
                                <c:choose>
                                    <c:when test="${nextPuzzleObj.puzzle.type == 'EQUIVALENCE'}">
                                        src="${url.forPath("/images/ingameicons/equivalence.png")}"
                                    </c:when>
                                    <c:otherwise>
                                        src="${url.forPath("/images/achievements/")}codedefenders_achievements_${nextPuzzleObj.puzzle.type == 'ATTACKER' ? 1 : 2}_lvl_0.png"
                                    </c:otherwise>
                                </c:choose>
                        />
                        <h2>
                            <span class="next-puzzle__title__next-puzzle">Next puzzle:</span><br>
                            <span class="next-puzzle__title__chapter">${nextPuzzleObj.puzzle.chapter.title},</span>
                            <span class="next-puzzle__title__title">${nextPuzzleObj.puzzle.title}</span>
                        </h2>
                        <p>${nextPuzzleObj.puzzle.description}</p>
                    </div>
                </a>
            </c:if>
            <c:if test="${puzzleNavigation.puzzleChapters.size() == 0}">
                <div class="no-puzzles">There are currently no puzzles available.</div>
            </c:if>
            <c:forEach items="${puzzleNavigation.puzzleChapters}" var="ChapterEntry">
                <%--@elvariable id="ChapterEntry" type="org.codedefenders.model.PuzzleChapterEntry"--%>
                <c:set var="chapter" value="${ChapterEntry.chapter}"/>
                <div class="chapter">
                    <div class="chapter__title">
                        <h2>${chapter.title}</h2>
                    </div>
                    <div class="chapter__levels">
                        <c:forEach var="puzzleEntry" items="${ChapterEntry.puzzleEntries}">
                            <c:set var="puzzle" value="${puzzleEntry.puzzle}"/>
                            <c:set var="status"
                                   value="${puzzleEntry.equals(nextPuzzleObj) ? 'next' : (puzzleEntry.solved ? 'solved' : 'locked')}"/>
                            <a class="chapter__level puzzle-${status}"
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
                            >
                                <img class="chapter__level__watermark" alt="${puzzle.type}"
                                        <c:choose>
                                            <c:when test="${puzzle.type == 'EQUIVALENCE'}">
                                                src="${url.forPath("/images/ingameicons/equivalence.png")}"
                                            </c:when>
                                            <c:otherwise>
                                                src="${url.forPath("/images/achievements/")}codedefenders_achievements_${puzzle.type == 'ATTACKER' ? 1 : 2}_lvl_0.png"
                                            </c:otherwise>
                                        </c:choose>
                                />
                                <div class="chapter__level__image">
                                    <c:if test="${status == 'next'}">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 384 512">
                                            <path d="M73 39c-14.8-9.1-33.4-9.4-48.5-.9S0 62.6 0 80V432c0 17.4 9.4 33.4 24.5 41.9s33.7 8.1 48.5-.9L361 297c14.3-8.7 23-24.2 23-41s-8.7-32.2-23-41L73 39z"></path>
                                        </svg>
                                    </c:if>
                                    <c:if test="${status == 'solved'}">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
                                            <path d="M256 512A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM369 209L241 337c-9.4 9.4-24.6 9.4-33.9 0l-64-64c-9.4-9.4-9.4-24.6 0-33.9s24.6-9.4 33.9 0l47 47L335 175c9.4-9.4 24.6-9.4 33.9 0s9.4 24.6 0 33.9z"></path>
                                        </svg>
                                        <span class="puzzle-attempt-counter"
                                              data-bs-toggle="tooltip"
                                              title="Puzzle solved in ${puzzleEntry.rounds} ${fn:pluralizeWithS(puzzleEntry.rounds, "attempt")}."
                                        >${puzzleEntry.rounds}</span>
                                    </c:if>
                                    <c:if test="${status == 'locked'}">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
                                            <path d="M144 144v48H304V144c0-44.2-35.8-80-80-80s-80 35.8-80 80zM80 192V144C80 64.5 144.5 0 224 0s144 64.5 144 144v48h16c35.3 0 64 28.7 64 64V448c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V256c0-35.3 28.7-64 64-64H80z"></path>
                                        </svg>
                                    </c:if>
                                </div>
                                <div class="chapter__level__title">
                                    <h3>${puzzle.title}</h3>
                                    <p>${puzzle.description}</p>
                                </div>
                            </a>
                        </c:forEach>
                    </div>
                </div>
            </c:forEach>
        </div>
    </jsp:body>
</p:main_page>



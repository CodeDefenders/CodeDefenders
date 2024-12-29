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
<%@ page import="org.codedefenders.game.GameType" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="fn" uri="org.codedefenders.functions" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="profile" type="org.codedefenders.beans.user.UserProfileBean"--%>

<c:set var="title" value="${profile.self ? 'My Profile' : 'Profile of ' += profile.user.username}"/>

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link rel="stylesheet" href="${url.forPath("/css/specific/user_profile.css")}">
        <link rel="stylesheet" href="${url.forPath("/css/specific/puzzle_overview.css")}">
    </jsp:attribute>
    <jsp:body>
        <div class="container">
            <h1>${title}</h1>

            <section>
                <h2>Achievements</h2>
                <div class="achievements">
                        <%--@elvariable id="achievement" type="org.codedefenders.model.Achievement"--%>
                    <c:if test="${profile.unlockedAchievements.size() == 0}">
                        <div class="no-achievements">No achievement unlocked yet.</div>
                    </c:if>
                    <c:forEach items="${profile.unlockedAchievements}" var="achievement">
                        <t:achievement_badge achievement="${achievement}"/>
                    </c:forEach>
                </div>
                <c:if test="${profile.lockedAchievements.size() > 0}">
                    <button class="btn btn-outline-primary btn-sm mt-3">
                        Show all achievements
                    </button>
                    <script>
                        const button = document.currentScript.previousElementSibling;
                        button.addEventListener("click", function (event) {
                            document.querySelector('.locked-achievements').classList.toggle('hidden');
                            button.innerText = button.innerText === 'Show all achievements'
                                ? 'Hide locked achievements'
                                : 'Show all achievements';
                        });
                    </script>
                    <div class="achievements locked-achievements hidden">
                        <c:forEach items="${profile.lockedAchievements}" var="achievement">
                            <t:achievement_badge achievement="${achievement}"/>
                        </c:forEach>
                    </div>
                </c:if>
            </section>

            <section class="mt-5 statistics" aria-labelledby="stats-multiplayer">
                <h2 class="mb-3" id="stats-multiplayer">Statistics for Multiplayer Games</h2>

                <div class="dashboards">
                        <%--@elvariable id="stats" type="org.codedefenders.dto.UserStats"--%>
                    <c:set var="stats" value="${profile.stats.get(GameType.MULTIPLAYER)}"/>

                    <t:dashboard_pie
                            type="mutants" title="Mutants created"
                            total="${stats.totalMutants}"
                            percentage="${stats.aliveMutantsPercentage}"
                            label1="Mutants still alive:" value1="${stats.aliveMutants}"
                            label2="Killed mutants:" value2="${stats.killedMutants}"
                    />

                    <t:dashboard_pie
                            type="tests" title="Tests written"
                            total="${stats.totalTests}"
                            percentage="${stats.killingTestsPercentage}"
                            label1="Tests that killed mutants:" value1="${stats.killingTests}"
                            label2="Non-killing tests:" value2="${stats.nonKillingTests}"
                    />

                    <t:dashboard_pie
                            type="points" title="Points earned"
                            total="${stats.totalPoints}"
                            percentage="${stats.testPointsPercentage}"
                            label1="By writing tests:" value1="${stats.totalPointsTests}"
                            label2="By creating mutants:" value2="${stats.totalPointsMutants}"
                    />

                    <t:dashboard_pie
                            type="games" title="Games played"
                            total="${stats.totalGames}"
                            percentage="${stats.defenderGamesPercentage}"
                            label1="As defender:" value1="${stats.defenderGames}"
                            label2="As attacker:" value2="${stats.attackerGames}"
                    />
                </div>

                <dl class="other-stats mt-3">
                    <dt>Average points per tests:</dt>
                    <dd>${stats.avgPointsTests}</dd>

                    <dt>Average points per mutant:</dt>
                    <dd>${stats.avgPointsMutants}</dd>
                </dl>
            </section>

            <section class="mt-5 statistics" aria-labelledby="stats-melee">
                <h2 class="mb-3" id="stats-melee">Statistics for Melee Games</h2>

                <div class="dashboards">
                    <c:set var="stats" value="${profile.stats.get(GameType.MELEE)}"/>

                    <t:dashboard_pie
                            type="mutants" title="Mutants created"
                            total="${stats.totalMutants}"
                            percentage="${stats.aliveMutantsPercentage}"
                            label1="Mutants still alive:" value1="${stats.aliveMutants}"
                            label2="Killed mutants:" value2="${stats.killedMutants}"
                    />

                    <t:dashboard_pie
                            type="tests" title="Tests written"
                            total="${stats.totalTests}"
                            percentage="${stats.killingTestsPercentage}"
                            label1="Tests that killed mutants:" value1="${stats.killingTests}"
                            label2="Non-killing tests:" value2="${stats.nonKillingTests}"
                    />

                    <t:dashboard_pie
                            type="points" title="Points earned"
                            total="${stats.totalPoints}"
                            percentage="${stats.testPointsPercentage}"
                            label1="By writing tests:" value1="${stats.totalPointsTests}"
                            label2="By creating mutants:" value2="${stats.totalPointsMutants}"
                    />
                </div>

                <dl class="other-stats mt-3">
                    <dt>Total melee games played:</dt>
                    <dd>${stats.totalGames}</dd>

                    <dt>Average points per tests:</dt>
                    <dd>${stats.avgPointsTests}</dd>

                    <dt>Average points per mutant:</dt>
                    <dd>${stats.avgPointsMutants}</dd>
                </dl>
            </section>

            <section class="mt-5 statistics" aria-labelledby="stats-puzzle">
                <h2 class="mb-3" id="stats-puzzle">Statistics for Puzzle Games</h2>

                <dl class="puzzle-stats">
                    <c:forEach items="${profile.puzzleGames}" var="chapter">
                        <dt>Chapter ${chapter.chapter.position} - ${chapter.chapter.title}</dt>
                        <dd>
                            <div class="chapter__levels">
                                <c:set var="solvedPuzzles"
                                       value="${chapter.puzzleEntries.stream().filter(p -> p.solved).count()}"/>
                                <c:set var="unsolvedPuzzles" value="${chapter.puzzleEntries.size() - solvedPuzzles}"/>

                                <c:forEach items="${chapter.puzzleEntries}" var="puzzle">
                                    <c:if test="${puzzle.solved}">
                                        <div class="chapter__level puzzle-solved">
                                            <img class="chapter__level__watermark" alt="${puzzle.puzzle.type}"
                                                    <c:choose>
                                                        <c:when test="${puzzle.puzzle.type == 'EQUIVALENCE'}">
                                                            src="${url.forPath("/images/ingameicons/equivalence.png")}"
                                                        </c:when>
                                                        <c:otherwise>
                                                            src="${url.forPath("/images/achievements/")}codedefenders_achievements_${puzzle.puzzle.type == 'ATTACKER' ? 1 : 2}_lvl_0.png"
                                                        </c:otherwise>
                                                    </c:choose>
                                            />
                                            <div class="chapter__level__image">
                                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
                                                    <path d="M256 512A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM369 209L241 337c-9.4 9.4-24.6 9.4-33.9 0l-64-64c-9.4-9.4-9.4-24.6 0-33.9s24.6-9.4 33.9 0l47 47L335 175c9.4-9.4 24.6-9.4 33.9 0s9.4 24.6 0 33.9z"></path>
                                                </svg>
                                                <span class="puzzle-attempt-counter"
                                                      data-bs-toggle="tooltip"
                                                      title="Puzzle solved in ${puzzle.rounds} ${fn:pluralizeWithS(puzzle.rounds, "attempt")}."
                                                >${puzzle.rounds}</span>
                                            </div>
                                            <div class="chapter__level__title">
                                                <h3>${puzzle.puzzle.title}</h3>
                                                <p>${puzzle.puzzle.type == 'EQUIVALENCE' ? 'Equivalence'
                                                        : puzzle.puzzle.type == 'ATTACKER' ? 'Attacker' : 'Defender'}
                                                    puzzle</p>
                                            </div>
                                        </div>
                                    </c:if>
                                </c:forEach>
                                <c:if test="${unsolvedPuzzles > 0}">
                                    <div class="chapter__level puzzle-locked">
                                        <div class="chapter__level__image">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
                                                <path d="M144 144v48H304V144c0-44.2-35.8-80-80-80s-80 35.8-80 80zM80 192V144C80 64.5 144.5 0 224 0s144 64.5 144 144v48h16c35.3 0 64 28.7 64 64V448c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V256c0-35.3 28.7-64 64-64H80z"></path>
                                            </svg>
                                        </div>
                                        <div class="chapter__level__title">
                                            <h3>${unsolvedPuzzles} ${fn:pluralizeWithS(unsolvedPuzzles, "puzzle")}
                                                unsolved</h3>
                                        </div>
                                    </div>
                                </c:if>
                            </div>
                        </dd>
                    </c:forEach>
                </dl>
            </section>

            <c:if test="${profile.self}">
                <section class="mt-5" aria-labelledby="played-games">
                    <h2 class="mb-3" id="played-games">Played games</h2>
                    <p>
                        You can find a list of your past games in the
                        <a href="${url.forPath(Paths.GAMES_HISTORY)}">games history</a>.
                    </p>
                </section>

                <section class="mt-5" aria-labelledby="account-information">
                    <h2 class="mb-3" id="account-information">Account Information</h2>
                    <p>
                        Your current email:
                        <span class="d-inline-block px-2 ms-2 border">${profile.user.email}</span>
                    </p>
                    <p>
                        Change your account information, password or delete your account in the
                        <a href="${url.forPath(Paths.USER_SETTINGS)}"
                           title="Edit or delete your CodeDefenders account.">account settings</a>.
                    </p>
                </section>
            </c:if>

        </div>
    </jsp:body>
</p:main_page>

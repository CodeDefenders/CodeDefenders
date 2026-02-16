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
<%@ page import="org.codedefenders.game.GameType" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="fn" uri="org.codedefenders.functions" %>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="profile" type="org.codedefenders.beans.user.UserProfileBean"--%>

<c:set var="title" value="${profile.self ? i18n.tr('My Profile') : i18n.tr('Profile of {0}', profile.user.username)}"/>

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link rel="stylesheet" href="${url.forPath("/css/specific/user_profile.css")}">
        <link rel="stylesheet" href="${url.forPath("/css/specific/puzzle_overview.css")}">
    </jsp:attribute>
    <jsp:body>
        <div class="container">
            <h1>${title}</h1>

            <section>
                <h2>${i18n.tr('Achievements')}</h2>
                <div class="achievements">
                        <%--@elvariable id="achievement" type="org.codedefenders.model.Achievement"--%>
                    <c:if test="${profile.unlockedAchievements.size() == 0}">
                        <div class="no-achievements">${i18n.tr('No achievement unlocked yet.')}</div>
                    </c:if>
                    <c:forEach items="${profile.unlockedAchievements}" var="achievement">
                        <t:achievement_badge achievement="${achievement}"/>
                    </c:forEach>
                </div>
                <c:if test="${profile.lockedAchievements.size() > 0}">
                    <button class="btn btn-outline-primary btn-sm mt-3">
                            ${i18n.tr('Show all achievements')}
                    </button>
                    <script>
                        const button = document.currentScript.previousElementSibling;
                        button.addEventListener("click", function (event) {
                            document.querySelector('.locked-achievements').classList.toggle('hidden');
                            button.innerText = button.innerText === i18n.tr('Show all achievements')
                                ? i18n.tr('Hide locked achievements')
                                : i18n.tr('Show all achievements');
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
                <h2 class="mb-3" id="stats-multiplayer">${i18n.tr('Statistics for Multiplayer Games')}</h2>

                <div class="dashboards">
                        <%--@elvariable id="stats" type="org.codedefenders.dto.UserStats"--%>
                    <c:set var="stats" value="${profile.stats.get(GameType.MULTIPLAYER)}"/>

                    <t:dashboard_pie
                            type="mutants" title="${i18n.tr('Mutants created')}"
                            total="${stats.totalMutants}"
                            percentage="${stats.aliveMutantsPercentage}"
                            label1="${i18n.tr('Mutants still alive:')}" value1="${stats.aliveMutants}"
                            label2="${i18n.tr('Killed mutants:')}" value2="${stats.killedMutants}"
                    />

                    <t:dashboard_pie
                            type="tests" title="${i18n.tr('Tests written')}"
                            total="${stats.totalTests}"
                            percentage="${stats.killingTestsPercentage}"
                            label1="${i18n.tr('Tests that killed mutants:')}" value1="${stats.killingTests}"
                            label2="${i18n.tr('Non-killing tests:')}" value2="${stats.nonKillingTests}"
                    />

                    <t:dashboard_pie
                            type="points" title="${i18n.tr('Points earned')}"
                            total="${stats.totalPoints}"
                            percentage="${stats.testPointsPercentage}"
                            label1="${i18n.tr('By writing tests:')}" value1="${stats.totalPointsTests}"
                            label2="${i18n.tr('By creating mutants:')}" value2="${stats.totalPointsMutants}"
                    />

                    <t:dashboard_pie
                            type="games" title="${i18n.tr('Games played')}"
                            total="${stats.totalGames}"
                            percentage="${stats.defenderGamesPercentage}"
                            label1="${i18n.tr('As defender:')}" value1="${stats.defenderGames}"
                            label2="${i18n.tr('As attacker:')}" value2="${stats.attackerGames}"
                    />
                </div>

                <dl class="other-stats mt-3">
                    <dt>${i18n.tr('Average points per tests:')}</dt>
                    <dd>${stats.avgPointsTests}</dd>

                    <dt>${i18n.tr('Average points per mutant:')}</dt>
                    <dd>${stats.avgPointsMutants}</dd>
                </dl>
            </section>

            <section class="mt-5 statistics" aria-labelledby="stats-melee">
                <h2 class="mb-3" id="stats-melee">${i18n.tr('Statistics for Melee Games')}</h2>

                <div class="dashboards">
                    <c:set var="stats" value="${profile.stats.get(GameType.MELEE)}"/>

                    <t:dashboard_pie
                            type="mutants" title="${i18n.tr('Mutants created')}"
                            total="${stats.totalMutants}"
                            percentage="${stats.aliveMutantsPercentage}"
                            label1="${i18n.tr('Mutants still alive:')}" value1="${stats.aliveMutants}"
                            label2="${i18n.tr('Killed mutants:')}" value2="${stats.killedMutants}"
                    />

                    <t:dashboard_pie
                            type="tests" title="${i18n.tr('Tests written')}"
                            total="${stats.totalTests}"
                            percentage="${stats.killingTestsPercentage}"
                            label1="${i18n.tr('Tests that killed mutants:')}" value1="${stats.killingTests}"
                            label2="${i18n.tr('Non-killing tests:')}" value2="${stats.nonKillingTests}"
                    />

                    <t:dashboard_pie
                            type="points" title="${i18n.tr('Points earned')}"
                            total="${stats.totalPoints}"
                            percentage="${stats.testPointsPercentage}"
                            label1="${i18n.tr('By writing tests:')}" value1="${stats.totalPointsTests}"
                            label2="${i18n.tr('By creating mutants:')}" value2="${stats.totalPointsMutants}"
                    />
                </div>

                <dl class="other-stats mt-3">
                    <dt>${i18n.tr('Total melee games played:')}</dt>
                    <dd>${stats.totalGames}</dd>

                    <dt>${i18n.tr('Average points per tests:')}</dt>
                    <dd>${stats.avgPointsTests}</dd>

                    <dt>${i18n.tr('Average points per mutant:')}</dt>
                    <dd>${stats.avgPointsMutants}</dd>
                </dl>
            </section>

            <section class="mt-5 statistics" aria-labelledby="stats-melee">
                <h2 class="mb-3" id="stats-duel">${i18n.tr('Statistics for Equivalence Duels')}</h2>

                <div class="dashboards">
                    <c:set var="duelStats" value="${profile.totalDuelStats}"/>
                    <t:dashboard_pie
                            type="duels" title="${i18n.tr('All duels')}"
                            total="${duelStats.duelsTotal}"
                            percentage="${duelStats.winPercentage}"
                            label1="${i18n.tr('Equivalence duels won:')}" value1="${duelStats.duelsWon}"
                            label2="${i18n.tr('Equivalence duels lost:')}" value2="${duelStats.duelsLost}"
                    />

                    <c:set var="duelStats" value="${profile.attackerDuelStats}"/>
                    <t:dashboard_pie
                            type="duels" title="${i18n.tr('Duels as attacker')}"
                            total="${duelStats.duelsTotal}"
                            percentage="${duelStats.winPercentage}"
                            label1="${i18n.tr('Rejected equivalence claim:')}" value1="${duelStats.duelsWon}"
                            label2="${i18n.tr('Failed to reject claim:')}" value2="${duelStats.duelsLost}"
                    />

                    <c:set var="duelStats" value="${profile.defenderDuelStats}"/>
                    <t:dashboard_pie
                            type="duels" title="${i18n.tr('Duels as defender')}"
                            total="${duelStats.duelsTotal}"
                            percentage="${duelStats.winPercentage}"
                            label1="${i18n.tr('Successfully claimed as equivalent:')}" value1="${duelStats.duelsWon}"
                            label2="${i18n.tr('Incorrectly claimed mutants:')}" value2="${duelStats.duelsLost}"
                    />
                </div>
            </section>

            <section class="mt-5 statistics" aria-labelledby="stats-puzzle">
                <h2 class="mb-3" id="stats-puzzle">${i18n.tr('Statistics for Puzzle Games')}</h2>

                <dl class="puzzle-stats">
                    <c:forEach items="${profile.puzzleGames}" var="chapter">
                        <dt>${i18n.tr('Chapter {0} - {1}', chapter.chapter.position, chapter.chapter.title)}</dt>
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
                                                      title="${i18n.tr('Puzzle solved in {0} {1}.', puzzle.rounds, i18n.trn("attempt", "attempts", puzzle.rounds))}"
                                                >${puzzle.rounds}</span>
                                            </div>
                                            <div class="chapter__level__title">
                                                <h3>${puzzle.puzzle.title}</h3>
                                                <p>${puzzle.puzzle.type == 'EQUIVALENCE' ? i18n.tr('Equivalence')
                                                        : puzzle.puzzle.type == 'ATTACKER' ? i18n.tr('Attacker') : i18n.tr('Defender')}
                                                        ${i18n.tr('puzzle')}</p>
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
                                            <h3>${i18n.tr('{0} {1} unsolved', unsolvedPuzzles, i18n.trn("puzzle", "puzzles", unsolvedPuzzles))}</h3>
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
                    <h2 class="mb-3" id="played-games">${i18n.tr('Played games')}</h2>
                    <p>
                            ${i18n.tr('You can find a list of your past games in the')}
                        <a href="${url.forPath(Paths.GAMES_HISTORY)}">${i18n.tr('games history')}</a>.
                    </p>
                </section>

                <section class="mt-5" aria-labelledby="account-information">
                    <h2 class="mb-3" id="account-information">${i18n.tr('Account Information')}</h2>
                    <p>
                            ${i18n.tr('Your current email:')}
                        <span class="d-inline-block px-2 ms-2 border">${profile.user.email}</span>
                    </p>
                    <p>
                            ${i18n.tr('Change your account information, password or delete your account in the')}
                        <a href="${url.forPath(Paths.USER_SETTINGS)}"
                           title="${i18n.tr('Edit or delete your CodeDefenders account.')}">${i18n.tr('account settings')}</a>.
                    </p>
                </section>
            </c:if>

        </div>
    </jsp:body>
</p:main_page>

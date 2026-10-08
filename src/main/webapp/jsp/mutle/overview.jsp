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
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ page import="org.codedefenders.util.Paths" %>
<p:main_page title="${i18n.tr('Mutle')}">
    <link rel="stylesheet" href="${url.forPath('/css/mutle.css')}">
    <main class="container">
        <section class="mutle-hero p-4 p-md-5 mb-5">
            <p class="text-uppercase fw-semibold mb-2">${i18n.tr('Weekly mutation puzzle')}</p>
            <h1>${i18n.tr('Current case')}</h1>
            <p class="lead mb-2">
                <c:choose>
                    <c:when test="${not empty currentCase}">
                        ${i18n.tr('Series')} ${currentCase.series} · ${currentCase.title}
                    </c:when>
                    <c:otherwise>
                        ${i18n.tr('Series')} ${currentFixture.series} · ${currentFixture.title}
                    </c:otherwise>
                </c:choose>
            </p>
            <p class="mb-4">${i18n.tr('You have six accepted tests to isolate this week’s hidden change.')}</p>
            <c:if test="${not empty currentCase}">
                <p class="mb-3">
                    <span class="badge bg-${currentCase.solved ? 'success' : (currentCase.failed ? 'danger' : (currentCase.expired ? 'secondary' : 'primary'))} text-white">
                        ${currentCase.solved ? i18n.tr('Solved') : (currentCase.failed ? i18n.tr('Failed in six') : (currentCase.expired ? i18n.tr('Expired') : i18n.tr('In progress')))}
                    </span>
                    <span class="ms-2">${currentCase.acceptedGuesses}/${currentCase.acceptedGuesses + currentCase.remainingGuesses} ${i18n.tr('accepted tests')}</span>
                </p>
            </c:if>
            <c:choose>
                <c:when test="${empty currentCase}">
                    <a class="btn btn-primary btn-lg" href="${url.forPath(Paths.MUTLE_PLAY)}">${i18n.tr('Start current case')}</a>
                </c:when>
                <c:when test="${currentCase.solved || currentCase.failed || currentCase.expired}">
                    <a class="btn btn-primary btn-lg" href="${url.forPath(Paths.MUTLE_PLAY)}?gameId=${currentCase.gameId}">${i18n.tr('Review current case')}</a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-primary btn-lg" href="${url.forPath(Paths.MUTLE_PLAY)}?gameId=${currentCase.gameId}">${i18n.tr('Continue current case')}</a>
                </c:otherwise>
            </c:choose>
        </section>
        <c:if test="${login.admin}">
            <section class="card mb-5" aria-labelledby="mutle-practice-title">
                <div class="card-body">
                    <h2 id="mutle-practice-title" class="h4">${i18n.tr('Fixture practice')}</h2>
                    <p class="text-muted">
                        ${i18n.tr('Start a fresh, non-scoring game for any fixture. Practice games do not award achievements.')}
                    </p>
                    <form class="row g-2 align-items-end" action="${url.forPath(Paths.MUTLE_PLAY)}" method="get">
                        <div class="col-md">
                            <label class="form-label" for="mutle-fixture">${i18n.tr('Fixture')}</label>
                            <select class="form-select" id="mutle-fixture" name="fixture">
                                <c:forEach items="${fixtures}" var="fixture">
                                    <option value="${fixture.fixtureKey}">
                                        ${i18n.tr('Series')} ${fixture.series} · ${fixture.title}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-auto">
                            <button class="btn btn-outline-primary" type="submit">
                                ${i18n.tr('Start fresh practice game')}
                            </button>
                        </div>
                    </form>
                </div>
            </section>
        </c:if>
        <section aria-labelledby="mutle-history-title">
            <h2 id="mutle-history-title">${i18n.tr('Case history')}</h2>
            <c:choose>
                <c:when test="${empty history}">
                    <p class="text-muted">${i18n.tr('Cases you attempt will appear here. Practice games are not included.')}</p>
                </c:when>
                <c:otherwise>
                    <div class="list-group">
                        <c:forEach items="${history}" var="game">
                            <a class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                               href="${url.forPath(Paths.MUTLE_PLAY)}?gameId=${game.gameId}">
                                <span><strong>${i18n.tr('Series')} ${game.series}</strong> · ${game.title}
                                    <small class="text-muted d-block">${game.acceptedGuesses}/${game.acceptedGuesses + game.remainingGuesses} ${i18n.tr('accepted tests')}</small>
                                </span>
                                <span class="badge bg-${game.solved ? 'success' : (game.failed ? 'danger' : (game.expired ? 'secondary' : 'primary'))} text-white">
                                    ${game.solved ? i18n.tr('Solved') : (game.failed ? i18n.tr('Failed in six') : (game.expired ? i18n.tr('Expired') : i18n.tr('Active')))}
                                </span>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
</p:main_page>

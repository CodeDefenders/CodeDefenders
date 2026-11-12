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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="static org.codedefenders.util.Constants.DEFAULT_NB_ASSERTIONS" %>
<% pageContext.setAttribute("MUTLE_MAX_ASSERTIONS", DEFAULT_NB_ASSERTIONS); %>
<p:main_page title="${i18n.tr('Mutle')}">
    <link rel="stylesheet" href="${url.forPath('/css/mutle.css')}">
    <main class="container mutle-play" id="mutle-play"
          data-game-id="${mutleGameId}"
          data-fixture-key="${mutleFixtureKey}"
          data-keymap="${login.loggedIn ? login.user.keyMap.CMName : 'default'}"
          data-series="${i18n.tr('Series')}"
          data-remaining="${i18n.tr('{0} guesses remaining')}"
          data-solved="${i18n.tr('Solved')}"
          data-failed="${i18n.tr('Failed in six')}"
          data-expired="${i18n.tr('Expired')}"
          data-guess="${i18n.tr('Guess {0}: {1}')}"
          data-empty="${i18n.tr('Guess {0}: empty')}"
          data-test="${i18n.tr('Test {0}: {1}')}"
          data-reveal="${i18n.tr('Reveal')}"
          data-mutant-source="${i18n.tr('Mutant source')}"
          data-mutant-diff="${i18n.tr('Mutant diff')}"
          data-proof-test="${i18n.tr('Reference proof test')}"
          data-mark-none="${i18n.tr('No relevant coverage')}"
          data-mark-method="${i18n.tr('Mutated method covered')}"
          data-mark-line="${i18n.tr('Mutated line covered')}"
          data-mark-killed="${i18n.tr('Mutant killed')}"
          data-reuse-test="${i18n.tr('Reuse test')}"
          data-test-copied="${i18n.tr('Test copied to the editor.')}"
          data-copied="${i18n.tr('Result copied')}"
          data-copy-failed="${i18n.tr('Could not copy result')}"
          data-running="${i18n.tr('Evaluating test…')}"
          data-accepted="${i18n.tr('Test accepted: {0}')}"
          data-in-progress="${i18n.tr('A test is already being evaluated. Please wait.')}"
          data-submit-error="${i18n.tr('The test could not be evaluated. Please try again.')}"
          data-submit-label="${i18n.tr('Submit test')}"
          data-unavailable="${i18n.tr('This case is unavailable.')}"
          data-load-error="${i18n.tr('Unable to load the case.')}">
        <div id="mutle-loading" role="status">${i18n.tr('Loading case…')}</div>
        <div id="mutle-load-error" class="alert alert-danger d-none" role="alert"></div>
        <c:if test="${not login.loggedIn}">
            <p class="alert alert-info">${i18n.tr('Guest progress stays in this browser session. Log in to play with your account and earn achievements.')}
                <a href="${url.forPath(Paths.LOGIN)}">${i18n.tr('Log in or Sign up')}</a>
            </p>
        </c:if>
        <div id="mutle-content" class="d-none">
            <header class="mutle-case-header mb-4">
                <div class="mutle-case-copy">
                    <p class="mutle-series mb-2" id="mutle-series"></p>
                    <h1 id="mutle-title"></h1>
                    <p id="mutle-specification" class="lead mb-0"></p>
                </div>
                <div class="mutle-case-state">
                    <p id="mutle-status" class="mb-1" aria-live="polite"></p>
                    <p id="mutle-remaining" class="mb-0" role="status" aria-live="polite" aria-atomic="true"></p>
                </div>
            </header>
            <section class="card mb-4" aria-label="${i18n.tr('How to play')}">
                <div class="card-body">
                    <p class="mb-2" id="mutle-goal">${i18n.tr('Find the hidden change in six tests. Write a test that passes on the original code but fails on the mutant. After each attempt, feedback tells you whether you covered the mutated method or line.')}</p>
                    <details id="mutle-how-to-play">
                        <summary class="fw-semibold">${i18n.tr('How to play')}</summary>
                        <div class="mt-3">
                            <t:mutle_instructions/>
                        </div>
                    </details>
                </div>
            </section>
            <div id="mutle-notice" class="alert d-none" role="status" aria-live="polite">
                <span id="mutle-notice-spinner" class="spinner-border spinner-border-sm d-none" aria-hidden="true"></span>
                <span id="mutle-notice-message"></span>
                <details id="mutle-notice-details" class="d-none">
                    <summary>${i18n.tr('Show details')}</summary>
                    <pre id="mutle-notice-detail-text" class="mb-0 mt-2"></pre>
                </details>
            </div>
            <div class="row g-4 align-items-stretch">
                <section class="col-lg-6">
                    <div class="card mutle-code-card h-100">
                        <div class="card-header">
                            <h2>${i18n.tr('Original class')}</h2>
                        </div>
                        <div class="card-body p-0">
                            <textarea id="mutle-cut" aria-label="${i18n.tr('Original class source')}"></textarea>
                        </div>
                        <div class="card-footer mutle-code-hint">
                            <i class="fa fa-lightbulb-o" aria-hidden="true"></i>
                            <span>${i18n.tr('Select an attempt to show its coverage.')}</span>
                        </div>
                    </div>
                </section>
                <section class="col-lg-6" id="mutle-test-panel">
                    <div class="card mutle-code-card h-100">
                        <div class="card-header">
                            <h2>${i18n.tr('Your test')}</h2>
                        </div>
                        <div class="card-body p-0">
                            <textarea id="mutle-test" aria-label="${i18n.tr('Test source')}"></textarea>
                        </div>
                        <div class="card-footer text-end">
                            <button id="mutle-submit" class="btn btn-primary btn-lg">
                                <i id="mutle-submit-icon" class="fa fa-flask" aria-hidden="true"></i>
                                <span id="mutle-submit-spinner" class="spinner-border spinner-border-sm d-none" aria-hidden="true"></span>
                                <span id="mutle-submit-label">${i18n.tr('Submit test')}</span>
                            </button>
                        </div>
                    </div>
                </section>
                <section class="col-lg-6 d-none" id="mutle-reveal-panel">
                    <section id="mutle-reveal" class="mutle-reveal h-100"></section>
                </section>
            </div>
            <section class="mutle-test-rules mt-3" id="mutle-test-rules-panel">
                <div class="d-flex flex-row gap-3 justify-content-end">
                    <button class="btn btn-sm btn-outline-secondary" type="button" data-bs-toggle="collapse"
                            data-bs-target="#mutle-test-rules" aria-expanded="false" aria-controls="mutle-test-rules">
                        <i class="fa fa-info-circle" aria-hidden="true"></i>
                        ${i18n.tr('What tests are allowed?')}
                    </button>
                    <c:if test="${login.loggedIn}">
                        <jsp:include page="/jsp/game_components/keymap_config.jsp"/>
                    </c:if>
                </div>
                <div class="collapse mt-3" id="mutle-test-rules">
                    <div class="card card-body">
                        <t:validator_explanation_test maxAssertions="${MUTLE_MAX_ASSERTIONS}"/>
                    </div>
                </div>
            </section>
            <div class="row g-4 align-items-start mutle-attempts-layout">
                <section class="col-lg-4" id="mutle-feedback-panel">
                    <div class="card mutle-feedback h-100" aria-labelledby="mutle-attempts-title">
                        <div class="card-body">
                            <h2 id="mutle-attempts-title">${i18n.tr('Your attempts')}</h2>
                            <ol id="mutle-results" class="mutle-results" aria-label="${i18n.tr('Six result cells')}"></ol>
                            <div class="mutle-legend">
                                <span><i class="mutle-legend-swatch none" aria-hidden="true"></i>${i18n.tr('No relevant coverage')}</span>
                                <span><i class="mutle-legend-swatch method" aria-hidden="true"></i>${i18n.tr('Mutated method covered')}</span>
                                <span><i class="mutle-legend-swatch line" aria-hidden="true"></i>${i18n.tr('Changed line covered, mutant survived')}</span>
                                <span><i class="mutle-legend-swatch killed" aria-hidden="true"></i>${i18n.tr('Mutant killed')}</span>
                            </div>
                            <div class="text-end mt-3 d-none" id="mutle-share-container">
                                <button id="mutle-share" class="btn btn-outline-primary">
                                    <i class="fa fa-clipboard" aria-hidden="true"></i>
                                    ${i18n.tr('Copy result')}
                                </button>
                            </div>
                        </div>
                    </div>
                </section>
                <section class="col-lg-8" id="mutle-history-panel">
                    <div id="mutle-history" class="accordion mutle-history"></div>
                </section>
            </div>
            <p id="mutle-copy-status" class="visually-hidden" aria-live="polite"></p>
        </div>
    </main>
    <script type="module" src="${url.forPath('/js/mutle.mjs')}"></script>
</p:main_page>

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

<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>

<%
    List<MultiplayerGame> openMultiplayerGames = (List<MultiplayerGame>) request.getAttribute("openMultiplayerGames");
    List<MeleeGame> openMeleeGames = (List<MeleeGame>) request.getAttribute("openMeleeGames");
    Map<Integer, String> gameCreatorNames = (Map<Integer, String>) request.getAttribute("gameCreatorNames");
    pageContext.setAttribute("openMultiplayerGames", openMultiplayerGames);
    pageContext.setAttribute("openMeleeGames", openMeleeGames);
    pageContext.setAttribute("gameCreatorNames", gameCreatorNames);
%>

<p:main_page title="${i18n.tr('Welcome to Code Defenders')}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/landing_page.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <div class="container py-4 page">
            <div class="landing-panel mb-4">
                <div class="row g-4 align-items-center landing-hero">
                    <div class="${empty mutleFixture ? 'col-12' : 'col-lg-5'}">
                        <div class="d-flex align-items-center gap-3 mb-3">
                            <img src="${url.forPath("/images/logo.png")}" alt="${i18n.tr('Code Defenders Logo')}" width="48">
                            <h1 class="d-flex flex-column mb-0">
                                <span class="title">${i18n.tr('Code Defenders')}</span>
                                <span class="subtitle">${i18n.tr('A Mutation Testing Game')}</span>
                            </h1>
                        </div>
                        <p class="landing-summary">${i18n.tr('CodeDefenders is a web-based game about testing code. It turns mutation testing into a simple, competitive challenge between two roles: defenders and attackers.')}</p>
                        <a href="${url.forPath(Paths.LOGIN)}" class="btn btn-lg btn-primary btn-highlight">
                            ${i18n.tr('Log in or Sign up')}
                        </a>
                    </div>
                    <c:if test="${not empty mutleFixture}">
                        <div class="col-lg-7">
                            <section class="landing-mutle" aria-labelledby="landing-mutle-title">
                                <h2 id="landing-mutle-title">${i18n.tr('Mutle: weekly mutation puzzles')}</h2>
                                <p class="landing-summary">${i18n.tr('Find the hidden change in Java code with six tests.')}</p>
                                <p class="landing-case">${i18n.tr('Series')} ${mutleFixture.series} · <c:out value="${mutleFixture.title}"/></p>
                                <a class="btn btn-lg btn-primary" href="${url.forPath(Paths.MUTLE_PLAY)}">
                                    ${i18n.tr('Play Mutle')}
                                </a>
                                <p class="landing-note mt-3 mb-0">${i18n.tr('Guest progress stays in this browser session. Log in to play with your account and earn achievements.')}</p>
                            </section>
                        </div>
                    </c:if>
                </div>
            </div>

            <section class="landing-panel landing-demo mb-4" aria-label="${i18n.tr('A Mutation Testing Game')}">
                <div class="anim">
                    <div class="status" id="status"></div>

                    <div class="row">
                        <div>
                            <div class="browser-window">
                                <div class="browser-titlebar">
                                    <div class="browser-dots">
                                        <div class="browser-dot close"></div>
                                        <div class="browser-dot minimize"></div>
                                        <div class="browser-dot maximize"></div>
                                    </div>
                                    <div class="pane-title" id="leftTitle"></div>
                                </div>
                                <div class="code" id="leftCodeEditor"></div>
                            </div>
                        </div>

                        <div>
                            <div class="browser-window">
                                <div class="browser-titlebar">
                                    <div class="browser-dots">
                                        <div class="browser-dot close"></div>
                                        <div class="browser-dot minimize"></div>
                                        <div class="browser-dot maximize"></div>
                                    </div>
                                    <div class="pane-title" id="rightTitle"></div>
                                </div>
                                <div class="code" id="rightCodeEditor"></div>
                            </div>
                        </div>
                    </div>

                    <div id="splash" aria-hidden="true">
                        <div class="inner"></div>
                    </div>
                </div>

                <div id="prompt" class="hidden">${i18n.tr('Can you kill the mutant?')}</div>

                <div class="intro">
                    <p>${i18n.tr('Defenders write tests for a small piece of code, the Class Under Test (CUT). Attackers then change the CUT in tiny ways, creating mutants. If a test fails on a mutant, that mutant is killed. If all tests still pass, the mutant survives and scores for the attacker.')}</p>
                </div>
            </section>

            <div class="page landing-panel landing-details mb-4">
                <div class="row">
                    <div class="p-5 col-xxl-6 col-12">
                        <h2 class="mb-3">${i18n.tr('Active Battleground Games')}</h2>
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                <tr>
                                    <th>${i18n.tr('Creator')}</th>
                                    <th>${i18n.tr('Class')}</th>
                                    <th>${i18n.tr('Attackers')}</th>
                                    <th>${i18n.tr('Defenders')}</th>
                                    <th>${i18n.tr('Level')}</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:choose>
                                    <c:when test="${empty openMultiplayerGames}">
                                        <tr>
                                            <td colspan="100" class="text-center">
                                                ${i18n.tr('There are currently no open games.')}
                                            </td>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach items="${openMultiplayerGames}" var="game">
                                            <tr id="game-${game.id}">
                                                <td>${gameCreatorNames[game.id]}</td>
                                                <td><span>${game.CUT.alias}</span></td>
                                                <td>${game.attackerPlayers.size()}</td>
                                                <td>${game.defenderPlayers.size()}</td>
                                                <td>${i18n.tr(game.level.formattedString)}</td>
                                            </tr>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="p-5 col-xxl-6 col-12">
                        <h2 class="mb-3">${i18n.tr('Active Melee Games')}</h2>
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                <tr>
                                    <th>${i18n.tr('Creator')}</th>
                                    <th>${i18n.tr('Class')}</th>
                                    <th>${i18n.tr('Players')}</th>
                                    <th>${i18n.tr('Level')}</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:choose>
                                    <c:when test="${empty openMeleeGames}">
                                        <tr>
                                            <td colspan="100" class="text-center">
                                                ${i18n.tr('There are currently no open games.')}
                                            </td>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach items="${openMeleeGames}" var="game">
                                            <tr id="game-${game.id}">
                                                <td>${gameCreatorNames[game.id]}</td>
                                                <td><span>${game.CUT.alias}</span></td>
                                                <td>${game.players.size()}</td>
                                                <td>${i18n.tr(game.level.formattedString)}</td>
                                            </tr>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <div class="page landing-panel landing-details mb-4">
                <div class="p-5">
                    <h2 class="mb-3">${i18n.tr('Research')}</h2>
                    <div class="two-cols-xxl">
                        <%@ include file="/jsp/research.jsp" %>
                    </div>
                </div>
            </div>


            <div class="page landing-panel landing-lessons">
                <div>
                    <h2 class="mb-3">${i18n.tr('Use CodeDefenders for your lessons')}</h2>
                    <p>
                        ${i18n.tr('CodeDefenders is an open source software developed and maintained at the Chair of Software Engineering&nbsp;II at the University of Passau.')}
                        ${i18n.tr('The source code is available along with install instructions on <a href="{0}" rel="noopener" target="_blank" title="CodeDefenders repo on GitHub">GitHub</a>.',
                            'https://github.com/CodeDefenders/CodeDefenders/')}
                        <br>
                        ${i18n.tr('We also provide docker containers for all major releases.')}
                        ${i18n.tr('You can find links to these containers and more information in our <a href="{0}" rel="noopener" target="_blank" title="CodeDefenders Docker README">docker documentation</a>.',
                            'https://github.com/CodeDefenders/CodeDefenders/blob/master/docker/README.md')}
                    </p>
                    <p>
                        ${i18n.tr('While the public instance at <a href="{0}" title="Public CodeDefenders instance">code-defenders.org</a> is available for everyone to try out CodeDefenders, we strongly recommend setting up a private instance for use in the classroom to ensure a consistent performance.',
                            'https://code-defenders.org/')}
                    </p>
                </div>
            </div>

        </div>

        <script type="module">
            import {runLandingPageAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';

            runLandingPageAnimation();
        </script>
    </jsp:body>
</p:main_page>

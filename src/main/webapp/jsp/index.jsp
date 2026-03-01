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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%
    List<MultiplayerGame> openMultiplayerGames = (List<MultiplayerGame>) request.getAttribute("openMultiplayerGames");
    List<MeleeGame> openMeleeGames = (List<MeleeGame>) request.getAttribute("openMeleeGames");
    Map<Integer, String> gameCreatorNames = (Map<Integer, String>) request.getAttribute("gameCreatorNames");
    pageContext.setAttribute("openMultiplayerGames", openMultiplayerGames);
    pageContext.setAttribute("openMeleeGames", openMeleeGames);
    pageContext.setAttribute("gameCreatorNames", gameCreatorNames);
%>

<p:main_page title="Welcome to Code Defenders">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/landing_page.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <%-- Vertically align content if enough space is available. --%>
        <div class="container py-4 page">
            <div class="d-flex flex-column align-items-center gap-3 mb-3">
                <img src="${url.forPath("/images/logo.png")}" alt="Code Defenders Logo" width="58">
                    <%-- Make the header break nicely on smaller screens. --%>
                <h1 class="d-flex flex-column">
                    <span class="title">Code Defenders</span>
                    <span class="subtitle">A Mutation Testing Game</span>
                </h1>
            </div>

            <div class="d-flex justify-content-center">
                <a href="${url.forPath(Paths.LOGIN)}"
                   class="btn btn-lg btn-primary btn-highlight"
                   style="margin-bottom: 5rem;">
                    Log in or Sign up
                </a>
            </div>

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

            <div id="prompt" class="hidden">Can you kill the mutant?</div>

            <div class="intro">
                <p>CodeDefenders is a web-based game about testing code. It turns mutation testing into a simple,
                    competitive challenge between two roles: defenders and attackers.</p>
                <p>Defenders write tests for a small piece of code, the Class Under Test (CUT). Attackers then change
                    the CUT in tiny ways, creating mutants. If a test fails on a mutant, that mutant is killed. If all
                    tests still pass, the mutant survives and scores for the attacker.</p>
            </div>
        </div>

        <div class="page container py-4">
            <div class="row g-4">
                <div class="col-xxl-6 col-12">
                    <div class="p-5 bg-light rounded-3">
                        <h2 class="mb-3">Active Battleground Games</h2>
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                <tr>
                                    <th>Creator</th>
                                    <th>Class</th>
                                    <th>Attackers</th>
                                    <th>Defenders</th>
                                    <th>Level</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:choose>
                                    <c:when test="${empty openMultiplayerGames}">
                                        <tr>
                                            <td colspan="100" class="text-center">
                                                There are currently no open games.
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
                                                <td>${game.level.formattedString}</td>
                                            </tr>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                                </tbody>
                            </table>
                        </div>
                    </div>


                    <div class="p-5 bg-light rounded-3">
                        <h2 class="mb-3">Active Melee Games</h2>
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                <tr>
                                    <th>Creator</th>
                                    <th>Class</th>
                                    <th>Players</th>
                                    <th>Level</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:choose>
                                    <c:when test="${empty openMeleeGames}">
                                        <tr>
                                            <td colspan="100" class="text-center">
                                                There are currently no open games.
                                            </td>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach items="${openMeleeGames}" var="game">
                                            <tr id="game-${game.id}">
                                                <td>${gameCreatorNames[game.id]}</td>
                                                <td><span>${game.CUT.alias}</span></td>
                                                <td>${game.players.size()}</td>
                                                <td>${game.level.formattedString}</td>
                                            </tr>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="col-xxl-6 col-12">
                    <div class="p-5 bg-light rounded-3">
                        <h2 class="mb-3">Research</h2>
                        <%@ include file="/jsp/research.jsp" %>
                    </div>
                </div>
            </div>
        </div>

        <script type="module">
            import {runLandingPageAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';

            runLandingPageAnimation();
        </script>
    </jsp:body>
</p:main_page>

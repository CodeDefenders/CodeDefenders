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
    <%-- Vertically align content if enough space is available. --%>
    <div class="container py-4 h-100 d-flex flex-column justify-content-center align-items-center">

        <div class="d-flex align-items-center gap-3 mb-3">
            <img src="${url.forPath("/images/logo.png")}" alt="Code Defenders Logo" width="58">
                <%-- Make the header break nicely on smaller screens. --%>
            <h1 class="d-lg-block d-flex flex-column">
                <span>Code Defenders: </span>
                <span>A Mutation Testing Game</span>
            </h1>
        </div>

        <a href="${url.forPath(Paths.LOGIN)}"
           class="btn btn-lg btn-primary btn-highlight"
           style="margin-bottom: 5rem;">
            Log in or Sign up
        </a>

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
</p:main_page>

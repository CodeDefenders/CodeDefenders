<%--

    Copyright (C) 2016-2023 Code Defenders contributors

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
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.Optional" %>
<%@ page import="org.codedefenders.auth.CodeDefendersAuth" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="game" type="org.codedefenders.game.multiplayer.MeleeGame"--%>
<%--@elvariable id="playerId" type="java.util.Optional"--%>
<%--@elvariable id="meleeScoreboard" type="org.codedefenders.beans.game.MeleeScoreboardBean"--%>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
    Role role = game.getRole(login.getUserId());
    pageContext.setAttribute("role", role);
%>

<jsp:useBean id="playerFeedback" class="org.codedefenders.beans.game.PlayerFeedbackBean" scope="request"/>
<%
    playerFeedback.setGameInfo(game.getId(), game.getCreatorId());
    playerFeedback.setPlayerInfo(login.getUserId(), role);
%>


<jsp:useBean id="history" class="org.codedefenders.beans.game.HistoryBean" scope="request"/>
<%
    history.setLogin(login);
    history.setGameId(game.getId());

    Player userPlayer = null;
    List<Player> otherPlayers = new ArrayList<>();
    for (Player player : game.getPlayers()) {
        if (player.getUser().getId() == login.getUserId()) {
            userPlayer = player;
        } else {
            otherPlayers.add(player);
        }
    }

    // We simply need two distinct sets, to determine which events to display on the left/right side of the timeline
    history.setPlayers(Collections.singletonList(userPlayer), otherPlayers);
%>

<%--
<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<%
    classViewer.setClassCode(game.getCUT());
    classViewer.setDependenciesForClass(game.getCUT());
%>
--%>

<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<%
    final CodeDefendersAuth finalLogin = login; // login is not considered effectively final
    Optional<Integer> playerId = game.getPlayers().stream()
            .filter(p -> p.getUser().getId() == finalLogin.getUserId())
            .map(Player::getId)
            .findFirst();
    List<Test> enemyTests = game.getTests();
    List<Test> playerTests = game.getTests();
    if (playerId.isPresent()) {
        int pId = playerId.get();
        enemyTests = enemyTests.stream()
                .filter(t -> t.getPlayerId() != pId)
                .collect(Collectors.toList());
        playerTests = playerTests.stream()
                .filter(t -> t.getPlayerId() == pId)
                .collect(Collectors.toList());
    } else {
        // the user is not a player in this game, but only a spectator
        playerTests = Collections.emptyList();
    }

    gameHighlighting.setGameData(game.getMutants(), playerTests, login.getUserId());
    gameHighlighting.setFlaggingData(game.getMode(), game.getId());
    gameHighlighting.setEnableFlagging(false);
    gameHighlighting.setAlternativeTests(enemyTests);
%>

<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>

<c:set var="title" value="${'Details of Game ' += game.id += ' (' += role.formattedString += ')'}"/>

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">
        <link href="${url.forPath("/css/specific/game_details_view.css")}" rel="stylesheet">
        <link href="${url.forPath("/css/specific/game_scoreboard.css")}" rel="stylesheet">
        <link href="${url.forPath("/css/specific/timeline.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <t:game_js_init/>

        <div id="game-container" class="container-fluid">
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3">
                <h2 class="m-0 text-center">${title}</h2>
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <c:if test="${game.creatorId == login.userId && (game.state == GameState.ACTIVE || game.state == GameState.FINISHED)}">
                        <div>
                            <div data-bs-toggle="tooltip"
                                 title="Start a new game with the same settings and opposite roles.">
                                <button type="submit" class="btn btn-sm btn-warning" id="rematch"
                                        data-bs-toggle="modal" data-bs-target="#rematch-modal">
                                    Rematch
                                </button>
                            </div>
                            <form id="rematch-form" action="${url.forPath(Paths.MELEE_SELECTION)}" method="post">
                                <input type="hidden" name="formType" value="rematch">
                                <input type="hidden" name="gameId" value="${game.id}">
                                <t:modal title="Confirm Rematch" id="rematch-modal" closeButtonText="Cancel">
                                <jsp:attribute name="content">
                                    Are you sure you want to create a new game with opposite roles?
                                </jsp:attribute>
                                    <jsp:attribute name="footer">
                                    <button type="submit" class="btn btn-primary">Confirm Rematch</button>
                                </jsp:attribute>
                                </t:modal>
                            </form>
                        </div>
                    </c:if>

                    <a href="${url.forPath(Paths.PROJECT_EXPORT)}?gameId=${game.id}"
                       class="btn btn-sm btn-outline-secondary text-nowrap" id="btnProjectExport"
                       title="Export as a Gradle project to import into an IDE.">
                        <i class="fa fa-download"></i>
                        Gradle Export
                    </a>
                </div>
            </div>
                <%-- /HEADER --%>

            <div class="details-content">
                <div class="details-content__item">
                    <h3 class="align-items-center d-flex gap-2 justify-content-between">
                        Scoreboard
                        <button class="btn btn-sm btn-outline-secondary" id="btnScoringModal"
                                data-bs-toggle="modal" data-bs-target="#scoringModal">
                            <i class="fa fa-question-circle"></i> Info
                        </button>
                    </h3>

                    <t:modal title="Scoring System" id="scoringModal" modalBodyClasses="bg-light">
                    <jsp:attribute name="content">
                        <jsp:include page="/jsp/scoring_system.jsp"/>
                    </jsp:attribute>
                    </t:modal>

                    <t:melee_game_scoreboard/>
                </div>
                <div class="details-content__item">
                    <h3>The game's duration</h3>

                    <c:set var="duration" value="${game.gameDurationMinutes}"/>
                    <c:set var="startTime" value="${game.startTimeUnixSeconds}"/>

                        <%-- Progress Bar --%>
                    <div class="progress mb-3" style="height: 1em;">
                        <div class="progress-bar progress-bar-striped time-left"
                             data-type="progress"
                             data-duration="${duration}"
                             data-start="${startTime}"
                             style="width: 0; animation-duration: 2s; transition-duration: 10s;"
                             role="progressbar">
                        </div>
                    </div>

                        <%-- Duration Info --%>
                    <div class="row text-center">
                        <div class="col-6 d-flex flex-column align-items-center">
                            <small>Total Duration</small>
                            <span class="time-left"
                                  data-type="total"
                                  data-duration="${duration}">
                            &hellip;
                        </span>
                        </div>

                        <div class="col-6 d-flex flex-column align-items-center">
                            <small>End date of the game</small>
                            <span class="time-left"
                                  data-type="end"
                                  data-duration="${duration}"
                                  data-start="${startTime}">
                            &hellip;
                        </span>
                        </div>

                        <script type="module">
                            import {GameTimeManager} from '${url.forPath("/js/codedefenders_game.mjs")}';

                            const gameTimeManager = new GameTimeManager(".time-left", 10);
                        </script>
                    </div>
                </div>

                <div class="details-content__item">
                    <div class="d-flex align-items-center justify-content-between mb-3">
                        <h3 class="mb-0">Class under test</h3>
                        <c:choose>
                            <c:when test="${role == Role.OBSERVER}">
                                <div data-bs-toggle="tooltip" data-bs-html="true"
                                     title="<p>You are an observer in this game.<br>The coverage of all player tests is shown.</p>">
                                    <i class="fa fa-eye"></i>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div data-bs-toggle="tooltip" data-bs-html="true"
                                     title='<p>Switch between showing coverage of your tests (off) and enemy tests (on).</p>'>
                                    <input class="btn-check" type="checkbox" id="highlighting-switch"
                                           autocomplete="off">
                                    <label class="btn btn-outline-secondary" for="highlighting-switch">
                                        Enemy Coverage
                                        <i class="fa fa-check ms-1 btn-check-active"></i>
                                    </label>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
                    <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
                    <script type="module">
                        import $ from '${url.forPath("/js/jquery.mjs")}';
                        import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';

                        const gameHighlighting = await objects.await('gameHighlighting');

                        if (${role == Role.OBSERVER}) {
                            gameHighlighting.highlightAlternativeCoverage();
                        } else {
                            $('#highlighting-switch').change(function () {
                                gameHighlighting.clearCoverage();
                                if (this.checked) {
                                    gameHighlighting.highlightAlternativeCoverage();
                                } else {
                                    gameHighlighting.highlightCoverage();
                                }
                            });
                        }
                    </script>
                </div>

                <div class="details-content__item">
                    <h3>Timeline</h3>
                    <t:game_timeline/>
                </div>

                <div class="details-content__item">
                    <t:killmap_mutant_accordion/>
                </div>

                <div class="details-content__item">
                    <t:killmap_test_accordion/>
                </div>
            </div>
        </div>
    </jsp:body>
</p:main_page>

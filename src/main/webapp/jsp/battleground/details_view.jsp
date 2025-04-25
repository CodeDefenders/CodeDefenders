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
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="game" type="org.codedefenders.game.multiplayer.MultiplayerGame"--%>
<%--@elvariable id="playerId" type="java.lang.Integer"--%>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
	MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
	Role role = game.getRole(login.getUserId());
    pageContext.setAttribute("role", role);
%>

<jsp:useBean id="playerFeedback" class="org.codedefenders.beans.game.PlayerFeedbackBean" scope="request"/>
<%
	playerFeedback.setGameInfo(game.getId(), game.getCreatorId());
	playerFeedback.setPlayerInfo(login.getUserId(), role);
%>

<jsp:useBean id="scoreboard" class="org.codedefenders.beans.game.ScoreboardBean" scope="request"/>
<%
	scoreboard.setGameId(game.getId());
	scoreboard.setScores(game.getMutantScores(), game.getTestScores());
	scoreboard.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());
%>

<jsp:useBean id="history" class="org.codedefenders.beans.game.HistoryBean" scope="request"/>
<%
	history.setLogin(login);
	history.setGameId(game.getId());
	history.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());
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
	gameHighlighting.setGameData(game.getMutants(), game.getTests());
	gameHighlighting.setFlaggingData(game.getMode(), game.getId());
	gameHighlighting.setEnableFlagging(false);
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
                            <form id="rematch-form" action="${url.forPath(Paths.BATTLEGROUND_SELECTION)}" method="post">
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

                    <t:battleground_game_scoreboard
                            playerId="${playerId}"
                            gameFinished="${game.state == GameState.ACTIVE || game.state == GameState.FINISHED}"
                    />
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

                <div class="details-content__item" id="cut-div">
                    <h3>Class under test</h3>
                    <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
                    <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
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


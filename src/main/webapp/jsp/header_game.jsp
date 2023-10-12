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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.game.GameState"%>
<%@ page import="org.codedefenders.game.Role"%>
<%@ page import="org.codedefenders.util.Paths"%>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.AbstractGame" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.util.URLUtils" %>
<%@ page import="org.codedefenders.util.CDIUtil" %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>
<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

<%
    AbstractGame game = (AbstractGame) request.getAttribute("game");
    int gameId = game.getId();

    Role role = null;
    String selectionManagerUrl = null;
    int duration = -1;
    long startTime = -1;
    if (game instanceof MeleeGame) {
        selectionManagerUrl = CDIUtil.getBeanFromCDI(URLUtils.class).forPath(Paths.MELEE_SELECTION);
        role = ((MeleeGame) game).getRole(login.getUserId());
        duration = ((MeleeGame) game).getGameDurationMinutes();
        startTime = ((MeleeGame) game).getStartTimeUnixSeconds();

        if (game.getState() == GameState.ACTIVE) {
            startTime = ((MeleeGame) game).getStartTimeUnixSeconds();
        }
    } else if (game instanceof MultiplayerGame) {
        selectionManagerUrl = CDIUtil.getBeanFromCDI(URLUtils.class).forPath(Paths.BATTLEGROUND_SELECTION);
        role = ((MultiplayerGame) game).getRole(login.getUserId());
        duration = ((MultiplayerGame) game).getGameDurationMinutes();

        if (game.getState() == GameState.ACTIVE) {
            startTime = ((MultiplayerGame) game).getStartTimeUnixSeconds();
        }
    }
%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request" />
<% pageInfo.setPageTitle("Game " + game.getId() + " (" + role.getFormattedString() + ")"); %>

<jsp:include page="/jsp/header.jsp" />

<link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">

<div id="game-container" class="container-fluid"> <%-- closed in footer --%>
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">

        <%
            String modeText = "Unknown Mode";
            String modeBg = "bg-secondary";
            switch (game.getMode()) {
                case PARTY:
                    modeText = "Battleground";
                    modeBg = "bg-battleground";
                    break;
                case MELEE:
                    modeText = "Melee";
                    modeBg = "bg-player";
                    break;
            }

            String roleText = role.getFormattedString();
            String roleBg = "bg-secondary";
            switch (role) {
                case ATTACKER:
                    roleBg = "bg-attacker";
                    break;
                case DEFENDER:
                    roleBg = "bg-defender";
                    break;
                case PLAYER:
                    roleBg = "bg-player";
                    break;
            }
        %>
        <div class="d-flex gap-1 align-items-center">
            <h2 class="m-0 me-2 mt-1">
                Game
                <span class="font-monospace text-muted">#<%=game.getId()%></span>
            </h2>
            <span class="badge fs-5 rounded-pill <%=modeBg%>" title="Game Mode: <%=modeText%>">
                <i class="fa fa-play-circle"></i>
                <%=modeText%>
            </span>
            <span class="badge fs-5 rounded-pill <%=roleBg%>" title="Your Role: <%=roleText%>">
                <i class="fa fa-user-circle"></i>
                <%=roleText%>
            </span>
        </div>

        <div class="d-flex flex-wrap align-items-center gap-2">

            <%
                if (game.getCreatorId() == login.getUserId()) {
                    if (game.getState() == GameState.ACTIVE) {
            %>
                    <div>
                        <button type="button" class="btn btn-sm btn-danger" id="endGame"
                                data-bs-toggle="modal" data-bs-target="#end-game-modal">
                            End Game
                        </button>
                        <form id="adminEndBtn" action="<%=selectionManagerUrl%>" method="post">
                            <input type="hidden" name="formType" value="endGame">
                            <input type="hidden" name="gameId" value="<%=game.getId()%>">
                            <t:modal title="Confirm End Game" id="end-game-modal" closeButtonText="Cancel">
                                <jsp:attribute name="content">
                                    Are you sure you want to end the game?
                                </jsp:attribute>
                                <jsp:attribute name="footer">
                                    <button type="submit" class="btn btn-primary">Confirm</button>
                                </jsp:attribute>
                            </t:modal>
                        </form>
                    </div>
            <%
                    }

                    if (game.getState() == GameState.CREATED) {
            %>
                    <form id="adminStartBtn" action="<%=selectionManagerUrl%>" method="post">
                        <input type="hidden" name="formType" value="startGame">
                        <input type="hidden" name="gameId" value="<%=game.getId()%>">
                        <button type="submit" class="btn btn-sm btn-success" id="startGame" form="adminStartBtn">
                            Start Game
                        </button>
                    </form>
            <%
                    }

                    if (game.getState() == GameState.ACTIVE || game.getState() == GameState.FINISHED) {
            %>
                <div>
                    <div data-bs-toggle="tooltip"
                         title="Start a new game with the same settings and opposite roles.">
                        <button type="submit" class="btn btn-sm btn-warning" id="rematch"
                                data-bs-toggle="modal" data-bs-target="#rematch-modal">
                            Rematch
                        </button>
                    </div>
                    <form id="rematch-form" action="<%=selectionManagerUrl%>" method="post">
                        <input type="hidden" name="formType" value="rematch">
                        <input type="hidden" name="gameId" value="<%=game.getId()%>">
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
            <%
                    }
                }
            %>

            <%
                final boolean isCreator = game.getCreatorId() == login.getUserId();
                if (game.getState() == GameState.ACTIVE || (game.getState() == GameState.CREATED && isCreator)) {
                    request.setAttribute("selectionManagerUrl", selectionManagerUrl);
                    request.setAttribute("canSetDuration", isCreator);
                    request.setAttribute("duration", duration);
                    request.setAttribute("maxDuration", AdminDAO.getSystemSetting(
                            AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_MAX).getIntValue());
                    request.setAttribute("startTime", startTime);
            %>
            <t:game_time
                    gameId="${gameProducer.game.id}"
                    selectionManagerUrl="${selectionManagerUrl}"
                    duration="${duration}"
                    maxDuration="${maxDuration}"
                    startTime="${startTime}"
                    canSetDuration="${canSetDuration}"/>
            <%
                }
            %>

            <div class="btn-group">
                <button class="btn btn-sm btn-outline-secondary text-nowrap" id="btnScoreboard"
                        data-bs-toggle="modal" data-bs-target="#scoreboard">
                    <i class="fa fa-book"></i>
                    Scoreboard
                </button>
                <button class="btn btn-sm btn-outline-secondary" id="btnScoringModal"
                        data-bs-toggle="modal" data-bs-target="#scoringModal">
                    <i class="fa fa-question-circle"></i>
                </button>
            </div>
            <t:modal title="Scoring System" id="scoringModal" modalBodyClasses="bg-light">
                <jsp:attribute name="content">
                    <jsp:include page="/jsp/scoring_system.jsp"/>
                </jsp:attribute>
            </t:modal>

            <button type="button" class="btn btn-sm btn-outline-secondary text-nowrap" id="btnHistory"
                    data-bs-toggle="modal" data-bs-target="#history">
                <i class="fa fa-history"></i>
                Timeline
            </button>

            <a href="${url.forPath(Paths.PROJECT_EXPORT)}?gameId=<%=gameId%>"
               class="btn btn-sm btn-outline-secondary text-nowrap" id="btnProjectExport"
               title="Export as a Gradle project to import into an IDE.">
                <i class="fa fa-download"></i>
                Gradle Export
            </a>

            <button type="button" class="btn btn-sm btn-outline-secondary text-nowrap" id="btnFeedback"
                    data-bs-toggle="modal" data-bs-target="#playerFeedback">
                <i class="fa fa-comment"></i>
                Feedback
            </button>

            <jsp:include page="/jsp/game_components/keymap_config.jsp"/>

            <t:game_chat/>
        </div>
    </div>

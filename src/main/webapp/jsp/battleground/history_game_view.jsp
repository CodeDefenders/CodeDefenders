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
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ page import="org.codedefenders.model.User"%>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="java.util.Optional" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.database.MultiplayerGameDAO" %>
<%@ page import="org.codedefenders.servlets.util.ServletUtils" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.Role" %>

<%--
    @param Integer gameId
        The id of the game to be displayed.
--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Game History"); %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    MultiplayerGame game;
    {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            response.sendRedirect(request.getContextPath() + Paths.GAMES_HISTORY);
            return;
        }
        game = MultiplayerGameDAO.getMultiplayerGame(gameIdOpt.get());

        if (game == null || game.getState() != GameState.FINISHED) {
            response.sendRedirect(request.getContextPath() + Paths.GAMES_OVERVIEW);
            return;
        }

        request.setAttribute("game", game);
    }

    final GameClass cut = game.getCUT();
    Role role = game.getRole(login.getUserId());

    final User user = login.getUser();

%>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<%
    classViewer.setClassCode(game.getCUT());
    classViewer.setDependenciesForClass(game.getCUT());
%>


<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<%
    gameHighlighting.setGameData(game.getMutants(), game.getTests());
    gameHighlighting.setFlaggingData(game.getMode(), game.getId());
    gameHighlighting.setEnableFlagging(false);
    gameHighlighting.setCodeDivSelector("#cut-div");
%>

<%--
<jsp:useBean id="mutantAccordion" class="org.codedefenders.beans.game.MutantAccordionBean" scope="request"/>
<%
    mutantAccordion.setMutantAccordionData(cut, user, game.getMutants());
    mutantAccordion.setFlaggingData(game.getMode(), game.getId());
    mutantAccordion.setEnableFlagging(false);
    mutantAccordion.setViewDiff(true);
%>
--%>

<%--
<jsp:useBean id="testAccordion" class="org.codedefenders.beans.game.TestAccordionBean" scope="request"/>
<% testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants()); %>
--%>

<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>


<jsp:useBean id="playerFeedback" class="org.codedefenders.beans.game.PlayerFeedbackBean" scope="request"/>
<%
    playerFeedback.setGameInfo(game.getId(), game.getCreatorId());
    playerFeedback.setPlayerInfo(login.getUser(), role);
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


<%-- -------------------------------------------------------------------------------- --%>


<jsp:include page="/jsp/header_game.jsp"/>

<link href="${pageContext.request.contextPath}/css/game.css" rel="stylesheet">

<jsp:include page="/jsp/player_feedback.jsp"/>
<jsp:include page="/jsp/battleground/game_scoreboard.jsp"/>

<jsp:include page="/jsp/battleground/game_history.jsp"/>

<div class="row">
    <div class="col-xl-6 col-12">
        <div id="mutants-div">
            <div class="game-component-header"><h3>Existing Mutants</h3></div>
            <t:mutant_accordion/>
        </div>

        <div id="tests-div">
            <div class="game-component-header"><h3>JUnit Tests</h3></div>
            <t:test_accordion/>
        </div>
    </div>

    <div class="col-xl-6 col-12" id="cut-div">
        <div class="game-component-header"><h3>Class Under Test</h3></div>
        <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
        <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
    </div>
</div>

<%@ include file="/jsp/footer_game.jsp" %>

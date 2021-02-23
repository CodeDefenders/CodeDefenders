<%--
  ~ Copyright (C) 2020 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ page import="org.codedefenders.model.User" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="java.util.Optional" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.servlets.util.ServletUtils" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>
<%@ page import="org.codedefenders.database.MeleeGameDAO" %>
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="org.codedefenders.database.PlayerDAO" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.Collections" %>

<%--
    @param Integer gameId
        The id of the game to be displayed.
--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Game History"); %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
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

<jsp:useBean id="scoreboard" class="org.codedefenders.beans.game.MeleeScoreboardBean" scope="request"/>

<jsp:useBean id="history" class="org.codedefenders.beans.game.HistoryBean" scope="request"/>
<%
    history.setLogin(login);
    history.setGameId(game.getId());

    Player player = PlayerDAO.getPlayerForUserAndGame(login.getUserId(), game.getId());
    List<Player> otherPlayers = game.getPlayers().stream()
            .filter(p -> {
                if (player != null) {
                    return p.getId() != player.getId();
                } else {
                    return true;
                }
            })
            .collect(Collectors.toList());
    // We simply need two distinct sets, to determine which events to display on the left/right side of the timeline
    history.setPlayers(Collections.singletonList(player), otherPlayers);
%>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:include page="/jsp/melee/header_game.jsp"/>

<jsp:include page="/jsp/scoring_tooltip.jsp"/>
<jsp:include page="/jsp/melee/game_scoreboard.jsp"/>
<jsp:include page="/jsp/melee/game_scoreboard.jsp"/>

<div class="row">

    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Existing Mutants</h3>
            <t:mutant_accordion/>
        </div>

        <div id="tests-div">
            <h3>JUnit tests </h3>
            <t:test_accordion/>
        </div>
    </div>

    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
        <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
        <jsp:include page="/jsp/game_components/mutant_explanation.jsp"/>
    </div>
</div>

<%@ include file="/jsp/melee/footer_game.jsp" %>

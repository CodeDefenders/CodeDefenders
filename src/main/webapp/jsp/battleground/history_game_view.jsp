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
<%@page import="org.codedefenders.game.Role"%>
<%@page import="org.codedefenders.util.Paths"%>
<%@page import="java.util.Optional"%>
<%@page import="org.codedefenders.game.multiplayer.MultiplayerGame"%>
<%@ page import="org.codedefenders.game.GameMode" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.database.MultiplayerGameDAO" %>
<%@ page import="org.codedefenders.servlets.util.ServletUtils" %>

<%
    String pageTitle="Game History";

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
    }

    int uid = ((Integer) session.getAttribute("uid"));
    Role role = game.getRole(uid);
%>

<%-- Set request attributes for the components. --%>
<%
     /* playerFeedback and scoreboard */
    request.setAttribute("game", game);

    /* class_viewer */
    final GameClass cut = game.getCUT();
    request.setAttribute("className", cut.getBaseName());
    request.setAttribute("classCode", cut.getAsHTMLEscapedString());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

    /* test_accordion */
    request.setAttribute("cut", cut);
    request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
    request.setAttribute("mutantsMarkedEquivalent", game.getMutantsMarkedEquivalentPending());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", GameMode.PARTY);
    request.setAttribute("gameId", game.getId());
    int userId = ServletUtils.userId(request); // required for playerFeedback, too

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
    // request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    // request.setAttribute("gameType", GameMode.PARTY);
//    request.setAttribute("gameId", game.getId());
%>
<%@ include file="/jsp/battleground/header_game.jsp" %>

<jsp:include page="/jsp/scoring_tooltip.jsp"/>
<jsp:include page="/jsp/playerFeedback.jsp"/>
<jsp:include page="/jsp/battleground/game_scoreboard.jsp"/>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Existing Mutants</h3>
            <%@include file="../game_components/mutants_list.jsp"%>
        </div>

        <div id="tests-div">
            <h3>JUnit tests </h3>
            <jsp:include page="../game_components/test_accordion.jsp"/>
        </div>
    </div>

    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <%@include file="../game_components/class_viewer.jsp"%>
        <%@ include file="../game_components/game_highlighting.jsp" %>
        <%@include file="../game_components/mutant_explanation.jsp"%>
    </div>
</div>

<%@ include file="/jsp/battleground/footer_game.jsp" %>

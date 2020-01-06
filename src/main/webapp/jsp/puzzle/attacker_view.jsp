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
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.puzzle.Puzzle" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME" %>
<%@ page import="static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="org.codedefenders.validation.code.CodeValidatorLevel" %>
<%@ page import="org.codedefenders.game.GameMode" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%--
    Puzzle game view for a attacker. Retrieves the given puzzle game
    from the request and calls the required game components.

    @param PuzzleGame Constants#REQUEST_ATTRIBUTE_PUZZLE_GAME
        The puzzle game to be displayed.
--%>
<jsp:include page="/jsp/header_main.jsp"/>

</div></div></div></div></div>

<div class="game-container">

<%-- Set request attributes for the components. --%>
<%
    PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);

    final GameClass cut = game.getCUT();
    final Puzzle puzzle = game.getPuzzle();
%>

<jsp:useBean id="mutantEditor" class="org.codedefenders.beans.game.MutantEditorBean" scope="request"/>
<% mutantEditor.setDependenciesForClass(game.getCUT()); %>
<% mutantEditor.setEditableLinesForPuzzle(puzzle); %>

<%
    /* mutant_editor */
    String previousMutantCode = (String) request.getSession().getAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    request.getSession().removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    if (previousMutantCode != null) {
        mutantEditor.setPreviousMutantCode(cut, previousMutantCode);
    } else {
        mutantEditor.setMutantCodeForClass(cut);
    }

    request.setAttribute("mutantName", cut.getBaseName());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

    /* test_accordion */
    request.setAttribute("cut", cut);
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("tests", game.getTests());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", new LinkedList<Mutant>());
    request.setAttribute("mutantsMarkedEquivalent", new LinkedList<Mutant>());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", GameMode.PUZZLE);
    request.setAttribute("gameId", game.getId());

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
    // request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    // request.setAttribute("gameType", GameMode.PUZZLE);
    // request.setAttribute("gameId", game.getId());

    /* finished_modal TODO */

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", CodeValidatorLevel.MODERATE);

    /* mutant_progressbar */
    // request.setAttribute("gameId", game.getId());

    final String title = puzzle.getTitle();
    final String description = puzzle.getDescription();
%>

<jsp:include page="/jsp/push_notifications.jsp"/>

    <div class="row" style="padding: 0px 15px;">
        <h4 class="col-md-2"><b><%=title%></b></h4>
        <h4><%=description%></h4>
    </div>
    <hr class="hr-primary" style="margin: 5px">
<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Mutants</h3>
            <jsp:include page="/jsp/game_components/mutants_list.jsp"/>
        </div>

        <% if (game.getLevel() == GameLevel.EASY) { %>
        <div id="tests-div">
            <h3>JUnit tests</h3>
            <jsp:include page="/jsp/game_components/test_accordion.jsp"/>
        </div>
        <% } %>
    </div>

    <div class="col-md-6" id="cut-div">
        <h3 style="margin-bottom: 0;">Create a mutant here</h3>

        <form id="reset" action="<%=request.getContextPath() + Paths.PUZZLE_GAME%>" method="post">
            <input type="hidden" name="formType" value="reset">
            <input type="hidden" name="gameId" value="<%= game.getId() %>">
            <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset" style="margin-top: -40px; margin-right: 80px">
                Reset
            </button>
        </form>

        <jsp:include page="/jsp/game_components/push_mutant_progress_bar.jsp"/>
        <form id="atk" action="<%=request.getContextPath() + Paths.PUZZLE_GAME%>" method="post">
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk"
                    onClick="mutantProgressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';" style="margin-top: -50px"
                <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                Attack!
            </button>

            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%= game.getId() %>">

            <jsp:include page="/jsp/game_components/mutant_editor.jsp"/>
            <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
        </form>
        <jsp:include page="/jsp/game_components/mutant_explanation.jsp"/>
        <jsp:include page="/jsp/game_components/editor_help_config_toolbar.jsp"/>
    </div>
</div>

<jsp:include page="/jsp/game_components/editor_help_config_modal.jsp"/>

<%@ include file="/jsp/footer_game.jsp" %>

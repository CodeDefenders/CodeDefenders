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

<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.puzzle.Puzzle" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%--
    Puzzle game view for a attacker. Retrieves the given puzzle game
    from the request and calls the required game components.

    @param PuzzleGame Constants#REQUEST_ATTRIBUTE_PUZZLE_GAME
        The puzzle game to be displayed.
--%>

<%
    PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);

    final GameClass cut = game.getCUT();
    final Puzzle puzzle = game.getPuzzle();

    final String title = puzzle.getTitle();
    final String description = puzzle.getDescription();

    boolean showTestAccordion = game.getLevel() == GameLevel.EASY || game.getState() == GameState.SOLVED;
%>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="previousSubmission" class="org.codedefenders.beans.game.PreviousSubmissionBean" scope="request"/>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:useBean id="mutantEditor" class="org.codedefenders.beans.game.MutantEditorBean" scope="request"/>
<%
    mutantEditor.setDependenciesForClass(game.getCUT());
    mutantEditor.setClassName(cut.getName());
    mutantEditor.setEditableLinesForPuzzle(puzzle);
    if (previousSubmission.hasMutant()) {
        mutantEditor.setPreviousMutantCode(previousSubmission.getMutantCode());
    } else {
        mutantEditor.setMutantCodeForClass(cut);
    }
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
    mutantAccordion.setMutantAccordionData(cut, login.getUser(), game.getMutants());
    mutantAccordion.setFlaggingData(game.getMode(), game.getId());
    mutantAccordion.setEnableFlagging(false);
    mutantAccordion.setViewDiff(true);
%>
--%>

<%--
<jsp:useBean id="testAccordion" class="org.codedefenders.beans.game.TestAccordionBean" scope="request"/>
<%
    if (showTestAccordion) {
        testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants());
    }
%>
--%>

<jsp:useBean id="mutantProgressBar" class="org.codedefenders.beans.game.MutantProgressBarBean" scope="request"/>
<% mutantProgressBar.setGameId(game.getId()); %>


<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>


<% previousSubmission.clear(); %>

<%-- -------------------------------------------------------------------------------- --%>


<jsp:include page="/jsp/header_main.jsp"/>
</div></div></div></div></div>
<div class="game-container">

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
            <t:mutant_accordion/>
        </div>

        <% if (showTestAccordion) { %>
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

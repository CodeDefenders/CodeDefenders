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
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

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

    boolean showTestAccordion = game.getLevel() == GameLevel.EASY || game.getState() == GameState.SOLVED;
    String title = "Puzzle: " + puzzle.getChapter().getTitle() + " - " + puzzle.getTitle();

    pageContext.setAttribute("game", game);
    pageContext.setAttribute("cut", cut);
    pageContext.setAttribute("puzzle", puzzle);
    pageContext.setAttribute("showTestAccordion", showTestAccordion);
    pageContext.setAttribute("title", title);
%>

<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

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


<jsp:useBean id="mutantErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>
<%
    mutantErrorHighlighting.setCodeDivSelector("#cut-div");
    if (previousSubmission.hasErrorLines()) {
        mutantErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>

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


<%-- -------------------------------------------------------------------------------- --%>


<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <div id="game-container" class="container-fluid">

            <h4><b>${title}</b></h4>
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3">
                <h4 class="m-0">${puzzle.description}</h4>
                <jsp:include page="/jsp/game_components/keymap_config.jsp"/>
            </div>
            <hr>

            <div class="row">
                <div class="col-xl-6 col-12">
                    <t:mutant_accordion/>

                    <c:if test="${showTestAccordion}">
                        <div id="tests-div">
                            <div class="game-component-header"><h3>JUnit Tests</h3></div>
                            <t:test_accordion/>
                        </div>
                    </c:if>
                </div>

                <div class="col-xl-6 col-12" id="cut-div">
                    <jsp:include page="/jsp/game_components/mutant_progress_bar.jsp"/>

                    <div class="game-component-header">
                        <h3>Create a mutant here</h3>
                        <div>

                            <form id="reset" action="${url.forPath(Paths.PUZZLE_GAME)}" method="post">
                                <input type="hidden" name="formType" value="reset">
                                <input type="hidden" name="gameId" value="${game.id}">
                                <button class="btn btn-warning" id="btnReset">
                                    Reset
                                </button>
                            </form>

                            <t:submit_mutant_button gameActive="${gameProducer.game.state == GameState.ACTIVE}"
                                                    intentionCollectionEnabled="false"/>
                        </div>
                    </div>

                    <form id="atk" action="${url.forPath(Paths.PUZZLE_GAME)}" method="post">
                        <input type="hidden" name="formType" value="createMutant">
                        <input type="hidden" name="gameId" value="${game.id}">
                        <input type="hidden" id="attacker_intention" name="attacker_intention" value="">

                        <jsp:include page="/jsp/game_components/mutant_editor.jsp"/>
                        <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
                        <jsp:include page="/jsp/game_components/mutant_error_highlighting.jsp"/>
                    </form>
                </div>
            </div>
        </div>

        <t:game_js_init/>
    </jsp:body>
</p:main_page>

<% previousSubmission.clear(); %>

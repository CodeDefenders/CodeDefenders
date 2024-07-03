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

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.puzzle.Puzzle" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.Mutant" %>

<%--
    Puzzle game view for a attacker. Retrieves the given puzzle game
    from the request and calls the required game components.

    @param PuzzleGame Constants#REQUEST_ATTRIBUTE_PUZZLE_GAME
        The puzzle game to be displayed.
--%>

<%
    PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);
    final Puzzle puzzle = game.getPuzzle();
    final String title = puzzle.getTitle();
    final String description = puzzle.getDescription();
    final GameClass cut = game.getCUT();
    final Mutant equivMutant = game.getMutants().get(0);
    final String mutantClaimedMessage = "Mutant " + equivMutant.getId() + " automatically claimed equivalent";
%>

<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Puzzle: " + puzzle.getChapter().getTitle() + " - " + puzzle.getTitle()); %>

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

<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<%
    classViewer.setClassCode(game.getCUT());
    classViewer.setDependenciesForClass(game.getCUT());
%>


<jsp:useBean id="testEditor" class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>
<%
    // Set editable lines from class since they depend on the generated test template
    testEditor.setEditableLinesForClass(cut);

    testEditor.setMockingEnabled(false);
    testEditor.setAssertionLibrary(cut.getAssertionLibrary());
    if (previousSubmission.hasTest()) {
        testEditor.setPreviousTestCode(previousSubmission.getTestCode());
    } else {
        testEditor.setTestCodeForClass(cut);
    }
%>


<jsp:useBean id="testErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>
<%
    testErrorHighlighting.setCodeDivSelector("#ut-div");
    if (previousSubmission.hasErrorLines()) {
        testErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>

<%--
<jsp:useBean id="testAccordion" class="org.codedefenders.beans.game.TestAccordionBean" scope="request"/>
<% testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants()); %>
--%>

<jsp:useBean id="testProgressBar" class="org.codedefenders.beans.game.TestProgressBarBean" scope="request"/>
<% testProgressBar.setGameId(game.getId()); %>


<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:include page="/jsp/header.jsp"/>

<link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">

<div id="game-container" class="container-fluid">

    <h4><b><%=title%>
    </b></h4>
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3">
        <h4 class="m-0"><%=description%>
        </h4>
        <jsp:include page="/jsp/game_components/keymap_config.jsp"/>
    </div>
    <hr>

    <div class="row">
        <div class="col-xl-6 col-12" id="equivmut-div">
            <div class="game-component-header"><h3><%=mutantClaimedMessage%>
            </h3></div>

            <div class="equivalence-container">

                <h3>Diff</h3>
                <div class="card">
                    <div class="card-body p-0 loading loading-height-200">
                    <pre id="diff-pre" class="m-0"><textarea id="diff" class="mutdiff" title="mutdiff"
                                                             readonly><%=equivMutant.getHTMLEscapedPatchString()%></textarea></pre>
                    </div>
                </div>

                <script type="module">
                    import CodeMirror from '${url.forPath("/js/codemirror.mjs")}';
                    import {LoadingAnimation} from '${url.forPath("/js/codedefenders_main.mjs")}';


                    const textarea = document.getElementById('diff');
                    const codemirror = CodeMirror.fromTextArea(textarea, {
                        lineNumbers: true,
                        mode: "text/x-diff",
                        readOnly: true,
                        autoRefresh: true
                    });
                    codemirror.getWrapperElement().classList.add('codemirror-readonly');
                    codemirror.setSize('100%', '100%');
                    LoadingAnimation.hideAnimation(textarea);
                </script>

                <jsp:include page="/jsp/game_components/test_progress_bar.jsp"/>

                <h3 class="mt-3">Not equivalent? Write a killing test here:</h3>
                <form id="equivalenceForm" action="${url.forPath(Paths.BATTLEGROUND_GAME)}" method="post">
                    <input type="hidden" name="formType" value="resolveEquivalence">
                    <input type="hidden" name="gameId" value="<%=game.getId()%>">
                    <input type="hidden" id="equivMutantId" name="equivMutantId" value="<%=equivMutant.getId()%>">
                    <input type="hidden" id="resolveAction" name="resolveAction" value="">

                    <jsp:include page="/jsp/game_components/test_editor.jsp"/>

                    <div class="d-flex justify-content-between mt-2 mb-2">
                        <button class="btn btn-danger" id="accept-equivalent-button" type="button">Accept As Equivalent
                        </button>
                        <button class="btn btn-primary" id="reject-equivalent-button" type="button">Submit Killing Test
                        </button>

                        <script type="module">
                            import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';

                            const testProgressBar = await objects.await('testProgressBar');


                            document.getElementById("accept-equivalent-button").addEventListener('click', function (event) {
                                if (confirm('Accepting Equivalence will lose all mutant points. Are you sure?')) {
                                    this.form['resolveAction'].value = 'accept';
                                    this.form.submit();
                                    this.disabled = true;
                                }
                            });
                            document.getElementById("reject-equivalent-button").addEventListener('click', function (event) {
                                this.form['resolveAction'].value = 'reject';
                                this.form.submit();
                                this.disabled = true;
                                testProgressBar.activate();
                            });
                        </script>

                    </div>

                    <span>Note: If the game finishes with this equivalence unsolved, you will lose points!</span>
                </form>

            </div>
        </div>

        <div class="col-xl-6 col-12" id="cut-div">
            <div class="game-component-header"><h3>Class Under Test</h3></div>
            <t:defender_intention_collection_note/>
            <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
            <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
            <jsp:include page="/jsp/game_components/test_error_highlighting.jsp"/>
        </div>

    </div>
</div>


<%@ include file="/jsp/footer_game.jsp" %>


<% previousSubmission.clear(); %>

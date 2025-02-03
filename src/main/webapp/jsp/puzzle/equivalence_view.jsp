<%--

    Copyright (C) 2024 Code Defenders contributors

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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.game.puzzle.Puzzle" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.GameLevel" %>

<%--
    Puzzle game view for a attacker. Retrieves the given puzzle game
    from the request and calls the required game components.

    @param PuzzleGame Constants#REQUEST_ATTRIBUTE_PUZZLE_GAME
        The puzzle game to be displayed.
--%>

<%
    PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);
    final Puzzle puzzle = game.getPuzzle();
    final GameClass cut = game.getCUT();
    final Mutant equivMutant = game.getMutants().get(0);
    final String mutantClaimedMessage = "Mutant " + equivMutant.getId() + " automatically claimed equivalent";
    final boolean showTestAccordion = game.getLevel() == GameLevel.EASY || game.getState() == GameState.SOLVED;
    final int mutantLine = equivMutant.getLines().stream().min(Integer::compare).orElse(0);

    pageContext.setAttribute("game", game);
    pageContext.setAttribute("puzzle", puzzle);
    pageContext.setAttribute("title", puzzle.getTitle());
    pageContext.setAttribute("description", puzzle.getDescription());
    pageContext.setAttribute("equivMutant", equivMutant);
    pageContext.setAttribute("mutantClaimedMessage", mutantClaimedMessage);
    pageContext.setAttribute("showTestAccordion", showTestAccordion);
    pageContext.setAttribute("mutantLine", mutantLine);
%>

<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>

<jsp:useBean id="previousSubmission" class="org.codedefenders.beans.game.PreviousSubmissionBean" scope="request"/>


<%-- -------------------------------------------------------------------------------- --%>

<%--
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
--%>

<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<%
    gameHighlighting.setGameData(game.getMutants(), game.getTests());
    gameHighlighting.setFlaggingData(game.getMode(), game.getId());
    gameHighlighting.setEnableFlagging(false);
    // gameHighlighting.setCodeDivSelector("#cut-div");
%>

<%--
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
    testEditor.setReadonly(game.getState() == GameState.SOLVED);
%>
--%>


<jsp:useBean id="testErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>
<%
    // testErrorHighlighting.setCodeDivSelector("#ut-div");
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

<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <div id="game-container" class="container-fluid">
            <h4><b>${title}</b></h4>
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3">
                <h4 class="m-0">${description}</h4>
                <div class="align-items-center d-flex gap-4">
                    <t:round_counter game="${game}"/>
                    <jsp:include page="/jsp/game_components/keymap_config.jsp"/>
                </div>
            </div>
            <hr>

            <div class="row">
                <div class="col-xl-6 col-12" id="equivmut-div">
                    <div class="game-component-header"><h3>${mutantClaimedMessage}
                    </h3></div>

                    <div class="equivalence-container">

                        <h3>Diff</h3>
                        <div class="card">
                            <div class="card-body p-0 loading loading-height-200">
                                <pre id="diff-pre" class="m-0"><textarea id="diff" class="mutdiff" title="mutdiff"
                                                                         readonly>${equivMutant.getHTMLEscapedPatchString()}</textarea></pre>
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

                        <c:choose>
                            <c:when test="${game.state == GameState.SOLVED}">
                                <c:choose>
                                    <c:when test="${puzzle.equivalent}">
                                        <h3 class="mt-3">The mutant is equivalent!</h3>
                                    </c:when>
                                    <c:otherwise>
                                        <h3 class="mt-3">Not equivalent! Your killing test:</h3>
                                    </c:otherwise>
                                </c:choose>
                            </c:when>
                            <c:otherwise>
                                <h3 class="mt-3">Not equivalent? Write a killing test here:</h3>
                            </c:otherwise>
                        </c:choose>
                        <c:choose>
                            <c:when test="${game.state == GameState.SOLVED and puzzle.equivalent}">
                                <p>The mutant was accepted as equivalent</p>
                            </c:when>
                            <c:otherwise>
                                <form id="equivalenceForm" action="${url.forPath(Paths.PUZZLE_GAME)}" method="post">
                                    <input type="hidden" name="formType" value="resolveEquivalence">
                                    <input type="hidden" name="gameId" value="${game.id}">
                                    <input type="hidden" id="equivMutantId" name="equivMutantId"
                                           value="${equivMutant.id}">
                                    <input type="hidden" id="resolveAction" name="resolveAction" value="">

                                    <jsp:include page="/jsp/game_components/test_editor.jsp"/>

                                    <c:if test="${game.state == GameState.ACTIVE}">
                                        <div class="d-flex justify-content-between mt-2 mb-2">
                                            <button class="btn btn-danger" id="accept-equivalent-button" type="button"
                                                ${game.state != GameState.ACTIVE ? 'disabled' : ''}>Accept As Equivalent
                                            </button>
                                            <button class="btn btn-primary" id="reject-equivalent-button" type="button"
                                                ${game.state != GameState.ACTIVE ? 'disabled' : ''}>Submit Killing Test
                                            </button>

                                            <script type="module">
                                                import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';

                                                const testProgressBar = await objects.await('testProgressBar');

                                                document.getElementById("accept-equivalent-button").addEventListener('click', function () {
                                                    if (confirm('Are you sure that this mutant is equivalent and cannot be killed?')) {
                                                        this.form['resolveAction'].value = 'accept';
                                                        this.form.submit();
                                                        this.disabled = true;
                                                    }
                                                });
                                                document.getElementById("reject-equivalent-button").addEventListener('click', function () {
                                                    this.form['resolveAction'].value = 'reject';
                                                    this.form.submit();
                                                    this.disabled = true;
                                                    testProgressBar.activate();
                                                });
                                            </script>
                                        </div>
                                    </c:if>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <div class="col-xl-6 col-12" id="cut-div">
                    <div class="game-component-header"><h3>Class Under Test</h3></div>
                    <t:defender_intention_collection_note/>
                    <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
                    <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
                    <jsp:include page="/jsp/game_components/test_error_highlighting.jsp"/>
                    <script type="module">
                        import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';

                        const classViewer = await objects.await("classViewer");
                        classViewer.jumpToLine(${mutantLine});
                    </script>
                </div>
            </div>

            <c:if test="${showTestAccordion}">
                <div class="row">
                    <div class="col-xl-6 col-12">
                        <div id="tests-div">
                            <div class="game-component-header"><h3>JUnit Tests</h3></div>
                            <t:test_accordion/>
                        </div>
                    </div>
                </div>
            </c:if>
        </div>

        <t:game_js_init/>
    </jsp:body>
</p:main_page>


<% previousSubmission.clear(); %>

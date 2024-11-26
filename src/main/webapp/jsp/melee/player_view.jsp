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
<%--@elvariable id="previousTest" type="org.codedefenders.game.Test"--%>
<%--@elvariable id="game" type="org.codedefenders.game.multiplayer.MeleeGame"--%>

<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.dto.SimpleUser" %>

<%--
    @param MeleeGame game
        The game to be displayed.
--%>

<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>
<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
    final GameClass cut = game.getCUT();

    boolean openEquivalenceDuel = request.getAttribute("openEquivalenceDuel") != null;

    // This is set by the GameManager but we could have it set by a different servlet common for all the games which require equivalence duels
    Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
    // This is set by the GameManager but we could have it set by a different servlet common for all the games which require equivalence duels
    SimpleUser equivDefender = (SimpleUser) request.getAttribute("equivDefender");

    String mutantClaimedMessage = null;
    int mutantLine = 0;
    if (openEquivalenceDuel) {
        mutantClaimedMessage = equivDefender.getId() == Constants.DUMMY_CREATOR_USER_ID
                ? "Mutant " + equivMutant.getId() + " automatically claimed equivalent"
                : "Mutant " + equivMutant.getId() + " claimed equivalent by " + equivDefender.getName();
        mutantLine = equivMutant.getLines().stream().min(Integer::compare).orElse(0);
    }

    // These two are set in the MeleeGameManager, since we need to do a getUserIdForPlayerId lookup for test filtering.
    final List<Test> playerTests = (List<Test>) request.getAttribute("playerTests");
    final List<Test> enemyTests = (List<Test>) request.getAttribute("enemyTests");

    pageContext.setAttribute("mutantLine", mutantLine);
%>

<jsp:useBean id="previousSubmission"
             class="org.codedefenders.beans.game.PreviousSubmissionBean"
             scope="request"/>

<%-- -------------------------------------------------------------------------------- --%>

<%-- Mutant editor in player mode is the same as class viewer in defender --%>
<%--
<jsp:useBean id="mutantEditor"
             class="org.codedefenders.beans.game.MutantEditorBean" scope="request"/>
<%
    mutantEditor.setClassName(cut.getName());
    mutantEditor.setDependenciesForClass(game.getCUT());
    if (previousSubmission.hasMutant()) {
        mutantEditor.setPreviousMutantCode(previousSubmission.getMutantCode());
    } else {
        mutantEditor.setMutantCodeForClass(cut);
    }
%>
--%>


<jsp:useBean id="gameHighlighting"
             class="org.codedefenders.beans.game.GameHighlightingBean"
             scope="request"/>
<%
    gameHighlighting.setGameData(game.getMutants(), playerTests, login.getUserId());
    gameHighlighting.setFlaggingData(game.getMode(), game.getId());
    gameHighlighting.setEnableFlagging(true);
    // We should show game highlighting only inside the mutant editor
    if (!openEquivalenceDuel) {
        gameHighlighting.setAlternativeTests(enemyTests);
    }
%>

<jsp:useBean id="testErrorHighlighting"
             class="org.codedefenders.beans.game.ErrorHighlightingBean"
             scope="request"/>
<%
    if (previousSubmission.hasTest() && previousSubmission.hasErrorLines()) {
        testErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>

<jsp:useBean id="mutantErrorHighlighting"
             class="org.codedefenders.beans.game.ErrorHighlightingBean"
             scope="request"/>
<%
    if (previousSubmission.hasMutant() && previousSubmission.hasErrorLines()) {
        mutantErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>
<%--
<jsp:useBean id="mutantAccordion"
             class="org.codedefenders.beans.game.MutantAccordionBean"
             scope="request"/>
<%
    mutantAccordion.setGame(game);
    mutantAccordion.setMutantAccordionData(cut, user, game.getMutants());
    mutantAccordion.setFlaggingData(game.getMode(), game.getId());
    mutantAccordion.setEnableFlagging(true);
    mutantAccordion.setViewDiff(game.getLevel() == GameLevel.EASY);
%>
--%>

<%--
<jsp:useBean id="testAccordion"
             class="org.codedefenders.beans.game.TestAccordionBean" scope="request"/>
<%
    testAccordion.setTestAccordionData(cut, playerTests, game.getMutants());
%>
--%>

<jsp:useBean id="mutantProgressBar"
             class="org.codedefenders.beans.game.MutantProgressBarBean"
             scope="request"/>
<%
    mutantProgressBar.setGameId(game.getId());
%>

<jsp:useBean id="testProgressBar"
             class="org.codedefenders.beans.game.TestProgressBarBean"
             scope="request"/>
<%
    testProgressBar.setGameId(game.getId());
%>


<jsp:useBean id="mutantExplanation"
             class="org.codedefenders.beans.game.MutantExplanationBean"
             scope="request"/>
<%
    mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel());
%>

<jsp:useBean id="testEditor"
             class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>
<%
    testEditor.setEditableLinesForClass(cut);
    testEditor.setMockingEnabled(cut.isMockingEnabled());
    testEditor.setAssertionLibrary(cut.getAssertionLibrary());
    if (previousSubmission.hasTest()) { // TODO: don't display the wron previous submission for equivalence duels
        testEditor.setPreviousTestCode(previousSubmission.getTestCode());
    } else {
        testEditor.setTestCodeForClass(cut);
    }
%>

<%--
<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<%
    classViewer.setClassCode(game.getCUT());
    classViewer.setDependenciesForClass(game.getCUT());
%>
--%>


<div class="row">

<% if (openEquivalenceDuel) { %>

    <%-- -------------------------------------------------------------------------------- --%>
    <%-- Equivalence Duel view --%>
    <%-- -------------------------------------------------------------------------------- --%>

    <div class="col-xl-6 col-12" id="equivmut-div">
        <div class="game-component-header"><h3><%=mutantClaimedMessage%></h3></div>

        <div class="equivalence-container">

            <h3>Diff</h3>
            <div class="card">
                <div class="card-body p-0 loading loading-height-200">
                    <pre id="diff-pre" class="m-0"><textarea id="diff" class="mutdiff" title="mutdiff" readonly><%=equivMutant.getHTMLEscapedPatchString()%></textarea></pre>
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
            <form id="equivalenceForm" action="${url.forPath(Paths.EQUIVALENCE_DUELS_GAME)}" method="post">
                <input type="hidden" name="formType" value="resolveEquivalence">
                <input type="hidden" name="gameId" value="<%=game.getId()%>">
                <input type="hidden" id="equivMutantId" name="equivMutantId" value="<%=equivMutant.getId()%>">
                <input type="hidden" id="resolveAction" name="resolveAction" value="">

                <jsp:include page="/jsp/game_components/test_editor.jsp"/>

                <div class="d-flex justify-content-between mt-2 mb-2">
                    <button class="btn btn-danger" id="accept-equivalent-button" type="button">Accept As Equivalent</button>
                    <button class="btn btn-primary" id="reject-equivalent-button" type="button">Submit Killing Test</button>

                    <script type="module">
                        import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
                        const testProgressBar = objects.await('testProgressBar');


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
        <jsp:include page="/jsp/game_components/test_error_highlighting.jsp"/>
    </div>

    <div class="col-xl-6 col-12" id="cut-div">
        <div class="game-component-header"><h3>Class Under Test</h3></div>
        <t:defender_intention_collection_note/>
        <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
        <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
        <script type="module">
            import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
            const classViewer = await objects.await("classViewer");
            classViewer.jumpToLine(${mutantLine});
        </script>
    </div>

<% } else { %>

    <%-- -------------------------------------------------------------------------------- --%>
    <%-- Attacker view --%>
    <%-- -------------------------------------------------------------------------------- --%>

    <div class="col-xl-6 col-12" id="newmut-div">

        <jsp:include page="/jsp/game_components/mutant_progress_bar.jsp"/>

        <div class="game-component-header">
            <h3>Create a mutant here</h3>
            <div>

                <div data-bs-toggle="tooltip" data-bs-html="true"
                     title='<p>Switch between showing coverage of your tests (off) and enemy tests (on).</p><p class="mb-0"><i>Note: If you add/remove lines while creating a mutant the coverage highlighting may be misaligned until you submit the mutant.</i></p>'>
                    <input class="btn-check" type="checkbox" id="highlighting-switch" autocomplete="off">
                    <label class="btn btn-outline-secondary" for="highlighting-switch">
                        Enemy Coverage
                        <i class="fa fa-check ms-1 btn-check-active"></i>
                    </label>
                </div>

                <script type="module">
                    import $ from '${url.forPath("/js/jquery.mjs")}';

                    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';


                    const gameHighlighting = await objects.await('gameHighlighting');

                    $('#highlighting-switch').change(function () {
                        gameHighlighting.clearCoverage();
                        if (this.checked) {
                            gameHighlighting.highlightAlternativeCoverage();
                        } else {
                            gameHighlighting.highlightCoverage();
                        }
                    })
                </script>

                <form id="reset" action="${url.forPath(Paths.MELEE_GAME)}" method="post">
                    <input type="hidden" name="formType" value="reset">
                    <input type="hidden" name="gameId" value="<%=game.getId()%>">
                    <button class="btn btn-warning" id="btnReset">Reset</button>
                </form>

                <t:submit_mutant_button gameActive="${gameProducer.game.state == GameState.ACTIVE}"
                                        intentionCollectionEnabled="${gameProducer.game.capturePlayersIntention}"/>
            </div>
        </div>

        <t:defender_intention_collection_note/>

        <form id="atk"
              action="${url.forPath(Paths.MELEE_GAME)}"
              method="post">
            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%=game.getId()%>">
            <input type="hidden" id="attacker_intention" name="attacker_intention" value="">

            <jsp:include page="/jsp/game_components/mutant_editor.jsp"/>
            <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
            <jsp:include page="/jsp/game_components/mutant_error_highlighting.jsp"/>
        </form>
    </div>

    <%-- -------------------------------------------------------------------------------- --%>
    <%-- Defender view --%>
    <%-- -------------------------------------------------------------------------------- --%>

    <div class="col-xl-6 col-12" id="utest-div">

        <jsp:include page="/jsp/game_components/test_progress_bar.jsp"/>

        <div class="game-component-header">
            <h3>Write a new JUnit test here</h3>
            <div>
                <t:clone_previous_test_button game="${game}" previousTest="${previousTest}"/>

                <button type="submit" class="btn btn-defender btn-highlight"
                        id="submitTest" form="def"
                        <%if (game.getState() != GameState.ACTIVE) {%> disabled <%}%>>
                    Defend
                </button>

                <script type="module">
                    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
                    const testProgressBar = await objects.await('testProgressBar');


                    document.getElementById('submitTest').addEventListener('click', function (event) {
                        this.form.submit();
                        this.disabled = true;
                        testProgressBar.activate();
                    });
                </script>
            </div>
        </div>

        <form id="def"
              action="${url.forPath(Paths.MELEE_GAME)}"
              method="post">
            <jsp:include page="/jsp/game_components/test_editor.jsp"/>
            <input type="hidden" name="formType" value="createTest">
            <input type="hidden" name="gameId" value="<%=game.getId()%>">
            <input type="hidden" id="selected_lines" name="selected_lines" value="">
        </form>
        <jsp:include page="/jsp/game_components/test_error_highlighting.jsp"/>

    </div>

<% } %>

</div>

<div class="row">
    <div class="col-xl-6 col-12">
        <t:mutant_accordion/>
    </div>

    <div class="col-xl-6 col-12">
        <div class="game-component-header"><h3>JUnit Tests</h3></div>
        <t:test_accordion/>
    </div>
</div>

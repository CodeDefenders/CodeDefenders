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
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.Mutant" %>

<%--
    @param MutliplayerGame game
        The game to be displayed.
    @param Mutant equivMutant
        The mutant flagged as equivalent.
    @param User equivDefender
        The user that flagged the mutant.
--%>

<%
    Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
    User equivDefender = (User) request.getAttribute("equivDefender");

    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    final GameClass cut = game.getCUT();

    final String mutantClaimedMessage = equivDefender.getId() == Constants.DUMMY_CREATOR_USER_ID
            ? "Mutant " + equivMutant.getId() + " automatically claimed equivalent"
            : "Mutant " + equivMutant.getId() + " claimed equivalent by " + equivDefender.getUsername();
%>

<jsp:useBean id="previousSubmission" class="org.codedefenders.beans.game.PreviousSubmissionBean" scope="request"/>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<%
    classViewer.setClassCode(game.getCUT());
    classViewer.setDependenciesForClass(game.getCUT());
%>


<jsp:useBean id="testEditor" class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>
<%
    testEditor.setEditableLinesForClass(cut);
    testEditor.setMockingEnabled(cut.isMockingEnabled());
    if (previousSubmission.hasTest()) {
        testEditor.setPreviousTestCode(previousSubmission.getTestCode());
    } else {
        testEditor.setTestCodeForClass(cut);
    }
%>


<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<%
    gameHighlighting.setGameData(game.getMutants(), game.getTests());
    gameHighlighting.setFlaggingData(game.getMode(), game.getId());
    gameHighlighting.setEnableFlagging(false);
    gameHighlighting.setCodeDivSelector("#cut-div");
%>


<jsp:useBean id="testErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>
<%
    testErrorHighlighting.setCodeDivSelector("#equivmut-div");
    if (previousSubmission.hasTest() && previousSubmission.hasErrorLines()) {
        testErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>


<jsp:useBean id="testProgressBar" class="org.codedefenders.beans.game.TestProgressBarBean" scope="request"/>
<% testProgressBar.setGameId(game.getId()); %>


<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>


<% previousSubmission.clearButKeepMutant(); %>

<%-- -------------------------------------------------------------------------------- --%>


<div class="row">
    <div class="col-lg-6 col-sm-12" id="equivmut-div">
        <div class="game-component-header"><h3><%=mutantClaimedMessage%></h3></div>

        <div class="equivalence-container">

            <h3>Diff</h3>
            <div class="card">
                <div class="card-body p-0">
                    <pre id="diff-pre" class="m-0"><textarea id="diff" class="mutdiff" title="mutdiff" readonly><%=equivMutant.getHTMLEscapedPatchString()%></textarea></pre>
                </div>
            </div>

            <script>
                (function () {
                    const codemirror = CodeMirror.fromTextArea(document.getElementById('diff'), {
                        lineNumbers: true,
                        mode: "text/x-diff",
                        readOnly: 'nocursor',
                        autoRefresh: true
                    });
                    codemirror.setSize('100%', '100%');
                })();
            </script>

            <jsp:include page="/jsp/game_components/push_test_progress_bar.jsp"/>

            <h3 class="mt-3">Not equivalent? Write a killing test here:</h3>
            <form id="equivalenceForm" action="<%= request.getContextPath() + Paths.BATTLEGROUND_GAME %>" method="post">
                <input type="hidden" name="formType" value="resolveEquivalence">
                <input type="hidden" name="gameId" value="<%=game.getId()%>">
                <input type="hidden" id="equivMutantId" name="equivMutantId" value="<%=equivMutant.getId()%>">
                <input type="hidden" id="resolveAction" name="resolveAction" value="">

                <jsp:include page="/jsp/game_components/test_editor.jsp"/>

                <div class="d-flex justify-content-between mt-2 mb-2">
                    <button class="btn btn-danger" id="accept-equivalent-button" type="button">Accept As Equivalent</button>
                    <button class="btn btn-primary" id="reject-equivalent-button" type="button">Submit Killing Test</button>
                    <script>
                        document.getElementById("accept-equivalent-button").addEventListener('click', function (event) {
                            if (confirm('Accepting Equivalence will lose all mutant points. Are you sure?')) {
                                this.form['resolveAction'].value = 'accept';
                                this.form.submit();
                                this.disabled = true;
                            }
                        });
                        document.getElementById("reject-equivalent-button").addEventListener('click', function (event) {
                            testProgressBar();
                            this.form['resolveAction'].value = 'reject';
                            this.form.submit();
                            this.disabled = true;
                        });
                    </script>
                </div>

                <span>Note: If the game finishes with this equivalence unsolved, you will lose points!</span>
            </form>

        </div>
    </div>

    <div class="col-lg-6 col-sm-12" id="cut-div">
        <div class="game-component-header"><h3>Class Under Test</h3></div>
        <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
        <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
        <jsp:include page="/jsp/game_components/test_error_highlighting.jsp" />
    </div>

</div>

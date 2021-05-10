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
    <div class="col-lg-6" id="equivmut-div">
        <h3>Mutant <%= equivMutant.getId() %>
        <!-- check for automatically triggered equivalence duels -->
        <% if (equivDefender.getId() == Constants.DUMMY_CREATOR_USER_ID) { %>
            automatically claimed equivalent</h3>
        <% } else { %>
            claimed equivalent by <%= equivDefender.getUsername() %> </h3>
        <% } %>

        <div style="border: 5px dashed #f00; border-radius: 10px; width: 100%; padding: 10px;">
            <p><%=String.join("\n", equivMutant.getHTMLReadout())%></p>
            <a class="btn btn-default" data-toggle="collapse" href="#diff-collapse">Show Diff</a>
            <p></p>
            <pre id="diff-collapse" class="readonly-pre collapse"><textarea
                    id="diff" class="mutdiff readonly-textarea"
                    title="mutdiff"><%=equivMutant.getHTMLEscapedPatchString()%></textarea></pre>
            <script>
                $('#diff-collapse').on('shown.bs.collapse', function() {
                    var codeMirrorContainer = $(this).find(".CodeMirror")[0];
                    if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                        codeMirrorContainer.CodeMirror.refresh();
                    } else {
                        var showDiff = CodeMirror.fromTextArea(document.getElementById('diff'), {
                            lineNumbers: false,
                            mode: "text/x-diff",
                            readOnly: true,
                            autoRefresh: true
                        });
                        showDiff.setSize("100%", 210);
                    }
                });
            </script>

            <jsp:include page="/jsp/game_components/push_test_progress_bar.jsp"/>
            <h3>Not equivalent? Write a killing test here:</h3>
            <form id="equivalenceForm" action="<%= request.getContextPath() + Paths.BATTLEGROUND_GAME %>" method="post">
                <input type="hidden" name="formType" value="resolveEquivalence">
                <input type="hidden" name="gameId" value="<%= game.getId() %>">
                <input type="hidden" id="equivMutantId" name="equivMutantId" value="<%= equivMutant.getId() %>">

                <jsp:include page="/jsp/game_components/test_editor.jsp"/>

                <button class="btn btn-danger" name="acceptEquivalent" type="submit"
                        onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');">Accept Equivalence</button>
                <button class="btn btn-primary btn-bold pull-right" name="rejectEquivalent" type="submit"
                        onclick="testProgressBar(); return true;">Submit Killing Test</button>

                <div>
                    Note: If the game finishes with this equivalence unsolved, you will lose points!
                </div>
            </form>
        </div>
    </div>

    <div class="col-lg-6" id="cut-div">
        <div class="game-component-header"><h3>Class Under Test</h3></div>
        <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
        <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
        <jsp:include page="/jsp/game_components/test_error_highlighting.jsp" />
    </div>

</div>

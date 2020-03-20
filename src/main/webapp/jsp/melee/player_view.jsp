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

<%@page import="org.codedefenders.game.GameClass"%>
<%@ page import="org.codedefenders.game.GameLevel"%>
<%@ page import="org.codedefenders.game.GameState"%>
<%@ page import="org.codedefenders.game.Mutant"%>
<%@ page import="org.codedefenders.game.Test"%>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame"%>
<%@ page import="org.codedefenders.model.User"%>
<%@ page import="org.codedefenders.util.Constants"%>
<%@ page import="org.codedefenders.util.Paths"%>
<%@ page import="java.util.stream.Collectors"%>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.database.UserDAO" %>

<%--
    @param MeleeGame game
        The game to be displayed.
--%>
<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean"
	scope="request" />
<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
			final GameClass cut = game.getCUT();

			boolean openEquivalenceDuel = request.getAttribute("openEquivalenceDuel") != null;

			// This is set by the GameManager but we could have it set by a different servlet common for all the games which require equivalence duels
			Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
			// This is set by the GameManager but we could have it set by a different servlet common for all the games which require equivalence duels
			User equivDefender = (User) request.getAttribute("equivDefender");

			final User user = login.getUser();
            // Trying to add this lookup inside the filter statement will lead to some weird, not working behaviour.
            final int userId = login.getUserId();
            final List<Test> playerTests = game.getTests()
                    .stream()
                    .filter(t -> UserDAO.getUserForPlayer(t.getPlayerId()).getId() == userId)
                    .collect(Collectors.toList());
%>

<jsp:useBean id="testPreviousSubmission"
	class="org.codedefenders.beans.game.PreviousSubmissionBean"
	scope="request" />
<jsp:useBean id="mutantPreviousSubmission"
	class="org.codedefenders.beans.game.PreviousSubmissionBean"
	scope="request" />
<jsp:useBean id="testForEquivalenceDuelPreviousSubmission"
	class="org.codedefenders.beans.game.PreviousSubmissionBean"
	scope="request" />

<%-- -------------------------------------------------------------------------------- --%>

<%-- Mutant editor in player mode is the same as class viewer in defender --%>
<jsp:useBean id="mutantEditor"
	class="org.codedefenders.beans.game.MutantEditorBean" scope="request" />
<%
    mutantEditor.setClassName(cut.getName());
			mutantEditor.setDependenciesForClass(game.getCUT());
			if (mutantPreviousSubmission.hasMutant()) {
				mutantEditor.setPreviousMutantCode(mutantPreviousSubmission.getMutantCode());
				mutantPreviousSubmission.clearMutant();
			} else {
				mutantEditor.setMutantCodeForClass(cut);
			}
%>


<jsp:useBean id="gameHighlighting"
	class="org.codedefenders.beans.game.GameHighlightingBean"
	scope="request" />
<%
    gameHighlighting.setGameData(game.getMutants(), playerTests, user);
			gameHighlighting.setFlaggingData(game.getMode(), game.getId());
			gameHighlighting.setEnableFlagging(true);
			// We should show game highlithing only inside the mutant editor
			gameHighlighting.setCodeDivSelector("#newmut-div");
%>

<jsp:useBean id="testErrorHighlighting"
	class="org.codedefenders.beans.game.ErrorHighlightingBean"
	scope="request" />
<%
    testErrorHighlighting.setCodeDivSelector("#utest-div");
			if (testPreviousSubmission.hasErrorLines()) {
				testErrorHighlighting.setErrorLines(testPreviousSubmission.getErrorLines());
				testPreviousSubmission.clearErrorLines();
			}
%>

<jsp:useBean id="mutantErrorHighlighting"
	class="org.codedefenders.beans.game.ErrorHighlightingBean"
	scope="request" />
<%
    mutantErrorHighlighting.setCodeDivSelector("#newmut-div");
			if (mutantPreviousSubmission.hasErrorLines()) {
				mutantErrorHighlighting.setErrorLines(mutantPreviousSubmission.getErrorLines());
				mutantPreviousSubmission.clearErrorLines();
			}
%>

<jsp:useBean id="mutantAccordion"
	class="org.codedefenders.beans.game.MutantAccordionBean"
	scope="request" />
<%
    mutantAccordion.setMutantAccordionData(cut, user, game.getAliveMutants(), game.getKilledMutants(),
					game.getMutantsMarkedEquivalent(), game.getMutantsMarkedEquivalentPending(), true);
			mutantAccordion.setFlaggingData(game.getMode(), game.getId());
			mutantAccordion.setEnableFlagging(true);
			mutantAccordion.setViewDiff(game.getLevel() == GameLevel.EASY);
%>

<jsp:useBean id="testAccordion"
	class="org.codedefenders.beans.game.TestAccordionBean" scope="request" />
<%
    testAccordion.setTestAccordionData(cut, playerTests, game.getMutants());
%>

<jsp:useBean id="mutantProgressBar"
	class="org.codedefenders.beans.game.MutantProgressBarBean"
	scope="request" />
<%
    mutantProgressBar.setGameId(game.getId());
%>

<jsp:useBean id="testProgressBar"
	class="org.codedefenders.beans.game.TestProgressBarBean"
	scope="request" />
<%
    testProgressBar.setGameId(game.getId());
%>


<jsp:useBean id="mutantExplanation"
	class="org.codedefenders.beans.game.MutantExplanationBean"
	scope="request" />
<%
    mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel());
%>

<jsp:useBean id="testEditor"
	class="org.codedefenders.beans.game.TestEditorBean" scope="request" />
<%
    testEditor.setEditableLinesForClass(cut);
			testEditor.setMockingEnabled(cut.isMockingEnabled());
			if (testPreviousSubmission.hasTest()) {
				testEditor.setPreviousTestCode(testPreviousSubmission.getTestCode());
				testPreviousSubmission.clearTest();
			} else {
				testEditor.setTestCodeForClass(cut);
			}
%>

<jsp:useBean id="testEditorForEquivalenceDuel"
	class="org.codedefenders.beans.game.TestEditorBean" scope="request" />
<%
    testEditorForEquivalenceDuel.setEditableLinesForClass(cut);
			testEditorForEquivalenceDuel.setMockingEnabled(cut.isMockingEnabled());
			if (testForEquivalenceDuelPreviousSubmission.hasTest()) {
				testEditorForEquivalenceDuel
						.setPreviousTestCode(testForEquivalenceDuelPreviousSubmission.getTestCode());
			} else {
				testEditorForEquivalenceDuel.setTestCodeForClass(cut);
			}
%>


<%-- All the views must be on the same row --%>

<div class="row">

	<%
	    if (openEquivalenceDuel) {
	%>
	<%-- -------------------------------------------------------------------------------- --%>
	<%-- Equivalence Duel view --%>
	<%-- -------------------------------------------------------------------------------- --%>
	<div class="col-md-6" id="equivmut-div">
		<h3>
			Mutant <%=equivMutant.getId()%>
			<!-- check for automatically triggered equivalence duels -->
			<%
			    if (equivDefender.getId() == Constants.DUMMY_CREATOR_USER_ID) {
			%>
				automatically claimed equivalent
			<%
			    } else {
			%>
				claimed equivalent by <%=equivDefender.getUsername()%>
			<%
		    	}
			%>
		</h3>
		<div
			style="border: 5px dashed #f00; border-radius: 10px; width: 100%; padding: 10px;">
			<p><%=String.join("\n", equivMutant.getHTMLReadout())%></p>
			<a class="btn btn-default" data-toggle="collapse"
				href="#diff-collapse">Show Diff</a>
			<p></p>
			<pre id="diff-collapse" class="readonly-pre collapse">
				<textarea id="diff" class="mutdiff readonly-textarea"
					title="mutdiff"><%=equivMutant.getHTMLEscapedPatchString()%></textarea>
			</pre>
			<script>
				$('#diff-collapse').on(
						'shown.bs.collapse',
						function() {
							var codeMirrorContainer = $(this).find(
									".CodeMirror")[0];
							if (codeMirrorContainer
									&& codeMirrorContainer.CodeMirror) {
								codeMirrorContainer.CodeMirror.refresh();
							} else {
								var showDiff = CodeMirror.fromTextArea(document
										.getElementById('diff'), {
									lineNumbers : false,
									mode : "text/x-diff",
									readOnly : true
								});
								showDiff.setSize("100%", 210);
							}
						});
			</script>

			<jsp:include page="/jsp/game_components/push_test_progress_bar.jsp" />
			<h3>Not equivalent? Write a killing test here:</h3>
			<form id="equivalenceForm"
				action="<%=request.getContextPath() + Paths.EQUIVALENCE_DUELS_GAME%>"
				method="post">
				<input type="hidden" name="formType" value="resolveEquivalence">
				<input type="hidden" name="gameId" value="<%=game.getId()%>">
				<input type="hidden" id="equivMutantId" name="equivMutantId"
					value="<%=equivMutant.getId()%>">

				<jsp:include page="/jsp/game_components/test_editor.jsp" />

				<button class="btn btn-danger btn-left" name="acceptEquivalent"
					type="submit"
					onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');">Accept
					Equivalence</button>
				<button class="btn btn-primary btn-game btn-right"
					name="rejectEquivalent" type="submit"
					onclick="testProgressBar(); return true;">Submit Killing
					Test</button>

				<div>Note: If the game finishes with this equivalence
					unsolved, you will lose points!</div>
			</form>
		</div>
	</div>

	<%
	    } else {
	%>

	<%-- -------------------------------------------------------------------------------- --%>
	<%-- Attacker view --%>
	<%-- -------------------------------------------------------------------------------- --%>
	<div class="col-md-6" id="newmut-div">
		<div class="row" style="display: contents">
			<h3 style="margin-bottom: 0; display: inline">Create a mutant
				here</h3>

			<jsp:include page="/jsp/game_components/push_mutant_progress_bar.jsp" />
			<!-- Attack button with intention dropDown set in attacker_intention_collector.jsp -->
			<button type="submit" class="btn btn-primary btn-game btn-right"
				id="submitMutant" form="atk"
				onClick="mutantProgressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';"
				style="float: right; margin-right: 5px"
				<%if (game.getState() != GameState.ACTIVE) {%> disabled <%}%>>
				Attack!</button>

			<!-- Reset button -->
			<form id="reset"
				action="<%=request.getContextPath() + Paths.MELEE_GAME%>"
				method="post" style="float: right; margin-right: 5px">
				<button class="btn btn-primary btn-warning btn-game btn-right"
					id="btnReset">Reset</button>
				<input type="hidden" name="formType" value="reset"> <input
					type="hidden" name="gameId" value="<%=game.getId()%>" />
			</form>
		</div>

		<form id="atk"
			action="<%=request.getContextPath() + Paths.MELEE_GAME%>"
			method="post">
			<input type="hidden" name="formType" value="createMutant"> <input
				type="hidden" name="gameId" value="<%=game.getId()%>" />

			<jsp:include page="/jsp/game_components/mutant_editor.jsp" />
			<jsp:include page="/jsp/game_components/game_highlighting.jsp" />
			<!-- THE FOLLOWING IS DUPLICATED ! -->
			<jsp:include
				page="/jsp/game_components/mutant_error_highlighting.jsp" />
		</form>
		<jsp:include page="/jsp/game_components/mutant_explanation.jsp" />
		<jsp:include
			page="/jsp/game_components/editor_help_config_toolbar.jsp" />
	</div>

	<%
	    }
	%>




	<%-- -------------------------------------------------------------------------------- --%>
	<%-- Defender view --%>
	<%-- -------------------------------------------------------------------------------- --%>

	<div class="col-md-6" id="utest-div">

		<jsp:include page="/jsp/game_components/push_test_progress_bar.jsp" />
		<h3>
			Write a new JUnit test here
			<button type="submit" class="btn btn-primary btn-game btn-right"
				id="submitTest" form="def"
				onClick="window.testProgressBar(); this.form.submit(); this.disabled = true; this.value = 'Defending...';"
				<%if (game.getState() != GameState.ACTIVE) {%> disabled <%}%>>
				Defend!</button>
		</h3>

		<form id="def"
			action="<%=request.getContextPath() + Paths.MELEE_GAME%>"
			method="post">
			<jsp:include page="/jsp/game_components/test_editor.jsp" />
			<input type="hidden" name="formType" value="createTest"> <input
				type="hidden" name="gameId" value="<%=game.getId()%>" />
		</form>
		<jsp:include
			page="/jsp/game_components/editor_help_config_toolbar.jsp" />
		<!-- THE FOLLOWING IS DUPLICATED ! -->
		<jsp:include page="/jsp/game_components/test_error_highlighting.jsp" />
	</div>
</div>

</div>
<%-- TODO move the whole div here after changing the header --%>

<div class="row" style="padding: 0px 15px;">
	<div class="col-md-6" id="mutants-div">
		<h3>Existing Mutants</h3>
		<jsp:include page="/jsp/game_components/mutant_accordion.jsp" />
	</div>

	<div class="col-md-6">
		<h3>JUnit tests</h3>
		<jsp:include page="/jsp/game_components/test_accordion.jsp" />
	</div>
</div>

<div>

<%@page import="org.codedefenders.game.Mutant"%>
<%@page import="org.codedefenders.game.multiplayer.MeleeGame"%>
<%@ page import="org.codedefenders.game.GameLevel"%>
<%@ page import="org.codedefenders.game.GameState"%>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame"%>
<%@ page import="org.codedefenders.game.GameClass"%>
<%@ page import="org.codedefenders.util.Paths"%>

<%--
    @param MeleeGame game
        The game to be displayed.
--%>

<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
			final GameClass cut = game.getCUT();

	boolean openEquivalenceDuel = request.getAttribute("openEquivalenceDuel") != null;

	// Should this be ALWAYS visible?
	boolean showTestAccordion = game.getLevel().equals(GameLevel.EASY)
				|| game.getState().equals(GameState.FINISHED);
%>

<%-- TODO SHould this be split into previous test and previous mutant submitted? ! --%>
<jsp:useBean id="previousSubmission"
	class="org.codedefenders.beans.game.PreviousSubmissionBean"
	scope="request" />

<%-- -------------------------------------------------------------------------------- --%>


<jsp:useBean id="mutantEditor"
	class="org.codedefenders.beans.game.MutantEditorBean" scope="request" />
<%
    mutantEditor.setClassName(cut.getName());
			mutantEditor.setDependenciesForClass(game.getCUT());
			if (previousSubmission.hasMutant()) {
				mutantEditor.setPreviousMutantCode(previousSubmission.getMutantCode());
				previousSubmission.clearMutant();
			} else {
				mutantEditor.setMutantCodeForClass(cut);
			}
%>


<jsp:useBean id="gameHighlighting"
	class="org.codedefenders.beans.game.GameHighlightingBean"
	scope="request" />
<%
    gameHighlighting.setGameData(game.getMutants(), game.getTests());
			gameHighlighting.setFlaggingData(game.getMode(), game.getId());
			gameHighlighting.setEnableFlagging(false);
			// TODO Which one to use?
			gameHighlighting.setCodeDivSelector("#newmut-div");
			gameHighlighting.setCodeDivSelector("#cut-div");
%>

<jsp:useBean id="errorHighlighting"
	class="org.codedefenders.beans.game.ErrorHighlightingBean"
	scope="request" />
<%
    // TODO Which one to use?
			errorHighlighting.setCodeDivSelector("#utest-div");
			errorHighlighting.setCodeDivSelector("#newmut-div");
			if (previousSubmission.hasErrorLines()) {
				errorHighlighting.setErrorLines(previousSubmission.getErrorLines());
				previousSubmission.clearErrorLines();
			}
%>

<jsp:useBean id="mutantAccordion"
	class="org.codedefenders.beans.game.MutantAccordionBean"
	scope="request" />
<%
    mutantAccordion.setMutantAccordionData(cut, game.getAliveMutants(), game.getKilledMutants(),
					game.getMutantsMarkedEquivalent(), game.getMutantsMarkedEquivalentPending());
			mutantAccordion.setFlaggingData(game.getMode(), game.getId());
			mutantAccordion.setEnableFlagging(false);
			// TODO Which one to user
			mutantAccordion.setViewDiff(true);
			mutantAccordion.setViewDiff(game.getLevel() == GameLevel.EASY);
%>

<jsp:useBean id="testAccordion"
	class="org.codedefenders.beans.game.TestAccordionBean" scope="request" />
<%
    // Which one to use?
			// testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants());
			if (showTestAccordion) {
				testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants());
			}
%>

<!-- TODO Are those both there ?! -->
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



<jsp:useBean id="classViewer"
	class="org.codedefenders.beans.game.ClassViewerBean" scope="request" />
<%
    classViewer.setClassCode(game.getCUT());
			classViewer.setDependenciesForClass(game.getCUT());
%>


<jsp:useBean id="testEditor"
	class="org.codedefenders.beans.game.TestEditorBean" scope="request" />
<%
    testEditor.setEditableLinesForClass(cut);
			testEditor.setMockingEnabled(cut.isMockingEnabled());
			if (previousSubmission.hasTest()) {
				testEditor.setPreviousTestCode(previousSubmission.getTestCode());
				previousSubmission.clearTest();
			} else {
				testEditor.setTestCodeForClass(cut);
			}
%>

<div class="row">

<%-- -------------------------------------------------------------------------------- --%>
<%-- Defender view --%>
<%-- -------------------------------------------------------------------------------- --%>

	<div class="col-md-6" id="utest-div">
		<%@include file="../game_components/test_progress_bar.jsp"%>
		<%--<%@include file="../game_components/push_test_progress_bar.jsp"%>--%>

		<%-- TODO Why progress bar here is handled differently than mutant submission ?! --%>
		<%-- TODO: change back to registerTestProgressBar() --%>
		<h3>
			Write a new JUnit test here
			<button type="submit" class="btn btn-primary btn-game btn-right"
				id="submitTest" form="def"
				onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
				<%if (game.getState() != GameState.ACTIVE) {%> disabled <%}%>>
				Defend!</button>
		</h3>

		<form id="def"
			action="<%=request.getContextPath() + Paths.MELEE_GAME%>"
			method="post">
			<%@include file="../game_components/test_editor.jsp"%>
			<input type="hidden" name="formType" value="createTest"> <input
				type="hidden" name="gameId" value="<%=game.getId()%>" />
		</form>
		<%@include file="../game_components/editor_help_config_toolbar.jsp"%>
		<%@include file="../game_components/error_highlighting_in_test.jsp"%>
	</div>
</div>

<%-- TODO move the whole div here after changing the header --%>

<div class="row" style="padding: 0px 15px;">
	<div class="col-md-6" id="mutants-div">
		<h3>Existing Mutants</h3>
		<%@include file="../game_components/mutants_list.jsp"%>
	</div>

	<div class="col-md-6" id="tests-div">
		<h3>JUnit tests</h3>
		<jsp:include page="/jsp/game_components/test_accordion.jsp" />
	</div>
</div>
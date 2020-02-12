<%@ page import="org.codedefenders.model.User"%>
<%@ page import="org.codedefenders.game.Mutant"%>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame"%>
<%@ page import="org.codedefenders.game.GameLevel"%>
<%@ page import="org.codedefenders.game.GameState"%>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame"%>
<%@ page import="org.codedefenders.game.GameClass"%>
<%@ page import="org.codedefenders.util.Paths"%>

<%--
    @param MeleeGame game
        The game to be displayed.
--%>
<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<%
    MeleeGame game = (MeleeGame) request.getAttribute("game");
    final GameClass cut = game.getCUT();

	boolean openEquivalenceDuel = request.getAttribute("openEquivalenceDuel") != null;

    final User user = login.getUser(); 

%>

<%-- TODO SHould this be split into previous test and previous mutant submitted? ! --%>
<jsp:useBean id="previousSubmission"
	class="org.codedefenders.beans.game.PreviousSubmissionBean"
	scope="request" />

<%-- -------------------------------------------------------------------------------- --%>


<%-- Mutant editor in player mode is the same as class viewer in defender --%>
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
			// We should show game highlithing only inside the mutant editor
			gameHighlighting.setCodeDivSelector("#newmut-div");
%>

<jsp:useBean id="testErrorHighlighting"
	class="org.codedefenders.beans.game.ErrorHighlightingBean"
	scope="request" />
<%
			testErrorHighlighting.setCodeDivSelector("#utest-div");
			if (previousSubmission.hasErrorLines()) {
			    testErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
				previousSubmission.clearErrorLines();
			}
%>

<jsp:useBean id="mutantErrorHighlighting"
	class="org.codedefenders.beans.game.ErrorHighlightingBean"
	scope="request" />
<%
			mutantErrorHighlighting.setCodeDivSelector("#newmut-div");
			if (previousSubmission.hasErrorLines()) {
			    mutantErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
				previousSubmission.clearErrorLines();
			}
%>

<jsp:useBean id="mutantAccordion"
	class="org.codedefenders.beans.game.MutantAccordionBean"
	scope="request" />
<%
    mutantAccordion.setMutantAccordionData(cut, user, game.getAliveMutants(), game.getKilledMutants(),
					game.getMutantsMarkedEquivalent(), game.getMutantsMarkedEquivalentPending());
			mutantAccordion.setFlaggingData(game.getMode(), game.getId());
			mutantAccordion.setEnableFlagging(true);
			mutantAccordion.setViewDiff(game.getLevel() == GameLevel.EASY);
%>

<jsp:useBean id="testAccordion"
	class="org.codedefenders.beans.game.TestAccordionBean" scope="request" />
<%
	testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants());
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
			if (previousSubmission.hasTest()) {
				testEditor.setPreviousTestCode(previousSubmission.getTestCode());
				previousSubmission.clearTest();
			} else {
				testEditor.setTestCodeForClass(cut);
			}
%>


<%-- All the views must be on the same row --%>

<div class="row">
	<%-- -------------------------------------------------------------------------------- --%>
	<%-- Attacker view --%>
	<%-- -------------------------------------------------------------------------------- --%>
    <div class="col-md-6" id="newmut-div">
        <div class="row" style="display: contents">
            <h3 style="margin-bottom: 0; display: inline">Create a mutant here</h3>

            <jsp:include page="/jsp/game_components/push_mutant_progress_bar.jsp"/>
            <!-- Attack button with intention dropDown set in attacker_intention_collector.jsp -->
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk"
                onClick="mutantProgressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';"
                style="float: right; margin-right: 5px"
                <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                Attack!
            </button>

            <!-- Reset button -->
            <form id="reset" action="<%=request.getContextPath() + Paths.MELEE_GAME %>" method="post" style="float: right; margin-right: 5px">
                <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset">
                    Reset
                </button>
                <input type="hidden" name="formType" value="reset">
                <input type="hidden" name="gameId" value="<%= game.getId() %>"/>
            </form>
        </div>

        <form id="atk" action="<%=request.getContextPath() + Paths.MELEE_GAME %>" method="post">
            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%= game.getId() %>"/>

            <jsp:include page="/jsp/game_components/mutant_editor.jsp"/>
            <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
            <!-- THE FOLLOWING IS DUPLICATED ! -->
            <jsp:include page="/jsp/game_components/mutant_error_highlighting.jsp"/>
        </form>
        <jsp:include page="/jsp/game_components/mutant_explanation.jsp"/>
        <jsp:include page="/jsp/game_components/editor_help_config_toolbar.jsp"/>
    </div>


	<%-- -------------------------------------------------------------------------------- --%>
	<%-- Defender view --%>
	<%-- -------------------------------------------------------------------------------- --%>

    <div class="col-md-6" id="utest-div">

        <jsp:include page="/jsp/game_components/push_test_progress_bar.jsp"/>
        <h3>Write a new JUnit test here
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def"
                onClick="window.testProgressBar(); this.form.submit(); this.disabled = true; this.value = 'Defending...';"
                <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                Defend!
            </button>
        </h3>

            <form id="def" action="<%=request.getContextPath() + Paths.MELEE_GAME%>" method="post">
            <jsp:include page="/jsp/game_components/test_editor.jsp"/>
            <input type="hidden" name="formType" value="createTest">
            <input type="hidden" name="gameId" value="<%= game.getId() %>" />
        </form>
        <jsp:include page="/jsp/game_components/editor_help_config_toolbar.jsp"/>
        <!-- THE FOLLOWING IS DUPLICATED ! -->
        <jsp:include page="/jsp/game_components/test_error_highlighting.jsp"/>
    </div>
</div>

</div> <%-- TODO move the whole div here after changing the header --%>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="mutants-div">
        <h3>Existing Mutants</h3>
        <jsp:include page="/jsp/game_components/mutant_accordion.jsp"/>
    </div>

    <div class="col-md-6">
        <h3>JUnit tests </h3>
        <jsp:include page="/jsp/game_components/test_accordion.jsp"/>
    </div>
</div>

<div>

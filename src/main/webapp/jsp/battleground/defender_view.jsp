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

<%@ page import="org.codedefenders.model.User"%>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%--
    @param MutliplayerGame game
        The game to be displayed.
--%>
<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
	MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    final GameClass cut = game.getCUT();

    final User user = login.getUser();
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
    gameHighlighting.setEnableFlagging(true);
    gameHighlighting.setCodeDivSelector("#cut-div");
%>


<jsp:useBean id="testErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>
<%
    testErrorHighlighting.setCodeDivSelector("#utest-div");
    if (previousSubmission.hasErrorLines()) {
        testErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>

<%--
<jsp:useBean id="mutantAccordion" class="org.codedefenders.beans.game.MutantAccordionBean" scope="request"/>
<%
    mutantAccordion.setMutantAccordionData(cut, user, game.getMutants());
    mutantAccordion.setFlaggingData(game.getMode(), game.getId());
    mutantAccordion.setEnableFlagging(true);
    mutantAccordion.setViewDiff(game.getLevel() == GameLevel.EASY);
%>
--%>

<%--
<jsp:useBean id="testAccordion" class="org.codedefenders.beans.game.TestAccordionBean" scope="request"/>
<% testAccordion.setTestAccordionData(cut, game.getTests(), game.getMutants()); %>
--%>

<jsp:useBean id="testProgressBar" class="org.codedefenders.beans.game.TestProgressBarBean" scope="request"/>
<% testProgressBar.setGameId(game.getId()); %>


<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>


<% previousSubmission.clear(); %>


<%-- -------------------------------------------------------------------------------- --%>


<!--<div class="row" style="padding: 0px 15px;"> TODO change to this after changing the header -->
<div class="row">
    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <jsp:include page="../game_components/class_viewer.jsp"/>
        <jsp:include page="../game_components/game_highlighting.jsp"/>
        <jsp:include page="../game_components/mutant_explanation.jsp"/>
    </div>

    <div class="col-md-6" id="utest-div">

        <jsp:include page="/jsp/game_components/push_test_progress_bar.jsp"/>
        <h3>Write a new JUnit test here
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def"
                onClick="window.testProgressBar(); this.form.submit(); this.disabled = true; this.value = 'Defending...';"
                <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                Defend!
            </button>
        </h3>

            <form id="def" action="<%=request.getContextPath() + Paths.BATTLEGROUND_GAME%>" method="post">
            <jsp:include page="/jsp/game_components/test_editor.jsp"/>
            <input type="hidden" name="formType" value="createTest">
            <input type="hidden" name="gameId" value="<%= game.getId() %>" />
        </form>
        <jsp:include page="/jsp/game_components/editor_help_config_toolbar.jsp"/>
        <jsp:include page="/jsp/game_components/test_error_highlighting.jsp"/>
    </div>
</div>

</div> <%-- TODO move the whole div here after changing the header --%>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="mutants-div">
        <h3>Existing Mutants</h3>
        <t:mutant_accordion/>
    </div>

    <div class="col-md-6">
        <h3>JUnit tests </h3>
        <t:test_accordion/>
    </div>
</div>

<div>

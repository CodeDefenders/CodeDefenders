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

<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%--
    @param MutliplayerGame game
        The game to be displayed.
--%>

<%--@elvariable id="gameProducer" type="org.codedefenders.servlets.games.GameProducer"--%>
<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<%
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    final GameClass cut = game.getCUT();

    boolean showTestAccordion = game.getLevel().equals(GameLevel.EASY) || game.getState().equals(GameState.FINISHED);

%>

<jsp:useBean id="previousSubmission" class="org.codedefenders.beans.game.PreviousSubmissionBean" scope="request"/>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:useBean id="mutantEditor" class="org.codedefenders.beans.game.MutantEditorBean" scope="request"/>
<%
    mutantEditor.setClassName(cut.getName());
    mutantEditor.setDependenciesForClass(game.getCUT());
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
    gameHighlighting.setCodeDivSelector("#newmut-div");
%>


<jsp:useBean id="mutantErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>
<%
    mutantErrorHighlighting.setCodeDivSelector("#newmut-div");
    if (previousSubmission.hasErrorLines()) {
        mutantErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>

<%--
<jsp:useBean id="mutantAccordion" class="org.codedefenders.beans.game.MutantAccordionBean" scope="request"/>
<%
    mutantAccordion.setMutantAccordionData(cut, user, game.getMutants());
    mutantAccordion.setFlaggingData(game.getMode(), game.getId());
    mutantAccordion.setEnableFlagging(false);
    mutantAccordion.setViewDiff(true);
%>
--%>

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


<div class="row">
    <div class="col-xl-6 col-12">
        <t:mutant_accordion/>

        <% if (showTestAccordion) { %>
            <div id="tests-div">
                <div class="game-component-header"><h3>JUnit Tests</h3></div>
                <t:test_accordion/>
            </div>
        <% } %>
    </div>

    <div class="col-xl-6 col-12" id="newmut-div">
        <jsp:include page="/jsp/game_components/mutant_progress_bar.jsp"/>

        <div class="game-component-header">
            <h3>Create a mutant here</h3>
            <div>

                <form id="reset" action="<%=request.getContextPath() + Paths.BATTLEGROUND_GAME %>" method="post">
                    <button class="btn btn-warning" id="btnReset">
                        Reset
                    </button>
                    <input type="hidden" name="formType" value="reset">
                    <input type="hidden" name="gameId" value="<%= game.getId() %>"/>
                </form>

                <t:submit_mutant_button gameActive="${gameProducer.game.state == GameState.ACTIVE}"
                                        intentionCollectionEnabled="${gameProducer.game.capturePlayersIntention}"/>
            </div>
        </div>

        <form id="atk" action="<%=request.getContextPath() + Paths.BATTLEGROUND_GAME %>" method="post">
            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%= game.getId() %>"/>
            <input type="hidden" id="attacker_intention" name="attacker_intention" value="">

            <jsp:include page="/jsp/game_components/mutant_editor.jsp"/>
            <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
            <jsp:include page="/jsp/game_components/mutant_error_highlighting.jsp"/>
        </form>
    </div>
</div>

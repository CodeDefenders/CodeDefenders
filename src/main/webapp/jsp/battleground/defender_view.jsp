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
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.GameMode" %>

<%-- TODO: list parameters --%>

<%
	MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    final GameClass cut = game.getCUT();
%>

<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<% classViewer.setClassCode(game.getCUT()); %>
<% classViewer.setDependenciesForClass(game.getCUT()); %>

<jsp:useBean id="testEditor" class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>
<% testEditor.setEditableLinesForClass(cut); %>
<% testEditor.setMockingEnabled(cut.isMockingEnabled()); %>

<%-- Set request attributes for the components. --%>
<%
    /* test_editor */
    String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    List<Integer> errorLines = (List<Integer>) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES);

    if (previousTestCode != null) {
        testEditor.setPreviousTestCode(previousTestCode);

        /* error_highlighting */
        request.setAttribute("codeDivSelectorForError", "#utest-div");
        request.setAttribute("errorLines", errorLines);
    } else {
        testEditor.setTestCodeForClass(cut);
    }

    /* test_accordion */
    request.setAttribute("cut", cut);
    request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
    request.setAttribute("mutantsMarkedEquivalent", game.getMutantsMarkedEquivalentPending());
    request.setAttribute("markEquivalent", true);
    request.setAttribute("viewDiff", game.getLevel() == GameLevel.EASY);
    request.setAttribute("gameType", GameMode.PARTY);
    request.setAttribute("gameId", game.getId());

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", true);
    // request.setAttribute("gameType", GameMode.PARTY);
    // request.setAttribute("gameId", game.getId());

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

    /* test_progressbar */
    // request.setAttribute("gameId", game.getId());
%>

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
        <jsp:include page="/jsp/game_components/error_highlighting.jsp"/>
    </div>
</div>

</div> <%-- TODO move the whole div here after changing the header --%>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="mutants-div">
        <h3>Existing Mutants</h3>
        <jsp:include page="/jsp/game_components/mutants_list.jsp"/>
    </div>

    <div class="col-md-6">
        <h3>JUnit tests </h3>
        <jsp:include page="/jsp/game_components/test_accordion.jsp"/>
    </div>
</div>

<div>

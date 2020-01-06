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
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.GameMode" %>
<%@ page import="java.util.List" %>

<%-- TODO: list parameters --%>

<%-- Set request attributes for the components. --%>
<%
    String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    List<Integer> errorLines = (List<Integer>) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES);
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
    final GameClass cut = game.getCUT();
%>

<jsp:useBean id="mutantEditor" class="org.codedefenders.beans.game.MutantEditorBean" scope="request"/>
<% mutantEditor.setDependenciesForClass(game.getCUT()); %>

<%
    /* mutant_editor */
    if (previousMutantCode != null) {
        mutantEditor.setPreviousMutantCode(cut, previousMutantCode);

        /* error_highlighting */
        request.setAttribute("codeDivSelectorForError", "#newmut-div");
        request.setAttribute("errorLines", errorLines);
    } else {
        mutantEditor.setMutantCodeForClass(cut);
    }

    request.setAttribute("mutantName", cut.getBaseName());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

    /* test_accordion */
    request.setAttribute("cut", cut);
    request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
    request.setAttribute("mutantsMarkedEquivalent", game.getMutantsMarkedEquivalentPending());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", GameMode.PARTY);
    request.setAttribute("gameId", game.getId());

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#newmut-div");
    // request.setAttribute("tests", game.getTests());
    // request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    // request.setAttribute("gameType", GameMode.PARTY);
    // request.setAttribute("gameId", game.getId());

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

    /* mutant_progressbar */
    // request.setAttribute("gameId", game.getId());
%>

<!--<div class="row" style="padding: 0px 15px;"> TODO change to this after changing the header -->
<div class="row">
    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Existing Mutants</h3>
            <jsp:include page="/jsp/game_components/mutants_list.jsp"/>
        </div>

        <% if (game.getLevel().equals(GameLevel.EASY) || game.getState().equals(GameState.FINISHED)) { %>
            <div id="tests-div">
                <h3>JUnit tests </h3>
                <jsp:include page="/jsp/game_components/test_accordion.jsp"/>
            </div>
        <% } %>
    </div>

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
            <form id="reset" action="<%=request.getContextPath() + Paths.BATTLEGROUND_GAME %>" method="post" style="float: right; margin-right: 5px">
                <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset">
                    Reset
                </button>
                <input type="hidden" name="formType" value="reset">
                <input type="hidden" name="gameId" value="<%= game.getId() %>"/>
            </form>
        </div>

        <form id="atk" action="<%=request.getContextPath() + Paths.BATTLEGROUND_GAME %>" method="post">
            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%= game.getId() %>"/>

            <jsp:include page="/jsp/game_components/mutant_editor.jsp"/>
            <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
            <jsp:include page="/jsp/game_components/error_highlighting.jsp"/>
        </form>
        <jsp:include page="/jsp/game_components/mutant_explanation.jsp"/>
        <jsp:include page="/jsp/game_components/editor_help_config_toolbar.jsp"/>
    </div>
</div>

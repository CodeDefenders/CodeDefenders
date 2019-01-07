<%--
    Copyright (C) 2016-2018 Code Defenders contributors

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
<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="static org.codedefenders.util.Constants.*" %>

<%--
    Puzzle game view for a defender. Retrieves the given puzzle game
    from the request and calls the required game components.

    @param PuzzleGame Constants#REQUEST_ATTRIBUTE_PUZZLE_GAME
        The puzzle game to be displayed.
--%>

<% String pageTitle = "Defending Class"; %>
<%@ include file="/jsp/header_main.jsp" %>

</div></div></div></div></div>

<% { %>

<%-- Set request attributes for the components. --%>
<%
    PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);

    final GameClass cut = game.getCUT();

    /* class_viewer */
    request.setAttribute("className", cut.getBaseName());
    request.setAttribute("classCode", cut.getAsHTMLEscapedString());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

    /* test_editor */
    String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    if (previousTestCode != null) {
        request.setAttribute("testCode", previousTestCode);
    } else {
        request.setAttribute("testCode", cut.getHTMLEscapedTestTemplate());
    }
    request.setAttribute("mockingEnabled", false);
    request.setAttribute("startEditLine", cut.getTestTemplateFirstEditLine());

    /* tests_carousel */
    request.setAttribute("tests", game.getTests());
//    request.setAttribute("mutants", game.getMutants());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", new LinkedList<Mutant>());
    request.setAttribute("mutantsMarkedEquivalent", game.getMutantsMarkedEquivalentPending());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("markUncoveredEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", GameMode.PUZZLE);
    request.setAttribute("gameId", game.getId());

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", true);
    // request.setAttribute("markUncoveredEquivalent", game.isMarkUncovered());
    // request.setAttribute("gameType", GameMode.PUZZLE);
//    request.setAttribute("gameId", game.getId());

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", CodeValidatorLevel.MODERATE);

    /* test_progressbar */
//    request.setAttribute("gameId", game.getId());
%>
<div class="game-container">
    <div class="row" style="padding: 0px 15px;">
        <div class="col-md-6" id="cut-div">
            <h3>Class Under Test</h3>
            <%@include file="../game_components/class_viewer.jsp" %>
            <%@include file="../game_components/game_highlighting.jsp"%>
            <%@include file="../game_components/mutant_explanation.jsp" %>
        </div>

        <div class="col-md-6" id="ut-div">
            <%@include file="../game_components/test_progress_bar.jsp" %>
            <h3>Write a new JUnit test here</h3>

            <form id="def"
                  action="<%=request.getContextPath() + Paths.PUZZLE_GAME%>"
                  method="post">
                <button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def"
                        onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
                        <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                    Defend!
                </button>

                <input type="hidden" name="formType" value="createTest">
                <input type="hidden" name="gameId" value="<%= game.getId() %>">

                <%@include file="../game_components/test_editor.jsp" %>
            </form>
        </div>
    </div>

    <div class="row" style="padding: 0px 15px;">
        <div class="col-md-6" id="mutants-div">
            <h3>Existing Mutants</h3>
            <%@include file="../game_components/mutants_list.jsp" %>
        </div>

        <div class="col-md-6">
            <h3>JUnit tests</h3>
            <%@include file="../game_components/tests_carousel.jsp" %>
        </div>
    </div>
</div>

<%@include file="../footer_game.jsp" %>
<% } %>
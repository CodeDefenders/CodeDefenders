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

<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="static org.codedefenders.util.Constants.*" %>
<%@ page import="org.codedefenders.game.puzzle.Puzzle" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%--
    Puzzle game view for a defender. Retrieves the given puzzle game
    from the request and calls the required game components.

    @param PuzzleGame Constants#REQUEST_ATTRIBUTE_PUZZLE_GAME
        The puzzle game to be displayed.
--%>

<%
    PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);

    final GameClass cut = game.getCUT();
    final Puzzle puzzle = game.getPuzzle();

    final String title = puzzle.getTitle();
    final String description = puzzle.getDescription();
%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Puzzle: " + puzzle.getChapter().getTitle() + " - " + puzzle.getTitle()); %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>
<jsp:useBean id="previousSubmission" class="org.codedefenders.beans.game.PreviousSubmissionBean" scope="request"/>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>
<%
    classViewer.setClassCode(game.getCUT());
    classViewer.setDependenciesForClass(game.getCUT());
%>


<jsp:useBean id="testEditor" class="org.codedefenders.beans.game.TestEditorBean" scope="request"/>
<%
    testEditor.setEditableLinesForPuzzle(puzzle);
    testEditor.setMockingEnabled(false);
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

<%--
<jsp:useBean id="mutantAccordion" class="org.codedefenders.beans.game.MutantAccordionBean" scope="request"/>
<%
    mutantAccordion.setMutantAccordionData(cut, login.getUser(), game.getMutants());
    mutantAccordion.setFlaggingData(game.getMode(), game.getId());
    mutantAccordion.setEnableFlagging(false);
    mutantAccordion.setViewDiff(true);
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


<%-- -------------------------------------------------------------------------------- --%>


<jsp:include page="/jsp/header.jsp"/>

<link href="${pageContext.request.contextPath}/css/specific/game.css" rel="stylesheet">

<jsp:include page="/jsp/push_socket.jsp"/>

<div id="game-container" class="container-fluid">

    <h4><b><%=title%></b></h4>
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3">
        <h4 class="m-0"><%=description%></h4>
        <jsp:include page="/jsp/game_components/keymap_config.jsp"/>
    </div>
    <hr>

    <div class="row">
        <div class="col-xl-6 col-12" id="cut-div">
            <div class="game-component-header"><h3>Class Under Test</h3></div>
            <%-- <t:defender_intention_collection_note/>
                 for defender intetion collection, not needed here --%>
            <jsp:include page="/jsp/game_components/class_viewer.jsp"/>
            <jsp:include page="/jsp/game_components/game_highlighting.jsp"/>
        </div>

        <div class="col-xl-6 col-12" id="ut-div">
            <jsp:include page="/jsp/game_components/test_progress_bar.jsp"/>

            <div class="game-component-header">
                <h3>Write a new JUnit test here</h3>
                <div>
                    <button type="submit" class="btn btn-defender btn-highlight" id="submitTest" form="def"
                            <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                        Defend
                    </button>

                    <script type="module">
                        import {objects} from './js/codedefenders_main.mjs';
                        const testProgressBar = await objects.await('testProgressBar');


                        document.getElementById('submitTest').addEventListener('click', function (event) {
                            this.form.submit();
                            this.disabled = true;
                            testProgressBar.activate();
                        });
                    </script>
                </div>
            </div>

            <form id="def"
                  action="<%=request.getContextPath() + Paths.PUZZLE_GAME%>"
                  method="post">
                <input type="hidden" name="formType" value="createTest">
                <input type="hidden" name="gameId" value="<%= game.getId() %>">
                <%-- <input type="hidden" id="selected_lines" name="selected_lines" value="">
                     for defender intention collection, not needed here --%>

                <jsp:include page="/jsp/game_components/test_editor.jsp"/>
            </form>
        </div>
    </div>

    <div class="row">
        <div class="col-xl-6 col-12">
            <t:mutant_accordion/>
        </div>

        <div class="col-xl-6 col-12">
            <div class="game-component-header"><h3>JUnit Tests</h3></div>
            <t:test_accordion/>
        </div>
    </div>

<%@ include file="/jsp/footer_game.jsp"%>


<% previousSubmission.clear(); %>

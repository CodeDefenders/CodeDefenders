<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/page" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

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

    String title = "Puzzle: " + puzzle.getChapter().getTitle() + " - " + puzzle.getTitle();

    pageContext.setAttribute("game", game);
    pageContext.setAttribute("puzzle", puzzle);
    pageContext.setAttribute("title", title);
%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Puzzle: " + puzzle.getChapter().getTitle() + " - " + puzzle.getTitle()); %>

<jsp:useBean id="previousSubmission" class="org.codedefenders.beans.game.PreviousSubmissionBean" scope="request"/>


<%-- -------------------------------------------------------------------------------- --%>


<jsp:useBean id="gameHighlighting" class="org.codedefenders.beans.game.GameHighlightingBean" scope="request"/>
<%
    gameHighlighting.setGameData(game.getMutants(), game.getTests());
    gameHighlighting.setFlaggingData(game.getMode(), game.getId());
    gameHighlighting.setEnableFlagging(false);
%>


<jsp:useBean id="testErrorHighlighting" class="org.codedefenders.beans.game.ErrorHighlightingBean" scope="request"/>
<%
    if (previousSubmission.hasErrorLines()) {
        testErrorHighlighting.setErrorLines(previousSubmission.getErrorLines());
    }
%>


<jsp:useBean id="testProgressBar" class="org.codedefenders.beans.game.TestProgressBarBean" scope="request"/>
<% testProgressBar.setGameId(game.getId()); %>


<jsp:useBean id="mutantExplanation" class="org.codedefenders.beans.game.MutantExplanationBean" scope="request"/>
<% mutantExplanation.setCodeValidatorLevel(game.getMutantValidatorLevel()); %>


<%-- -------------------------------------------------------------------------------- --%>


<p:main_page title="${title}">
    <jsp:attribute name="additionalImports">
        <link href="${url.forPath("/css/specific/game.css")}" rel="stylesheet">
    </jsp:attribute>

    <jsp:body>
        <t:game_js_init/>

        <div id="game-container" class="container-fluid">

            <h4><b>${title}</b></h4>
            <div class="d-flex flex-wrap justify-content-between align-items-end gap-3">
                <h4 class="m-0">${puzzle.description}</h4>
                <div class="align-items-center d-flex gap-4">
                    <t:round_counter game="${game}"/>
                    <jsp:include page="/jsp/game_components/keymap_config.jsp"/>
                </div>
            </div>

            <hr>

            <c:if test="${game.state == GameState.SOLVED}">
                <div class="alert alert-success" role="alert">
                    <p class="m-0">
                        <strong class="alert-heading">Congratulations!</strong>
                        Your test solved the puzzle. ${requestScope.nextPuzzleMessage}
                    </p>
                </div>
            </c:if>

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
                        <c:choose>
                            <c:when test="${game.state == GameState.SOLVED}">
                                <h3>Solving JUnit Test</h3>
                            </c:when>
                            <c:otherwise>
                                <h3>Write a new JUnit test here</h3>
                            </c:otherwise>
                        </c:choose>
                        <div>
                            <button type="submit" class="btn btn-defender btn-highlight" id="submitTest" form="def"
                                ${game.state == GameState.SOLVED ? 'disabled' : ''}>
                                Defend
                            </button>
                            <c:if test="${game.state == GameState.ACTIVE}">
                                <script type="module">
                                    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';

                                    const testProgressBar = await objects.await('testProgressBar');

                                    document.getElementById('submitTest').addEventListener('click', function (event) {
                                        this.form.submit();
                                        this.disabled = true;
                                        testProgressBar.activate();
                                    });
                                </script>
                            </c:if>
                        </div>
                    </div>

                    <form id="def"
                          action="${url.forPath(Paths.PUZZLE_GAME)}"
                          method="post">
                        <input type="hidden" name="formType" value="createTest">
                        <input type="hidden" name="gameId" value="${game.id}">
                            <%-- <input type="hidden" id="selected_lines" name="selected_lines" value="">
                                 for defender intention collection, not needed here --%>

                        <jsp:include page="/jsp/game_components/test_editor.jsp"/>
                    </form>

                    <jsp:include page="/jsp/game_components/test_error_highlighting.jsp"/>
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
        </div>
    </jsp:body>
</p:main_page>


<% previousSubmission.clear(); %>

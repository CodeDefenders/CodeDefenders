<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="static org.codedefenders.util.Constants.*" %>
<%@ page import="org.codedefenders.game.*" %>

<% String pageTitle = "Defending Class"; %>
<%@ include file="/jsp/header_main.jsp" %>

</div></div></div></div></div>

<% { %>

<%-- Set request attributes for the components. --%>
<% PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);

    /* class_viewer */
    request.setAttribute("classCode", game.getCUT().getAsHTMLEscapedString());

    /* test_editor */
    String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    if (previousTestCode != null) {
        request.setAttribute("testCode", previousTestCode);
    } else {
        request.setAttribute("testCode", game.getCUT().getTestTemplate());
    }

    /* tests_carousel */
    request.setAttribute("tests", game.getTests());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", new LinkedList<Mutant>());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", "PUZZLE");

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", true);

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", CodeValidatorLevel.MODERATE);

    /* test_progressbar */
    request.setAttribute("gameId", game.getId());
%>
<div class="game-container">
    <div class="row" style="padding: 0px 15px;">
        <div class="col-md-6" id="cut-div">
            <h3>Class Under Test</h3>
            <%@include file="../game_components/class_viewer.jsp" %>
            <%@include file="../game_components/game_highlighting.jsp" %>
            <%@include file="../game_components/mutant_explanation.jsp" %>
        </div>

        <div class="col-md-6" id="ut-div">
            <%@include file="../game_components/test_progress_bar.jsp" %>
            <h3>Write a new JUnit test here
                <button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def"
                        onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
                        <% if (game.getState() != GameState.CREATED) { %> disabled <% } %>>
                    Defend!
                </button>
            </h3>
            <form id="def"
                  action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() + "?gameId=" + game.getPuzzleId()%>"
                  method="post">
                <%@include file="../game_components/test_editor.jsp" %>
                <input type="hidden" name="formType" value="createTest">
                <input type="hidden" name="gameID" value="<%= game.getId() %>"/>
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
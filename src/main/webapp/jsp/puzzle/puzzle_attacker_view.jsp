<%@ page import="org.codedefenders.game.puzzle.PuzzleGame" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME" %>
<%@ page import="static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT" %>

<% String pageTitle = "Attacking Class"; %>
<%@ include file="/jsp/header_main.jsp"%>

</div></div></div></div></div>
<div class="game-container">

<% { %>

<%-- Set request attributes for the components. --%>
<%  PuzzleGame game = (PuzzleGame) request.getAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME);

    /* mutant_editor */
    String previousMutantCode = (String) request.getSession().getAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    request.getSession().removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    if (previousMutantCode != null) {
        request.setAttribute("mutantCode", previousMutantCode);
    } else {
        request.setAttribute("mutantCode", game.getCUT().getAsHTMLEscapedString());
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
    request.setAttribute("showEquivalenceButton", false);

    /* finished_modal TODO */

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", CodeValidatorLevel.MODERATE);

    /* mutant_progressbar */
    request.setAttribute("gameId", game.getId());
%>


<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Mutants</h3>
            <%@include file="../game_components/mutants_list.jsp"%>
        </div>

        <% if (game.getLevel() == GameLevel.EASY) { %>
        <div id="tests-div">
            <h3>JUnit tests</h3>
            <%@include file="../game_components/tests_carousel.jsp"%>
        </div>
        <% } %>
    </div>

    <div class="col-md-6" id="cut-div">
        <%@include file="../game_components/mutant_progress_bar.jsp"%>
        <h3 style="margin-bottom: 0;">Create a mutant here</h3>

        <form id="reset" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() + "?gameId=" + game.getId()%>" method="post">
            <input type="hidden" name="formType" value="reset">
            <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset" style="margin-top: -40px; margin-right: 80px">
                Reset
            </button>
        </form>

        <form id="atk" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase()%>" method="post">
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';" style="margin-top: -50px"
                <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                Attack!
            </button>

            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%= game.getId() %>">

            <%@include file="../game_components/mutant_editor.jsp"%>
            <%@include file="../game_components/game_highlighting.jsp" %>
        </form>

        <%@include file="../game_components/mutant_explanation.jsp"%>
    </div>
</div>

<%@include file="../footer_game.jsp"%>

<% } %>

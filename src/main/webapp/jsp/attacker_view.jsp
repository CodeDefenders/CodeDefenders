<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.game.*" %>

<% String pageTitle = "Attacking Class"; %>
<%@ include file="/jsp/header_game.jsp" %>

<% { %>

<%-- TODO Set request attributes in the Servlet and redirect via RequestDispatcher? --%>

<%-- Set request attributes for the components. --%>
<%
    /* mutant_editor */
    String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    if (previousMutantCode != null) {
        request.setAttribute("mutantCode", previousMutantCode);
    } else {
        request.setAttribute("mutantCode", game.getCUT().getAsString());
    }

    /* tests_carousel */
    request.setAttribute("tests", game.getTests());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("markUncoveredEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", "DUEL");

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", true);

    /* finished_modal */
    int attackerScore = game.getAttackerScore();
    int defenderScore = game.getDefenderScore();
    request.setAttribute("win", defenderScore > attackerScore);
    request.setAttribute("loss", attackerScore > defenderScore);

    /* mutant_progressbar */
    request.setAttribute("gameId", game.getId());
%>

<% if (game.getState() == GameState.FINISHED) { %>
    <%@include file="game_components/finished_modal.jsp"%>
<% } %>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Mutants</h3>
            <%@include file="game_components/mutants_list.jsp"%>
        </div>

        <% if (game.getLevel().equals(GameLevel.EASY) || game.getState().equals(GameState.FINISHED)) { %>
            <div id="tests-div">
                <h3>JUnit tests </h3>
                <%@include file="game_components/tests_carousel.jsp"%>
            </div>
        <% } %>
    </div>

    <div class="col-md-6" id="cut-div">
        <%@include file="game_components/mutant_progress_bar.jsp"%>
        <h3 style="margin-bottom: 0;">Create a mutant here</h3>

        <form id="reset" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
            <input type="hidden" name="formType" value="reset">
            <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset" style="margin-top: -40px; margin-right: 80px">
                Reset
            </button>
        </form>

        <form id="atk" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';" style="margin-top: -50px"
                    <% if (game.getState() != GameState.ACTIVE || game.getActiveRole() != Role.ATTACKER) { %> disabled <% } %>>
                Attack!
            </button>

            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameID" value="<%= game.getId() %>"/>

            <%@include file="game_components/mutant_editor.jsp"%>
            <%@include file="game_components/game_highlighting.jsp"%>
        </form>

        <%@include file="game_components/mutant_explanation.jsp"%>
    </div>
</div>

<script>
    <% if (game.getActiveRole().equals(Role.DEFENDER)) {%>
    function checkForUpdate() {
        $.post('/play', {
            formType: "whoseTurn",
            gameID: <%= game.getId() %>
        }, function (data) {
            if (data === "attacker") {
                window.location.reload();
            }
        }, 'text');
    }
    setInterval("checkForUpdate()", 10000);
    <% } %>
</script>

<%@ include file="/jsp/footer_game.jsp" %>

<% } %>
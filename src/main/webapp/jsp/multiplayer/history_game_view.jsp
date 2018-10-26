<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>

<% { %>

<%
    String pageTitle="Game History";

    /* Get their user id from the session. */
    String gameIdString = request.getParameter("id");
    int gameId;

    if (gameIdString != null) {
        try {
            gameId = Integer.parseInt(request.getParameter("id"));
            session.setAttribute("mpGameId", gameId);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath()+"/games/history");
            return;
        }
    } else {
        response.sendRedirect(request.getContextPath()+"/games/history");
        return;
    }

    MultiplayerGame game = DatabaseAccess.getMultiplayerGame(gameId);

    if ((!game.getState().equals(GameState.FINISHED))) {
        response.sendRedirect(request.getContextPath()+"/games/user");
    }

    int uid = ((Integer) session.getAttribute("uid"));
    Role role = game.getRole(uid);
%>

<%-- Set request attributes for the components. --%>
<%
    /* class_viewer */
    final GameClass cut = game.getCUT();
    request.setAttribute("className", cut.getBaseName());
    request.setAttribute("classCode", cut.getAsHTMLEscapedString());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

    /* test_carousel */
    request.setAttribute("tests", game.getTests());

    /* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
    request.setAttribute("markEquivalent", false);
    request.setAttribute("markUncoveredEquivalent", false);
    request.setAttribute("viewDiff", true);
    request.setAttribute("gameType", "PARTY");

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    // request.setAttribute("gameType", "PARTY");
%>

<%@ include file="/jsp/multiplayer/header_game.jsp" %>
<%@ include file="/jsp/scoring_tooltip.jsp" %>
<%@ include file="/jsp/playerFeedback.jsp" %>
<%@ include file="/jsp/multiplayer/game_scoreboard.jsp" %>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6">
        <div id="mutants-div">
            <h3>Existing Mutants</h3>
            <%@include file="../game_components/mutants_list.jsp"%>
        </div>

        <div id="tests-div">
            <h3>JUnit tests </h3>
            <%@include file="../game_components/tests_carousel.jsp"%>
        </div>
    </div>

    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <%@include file="../game_components/class_viewer.jsp"%>
        <%@ include file="../game_components/game_highlighting.jsp" %>
        <%@include file="../game_components/mutant_explanation.jsp"%>
    </div>
</div>

<% } %>

<%@ include file="/jsp/multiplayer/footer_game.jsp" %>

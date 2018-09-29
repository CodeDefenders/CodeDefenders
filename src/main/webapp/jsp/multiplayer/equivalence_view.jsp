<%@ page import="org.codedefenders.util.Constants" %>

<%
    Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
%>

<%-- Set request attributes for the components. --%>
<%
    /* class_viewer */
    request.setAttribute("classCode", game.getCUT().getAsString());
    request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

    /* mutant_editor */
    String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    if (previousTestCode != null) {
        request.setAttribute("testCode", previousTestCode);
    } else {
        String equivText = "// " + equivMutant.getPatchString().replace("\n", "\n// ").trim() + "\n";
        request.setAttribute("testCode", equivText + game.getCUT().getTestTemplate());
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
    request.setAttribute("gameType", "PARTY");

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    // request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

    /* mutant_progressbar */
    request.setAttribute("gameId", game.getId());
%>

<div class="row">
    <div class="col-md-6" id="equivmut-div">
        <%@include file="../game_components/test_progress_bar.jsp"%>

        <h3>Mutant <%= equivMutant.getId() %> Claimed Equivalent</h3>
        <div style="border: 5px dashed #f00; border-radius: 10px; width: 100%; padding: 10px;">
            <form id="equivalenceForm" action="<%= request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
                <input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= equivMutant.getId() %>">
                <input type="hidden" name="formType" value="resolveEquivalence">

                <div class="w4"><p><%= equivMutant.getHTMLReadout() %></p></div>
                <%@include file="../game_components/test_editor.jsp"%>

                <a onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');" href="<%=request.getContextPath() %>/multiplayer/play?acceptEquiv=<%= equivMutant.getId() %>"><button type="button" class="btn btn-danger btn-left">Accept Equivalence</button></a>
                <button form="equivalenceForm" class="btn btn-primary btn-game btn-right" name="rejectEquivalent" type="submit" onclick="progressBar(); this.from.submit();">Submit Killing Test</button>

                <div>
                    <p>Note: If the game finishes with this equivalence unsolved, you will lose points!</p>
                </div>
            </form>
        </div>
    </div>

    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <%@include file="../game_components/class_viewer.jsp"%>
        <%@include file="../game_components/game_highlighting.jsp"%>
        <%@include file="../game_components/mutant_explanation.jsp"%>
    </div>

</div>

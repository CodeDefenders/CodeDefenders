<%@ page import="org.codedefenders.util.Constants" %>

<%
    Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
%>

<%-- Set request attributes for the components. --%>
<%
    /* class_viewer */
    request.setAttribute("classCode", game.getCUT().getAsString());

    /* test_editor */
    String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    if (previousTestCode != null) {
        request.setAttribute("testCode", previousTestCode);
    } else {
        request.setAttribute("testCode", game.getCUT().getTestTemplate());
    }
    request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    request.setAttribute("gameType", "PARTY");

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

    /* test_progressbar */
    request.setAttribute("gameId", game.getId());
%>

<div class="row">
    <div class="col-md-6" id="equivmut-div">
        <%@include file="../game_components/test_progress_bar.jsp"%>
        <h3>Mutant <%= equivMutant.getId() %> Claimed Equivalent</h3>

        <div style="border: 5px dashed #f00; border-radius: 10px; width: 100%; padding: 10px;">
            <p><%= equivMutant.getHTMLReadout() %></p>
            <a class="btn btn-default" data-toggle="collapse" href="#diff-collapse">Show Diff</a>
            <p></p>
            <pre id="diff-collapse" class="readonly-pre collapse"><textarea id="diff" class="mutdiff readonly-textarea" title="mutdiff"><%= equivMutant.getPatchString() %></textarea></pre>
            <script>
                $('#diff-collapse').on('shown.bs.collapse', function() {
                    var codeMirrorContainer = $(this).find(".CodeMirror")[0];
                    if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                        codeMirrorContainer.CodeMirror.refresh();
                    } else {
                        var showDiff = CodeMirror.fromTextArea(document.getElementById('diff'), {
                            lineNumbers: false,
                            mode: "text/x-diff",
                            readOnly: true
                        });
                        showDiff.setSize("100%", 210);
                    }
                });
            </script>

            <h3>Not equivalent? Write a killing test here:</h3>
            <form id="equivalenceForm" action="<%= request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
                <input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= equivMutant.getId() %>">
                <input type="hidden" name="formType" value="resolveEquivalence">

                <%@include file="../game_components/test_editor.jsp"%>

                <a onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');" href="<%=request.getContextPath() %>/multiplayer/play?acceptEquiv=<%= equivMutant.getId() %>"><button type="button" class="btn btn-danger btn-left">Accept Equivalence</button></a>
                <button form="equivalenceForm" class="btn btn-primary btn-game btn-right" name="rejectEquivalent" type="submit" onclick="progressBar(); this.from.submit();">Submit Killing Test</button>

                <div>
                    Note: If the game finishes with this equivalence unsolved, you will lose points!
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

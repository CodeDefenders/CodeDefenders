<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.game.GameState" %>

<% String pageTitle="Resolve Equivalence"; %>
<%@ include file="/jsp/header_game.jsp" %>

<% { %>

<%
    Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
%>

<%-- Set request attributes for the components. --%>
<%
    /* class_viewer */
    request.setAttribute("classCode", game.getCUT().getAsString());
    request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

    /* test_editor */
    String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
    if (previousTestCode != null) {
        request.setAttribute("testCode", previousTestCode);
    } else {
        // String equivText = "// " + equivMutant.getPatchString().replace("\n", "\n// ").trim() + "\n";
        // request.setAttribute("testCode", equivText + game.getCUT().getTestTemplate());
        request.setAttribute("testCode", game.getCUT().getTestTemplate());
    }

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    request.setAttribute("gameType", "PARTY");

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", CodeValidatorLevel.MODERATE);

    /* test_progressbar */
    request.setAttribute("gameId", game.getId());
%>

<% if (game.getState() == GameState.FINISHED) { %>
    <%@include file="game_components/finished_modal.jsp"%>
<% } %>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="equivmut-div">
        <%@include file="game_components/test_progress_bar.jsp"%>
        <h3>Mutant <%= equivMutant.getId() %> Claimed Equivalent</h3>

        <div style="border: 5px dashed #f00; border-radius: 10px; width: 100%; padding: 10px;">
            <div class="w4">
                <p><%= equivMutant.getHTMLReadout() %></p>
                <a href="#" class="btn btn-default" id="btnMut" data-toggle="modal" data-target="#modalMut">View Diff</a>

                <div id="modalMut" class="modal fade" role="dialog">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal">&times;</button>
                                <h4 class="modal-title">Mutant <%= equivMutant.getId ()%> - Diff</h4>
                            </div>
                            <div class="modal-body">
                            <pre class="readonly-pre">
                                <textarea class="mutdiff readonly-textarea" id="diff" title="mutdiff"><%=equivMutant.getPatchString()%></textarea>
                            </pre>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div>
                <script>
                    $('#modalMut').on('shown.bs.modal', function() {
                        var codeMirrorContainer = $(this).find(".CodeMirror")[0];
                        if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                            codeMirrorContainer.CodeMirror.refresh();
                        } else {
                            var showDiff = CodeMirror.fromTextArea(document.getElementById('diff'), {
                                lineNumbers: false,
                                mode: "text/x-diff",
                                readOnly: true
                            });
                            showDiff.setSize("100%", 500);
                        }
                    });
                </script>
            </div>

            <form id="equivalenceForm" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
                <input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= equivMutant.getId() %>">
                <input type="hidden" name="formType" value="resolveEquivalence">

                <h3>Not equivalent? Write a killing test here:</h3>

                <%@include file="game_components/test_editor.jsp"%>

                <button class="btn btn-danger btn-left" name="acceptEquivalent" type="submit" onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');">Accept Equivalence</button>
                <button class="btn btn-primary btn-game btn-right" name="rejectEquivalent" type="submit" onclick="progressBar(); return true;">Submit Killing Test</button>
            </form>
        </div>
    </div>

    <div class="col-md-6" id="cut-div">
        <h3>Class Under Test</h3>
        <%@include file="game_components/class_viewer.jsp"%>
        <%@include file="game_components/game_highlighting.jsp"%>
        <%@include file="game_components/mutant_explanation.jsp"%>
    </div>
</div>

<% } %>

<%@include file="/jsp/footer_game.jsp" %>

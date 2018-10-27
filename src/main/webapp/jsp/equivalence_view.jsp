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
    final GameClass cut = game.getCUT();
    request.setAttribute("className", cut.getBaseName());
    request.setAttribute("classCode", cut.getAsHTMLEscapedString());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());

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

    /* finished_modal */
    int attackerScore = game.getAttackerScore();
    int defenderScore = game.getDefenderScore();
    request.setAttribute("win", attackerScore > defenderScore);
    request.setAttribute("loss", defenderScore > attackerScore);

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
            <p><%=String.join("\n", equivMutant.getHTMLReadout())%></p>
            <a class="btn btn-default" data-toggle="collapse" href="#diff-collapse">Show Diff</a>
            <p></p>
            <pre id="diff-collapse" class="readonly-pre collapse"><textarea
                    id="diff" class="mutdiff readonly-textarea"
                    title="mutdiff"><%=equivMutant.getHTMLEscapedPatchString()%></textarea></pre>
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
            <form id="equivalenceForm" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
                <input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= equivMutant.getId() %>">
                <input type="hidden" name="formType" value="resolveEquivalence">

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

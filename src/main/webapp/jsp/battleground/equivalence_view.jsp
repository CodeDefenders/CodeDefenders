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
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameMode" %>
<%@ page import="org.codedefenders.util.Paths" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>

<%-- TODO: list parameters --%>

<%
    Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
    User equivDefender = (User) request.getAttribute("equivDefender");
    MultiplayerGame game = (MultiplayerGame) request.getAttribute("game");
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
        request.setAttribute("testCode", cut.getHTMLEscapedTestTemplate());
    }
    request.setAttribute("mockingEnabled", cut.isMockingEnabled());
    request.setAttribute("startEditLine", cut.getTestTemplateFirstEditLine());

    /* game_highlighting */
    request.setAttribute("codeDivSelector", "#cut-div");
    request.setAttribute("tests", game.getTests());
    request.setAttribute("mutants", game.getMutants());
    request.setAttribute("showEquivalenceButton", false);
    request.setAttribute("gameType", GameMode.PARTY);
    request.setAttribute("gameId", game.getId());

    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

    /* test_progressbar */
//    request.setAttribute("gameId", game.getId());
%>

<div class="row">
    <div class="col-md-6" id="equivmut-div">
        <%@include file="../game_components/test_progress_bar.jsp"%>
        <h3>Mutant <%= equivMutant.getId() %>
        <!-- check for automatically triggered equivalence duels -->
        <% if (equivDefender.getId() == Constants.DUMMY_CREATOR_USER_ID) { %>
            automatically claimed equivalent</h3>
        <% } else { %>
            claimed equivalent by <%= equivDefender.getUsername() %> </h3>
        <% } %>

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
            <form id="equivalenceForm" action="<%= request.getContextPath() + Paths.BATTLEGROUND_GAME %>" method="post">
                <input type="hidden" name="formType" value="resolveEquivalence">
                <input type="hidden" name="gameId" value="<%= game.getId() %>">
                <input type="hidden" id="equivMutantId" name="equivMutantId" value="<%= equivMutant.getId() %>">

                <%@include file="../game_components/test_editor.jsp"%>

                <button class="btn btn-danger btn-left" name="acceptEquivalent" type="submit" onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');">Accept Equivalence</button>
                <button class="btn btn-primary btn-game btn-right" name="rejectEquivalent" type="submit" onclick="progressBar(); return true;">Submit Killing Test</button>

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

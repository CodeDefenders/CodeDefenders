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
<%@page import="java.util.Iterator"%>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.util.Paths" %>

<% { %>

<%-- Set request attributes for the components. --%>
<%
    /* From Attacker View */
    String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
        
    List<Integer> errorLinesInMutant = (List<Integer>) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES_IN_MUTANT);
    // We should not remove this attribute now but later
    // request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES_IN_MUTANT);

    // This was set by the containing servlet !
    final GameClass cut = game.getCUT();

    /* mutant_editor -> replaces the class_view */
    /* equivalence_duel -> replaces the class_view */
    
    boolean openEquivalenceDuel = request.getAttribute("openEquivalenceDuel") != null;
    Mutant equivMutant = (Mutant) request.getAttribute("equivMutant");
    
    // TODO something to get here here for the equivalence ?
    
    if (previousMutantCode != null) {
        request.setAttribute("mutantCode", previousMutantCode);
        /* error_highlighting_in_mutant */
        request.setAttribute("codeDivSelectorForErrorInMutant", "#newmut-div");
        request.setAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES_IN_MUTANT, errorLinesInMutant);
    } else {
        request.setAttribute("mutantCode", cut.getAsHTMLEscapedString());
    }
    request.setAttribute("mutantName", cut.getBaseName());
    request.setAttribute("dependencies", cut.getHTMLEscapedDependencyCode());
    
    
	/* From Defender View */
	
	/* test_editor */
	String previousTestCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
	request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST);
	List<Integer> errorLinesInTest = (List<Integer>) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES_IN_TEST);
    // request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES_IN_TEST);
    
	if (previousTestCode != null) {
		request.setAttribute("testCode", previousTestCode);
		/* error_highlighting_in_test */
		request.setAttribute("codeDivSelectorForErrorInTest", "#utest-div");
		request.setAttribute(Constants.SESSION_ATTRIBUTE_ERROR_LINES_IN_TEST, errorLinesInTest);
	} else {
		request.setAttribute("testCode", cut.getHTMLEscapedTestTemplate());
	}
	request.setAttribute("mockingEnabled", cut.isMockingEnabled());
	
	request.setAttribute("startEditLineInTest", cut.getTestTemplateFirstEditLine());
	request.setAttribute("startEditLineInTestDuel", cut.getTestTemplateFirstEditLine());

	/* Common components */
	
	/*
	   Filter by ID so we show only our own tests and mutants
	*/
	int defenderId = (Integer) request.getAttribute( Constants.SESSION_ATTRIBUTE_DEFENDER_ID );
	int attackerId = (Integer) request.getAttribute( Constants.SESSION_ATTRIBUTE_ATTACKER_ID );
    //
    List<Test> tests = game.getTests(); 
    List<Mutant> mutants = game.getMutants();

    for (Iterator<Test> i = tests.iterator(); i.hasNext(); ){
        Test t = i.next();
        if( t.getPlayerId() != defenderId ){
            i.remove();
        }
    }
    
    // Do not filter mutants so they show up in mutant_editor and 
    
    request.setAttribute("cut", cut);
	request.setAttribute("tests", tests);
	request.setAttribute("mutants", mutants);

	/* mutants_list */
	request.setAttribute("mutantsAlive", game.getAliveMutants());
	request.setAttribute("mutantsKilled", game.getKilledMutants());
	request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
	request.setAttribute("mutantsMarkedEquivalent", game.getMutantsMarkedEquivalentPending());
	request.setAttribute("markEquivalent", true);
	request.setAttribute("viewDiff", game.getLevel() == GameLevel.EASY);
	request.setAttribute("gameType", GameMode.MELEE);
	request.setAttribute("gameId", game.getId());

	/* game_highlighting - Taken from the Attacker view*/
    request.setAttribute("codeDivSelector", "#newmut-div");

    // TODO We need to filter this somehow or at least show our mutants and others' mutants differently ?
    request.setAttribute("mutants", game.getMutants());
    
    // We can claim equivalence on other players
	request.setAttribute("showEquivalenceButton", true);
    
    /* mutant_explanation */
    request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());

	/* test_progressbar */
//	request.setAttribute("gameId", game.getId());
%>

<!--<div class="row" style="padding: 0px 15px;"> TODO change to this after changing the header -->
<div class="row">

    <%-- Show the regular view or the equivalence duel view --%>
    <% 
    if( openEquivalenceDuel) {
    %>
    <div class="col-md-6" id="equivmut-div">
        <%@include file="../game_components/test_progress_bar.jsp"%>
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
            <form id="equivalenceForm" action="<%= request.getContextPath() + Paths.MELEE_GAME %>" method="post">
                <input type="hidden" name="formType" value="resolveEquivalence">
                <input type="hidden" name="gameId" value="<%= game.getId() %>">
                <input type="hidden" id="equivMutantId" name="equivMutantId" value="<%= equivMutant.getId() %>">

                <%-- THIS CREATES PROBLEMS WITH DUPLICATE VARIABLE NAMES WHICH REQUIRES TO USE THE TEST_EDITOR ON THE RIGHT SIDE TO RESOLVE THE DUEL ! --%>
                <%@include file="../game_components/test_editor_duel.jsp"%>

                <button class="btn btn-danger btn-left" name="acceptEquivalent" type="submit" onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');">Accept Equivalence</button>
                <button class="btn btn-primary btn-game btn-right" name="rejectEquivalent" type="submit" onclick="progressBar(); return true;">Submit Killing Test</button>

                <div>
                    Note: If the game finishes with this equivalence unsolved, you will lose points!
                </div>
            </form>
        </div>
    </div>
    <%
    } else{
     %>
     <div class="col-md-6" id="newmut-div">
        <%@include file="../game_components/mutant_progress_bar.jsp"%>
        <div class="row" style="display: contents">
            <h3 style="margin-bottom: 0; display: inline">Create a mutant here</h3>

            <!-- Attack button with intention dropDown set in attacker_intention_collector.jsp -->
            <%-- TODO: change back to registerMutantProgressBar() --%>
            <button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk"
                onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';"
                style="float: right; margin-right: 5px"
                <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
                Attack!
            </button>

            <!-- Reset button -->
            <form id="reset" action="<%=request.getContextPath() + Paths.MELEE_GAME %>" method="post" style="float: right; margin-right: 5px">
                <button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset">
                    Reset
                </button>
                <input type="hidden" name="formType" value="reset">
                <input type="hidden" name="gameId" value="<%= game.getId() %>"/>
            </form>
        </div>

        <form id="atk" action="<%=request.getContextPath() + Paths.MELEE_GAME %>" method="post">
            <input type="hidden" name="formType" value="createMutant">
            <input type="hidden" name="gameId" value="<%= game.getId() %>"/>

            <%@include file="../game_components/mutant_editor.jsp"%>
            <%@include file="../game_components/game_highlighting.jsp" %>
            <%@include file="../game_components/error_highlighting_in_mutant.jsp" %>
        </form>
        <%@include file="../game_components/mutant_explanation.jsp"%>
        <%@include file="../game_components/editor_help_config_toolbar.jsp"%>
    </div>
     <%
     } 
     %>
    
    <%-- Defender view --%>

	<div class="col-md-6" id="utest-div">
		<%@include file="../game_components/test_progress_bar.jsp"%>
		<%--<%@include file="../game_components/push_test_progress_bar.jsp"%>--%>

		<%-- TODO Why progress bar here is handled differently than mutant submission ?! --%>
		<%-- TODO: change back to registerTestProgressBar() --%>
		<h3>Write a new JUnit test here
			<button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def"
                onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
                <% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
				Defend!
			</button>
		</h3>

		<form id="def" action="<%=request.getContextPath() + Paths.MELEE_GAME%>" method="post">
			<%@include file="../game_components/test_editor.jsp"%>
			<input type="hidden" name="formType" value="createTest">
			<input type="hidden" name="gameId" value="<%= game.getId() %>" />
		</form>
		<%@include file="../game_components/editor_help_config_toolbar.jsp"%>
		<%@include file="../game_components/error_highlighting_in_test.jsp" %>
	</div>
</div>

</div> <%-- TODO move the whole div here after changing the header --%>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="mutants-div">
        <h3>Existing Mutants</h3>
        <%@include file="../game_components/mutants_list.jsp"%>
	</div>

    <div class="col-md-6" id="tests-div">
        <h3>JUnit tests </h3>
        <jsp:include page="/jsp/game_components/test_accordion.jsp"/>
    </div>
</div>

<div>

<% } %>

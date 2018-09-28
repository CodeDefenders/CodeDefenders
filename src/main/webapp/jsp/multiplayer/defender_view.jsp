<%@ page import="org.codedefenders.util.Constants" %>

<% { %>

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
		request.setAttribute("testCode", game.getCUT().getTestTemplate());
	}

	/* test_carousel */
	request.setAttribute("tests", game.getTests());

	/* mutants_list */
	request.setAttribute("mutantsAlive", game.getAliveMutants());
	request.setAttribute("mutantsKilled", game.getKilledMutants());
	request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
	request.setAttribute("markEquivalent", role == Role.DEFENDER);
	request.setAttribute("markUncoveredEquivalent", game.isMarkUncovered());
	request.setAttribute("viewDiff", game.getLevel() == GameLevel.EASY || role == Role.ATTACKER);
	request.setAttribute("gameType", "PARTY");

	/* game_highlighting */
	request.setAttribute("codeDivSelector", "#cut-div");
	// request.setAttribute("tests", game.getTests());
	request.setAttribute("mutants", game.getMutants());
	request.setAttribute("showEquivalenceButton", true);

	/* test_progressbar */
	request.setAttribute("gameId", game.getId());
%>

<!--<div class="row" style="padding: 0px 15px;"> TODO change to this after fixing the header -->
<div class="row">
	<div class="col-md-6" id="cut-div">
		<h3>Class Under Test</h3>
		<%@include file="../game_components/class_viewer.jsp"%>
		<%@ include file="../game_components/game_highlighting.jsp" %>
		<%@include file="../game_components/mutant_explanation.jsp"%>
	</div>

	<div class="col-md-6" id="utest-div">
		<%@include file="../game_components/test_progress_bar.jsp"%>
		<h3>Write a new JUnit test here
			<button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
					<% if (game.getState() != GameState.ACTIVE) { %> disabled <% } %>>
				Defend!
			</button>
		</h3>
		<form id="def" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase()%>" method="post">
			<%@include file="../game_components/test_editor.jsp"%>
			<input type="hidden" name="formType" value="createTest">
			<input type="hidden" name="mpGameID" value="<%= game.getId() %>" />
		</form>
	</div>
</div>

</div> <%-- TODO fix this div when fixing the header --%>

<div class="row" style="padding: 0px 15px;">
    <div class="col-md-6" id="mutants-div">
        <h3>Existing Mutants</h3>
        <%@include file="../game_components/mutants_list.jsp"%>
	</div>

    <div class="col-md-6">
        <h3>JUnit tests </h3>
        <%@include file="../game_components/tests_carousel.jsp"%>
    </div>
</div>

<div>

<% } %>

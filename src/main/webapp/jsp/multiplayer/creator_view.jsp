<% { %>

<%-- Set request attributes for the components. --%>
<%
	/* class_viewer */
	request.setAttribute("classCode", game.getCUT().getAsString());
	request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

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

	/* mutant_explanation */
	request.setAttribute("mutantValidatorLevel", game.getMutantValidatorLevel());
%>

</div> <%-- TODO fix this div when fixing the header --%>

<div class="admin-panel">
	<h2>Admin</h2>
	<form id="adminEndBtn" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase()%>" method="post" style="display: inline-block;">
		<button type="submit" class="btn btn-primary btn-game btn-left" id="endGame" form="adminEndBtn"
				<% if (!game.getState().equals(GameState.ACTIVE)) { %> disabled <% } %>>
			End Game
		</button>
		<input type="hidden" name="formType" value="endGame">
		<input type="hidden" name="mpGameID" value="<%= game.getId() %>" />
	</form>
	<form id="adminStartBtn" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase()%>" method="post" style="display: inline-block;">
		<button type="submit" class="btn btn-primary btn-game" id="startGame" form="adminStartBtn"
				<% if (!game.getState().equals(GameState.CREATED)) { %> disabled <% } %>>
			Start Game
		</button>
		<input type="hidden" name="formType" value="startGame">
		<input type="hidden" name="mpGameID" value="<%= game.getId() %>" />
	</form>
</div>

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

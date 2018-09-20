<%-- Set request attributes for the components. --%>
<%
	codeDivName = "cut-div"; // TODO codeDivName as request attribute where needed

	/* class_viewer */
	request.setAttribute("classCode", game.getCUT().getAsString());
	request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

	/* test_editor */
	String previousTestCode = (String) request.getSession().getAttribute("previousTest");
	request.getSession().removeAttribute("previousTest");
	if (previousTestCode != null) {
		request.setAttribute("testCode", previousTestCode);
	} else {
		request.setAttribute("testCode", game.getCUT().getTestTemplate());
	}

	/* test_notifications */
	request.setAttribute("gameId", game.getId());

	/* test_carousel */
	request.setAttribute("tests", game.getTests());
%>

<div class="ws-12">

	<div class="col-md-6" id="cut-div">
		<h3>Class Under Test</h3>
		<%@include file="/jsp/game_components/class_viewer.jsp"%>
		<%@include file="/jsp/multiplayer/game_key.jsp"%>
	</div>

	<div class="col-md-6" id="utest-div" style="float: right; min-width: 480px">
		<%@include file="/jsp/game_components/test_progress_bar.jsp"%>

		<h3>Write a new JUnit test here
			<button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Defending...';"
					<% if (!game.getState().equals(GameState.ACTIVE)) { %> disabled <% } %>>
				Defend!
			</button>
		</h3>

		<form id="def" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase()%>" method="post">
			<%@include file="/jsp/game_components/test_editor.jsp"%>
			<input type="hidden" name="formType" value="createTest">
			<input type="hidden" name="mpGameID" value="<%= game.getId() %>" />
		</form>
	</div>

</div>

</div> <%-- TODO fix this div when fixing the header --%>

<div class="crow fly up">
	<%@include file="/jsp/multiplayer/game_mutants.jsp"%>

    <%
		// TODO is this necessary?
		if (role.equals(Role.DEFENDER)
				|| role.equals(Role.CREATOR)
				|| game.getLevel().equals(GameLevel.EASY)
				|| game.getState().equals(GameState.FINISHED)) {
    %>
        <div class="ws-12">
            <h3>JUnit tests </h3>
            <%@include file="/jsp/game_components/tests_carousel.jsp"%>
        </div>
    <% } %>
</div>

<div>
	<script>
        // TODO is this needed ?
		$('#finishedModal').modal('show');
	</script>

<% request.getSession().removeAttribute("lastTest"); %>


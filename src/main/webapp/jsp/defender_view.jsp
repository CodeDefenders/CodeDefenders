<%@ page import="static org.codedefenders.game.GameState.ACTIVE" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.util.Constants" %>

<% String pageTitle="Defending Class"; %>

<%@ include file="/jsp/header_game.jsp" %>

<%-- TODO Set request attributes in the Servlet and redirect via RequestDispatcher --%>

<%-- Set request attributes for the components. --%>
<%
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

	/* tests_carousel */
	request.setAttribute("tests", game.getTests());

	/* mutants_list */
    request.setAttribute("mutantsAlive", game.getAliveMutants());
    request.setAttribute("mutantsKilled", game.getKilledMutants());
    request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
	request.setAttribute("markEquivalent", true);
    request.setAttribute("markUncoveredEquivalent", false);
    request.setAttribute("viewDiff", game.getLevel() == GameLevel.EASY);
	request.setAttribute("gameType", "DUEL");

	/* game_highlighting */
	request.setAttribute("codeDivSelector", "#cut-div");
	// request.setAttribute("tests", game.getTests());
	request.setAttribute("mutants", game.getMutants());
	request.setAttribute("showEquivalenceButton", true);
%>

<%
	if (game.getState().equals(GameState.FINISHED)) {
		String message = Constants.DRAW_MESSAGE;
		if (game.getAttackerScore() > game.getDefenderScore())
			message = Constants.LOSER_MESSAGE;
		else if (game.getDefenderScore() > game.getAttackerScore())
			message = Constants.WINNER_MESSAGE;
%>
<div id="finishedModal" class="modal fade">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				<h4 class="modal-title">Game Over</h4>
			</div>
			<div class="modal-body">
				<p><%=message%></p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
			</div>
		</div>
	</div>
</div>
<%  } %>

<div class="row-fluid">

	<div class="col-md-6" id="cut-div">
		<h3>Class Under Test</h3>
		<%@include file="game_components/class_viewer.jsp"%>
	</div>

	<div class="col-md-6" id="utest-div">
		<h3>Write a new JUnit test here
			<% if (game.getState().equals(ACTIVE) && game.getActiveRole().equals(Role.DEFENDER)) {%>
			<button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def" onClick="this.form.submit(); this.disabled=true; this.value='Defending...';">Defend!</button>
			<% } %>
		</h3>
		<form id="def" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
			<%@include file="game_components/test_editor.jsp"%>
			<input type="hidden" name="formType" value="createTest">
		</form>
	</div>

</div>

<div class="row-fluid">
	<div class="col-md-6" id="submitted-div">
		<h3>JUnit tests </h3>
		<%@include file="game_components/tests_carousel.jsp"%>
	</div>

	<div class="col-md-6" id="mutants-div">
		<h3>Mutants</h3>
        <%@include file="game_components/mutants_list.jsp"%>
	</div>
</div>

<%@include file="game_components/game_highlighting.jsp"%>

<script>
	<% if (game.getActiveRole().equals(Role.ATTACKER)) {%>
        function checkForUpdate(){
            $.post('/play', {
                formType: "whoseTurn",
                gameID: <%= game.getId() %>
            }, function(data){
                if(data === "defender"){
                    window.location.reload();
                }
            },"text");
        }
        setInterval("checkForUpdate()", 10000);
	<% } %>

	$('#finishedModal').modal('show');
</script>
<%@ include file="/jsp/footer_game.jsp" %>

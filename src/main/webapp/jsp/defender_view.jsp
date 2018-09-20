<%@ page import="static org.codedefenders.game.GameState.ACTIVE" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.stream.Collectors" %>

<% String pageTitle="Defending Class"; %>
<%Gson gson = new Gson();%>

<%@ include file="/jsp/header_game.jsp" %>

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
		</div><!-- /.modal-content -->
	</div><!-- /.modal-dialog -->
</div><!-- /.modal -->
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
			<%}%>
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

<%
	List<Test> tests = game.getTests();
	HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<Integer, ArrayList<Test>>();

	for (Test t : tests) {

		for (Integer lc : t.getLineCoverage().getLinesCovered()) {
			if (!linesCovered.containsKey(lc)) {
				linesCovered.put(lc, new ArrayList<Test>());
			}

			linesCovered.get(lc).add(t);
		}
	}
%>

<script>
    testMap = {
        <% for (Integer i : linesCovered.keySet()){ %>
            <%= i %>: [ <%= linesCovered.get(i).stream().map(t -> Integer.toString(t.getId())).distinct().collect(Collectors.joining(",")) %> ],
        <% } %>
    };

    highlightCoverage = function(){
        highlightLine([<% for (Integer i : linesCovered.keySet()){%>
            [<%=i%>, <%=((float)linesCovered.get(i).size() / (float) tests.size())%>],
            <% } %>], COVERED_COLOR, "#cut-div");
    };

    getMutants = function(){
        return JSON.parse("<%= gson.toJson(mutants).replace("\"", "\\\"") %>");
    };

    showMutants = function(){
        mutantLine("#cut-div", "<%= game.getState().equals(ACTIVE) ? "true" : "false"%>");
    };

    var updateCUT = function(){
        showMutants();
        highlightCoverage();
    };

    editorSUT.on("viewportChange", function(){
        updateCUT();
    });
    $(document).ready(function(){
        updateCUT();
    });

    //inline due to bug in Chrome?
    $(window).resize(function (e){setTimeout(updateCUT, 500);});

	<% if (game.getActiveRole().equals(Role.ATTACKER)) {%>
	function checkForUpdate(){
		$.post('/play', {
			formType: "whoseTurn",
			gameID: <%= game.getId() %>
		}, function(data){
			if(data=="defender"){
				window.location.reload();
			}
		},"text");
	}
	setInterval("checkForUpdate()", 10000);
	<% } %>

	$('#finishedModal').modal('show');
</script>
<%@ include file="/jsp/footer_game.jsp" %>

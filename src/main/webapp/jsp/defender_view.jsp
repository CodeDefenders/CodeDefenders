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
		<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" cols="80" rows="30"><%=game.getCUT().getAsString()%></textarea></pre>
	</div> <!-- col-md6 left -->
	<div class="col-md-6" id="utest-div">
		<h3> <span class="text-nowrap">Write a new JUnit test here
			<% if (game.getState().equals(ACTIVE) && game.getActiveRole().equals(Role.DEFENDER)) {%>
			<button type="submit" class="btn btn-primary btn-game btn-right" id="submitTest" form="def" onClick="this.form.submit(); this.disabled=true; this.value='Defending...';">Defend!</button>
			<%}%></span>
		</h3>
		<form id="def" action="<%=request.getContextPath() %>/play" method="post">
			<%
				String testCode;
				String previousTestCode = (String) request.getSession().getAttribute("previousTest");
				request.getSession().removeAttribute("previousTest");
				if (previousTestCode != null) {
					testCode = previousTestCode;
				} else
					testCode = game.getCUT().getTestTemplate();
			%>
			<pre><textarea id="code" name="test" cols="80" rows="30"><%= testCode %></textarea></pre>
			<input type="hidden" name="formType" value="createTest">
		</form>
	</div> <!-- col-md6 right top -->
</div> <!-- row-fluid 1 -->

<div class="row-fluid">
	<div class="col-md-6" id="submitted-div">
		<h3>JUnit tests </h3>
		<div class="slider single-item">
			<%
				Role role = game.getRole(uid);

				String codeDivName = "cut-div";
				HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<Integer, ArrayList<Test>>();
				List<Test> tests = game.getTests();

				for (Test t : tests) {

					for (Integer lc : t.getLineCoverage().getLinesCovered()){
						if (!linesCovered.containsKey(lc)){
							linesCovered.put(lc, new ArrayList<Test>());
						}

						linesCovered.get(lc).add(t);
					}

					String tc = "";
					for (String l : t.getHTMLReadout()) { tc += l + "\n"; }
			%>
			<div><h4>Test <%= t.getId() %></h4><pre class="readonly-pre"><textarea class="utest" cols="20" rows="10"><%=tc%></textarea></pre></div>
			<%
				}
				if (tests.isEmpty()) {%>
			<div><h3></h3><p> There are currently no tests </p></div>
			<%}
			%>
		</div> <!-- slider single-item -->
	</div> <!-- col-md-6 left bottom -->
	<div class="col-md-6" id="mutants-div">
		<h3>Mutants</h3>
		<!-- Nav tabs -->
		<ul class="nav nav-tabs" role="tablist">
			<li class="active">
				<a href="#mutalivetab" role="tab" data-toggle="tab">Alive</a>
			</li>
			<li>
				<a href="#mutkilledtab" role="tab" data-toggle="tab">Killed</a>
			</li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane fade active in" id="mutalivetab">
				<table class="table table-hover table-responsive table-paragraphs">
					<%
						List<Mutant> mutantsAlive = game.getAliveMutants();

						List<Mutant> mutantsEquiv =  game.getMutantsMarkedEquivalent();

						Map<Integer, List<Mutant>> mutantLines = new HashMap<Integer, List<Mutant>>();

						for (Mutant m : mutantsAlive) {
							for (int line : m.getLines()){
								if (!mutantLines.containsKey(line)){
									mutantLines.put(line, new ArrayList<Mutant>());
								}
								mutantLines.get(line).add(m);
							}
						}

						if (! mutantsAlive.isEmpty()) {
						for (Mutant m : mutantsAlive) {
					%>
					<tr>
						<td>
							<h4>Mutant <%= m.getId() %></h4>
						</td>
						<td>
							<% if (game.getLevel().equals(GameLevel.EASY) || game.getState().equals(GameState.FINISHED)) { %>
							<a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
							<% } %>
							<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog">
								<div class="modal-dialog">
									<!-- Modal content-->
									<div class="modal-content">
										<div class="modal-header">
											<button type="button" class="close" data-dismiss="modal">&times;</button>
											<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
										</div>
										<div class="modal-body">
											<pre class="readonly-pre"><textarea class="mutdiff" id="diff<%=m.getId()%>"><%=m.getPatchString()%></textarea></pre>
										</div>
										<div class="modal-footer">
											<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
										</div>
									</div>
								</div>
							</div>
						</td>
						<td >
							<% if (game.getState().equals(ACTIVE) && game.getActiveRole().equals(Role.DEFENDER)) {%>
							<form id="equiv" action="<%=request.getContextPath() %>/play" method="post">
								<input type="hidden" name="formType" value="claimEquivalent">
								<input type="hidden" name="mutantId" value="<%=m.getId()%>">
								<button type="submit" class="btn btn-default btn-right">Claim Equivalent</button>
							</form>
							<%}%>
						</td>
					</tr>
					<tr>
						<td colspan="3">
							<% for (String change :	m.getHTMLReadout()) { %>
							<p><%=change%><p>
								<% } %>
						</td>
					</tr>
					<%
						}
					} else {%>
					<tr class="blank_row">
						<td class="row-borderless" colspan="2">No mutants alive.</td>
					</tr>
					<%}
					%>
				</table>
			</div>
			<div class="tab-pane fade" id="mutkilledtab">
				<table class="table table-hover table-responsive table-paragraphs">
					<%
					List<Mutant> mutantsKilled = game.getKilledMutants();
					Map<Integer, List<Mutant>> mutantKilledLines = new HashMap<>();
					if (! mutantsKilled.isEmpty()) {
						for (Mutant m : mutantsKilled) {
							for (int line : m.getLines()){
								if (!mutantKilledLines.containsKey(line)){
									mutantKilledLines.put(line, new
											ArrayList<Mutant>());
								}
								mutantKilledLines.get(line).add(m);
							}
					%>
					<tr>
						<td><h4>Mutant <%= m.getId() %></h4></td>
						<td>
							<a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
							<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog">
								<div class="modal-dialog">
									<!-- Modal content-->
									<div class="modal-content">
										<div class="modal-header">
											<button type="button" class="close" data-dismiss="modal">&times;</button>
											<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
										</div>
										<div class="modal-body">
											<p>Killed by Test <%= DatabaseAccess.getKillingTestIdForMutant(m.getId()) %></p>
											<pre class="readonly-pre"><textarea class="mutdiff" id="diff<%=m.getId()%>"><%=m.getPatchString()%></textarea></pre>
										</div>
										<div class="modal-footer">
											<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
										</div>
									</div>
								</div>
							</div>
						</td>
					</tr>
					<tr>
						<td colspan="2">
							<% for (String change : m.getHTMLReadout()) { %>
							<p><%=change%><p>
								<% } %>
						</td>
					</tr>
					<%
						}
					} else {%>
					<tr class="blank_row">
						<td class="row-borderless" colspan="2">No mutants killed.</td>
					</tr>
					<%}
					%>
				</table>
			</div>
		</div> <!-- tab-content -->
	</div> <!-- col-md-6 right bottom -->
</div>

<script>
	var editorTest = CodeMirror.fromTextArea(document.getElementById("code"), {
		lineNumbers: true,
		indentUnit: 4,
		indentWithTabs: true,
		matchBrackets: true,
		mode: "text/x-java"
	});
	editorTest.on('beforeChange',function(cm,change) {
		var text = cm.getValue();
		var lines = text.split(/\r|\r\n|\n/);
		var readOnlyLines = [0,1,2,3,4,5,6,7];
		var readOnlyLinesEnd = [lines.length-1,lines.length-2];
		if ( ~readOnlyLines.indexOf(change.from.line) || ~readOnlyLinesEnd.indexOf(change.to.line)) {
			change.cancel();
		}
	});
	editorTest.setSize("100%", 500);
	var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
		lineNumbers: true,
		matchBrackets: true,
		mode: "text/x-java",
		readOnly: true
	});
	editorSUT.setSize("100%", 500);

    testMap = {<% for (Integer i : linesCovered.keySet()){%>
    <%= i%>: [<%= linesCovered.get(i).stream().map(t -> Integer.toString(t.getId())).distinct().collect(Collectors.joining(","))%>],
    <% } %>
    };


    highlightCoverage = function(){
        highlightLine([<% for (Integer i : linesCovered.keySet()){%>
            [<%=i%>, <%=((float)linesCovered.get(i).size() / (float) tests.size())%>],
            <% } %>], COVERED_COLOR, "<%="#" + codeDivName%>");
    };

    getMutants = function(){
        return JSON.parse("<%= gson.toJson(mutants).replace("\"", "\\\"") %>");
    }

    showMutants = function(){
        mutantLine("<%="#" + codeDivName%>", "true");
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



	/* Submitted tests */
	var x = document.getElementsByClassName("utest");
	var i;
	for (i = 0; i < x.length; i++) {
		CodeMirror.fromTextArea(x[i], {
			lineNumbers: true,
			matchBrackets: true,
			mode: "text/x-java",
			readOnly: true
		});
	}
	/* Mutants diffs */
	$('.modal').on('shown.bs.modal', function() {
		var codeMirrorContainer = $(this).find(".CodeMirror")[0];
		if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
			codeMirrorContainer.CodeMirror.refresh();
		} else {
			var editorDiff = CodeMirror.fromTextArea($(this).find('textarea')[0], {
				lineNumbers: false,
				mode: "diff",
				readOnly: true /* onCursorActivity: null */
			});
			editorDiff.setSize("100%", 500);
		}
	});

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

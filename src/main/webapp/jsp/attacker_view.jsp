<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->

	<!-- App context -->
	<base href="${pageContext.request.contextPath}/">

	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
	<!-- Slick -->
	<link rel="stylesheet" type="text/css" href="//cdn.jsdelivr.net/jquery.slick/1.5.9/slick.css"/>
	<script type="text/javascript" src="//cdn.jsdelivr.net/jquery.slick/1.5.9/slick.min.js"></script>


	<!-- Bootstrap -->
	<link href="css/bootstrap.min.css" rel="stylesheet">
	<link href="css/gamestyle.css" rel="stylesheet">

	<script src="codemirror/lib/codemirror.js"></script>
	<script src="codemirror/mode/javascript/javascript.js"></script>
	<script src="codemirror/mode/diff/diff.js"></script>
	<link href="codemirror/lib/codemirror.css" rel="stylesheet">


	<script>
		$(document).ready(function() {
			$('.single-item').slick({
				arrows: true,
				infinite: true,
				speed: 300,
				draggable:false
			});
			$('#messages-div').delay(10000).fadeOut();
		});
	</script>
</head>

<body>
<%@ page import="java.util.*" %>
<%@ page import="org.codedefenders.Test" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.Constants" %>
<%@ page import="static org.codedefenders.Game.State.ACTIVE" %>
<% Game game = (Game) session.getAttribute("game"); %>

<nav class="navbar navbar-inverse navbar-fixed-top">
	<div class="container-fluid">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse-1">
			</button>
		</div>
		<div class= "collapse navbar-collapse" id="navbar-collapse-1">
			<ul class="nav navbar-nav navbar-left">
				<a class="navbar-brand" href="games">Code Defenders</a>
				<li class="navbar-text">ATK: <%= game.getAttackerScore() %> | DEF: <%= game.getDefenderScore() %></li>
				<li class="navbar-text">Round <%= game.getCurrentRound() %> of <%= game.getFinalRound() %></li>
				<% if (game.getAliveMutants().size() == 1) {%>
				<li class="navbar-text">1 Mutant Alive</li>
				<% } else {%>
				<li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants Alive</li>
				<% }%>
			</ul>
			<ul class="nav navbar-nav navbar-right">
				<% if (game.getState().equals(ACTIVE) && game.getActivePlayer().equals("ATTACKER")) {%>
				<button type="submit" class="btn btn-default navbar-btn" form="atk">Attack!</button><%}%>
			</ul>
		</div>
	</div>
</nav>

<%
	ArrayList<String> messages = (ArrayList<String>) request.getSession().getAttribute("messages");
	request.getSession().removeAttribute("messages");
	if (messages != null && ! messages.isEmpty()) {
%>
<div class="alert alert-info" id="messages-div">
	<% for (String m : messages) { %>
	<pre><strong><%=m%></strong></pre>
	<% } %>
</div>
<%	} %>

<%
	if (game.getState().equals(Game.State.FINISHED)) {
		String message = Constants.DRAW_MESSAGE;
 	    if (game.getAttackerScore() > game.getDefenderScore())
	        message = Constants.WINNER_MESSAGE;
		else if (game.getDefenderScore() > game.getAttackerScore())
	        message = Constants.LOSER_MESSAGE;
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
	<div id="info" class="col-md-6">

		<h2>Mutants</h2>
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
				<h3>Alive</h3>
				<table class="table table-hover table-responsive table-paragraphs">
					<%
					ArrayList<Mutant> mutantsAlive = game.getAliveMutants();
					if (! mutantsAlive.isEmpty()) {
						for (Mutant m : mutantsAlive) {
					%>
					<tr>
						<td><h4>Mutant <%= m.getId() %></h4></td>
						<td>
							<a href="#" class="btn btn-default" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
							<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog">
								<div class="modal-dialog">
									<!-- Modal content-->
									<div class="modal-content">
										<div class="modal-header">
											<button type="button" class="close" data-dismiss="modal">&times;</button>
											<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
										</div>
										<div class="modal-body">
											<pre><textarea id="diff<%=m.getId()%>" class="mutdiff"><%=m.getPatchString()%></textarea></pre>
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
				<h3>Killed</h3>
				<table class="table table-hover table-responsive table-paragraphs">
					<%
					ArrayList<Mutant> mutantsKilled = game.getKilledMutants();
					if (! mutantsKilled.isEmpty()) {
						for (Mutant m : mutantsKilled) {
					%>
					<tr>
						<td class="col-sm-1"><h4>Mutant <%= m.getId() %></h4></td>
						<td class="col-sm-1">
							<a href="#" class="btn btn-default" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
							<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog">
								<div class="modal-dialog">
									<!-- Modal content-->
									<div class="modal-content">
										<div class="modal-header">
											<button type="button" class="close" data-dismiss="modal">&times;</button>
											<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
										</div>
										<div class="modal-body">
											<pre><textarea id="diff<%=m.getId()%>" class="mutdiff"><%=m.getPatchString()%></textarea></pre>
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
						<td class="col-sm-1" colspan="3">
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

		<h2> Tests </h2>
		<div class="slider single-item">
			<%
				boolean isTests = false;
				int count = 1;
				for (Test t : game.getTests()) {
					isTests = true;
					String tc = "";
					for (String line : t.getHTMLReadout()) { tc += line + "\n"; }
			%>
			<div><h4>Test <%=count%></h4><pre><textarea id=<%="tc"+count%> name="utest" class="utest" cols="20" rows="10"><%=tc%></textarea></pre></div>
			<%
					count++;
				}
				if (!isTests) {%>
			<div><h2></h2><p> There are currently no tests </p></div>
			<%}
			%>
		</div> <!-- slider single-item -->
	</div>  <!-- col-md6 left -->

	<div id="right" class="col-md-6">
	    <form id="atk" action="play" method="post">
		    <h2>Create a mutant here</h2>
			<input type="hidden" name="formType" value="createMutant">
			<pre><textarea id="code" name="mutant" cols="80" rows="50"><%=game.getCUT().getAsString()%></textarea></pre>
			<br>
		</form>
	</div>
</div>

<!-- Include all compiled plugins (below), or include individual files as needed -->
<script src="js/bootstrap.min.js"></script>
<script>
	var editor = CodeMirror.fromTextArea(document.getElementById("code"), {
		lineNumbers: true,
		matchBrackets: true
	});
	editor.setSize("100%", 500);

	var x = document.getElementsByClassName("utest");
	var i;
	for (i = 0; i < x.length; i++) {
		CodeMirror.fromTextArea(x[i], {
			lineNumbers: true,
			matchBrackets: true,
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
				readOnly: true,
				lineNumbers: false,
				mode: "diff",
				onCursorActivity: null
			});
			editorDiff.setSize("100%", 500);
		}
	});
	$('#finishedModal').modal('show');
</script>
</body>
</html>

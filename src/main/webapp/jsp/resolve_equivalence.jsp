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

<%@ page import="org.codedefenders.*,java.io.*, java.util.*" %>
<%@ page import="org.codedefenders.Test" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.Game" %>
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
				<li class="navbar-text">Round <%= game.getCurrentRound() %></li>
				<% if (game.getAliveMutants().size() == 1) {%>
				<li class="navbar-text">1 Mutant Alive</li>
				<% } else {%>
				<li class="navbar-text"><%= game.getAliveMutants().size() %> Mutants Alive</li>
				<% }%>
			</ul>
			<ul class="nav navbar-nav navbar-right">
				<!--<button type="submit" class="btn btn-default navbar-btn" form="equivalence">Resolve</button>-->
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

<div class="row-fluid">
	<div class="col-md-6"> <!-- empty cell -->
	</div>

	<div class="col-md-6">
		<h2>Equivalent mutant?</h2>
		<table class="table table-hover table-responsive table-paragraphs">

			<%
				ArrayList<Mutant> equivMutants = game.getMutantsMarkedEquivalent();
				if (! equivMutants.isEmpty()) {
					Mutant m = equivMutants.get(0);
			%>
			<tr>
				<td>
					<h4>Mutant <%= m.getId() %></h4>
				</td>
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
				<td>
					<form id="equivalenceForm" action="play" method="post">
						<input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= m.getId() %>">
						<input type="hidden" name="formType" value="resolveEquivalence">
						<input class="btn btn-default" name="acceptEquivalent" type="submit" value="Accept Equivalent">
						<input class="btn btn-default turn" name="rejectEquivalent" type="submit" value="Submit Killing Test">
					</form>
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
			} else {
			%>
			<tr class="blank_row">
				<td class="row-borderless" colspan="2">No mutant alive is marked as equivalent.</td>
			</tr>
			<%
				}
			%>
		</table>
	</div> <!-- col-md6 right -->
</div> <!-- row-fluid -->

<div class="row-fluid">
	<div class="col-md-6">
		<h2> Source Code </h2>
		<input type="hidden" name="formType" value="createMutant">
		<pre><textarea id="sut" cols="80" rows="50"><%= game.getCUT().getAsString() %></textarea></pre>
	</div> <!-- col-md6 left -->
	<div class="col-md-6">
		<h2>Not Equivalent? Write a killing test here</h2>
        <pre>
			<%
				String testCode;
				String previousTestCode = (String) request.getSession().getAttribute("previousTest");
				request.getSession().removeAttribute("previousTest");
				if (previousTestCode != null) {
				    testCode = previousTestCode;
				} else
					testCode = game.getCUT().getTestTemplate();
			%>
	        <textarea id="newtest" name="test" form="equivalenceForm" cols="80" rows="30"><%= testCode %></textarea>
        </pre>
	</div> <!-- col-md6 right -->
</div> <!-- row-fluid -->

<div class="row-fluid">
	<div id="code" class="col-md-6">
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
	</div> <!-- col-md6 left -->
	<div class="col-md-6"></div>
</div>  <!-- row-fluid -->

<!-- Include all compiled plugins (below), or include individual files as needed -->
<script src="js/bootstrap.min.js"></script>
<script>
	var editorTest = CodeMirror.fromTextArea(document.getElementById("newtest"), {
		lineNumbers: true,
		matchBrackets: true
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
		matchBrackets: true
	});
	editorSUT.setSize("100%", 500);
	var x = document.getElementsByClassName("utest");
	var i;
	for (i = 0; i < x.length; i++) {
		CodeMirror.fromTextArea(x[i], {
			lineNumbers: true,
			matchBrackets: true,
			readOnly: true
		});
	};
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
</script>
</body>
</html>

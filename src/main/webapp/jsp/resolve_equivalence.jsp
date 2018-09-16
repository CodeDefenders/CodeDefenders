<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<% String pageTitle="Resolve Equivalence"; %>
<%@ include file="/jsp/header_game.jsp" %>

<div class="row-fluid">
	<div class="col-md-6" id="new-test">
		<h2> Class Under Test </h2>
		<input type="hidden" name="formType" value="createMutant">
		<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" cols="80" rows="50"><%= game.getCUT().getAsString() %></textarea></pre>
		<h2> Tests </h2>
		<div class="slider single-item">
			<%
				boolean isTests = false;
				for (Test t : game.getTests()) {
					isTests = true;
					String tc = "";
					for (String line : t.getHTMLReadout()) { tc += line + "\n"; }
			%>
			<div><h4>Test <%= t.getId() %></h4><pre class="readonly-pre"><textarea class="utest readonly-textarea" cols="20" rows="10"><%=tc%></textarea></pre></div>
			<%
				}
				if (!isTests) {%>
			<div><h2></h2><p> There are currently no tests </p></div>
			<%}
			%>
		</div> <!-- slider single-item -->
	</div> <!-- col-md6 left -->
	<div class="col-md-6">
		<h2>Equivalent mutant?</h2>
		<table class="table table-striped table-hover table-responsive table-paragraphs">

			<%
				List<Mutant> equivMutants = game.getMutantsMarkedEquivalent();
				HashMap<Integer, List<Mutant>> mutantLines = new HashMap<Integer, List<Mutant>>();
				if (! equivMutants.isEmpty()) {
					Mutant m = equivMutants.get(0);
					for (int line : m.getLines()){
						if (!mutantLines.containsKey(line)){
							mutantLines.put(line, new ArrayList<Mutant>());
						}
						mutantLines.get(line).add(m);
					}
			%>
			<tr>
				<td>
					<h4>Mutant <%= m.getId() %></h4>
				</td>
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
									<pre class="readonly-pre"><textarea class="mutdiff readonly-textarea" id="diff<%=m.getId()%>"><%=m.getPatchString()%></textarea></pre>
								</div>
								<div class="modal-footer">
									<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
								</div>
							</div>
						</div>
					</div>
				</td>
				<td>
					<form id="equivalenceForm" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase() %>" method="post">
					<input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= m.getId() %>">
					<input type="hidden" name="formType" value="resolveEquivalence">
					<div class="btn-right">
						<input class="btn btn-default" name="acceptEquivalent" type="submit" value="Accept Equivalent">
						<input class="btn btn-primary" name="rejectEquivalent" type="submit" value="Submit Killing Test">							</div>
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

		<h2>Not Equivalent? Write a killing test here</h2>
			<%
				String testCode;
				String previousTestCode = (String) request.getSession().getAttribute("previousTest");
				request.getSession().removeAttribute("previousTest");
				if (previousTestCode != null) {
				    testCode = previousTestCode;
				} else
					testCode = game.getCUT().getTestTemplate();
			%>
	        <pre><textarea id="newtest" name="test" form="equivalenceForm" cols="80" rows="30"><%= testCode %></textarea></pre>
	</div> <!-- col-md6 right -->
</div> <!-- row-fluid -->

<script>
	var editorTest = CodeMirror.fromTextArea(document.getElementById("newtest"), {
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

	showMutant = function(){
		mutantLine([
			<% for (Integer line : mutantLines.keySet()) {
            %>
			[<%= line %>,
				<%= mutantLines.get(line).size() %>, [
				<% for(Mutant mm : mutantLines.get(line)){%>
				<%= mm.getId() %>,
				<%}%>
			]],
			<%
                } %>
		],"#new-test", false );
	};
	editorSUT.on("viewportChange", function(){
		showMutant();
	});
	$(document).ready(function(){
		showMutant();
	});

	var x = document.getElementsByClassName("utest");
	var i;
	for (i = 0; i < x.length; i++) {
		CodeMirror.fromTextArea(x[i], {
			lineNumbers: true,
			matchBrackets: true,
			mode: "text/x-java",
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
				lineNumbers: false,
				mode: "diff",
				readOnly: true /* onCursorActivity: null */
			});
			editorDiff.setSize("100%", 500);
		}
	});
</script>
<%@ include file="/jsp/footer_game.jsp" %>
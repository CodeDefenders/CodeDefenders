<%
	codeDivName = "cut-div";
%>
<div class="ws-12">
	<div class="col-md-6" id="cut-div">
		<h2>Class Under Test</h2>
		<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" cols="80" rows="30"><%=mg.getCUT().getAsString()%></textarea></pre>
		<%@include file="/jsp/multiplayer/game_key.jsp"%>
	</div> <!-- col-md6 left -->
	<div class="col-md-6" id="utest-div" style="float: right; min-width: 480px">
		<h2> Write a new JUnit test here
			<button type="submit" class="btn btn-primary btn-game btn-right" form="def" onClick="this.form.submit(); this.disabled=true; this.value='Defending...';"
					<% if (!mg.getState().equals(GameState.ACTIVE)) { %> disabled <% } %>>
				Defend!
			</button>
		</h2>
		<form id="def" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
			<%
				String testCode;
				String previousTestCode = (String) request.getSession().getAttribute("previousTest");
				request.getSession().removeAttribute("previousTest");
				if (previousTestCode != null) {
					testCode = previousTestCode;
				} else
					testCode = mg.getCUT().getTestTemplate();
			%>
			<pre><textarea id="code" name="test" cols="80" rows="30"><%= testCode %></textarea></pre>
			<input type="hidden" name="formType" value="createTest">
			<input type="hidden" name="mpGameID" value="<%= mg.getId() %>" />
		</form>
	</div> <!-- col-md6 right top -->
</div> <!-- row-fluid 1 -->

</div>
<div class="crow fly up">
	<%@include file="/jsp/multiplayer/game_mutants.jsp"%>
	<%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
</div>
<div>
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

		$('#finishedModal').modal('show');
	</script>


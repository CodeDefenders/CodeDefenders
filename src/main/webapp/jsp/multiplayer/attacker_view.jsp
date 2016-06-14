
<div class="row-fluid">


	<%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
	<%@include file="/jsp/multiplayer/game_mutants.jsp"%>

	<div class="col-md-6" id="newmut-div">
		<form id="atk" action="/multiplayer/move" method="post">
			<h2>Create a mutant here
				<button type="submit" class="btn btn-primary btn-game btn-right" form="atk">Attack!</button>
			</h2>
			<input type="hidden" name="formType" value="createMutant">
			<input type="hidden" name="mpGameID" value="<%= mg.getId() %>" />
			<%
				String mutantCode;
				String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
				request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
				if (previousMutantCode != null) {
					mutantCode = previousMutantCode;
				} else
					mutantCode = mg.getCUT().getAsString();
			%>
			<pre><textarea id="code" name="mutant" cols="80" rows="50"><%= mutantCode %></textarea></pre>
		</form>
	</div> <!-- col-md6 newmut -->
</div> <!-- row-fluid -->

<script>
	var editor = CodeMirror.fromTextArea(document.getElementById("code"), {
		lineNumbers: true,
		indentUnit: 4,
		indentWithTabs: true,
		matchBrackets: true,
		mode: "text/x-java"
	});
	editor.setSize("100%", 500);

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
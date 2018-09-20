<%-- Set request attributes for the components. --%>
<%
	codeDivName = "cut-div"; // TODO codeDivName as request attribute where needed

	/* classCode */
	request.setAttribute("classCode", game.getCUT().getAsString());

	/* testCode */
	String previousTestCode = (String) request.getSession().getAttribute("previousTest");
	request.getSession().removeAttribute("previousTest");
	if (previousTestCode != null) {
		request.setAttribute("testCode", previousTestCode);
	} else {
		request.setAttribute("testCode", game.getCUT().getTestTemplate());
	}

	/* mockingEnabled */
	request.setAttribute("mockingEnabled", game.getCUT().isMockingEnabled());

	/* gameId */
	request.setAttribute("gameId", game.getId());
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
	<%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
</div>

<div>
	<script>
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
			var codeMirrorContainer = $(this).find(".CodeMirror")[0]; // TODO class .modal is to unspecific could also influence other modals
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

<% request.getSession().removeAttribute("lastTest"); %>


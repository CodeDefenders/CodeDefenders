<%
	codeDivName = "cut-div"; // TODO codeDivName as request attribute where needed
	request.setAttribute("game", game);
%>

<div class="ws-12">
	<div class="col-md-6" id="cut-div">
		<%@include file="/jsp/game_components/class_viewer.jsp"%>
		<%@include file="/jsp/multiplayer/game_key.jsp"%>
	</div>
	<div class="col-md-6" id="utest-div" style="float: right; min-width: 480px">
		<%@include file="/jsp/game_components/test_progress_bar.jsp"%>
        <%@include file="/jsp/game_components/test_editor.jsp"%>
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
	<script type="text/javascript">

</script>

<% request.getSession().removeAttribute("lastTest"); %>


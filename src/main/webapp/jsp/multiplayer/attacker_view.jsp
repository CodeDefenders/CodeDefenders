<%@ page import="org.codedefenders.duel.DuelGame" %>
<%@ page import="org.codedefenders.Role" %>
<%@ page import="org.codedefenders.Mutant" %>
<%@ page import="org.codedefenders.GameLevel" %>
<%@ page import="org.codedefenders.Constants" %>

<%
// Not sure where those variables come from...
boolean disableAttack = false;
if (role == Role.ATTACKER && mutantsPending != null ){

	boolean playerHasPendingMutants = false;
	for (Mutant m : mutantsPending){
        if (m.getPlayerId() == playerId &&  m.getEquivalent() == Mutant.Equivalence.PENDING_TEST){
			playerHasPendingMutants = true;
            break;
        }
    }

	disableAttack = ( playerHasPendingMutants ) &&
					( session.getAttribute( Constants.BLOCK_ATTACKER ) != null ) && // This should be superflous since we always set this session attribute
					((Boolean) session.getAttribute( Constants.BLOCK_ATTACKER ) );
}
%>
<% codeDivName = "newmut-div"; %>

<div class="crow">
	<div class="w-45 up">
	<%@include file="/jsp/multiplayer/game_mutants.jsp"%>
	<%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
	</div>
	<div class="w-55" id="newmut-div">
		<h2 style=" margin-bottom: 0">Create a mutant here</h2>
		<form id="reset" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
			<input type="hidden" name="formType" value="reset">
			<button class="btn btn-primary btn-warning btn-game btn-right " style="margin-top: -30px; margin-right: 80px">
			Reset
			</button>
		</form>
		<form id="atk" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
			<button type="submit" class="btn btn-primary btn-game btn-right" form="atk" onClick="this.form.submit(); this.disabled=true; this.value='Attacking...';"
					<% if (!mg.getState().equals(GameState.ACTIVE) || disableAttack ) { %> disabled <% } %>
					style="margin-top: -50px">
				Attack!
			</button>
			<input type="hidden" name="formType" value="createMutant">
			<input type="hidden" name="mpGameID" value="<%= mg.getId() %>" />
			<%
				String mutantCode;
				String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
				// request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
				if (previousMutantCode != null) {
					mutantCode = previousMutantCode;
				} else
					mutantCode = mg.getCUT().getAsString();
			%>
			<pre style=" margin-top: 20px; "><textarea id="code" name="mutant" cols="80" rows="50" style="min-width: 512px;"><%= mutantCode %></textarea></pre>
		</form>
		<script>
			var editorSUT = CodeMirror.fromTextArea(document.getElementById("code"), {
				lineNumbers: true,
				indentUnit: 4,
				indentWithTabs: true,
				matchBrackets: true,
				mode: "text/x-java"
			});
			editorSUT.setSize("100%", 500);

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
			    try {
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
                } catch (e){}
			});

			$('#finishedModal').modal('show');
		</script>
	</div> <!-- col-md6 newmut -->
</div>
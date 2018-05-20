<%@ page import="org.codedefenders.game.duel.DuelGame" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.codedefenders.validation.CodeValidator" %>

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
<%CodeValidator.CodeValidatorLevel validatorLevel = mg.getMutantValidatorLevel();
	String level = validatorLevel.toString().toLowerCase();
String levelStyling = validatorLevel.equals(CodeValidator.CodeValidatorLevel.RELAXED) ? "btn-success" :
		(validatorLevel.equals(CodeValidator.CodeValidatorLevel.MODERATE) ? "btn-warning" : "btn-danger");%>

<div class="crow">
	<div class="w-45 up">
	<%@include file="/jsp/multiplayer/game_mutants.jsp"%>
	<%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
	</div>
	<div class="w-55" id="newmut-div">
		<h2 style=" margin-bottom: 0">Create a mutant here</h2>

		<form id="reset" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
			<input type="hidden" name="formType" value="reset">
			<button class="btn btn-primary btn-warning btn-game btn-right" id="btnReset" style="margin-top: -40px; margin-right: 80px">
			Reset
			</button>
		</form>
		<form id="atk" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
			<button type="submit" class="btn btn-primary btn-game btn-right" id="submitMutant" form="atk" onClick="progressBar(); this.form.submit(); this.disabled=true; this.value='Attacking...';"
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

			<pre style=" margin-top: 10px; "><textarea id="code" name="mutant" cols="80" rows="50" style="min-width: 512px;"><%= mutantCode %></textarea></pre>

			<%@include file="/jsp/multiplayer/game_key.jsp"%>

			<div style = "float:right">
				<div style="display: inline-block;"> Mutant restrictions:</div>
				<div data-toggle="collapse" href="#validatorExplanation"
					 title="Click the question sign for more information on the levels"
					 class="<%="validatorLevelTag btn " + levelStyling%>">
					<%=level.substring(0, 1).toUpperCase() + level.substring(1)%>
				</div>
				<div style="display: inline-block;">
					<a data-toggle="collapse" href="#validatorExplanation" style="color:black">
						<span class="glyphicon glyphicon-question-sign"></span>
					</a>
				</div>
			</div>
			<div id="validatorExplanation" class="collapse panel panel-default" style="margin:auto; margin-top: 50px; max-width: 50%;">
				<%@ include file="/jsp/validator_explanation.jsp" %>
			</div>

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


            var updateProgressBar = function(url) {
                var progressBarDiv = document.getElementById("progress-bar");
                $.get(url, function (r) {
                        $(r).each(function (index) {
                            switch( r[index] ){
                                case 'COMPILE_MUTANT': // After test is compiled
                                    progressBarDiv.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 66%; font-size: 15px; line-height: 40px;" aria-valuenow="66" aria-valuemin="0" aria-valuemax="100">Running first Test Against Mutant</div>';
                                    break;
                                case "TEST_MUTANT": // After testing original
                                    progressBarDiv.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 90%; font-size: 15px; line-height: 40px;" aria-valuenow="90" aria-valuemin="0" aria-valuemax="100">Running more Tests Against Mutant</div>';
                                    break;
                            }
                        });
                    }
                );
            };

            function progressBar(){

                // Create the Div to host events if that's not there
                if( document.getElementById("progress-bar") == null ){
                    // Load the progress bar
                    var progressBar = document.createElement('div');
                    progressBar.setAttribute('class','progress');
                    progressBar.setAttribute('id','progress-bar');
                    progressBar.setAttribute('style','height: 40px; font-size: 30px');
                    //
                    progressBar.innerHTML='<div class="progress-bar bg-danger" role="progressbar" style="width: 33%; font-size: 15px; line-height: 40px;" aria-valuenow="33" aria-valuemin="0" aria-valuemax="100">Validating and Compiling Mutant</div>';
                    var form = document.getElementById('logout');
                    // Insert progress bar right under logout... this will conflicts with the other push-events
                    form.parentNode.insertBefore(progressBar, form.nextSibling);
                }
                // Do a first request right away, such that compilation of this test is hopefully not yet started. This one will set the session...
                var updateURL = "<%= request.getContextPath()%>" +
                    "/game_notifications?progressBar=1&userId=" + <%=uid%> +"&gameId=" + <%=gameId%>;
                updateProgressBar(updateURL);

                // Register the requests to start in 1 sec
                var interval = 1000;
                setInterval(function () {
                    updateProgressBar(updateURL);
                }, interval)
            }
		</script>
		<% request.getSession().removeAttribute("lastMutant"); %>
	</div> <!-- col-md6 newmut -->
</div>
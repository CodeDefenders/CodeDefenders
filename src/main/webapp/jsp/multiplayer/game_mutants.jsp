<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.GameLevel" %>

<%
	/* mutants_list */
	request.setAttribute("mutantsAlive", game.getAliveMutants());
	request.setAttribute("mutantsKilled", game.getKilledMutants());
	request.setAttribute("mutantsEquivalent", game.getMutantsMarkedEquivalent());
	request.setAttribute("markEquivalent", role == Role.DEFENDER);
	request.setAttribute("markUncoveredEquivalent", game.isMarkUncovered());
	request.setAttribute("viewDiff", game.getLevel() == GameLevel.EASY || role == Role.ATTACKER);
	request.setAttribute("gameType", "PARTY");
%>

<div class="ws-12 up" id="mutants-div">
	<%
if (role == Role.ATTACKER) {
    Mutant equiv = null;

    for (Mutant m : mutantsPending){
        if (m.getPlayerId() == playerId &&  m.getEquivalent() == Mutant.Equivalence.PENDING_TEST){
            renderMutants = false;
            equiv = m;

            break;
        }
    }

    if (equiv == null){
        renderMutants = true;
    }

    if (!renderMutants){

        %><div>
			<h3>Mutant <%=equiv.getId() %> Claimed Equivalent</h3>
			<div class="nest crow fly" style="border: 5px dashed #f00; border-radius: 10px; width: 100%;">
				<form id="equivalenceForm" action="<%=request.getContextPath() + "/" + game.getClass().getSimpleName().toLowerCase()%>" method="post">
					<input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= equiv.getId() %>">
					<input type="hidden" name="formType" value="resolveEquivalence">
					<%
									String mutTestCode;
									String mutPreviousTestCode = (String) request.getSession().getAttribute("previousTest");
									request.getSession().removeAttribute("previousTest");
									if (mutPreviousTestCode != null) {
										mutTestCode = mutPreviousTestCode;
									} else {
										String equivText = "// " + equiv.getPatchString().replace("\n", "\n// ").trim() + "\n";
										mutTestCode = equivText + game.getCUT().getTestTemplate();
									}
					%>
					<pre><textarea id="mutantSut" name="test" cols="80" rows="30"><%= mutTestCode %></textarea></pre>
							<!--btn-danger-->
							<a onclick="return confirm('Accepting Equivalence will lose all mutant points. Are you sure?');" href="<%=request.getContextPath() %>/multiplayer/play?acceptEquiv=<%= equiv.getId() %>"><button type="button" class="btn btn-danger btn-left">Accept Equivalence</button></a>
							<button form="equivalenceForm" class="btn btn-primary btn-game btn-right" name="rejectEquivalent" type="submit">Submit Killing Test</button>
					<div>
						<p>Note: If the game finishes with this equivalence unsolved, you will lose points!</p>
					</div>

				</form>
			</div>
			<div class="w4"><p><%= equiv.getHTMLReadout() %></p></div>
			<script>
				var mutantDuel = CodeMirror.fromTextArea(document.getElementById("mutantSut"), {
					lineNumbers: true,
					indentUnit: 4,
					indentWithTabs: true,
					matchBrackets: true,
					mode: "text/x-java"
				});
				mutantDuel.setSize("100%", 500);
			</script>
		</div>
        <%
    }

 }

 if (renderMutants) { %>
    <h3>Existing Mutants</h3>
    <%@include file="../game_components/mutants_list.jsp"%>
<% } %>

	</div> <!-- col-md6 mutants -->

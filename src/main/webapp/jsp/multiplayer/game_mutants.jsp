<%@ page import="org.codedefenders.game.duel.DuelGame" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.Collections" %>

<div class="ws-12 up" id="mutants-div">
	<%
if (role == Role.ATTACKER && true){
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
			<h2>Mutant <%=equiv.getId() %> Claimed Equivalent</h2>
			<div class="nest crow fly" style="border: 5px dashed #f00; border-radius: 10px; width: 100%;">
				<form id="equivalenceForm" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
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
										mutTestCode = equivText + mg.getCUT().getTestTemplate();
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
	<!-- Nav tabs -->
		<h2>Existing Mutants</h2>
		<div class="tabs bg-minus-3" role="tablist">
			<div class="crow fly no-gutter down">

				<div>
					<a class="tab-link button text-black" href="#mutalivetab" role="tab" data-toggle="tab">Alive (<%= mutantsAlive.size() %>)</a>
				</div>
				<div>
					<a class="tab-link button text-black" href="#mutkilledtab" role="tab" data-toggle="tab">Killed(<%= mutantsKilled.size() %>)</a>
				</div>
				<div>
					<a class="tab-link button text-black" href="#mutequivtab" role="tab" data-toggle="tab">Equivalent(<%= mutantsEquiv.size() %>)</a>
				</div>
			</div>
			<div class="tab-content">
				<div class="tab-pane fade active in" id="mutalivetab">
				<% if (! mutantsAlive.isEmpty()) { %>
				<table id="alive-mutants" class="mutant-table display dataTable table table-hover table-responsive table-paragraphs bg-white">
					<thead>  <!-- needed for datatable apparently -->
						<tr>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</tr>
						</thead>
						<tbody>
<%
						// Sorting mutants
						List<Mutant> sortedMutants = new ArrayList<Mutant>( mutantsAlive );
								Collections.sort( sortedMutants, Mutant.sortByLineNumberAscending());
%>
						<% for (Mutant m : sortedMutants) { %>
							<tr>
								<% User creator = DatabaseAccess.getUserFromPlayer(m.getPlayerId()); %>
								<td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= creator.getUsername() %> [UID: <%= creator.getId() %>]</h4>
									<% for (String change : m.getHTMLReadout()) { %>
									<p><%=change%><p>
									<% } %></td>
								<td class="col-sm-1"></td>
								<td class="col-sm-1">
									<h4>points: <%=m.getScore()%></h4>
								</td>
								<td class="col-sm-1">
									<% if (role.equals(Role.DEFENDER)
											&& m.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
											&& !mg.getState().equals(GameState.FINISHED)
											&& (m.isCovered() || mg.isMarkUncovered())){
											if( m.getLines().size() > 1 ){%>
											<a href="<%=request.getContextPath() %>/multiplayer/play?equivLines=<%=m.getLines().toString().replaceAll(", ", ",")%>"
											 class="btn btn-default btn-diff"
											 onclick="return confirm('This will mark all mutants on lines <%=m.getLines()%> as equivalent. Are you sure?');">
												Claim Equivalent</a>
										<% } else { %>
										<a href="<%=request.getContextPath() %>/multiplayer/play?equivLine=<%=m.getLines().get(0)%>"
										 class="btn btn-default btn-diff"
										 onclick="return confirm('This will mark all mutants on line <%=m.getLines().get(0)%> as equivalent. Are you sure?');">
											Claim Equivalent</a>
									<% 		}
										}
									if (m.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)){
										%><span>Flagged Equivalent</span><%
									}%>
									<% if (role.equals(Role.ATTACKER) || role.equals(Role.CREATOR) || mg.getLevel().equals(GameLevel.EASY) || mg.getState() == GameState.FINISHED){ %>
										<a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
									<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog"
										 style="z-index: 10000;">
										<div class="modal-dialog">
											<!-- Modal content-->
											<div class="modal-content">
												<div class="modal-header">
													<button type="button" class="close" data-dismiss="modal">&times;</button>
													<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
												</div>
												<div class="modal-body">
													<pre class="readonly-pre"><textarea class="mutdiff" id="diff<%=m.getId()%>"><%=m.getPatchString()%></textarea></pre>
												</div>
												<div class="modal-footer">
													<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
												</div>
											</div>
										</div>
									</div>
									<% } %>
								</td>
							</tr>
						<% } %>
						</tbody>
				</table>
					<% } else {%>
					<div class="panel panel-default" style="background: white">
						<div class="panel-body" style="    color: gray;    text-align: center;">
							No mutants alive.
						</div>
					</div>
					<% } %>


				</div>
				<div class="tab-pane fade" id="mutkilledtab">
					<% if (! mutantsKilled.isEmpty()) { %>
					<table id="killed-mutants" class="mutant-table display dataTable table table-hover table-responsive table-paragraphs bg-white">
					<thead>  <!-- needed for datatable apparently -->
						<tr>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</tr>
						</thead>

						<tbody>
<%
						//Sorting mutants
						List<Mutant> sortedKilledMutants = new ArrayList<Mutant>( mutantsKilled );
						Collections.sort( sortedKilledMutants, Mutant.sortByLineNumberAscending());
						for (Mutant m : sortedKilledMutants) { 
%>
						<tr>
							<% User creator = DatabaseAccess.getUserFromPlayer(m.getPlayerId()); %>
							<td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= creator.getUsername() %> [UID: <%= creator.getId() %>]</h4>
								<% for (String change : m.getHTMLReadout()) { %>
								<p><%=change%><p>
								<% } %></td>
							<td class="col-sm-1"></td>
							<td class="col-sm-1">
								<h4>points: <%=m.getScore()%></h4>
							</td>
							<td class="col-sm-1">
								<a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
								<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog"
									 style="z-index: 10000;">
									<div class="modal-dialog">
										<!-- Modal content-->
										<div class="modal-content">
											<div class="modal-header">
												<button type="button" class="close" data-dismiss="modal">&times;</button>
												<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
											</div>
											<div class="modal-body">
												<p>Killed by Test <%= DatabaseAccess.getKillingTestIdForMutant(m.getId()) %></p>
												<pre class="readonly-pre"><textarea class="mutdiff" id="diff<%=m.getId()%>"><%=m.getPatchString()%></textarea></pre>
											</div>
											<div class="modal-footer">
												<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
											</div>
										</div>
									</div>
								</div>
							</td>
						</tr>
						<% } %>
						</tbody>
					</table>
						<% } else {%>
						<div class="panel panel-default" style="background: white">
							<div class="panel-body" style="    color: gray;    text-align: center;">
								No mutants killed.
							</div>
						</div>
						<%}
						%>
				</div>
				<div class="tab-pane fade" id="mutequivtab">
					<% if (! mutantsEquiv.isEmpty()) { %>
					<table id="equiv-mutants" class="mutant-table display dataTable table table-hover table-responsive table-paragraphs bg-white">

					<thead>  <!-- needed for datatable apparently -->
						<tr>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</tr>
						</thead>

						<tbody>
<%
						//Sorting mutants
						List<Mutant> sortedMutantsEquiv = new ArrayList<Mutant>( mutantsEquiv );
						Collections.sort( sortedMutantsEquiv, Mutant.sortByLineNumberAscending());
						for (Mutant m : sortedMutantsEquiv) { 
%>
						<tr>
							<% User creator = DatabaseAccess.getUserFromPlayer(m.getPlayerId()); %>
							<td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= creator.getUsername() %> [UID: <%= creator.getId() %>]</h4>
								<% for (String change : m.getHTMLReadout()) { %>
								<p><%=change%><p>
								<% } %></td>
							<td class="col-sm-1"></td>
							<td class="col-sm-1">
								<h4>points: <%=m.getScore()%></h4>
							</td>
							<td class="col-sm-1">
								<a href="#" class="btn btn-default btn-diff" id="btnMut<%=m.getId()%>" data-toggle="modal" data-target="#modalMut<%=m.getId()%>">View Diff</a>
								<div id="modalMut<%=m.getId()%>" class="modal fade" role="dialog"
									 style="z-index: 10000;">
									<div class="modal-dialog">
										<!-- Modal content-->
										<div class="modal-content">
											<div class="modal-header">
												<button type="button" class="close" data-dismiss="modal">&times;</button>
												<h4 class="modal-title">Mutant <%=m.getId()%> - Diff</h4>
											</div>
											<div class="modal-body">
												<pre class="readonly-pre"><textarea class="mutdiff" id="diff<%=m.getId()%>"><%=m.getPatchString()%></textarea></pre>
											</div>
											<div class="modal-footer">
												<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
											</div>
										</div>
									</div>
								</div>
							</td>
						</tr>
						<% } %>
						</tbody>
					</table>
					<% } else {%>
					<div class="panel panel-default" style="background: white">
						<div class="panel-body" style="    color: gray;    text-align: center;">
							No mutants equivalent.
						</div>
					</div>
					<%
						}
					%>
				</div>
			</div>
		</div> <!-- tab-content -->


	<% } %>

	</div> <!-- col-md6 mutants -->

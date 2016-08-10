<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.Role" %>
<%@ page import="org.codedefenders.Mutant" %>
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
			<h2>Equivalent Mutant Claimed</h2>
			<div class="nest crow fly" style="border: 5px dashed #f00; border-radius: 10px; width: 100%;">
				<form id="equivalenceForm" action="multiplayer/move" method="post">
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
					<div class="crow fly">
						<div>
							<a class="btn btn-primary" href="multiplayer/play?acceptEquiv=<%= equiv.getId() %>">Accept Equivalence</a>
						</div>
						<div>
							<div class="btn-right">
								<input class="btn btn-primary" name="rejectEquivalent" type="submit" value="Submit Killing Test">
							</div>
						</div>
					</div>
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
		<div class="tabs bg-grey bg-minus-3" role="tablist">
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
			<div class="tab-content bg-grey">
				<div class="tab-pane fade active in" id="mutalivetab">
					<table id="alive-mutants" class="mutant-table display dataTable table table-hover table-responsive table-paragraphs bg-white">
						<% if (! mutantsAlive.isEmpty()) { %>
						<thead>  <!-- needed for datatable apparently -->
						<tr>
							<th></th>
							<th></th>
						</tr>
						</thead>
						<tbody>
						<% for (Mutant m : mutantsAlive) { %>
							<tr>
								<% User creator = DatabaseAccess.getUserFromPlayer(m.getPlayerId()); %>
								<td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= creator.getUsername() %> [UID: <%= creator.getId() %>]</h4>
									<% for (String change : m.getHTMLReadout()) { %>
									<p><%=change%><p>
									<% } %></td>
								<td class="col-sm-1">
									<% if (role.equals(Role.DEFENDER) && m.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)){ %>
										<a href="multiplayer/play?equivLine=<%=m.getLines().get(0)%>"
										 class="btn btn-default btn-diff"
										 onclick="return confirm('This will mark all mutants on line <%=m.getLines().get(0)%> as equivalent. Are you sure?');">
											Claim Equivalent</a>
									<% }
									if (m.getEquivalent().equals(Mutant.Equivalence.PENDING_TEST)){
										%><span>Flagged Equivalent</span><%
									}%>
									<% if (role.equals(Role.ATTACKER) || role.equals(Role.CREATOR) || mg.getLevel().equals(Game.Level.EASY)){ %>
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
						<% } else {%>
						<tr>
							<td class="col-sm-1" colspan="2">No mutants alive.</td>
						</tr>
						<% } %>
					</table>

				</div>
				<div class="tab-pane fade  bg-grey" id="mutkilledtab">
					<table id="killed-mutants" class="mutant-table display dataTable table table-hover table-responsive table-paragraphs bg-white">
						<% if (! mutantsKilled.isEmpty()) { %>
						<thead>  <!-- needed for datatable apparently -->
						<tr>
							<th></th>
							<th></th>
						</tr>
						</thead>

						<tbody>
						<%	for (Mutant m : mutantsKilled) { %>
						<tr>
							<% User creator = DatabaseAccess.getUserFromPlayer(m.getPlayerId()); %>
							<td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= creator.getUsername() %> [UID: <%= creator.getId() %>]</h4>
								<% for (String change : m.getHTMLReadout()) { %>
								<p><%=change%><p>
								<% } %></td>
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
						<% } else {%>
						<tr>
							<td  class="col-sm-1" colspan="2">No mutants killed.</td>
						</tr>
						<%}
						%>
					</table>
				</div>
				<div class="tab-pane fade  bg-grey" id="mutequivtab">
					<table id="equiv-mutants" class="mutant-table display dataTable table table-hover table-responsive table-paragraphs bg-white">
						<% if (! mutantsEquiv.isEmpty()) { %>
						<thead>  <!-- needed for datatable apparently -->
						<tr>
							<th></th>
							<th></th>
						</tr>
						</thead>

						<tbody>
						<%	for (Mutant m : mutantsEquiv) { %>
						<tr>
							<% User creator = DatabaseAccess.getUserFromPlayer(m.getPlayerId()); %>
							<td class="col-sm-1"><h4>Mutant <%= m.getId() %> | Creator: <%= creator.getUsername() %> [UID: <%= creator.getId() %>]</h4>
								<% for (String change : m.getHTMLReadout()) { %>
								<p><%=change%><p>
								<% } %></td>
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
						<% } else {%>
						<tr>
							<td class="col-sm-1">No mutants equivalent.</td>
						</tr>
						<%}
						%>
					</table>
				</div>
			</div>
		</div> <!-- tab-content -->


	<% } %>

	</div> <!-- col-md6 mutants -->

<%@ page import="org.codedefenders.Game" %>
<%@ page import="org.codedefenders.Role" %>
<div class="col-md-6" id="mutants-div">
		<h2>Mutants</h2>
<%
if (role == Role.ATTACKER && true){
    int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(uid, gameId);

    MultiplayerMutant equiv = null;

    for (MultiplayerMutant m : mutantsEquiv){
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

        %><h3>Equivalent Mutant Claimed</h3>
        <div class="w4"><p><%= equiv.getHTMLReadout() %></p></div>
        <div class="nest crow fly" style="border: 5px dashed #f00; border-radius: 10px; width: 100%;">
        <div class="w8"><pre class="readonly-pre"><textarea class="utest" cols="20" rows="10"><%= equiv.getPatchString() %></textarea></pre>
        <a href="multiplayer/play?acceptEquiv=<%= equiv.getId() %>">Accept Equivalence</a></div>
        <form id="equivalenceForm" action="multiplayer/move" method="post">
        <input form="equivalenceForm" type="hidden" id="currentEquivMutant" name="currentEquivMutant" value="<%= equiv.getId() %>">
        <input type="hidden" name="formType" value="resolveEquivalence">
        <%
        				String mutTestCode;
        				String mutPreviousTestCode = (String) request.getSession().getAttribute("previousTest");
        				request.getSession().removeAttribute("previousTest");
        				if (mutPreviousTestCode != null) {
        					mutTestCode = mutPreviousTestCode;
        				} else
        					mutTestCode = mg.getCUT().getTestTemplate();
        					%>
        <pre><textarea id="mutantSut" name="test" cols="80" rows="30"><%= mutTestCode %></textarea></pre>
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
        <div class="btn-right">
            <input class="btn btn-primary" name="rejectEquivalent" type="submit" value="Submit Killing Test">							</div>
        </form>
        <%
    }

 }

 if (renderMutants) { %>
		<!-- Nav tabs -->
		<ul class="nav nav-tabs" role="tablist">
			<li class="active">
				<a href="#mutalivetab" role="tab" data-toggle="tab">Alive</a>
			</li>
			<li>
				<a href="#mutkilledtab" role="tab" data-toggle="tab">Killed</a>
			</li>
            <li>
                <a href="#mutequivtab" role="tab" data-toggle="tab">Equivalent</a>
            </li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane fade active in" id="mutalivetab">
				<table class="table table-hover table-responsive table-paragraphs">
					<%
					if (! mutantsAlive.isEmpty()) {
						for (MultiplayerMutant m : mutantsAlive) {
					%>
					<tr>
						<td><h4>Mutant <%= m.getId() %></h4></td>
						<td>
							<% if (role.equals(Role.ATTACKER) || role.equals(Role.CREATOR) || mg.getLevel().equals(Game.Level.EASY)){ %>
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
											<pre class="readonly-pre"><textarea class="mutdiff" id="diff<%=m.getId()%>">
													%><%=m.getPatchString()%>
												</textarea></pre>
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
					<tr>
						<td colspan="3">
							<% for (String change :	m.getHTMLReadout()) { %>
							<p><%=change%><p>
								<% } %>
						</td>
					</tr>
					<%
						}
					} else {%>
					<tr class="blank_row">
						<td class="row-borderless" colspan="2">No mutants alive.</td>
					</tr>
					<%}
					%>
				</table>
			</div>
			<div class="tab-pane fade" id="mutkilledtab">
				<table class="table table-hover table-responsive table-paragraphs">
					<%
					if (! mutantsKilled.isEmpty()) {
						for (MultiplayerMutant m : mutantsKilled) {
					%>
					<tr>
						<td class="col-sm-1"><h4>Mutant <%= m.getId() %></h4></td>
						<td class="col-sm-1">
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
					<tr>
						<td class="col-sm-1" colspan="3">
							<% for (String change : m.getHTMLReadout()) { %>
							<p><%=change%><p>
								<% } %>
						</td>
					</tr>
					<%
						}
					} else {%>
					<tr class="blank_row">
						<td class="row-borderless" colspan="2">No mutants killed.</td>
					</tr>
					<%}
					%>
				</table>
			</div>
            <div class="tab-pane fade" id="mutequivtab">
				<table class="table table-hover table-responsive table-paragraphs">
					<%
					if (! mutantsEquiv.isEmpty()) {
						for (MultiplayerMutant m : mutantsEquiv) {
					%>
					<tr>
						<td class="col-sm-1"><h4>Mutant <%= m.getId() %></h4></td>
						<td class="col-sm-1">
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
					<tr>
						<td class="col-sm-1" colspan="3">
							<% for (String change : m.getHTMLReadout()) { %>
							<div class="nest crow fly">
							<p><%=change%></p>
							<p><%=m.getEquivalent() %></p>
							</div<
								<% } %>
						</td>
					</tr>
					<%
						}
					} else {%>
					<tr class="blank_row">
						<td class="row-borderless" colspan="2">No mutants equivalent.</td>
					</tr>
					<%}
					%>
				</table>
			</div>
		</div> <!-- tab-content -->

	<% } %>

	</div> <!-- col-md6 mutants -->

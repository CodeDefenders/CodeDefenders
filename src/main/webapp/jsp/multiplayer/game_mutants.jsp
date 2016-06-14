<div class="col-md-6" id="mutants-div">
		<h2>Mutants</h2>
		<!-- Nav tabs -->
		<ul class="nav nav-tabs" role="tablist">
			<li class="active">
				<a href="#mutalivetab" role="tab" data-toggle="tab">Alive</a>
			</li>
			<li>
				<a href="#mutkilledtab" role="tab" data-toggle="tab">Killed</a>
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
		</div> <!-- tab-content -->
	</div> <!-- col-md6 mutants -->
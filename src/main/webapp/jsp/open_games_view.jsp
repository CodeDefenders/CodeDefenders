<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.duel.DuelGame" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle="Open Games"; %>
<%@ include file="/jsp/header.jsp" %>
<div class="full-width">
	<h3>Duels</h3>
<table class="table table-hover table-responsive table-paragraphs games-table">
	<tr>
		<th class="col-sm-1">ID</th>
		<th class="col-sm-2">Class</th>
		<th class="col-sm-2">Attacker</th>
		<th class="col-sm-2">Defender</th>
		<th class="col-sm-1">Level</th>
		<th class="col-sm-2">Action</th>
	</tr>


	<%
		boolean isGames = false;
		String atkName;
		String defName;
		int uid = (Integer)request.getSession().getAttribute("uid");
		int atkId;
		int defId;
		for (DuelGame g : DatabaseAccess.getOpenGames()) {
			isGames = true;
			atkName = null;
			defName = null;

			// Single or UTesting games cannot be joined
			if (g.getMode().equals(GameMode.SINGLE) ||
					g.getMode().equals(GameMode.UTESTING)) {continue;}

			atkId = g.getAttackerId();
			defId = g.getDefenderId();

			// User is already playing this game
			if ((atkId == uid)||(defId == uid)) {continue;}

			if (atkId != 0) {atkName = DatabaseAccess.getUserForKey("User_ID", atkId).getUsername();}
			if (defId != 0) {defName = DatabaseAccess.getUserForKey("User_ID", defId).getUsername();}

			if ((atkName != null)&&(defName != null)) {continue;}

			if (atkName == null) {atkName = "Empty";}
			if (defName == null) {defName = "Empty";}
	%>

	<tr>
		<td class="col-sm-1"><%= g.getId() %></td>
		<td class="col-sm-2">
			<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=g.getId()%>">
				<%=g.getCUT().getAlias()%>
			</a>
			<div id="modalCUTFor<%=g.getId()%>" class="modal fade" role="dialog" style="text-align: left;" >
				<div class="modal-dialog">
					<!-- Modal content-->
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal">&times;</button>
							<h4 class="modal-title"><%=g.getCUT().getAlias()%></h4>
						</div>
						<div class="modal-body classPreview">
							<pre class="readonly-pre"><textarea class=	"readonly-textarea" id="sut<%=g.getId()%>" name="cut<%=g.getId()%>" cols="80" rows="30"><%=g.getCUT().getAsString()%></textarea></pre>
						</div>
						<div class="modal-footer">
							<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
						</div>
					</div>
				</div>
			</div>
		</td>
		<td class="col-sm-2"><%= atkName %></td>
		<td class="col-sm-2"><%= defName %></td>
		<td class="col-sm-1"><%= g.getLevel().name() %></td>
		<td class="col-sm-2">
			<form id="view" action="games" method="post">
				<input type="hidden" name="formType" value="joinGame">
				<input type="hidden" name="game" value=<%=g.getId()%>>
				<input type="submit" class="btn btn-primary" value="Join Game">
			</form>
		</td>
	</tr>

	<%
		}
		if (!isGames) {%>
	<tr><td colspan="100%"> There are currently no open duels. </td></tr>
	<%}
	%>
</table>

	<!-- Alessio disabled this to avoid students creating stuff. Then it should be enough to set visibility to null  -->
	<!-- 
	<a href="/games/create">Create Duel</a>
	-->
	
	
	<hr />
	<h3>Battlegrounds</h3>
	<table id="tableMPGames" class="table table-hover table-responsive table-paragraphs games-table dataTable display">
		<thead>
			<tr>
				<th>ID</th>
				<th>Class</th>
				<th>Owner</th>
				<!--<th>Prize</th>-->
				<th>Attackers</th>
				<th>Defenders</th>
				<th>Level</th>
				<th>Starting</th>
				<th>Finishing</th>
				<th>Actions</th>
			</tr>
		</thead>
		<tbody>
	<%
		List<MultiplayerGame> mgames = DatabaseAccess.getOpenMultiplayerGamesForUser(uid);
		if (mgames.isEmpty()) {
	%>
		<tr><td colspan="100%"> There are currently no open multiplayer games. </td></tr>
	<%
	} else {
	%>
		<%
			for (MultiplayerGame g : mgames) {
		%>
		<tr>
			<td class="col-sm-1"><%= g.getId() %></td>
			<td class="col-sm-2">
				<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=g.getId()%>">
					<%=g.getCUT().getAlias()%>
				</a>
				<div id="modalCUTFor<%=g.getId()%>" class="modal fade" role="dialog" style="text-align: left;" >
					<div class="modal-dialog">
						<!-- Modal content-->
						<div class="modal-content">
							<div class="modal-header">
								<button type="button" class="close" data-dismiss="modal">&times;</button>
								<h4 class="modal-title"><%=g.getCUT().getAlias()%></h4>
							</div>
							<div class="modal-body">
							<pre class="readonly-pre"><textarea class=	"readonly-textarea classPreview" id="sut<%=g.getId()%>" name="cut<%=g.getId()%>" cols="80" rows="30"><%=g.getCUT().getAsString()%></textarea></pre>
							</div>
							<div class="modal-footer">
								<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
							</div>
						</div>
					</div>
				</div>
			</td>
			<td class="col-sm-1"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).getUsername() %></td>
			<!--<td class="col-sm-1"><%/*= g.getPrize() */%></td>-->
			<td class="col-sm-1"><%int attackers = g.getAttackerIds().length; %><%=attackers %> of <%=g.getMinAttackers()%>&ndash;<%=g.getAttackerLimit()%></td>
			<td class="col-sm-1"><%int defenders = g.getDefenderIds().length; %><%=defenders %> of <%=g.getMinDefenders()%>&ndash;<%=g.getDefenderLimit()%></td>
			<td class="col-sm-1"><%= g.getLevel().name() %></td>
			<td class="col-sm-1"><%= g.getStartDateTime() %></td>
			<td class="col-sm-1"><%= g.getFinishDateTime() %></td>
			<td class="col-sm-2">
				<a href="multiplayer/games?attacker=1&id=<%= g.getId() %>">Join as Attacker</a><br>
				<a href="multiplayer/games?defender=1&id=<%= g.getId() %>">Join as Defender</a>
			</td>
		</tr>
		<%
				} // for (MultiplayerGame g : games) %>
	 <% } // if (games.isEmpty())
		%>
		</tbody>
	</table>

	<!-- Alessio disabled this -->
	<!-- 
	<a href="/multiplayer/games/create">Create Battleground</a>
	-->

	<script>
		$(document).ready(function() {
			$.fn.dataTable.moment( 'DD/MM/YY HH:mm' );
			$('#tableMPGames').DataTable( {
				"paging":   false,
				"searching": false,
				"order": [[ 6, "asc" ]],
				"language": {
					"info": ""
				}
			} );
		} );
		$('.modal').on('shown.bs.modal', function() {
			var codeMirrorContainer = $(this).find(".CodeMirror")[0];
			if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
				codeMirrorContainer.CodeMirror.refresh();
			} else {
				var editorDiff = CodeMirror.fromTextArea($(this).find('textarea')[0], {
					lineNumbers: false,
					readOnly: true,
					mode: "text/x-java"
				});
				editorDiff.setSize("100%", 500);
			}
		});
	</script>


</div>
<%@ include file="/jsp/footer.jsp" %>

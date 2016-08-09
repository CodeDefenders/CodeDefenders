<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.Role" %>
<%@ page import="org.codedefenders.*" %>
<% String pageTitle="My Games"; %>
<%@ include file="/jsp/header.jsp" %>
	<%
	String atkName;
	String defName;
	int uid = (Integer)request.getSession().getAttribute("uid");
	ArrayList<Game> games = DatabaseAccess.getGamesForUser(uid); %>
<div class="w-100">
	<h3>Duels</h3>
<table class="table table-hover table-responsive table-paragraphs games-table">
	<tr>
		<th class="col-sm-1">ID</th>
		<th class="col-sm-2">Class</th>
		<th class="col-sm-2">Attacker</th>
		<th class="col-sm-2">Defender</th>
		<th class="col-sm-2">Level</th>
		<th class="col-sm-2">Action</th>
	</tr>
	<%
	if (games.isEmpty()) {
%>
	<tr><td colspan="100%"> You are not currently playing any duel game. </td></tr>
<%
	} else {
		%>
	<%
		for (Game g : games) {
			atkName = null;
			defName = null;

			if (g.getAttackerId() != 0) {
				atkName = DatabaseAccess.getUserForKey("User_ID", g.getAttackerId()).getUsername();
			}

			if (g.getDefenderId() != 0) {
				defName = DatabaseAccess.getUserForKey("User_ID", g.getDefenderId()).getUsername();
			}

			int turnId = g.getAttackerId();
			if (g.getActiveRole().equals(Role.DEFENDER))
				turnId = g.getDefenderId();

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
		<td class="col-sm-2"><%= atkName %></td>
		<td class="col-sm-2"><%= defName %></td>
		<td class="col-sm-2"><%= g.getLevel().name() %></td>
		<td class="col-sm-2">
<%
			if (g.getState().equals(AbstractGame.State.ACTIVE)) { // Can enter only if game is in progress.
				String btnLabel = "Your Turn";
				if (g.getMode().equals(Game.Mode.UTESTING)) {
					btnLabel = "Enter";
				}
%>
			<form id="view" action="games" method="post">
				<input type="hidden" name="formType" value="enterGame">
				<input type="hidden" name="game" value="<%=g.getId()%>">
				<% if (uid == turnId ) {%>
				<input class="btn btn-primary" type="submit" value="<%=btnLabel%>">
				<% } else {%>
				<input  class="btn btn-default" type="submit" value="Enter Game">
				<% }%>
			</form>

<%
			}
%>
		</td>
	</tr>
<%
		} // for (MultiplayerGame g : games)
	} // if (games.isEmpty())
%>
</table>
	<a href="/games/create">Create Duel</a>

	<hr />

	<h3>Battlegrounds</h3>
	<table id="tableMPGames" class="table table-hover table-responsive table-paragraphs games-table">

		<thead>
			<tr>
				<th>ID</th>
				<th>Class</th>
				<!--<th>Prize</th>-->
				<th>Attackers</th>
				<th>Defenders</th>
				<th>Level</th>
				<th>Starting</th>
				<th>Finishing</th>
				<th>Actions</th>
			</tr>
		</thead>
<%
	ArrayList<MultiplayerGame> mgames = DatabaseAccess.getMultiplayerGamesForUser(uid);
	if (mgames.isEmpty()) {
%>
	<tbody>
<tr><td colspan="100%"> You are not currently playing any multiplayer game. </td></tr>
<%
} else {
%>
	<%
		for (MultiplayerGame g : mgames) {
			Role role = g.getRole(uid);
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
		<!--<td class="col-sm-1"><%/*= g.getPrize() */%></td>-->
		<td class="col-sm-1"><%= g.getAttackerIds().length %></td>
		<td class="col-sm-1"><%= g.getDefenderIds().length %></td>
		<td class="col-sm-1"><%= g.getLevel().name() %></td>
		<td class="col-sm-1"><%= g.getStartDateTime()%></td>
		<td class="col-sm-1"><%= g.getFinishDateTime()%></td>
		<td class="col-sm-2">
			<%
			switch(role){
				case CREATOR:
			%>
			<a href="multiplayer/games?id=<%= g.getId() %>">Observe</a>
			<%
					break;
				case ATTACKER:
					if(!g.getState().equals(AbstractGame.State.CREATED)) {
			%>
			<a href="multiplayer/games?id=<%= g.getId() %>">Attack</a>
			<%		} else { %>
			<p>Joined as Attacker</p>
			<form id="attLeave" action="multiplayer/games" method="post">
				<input type="hidden" name="formType" value="leaveGame">
				<input type="hidden" name="game" value="<%=g.getId()%>">
				<input type="submit" form="attLeave" value="Leave">
			</form>
			<% }
					break;
				case DEFENDER:
					if(!g.getState().equals(AbstractGame.State.CREATED)) { %>
				<a href="multiplayer/games?id=<%= g.getId() %>">Defend</a>
			<% 		} else { %>
			<p>Joined as Defender</p>
			<form id="defLeave" action="multiplayer/games" method="post">
				<input type="hidden" name="formType" value="leaveGame">
				<input type="hidden" name="game" value="<%=g.getId()%>">
				<input type="submit" form="defLeave" class="leave-button" value="Leave">
			</form>
			<% }
					break;
				default:
					break;
			}
			%>
		</td>
	</tr>
	<%
			} // for (MultiplayerGame g : games)
		} // if (games.isEmpty())
	%>
	</tbody>
</table>
	<a href="/multiplayer/games/create">Create Battleground</a>

	<script>
		$(document).ready(function() {
			$.fn.dataTable.moment( 'DD/MM/YY HH:mm' );
			$('#tableMPGames').DataTable( {
				"paging":   false,
				"searching": false,
				"order": [[ 5, "asc" ]],
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
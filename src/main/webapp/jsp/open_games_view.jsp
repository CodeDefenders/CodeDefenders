<%@ page import="org.codedefenders.*" %>
<% String pageTitle="Open Games"; %>
<%@ include file="/jsp/header.jsp" %>
<div class="full-width">
	<h2>Duels</h2>
<table class="table table-hover table-responsive table-paragraphs">
	<tr>
		<th class="col-sm-2">Game No.</th>
		<th class="col-sm-2">Attacker</th>
		<th class="col-sm-2">Defender</th>
		<th class="col-sm-2">Game State</th>
		<th class="col-sm-2">Class Under Test</th>
		<th class="col-sm-2">Level</th>
		<th class="col-sm-2"></th>
	</tr>


	<%
		boolean isGames = false;
		String atkName;
		String defName;
		int uid = (Integer)request.getSession().getAttribute("uid");
		int atkId;
		int defId;
		for (Game g : DatabaseAccess.getOpenGames()) {
			isGames = true;
			atkName = null;
			defName = null;

			// Single or UTesting games cannot be joined
			if (g.getMode().equals(AbstractGame.Mode.SINGLE) ||
					g.getMode().equals(AbstractGame.Mode.UTESTING)) {continue;}

			atkId = g.getAttackerId();
			defId = g.getDefenderId();

			// User is already playing this game
			if ((atkId == uid)||(defId == uid)) {continue;}

			if (atkId != 0) {atkName = DatabaseAccess.getUserForKey("User_ID", atkId).username;}
			if (defId != 0) {defName = DatabaseAccess.getUserForKey("User_ID", defId).username;}

			if ((atkName != null)&&(defName != null)) {continue;}

			if (atkName == null) {atkName = "Empty";}
			if (defName == null) {defName = "Empty";}
	%>

	<tr>
		<td class="col-sm-2"><%= g.getId() %></td>
		<td class="col-sm-2"><%= atkName %></td>
		<td class="col-sm-2"><%= defName %></td>
		<td class="col-sm-2"><%= g.getState() %></td>
		<td class="col-sm-2"><%= g.getCUT().getAlias() %></td>
		<td class="col-sm-2"><%= g.getLevel().name() %></td>
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
	<tr><td colspan="8"> There are currently no open games </td></tr>
	<%}
	%>
</table>
	<a href="/games/create">Create Duel</a>
	<hr />
	<h2>Battlegrounds</h2>
	<table class="table table-hover table-responsive table-paragraphs"><tr><th>Game ID</th><th>Owner</th><th>Class</th><th>
		Price
	</th><th>Attackers</th><th>Defenders</th><th>Level</th><th>Actions</th></tr>
	<%
		ArrayList<MultiplayerGame> mgames = DatabaseAccess.getMultiplayerGamesExcludingUser(uid);
		if (mgames.isEmpty()) {
	%>
	<tr><td colspan="5"> There are currently no open games. </td></tr>
	<%
	} else {
	%>
		<%
			for (MultiplayerGame g : mgames) {
		%>
		<tr>
			<td class="col-sm-1"><%= g.getId() %></td>
			<td class="col-sm-1"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).username %></td>
			<td class="col-sm-1">
				<a href="#" id="btnMut<%=g.getId()%>" data-toggle="modal" data-target="#modalMut<%=g.getId()%>">
					<%=g.getCUT().getName()%>
				</a>
				<div id="modalMut<%=g.getId()%>" class="modal fade" role="dialog">
					<div class="modal-dialog">
						<!-- Modal content-->
						<div class="modal-content">
							<div class="modal-header">
								<button type="button" class="close" data-dismiss="modal">&times;</button>
								<h4 class="modal-title"><%=g.getCUT().getName()%></h4>
							</div>
							<div class="modal-body">
							<pre class="readonly-pre"><textarea class="readonly-textarea classPreview" id="sut<%=g.getId()%>" name="cut<%=g.getId()%>" cols="80" rows="30"><%=g.getCUT().getAsString()%>
							</textarea></pre>
							</div>
							<div class="modal-footer">
								<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
							</div>
						</div>
					</div>
				</div></td>
			<td class="col-sm-1"><%= g.getPrice() %></td>
			<td class="col-sm-2"><%int attackers = g.getAttackerIds().length; %><%=attackers %> of <%=g.getAttackerLimit()%>
				<% if (attackers < g.getMinAttackers()){%>
				(at least <%=g.getMinAttackers()%> required)
				<% } %></td>
			<td class="col-sm-2"><%int defenders = g.getAttackerIds().length; %><%=defenders %> of <%=g.getDefenderLimit()%>
				<% if (defenders < g.getMinDefenders()){%>
				(at least <%=g.getMinDefenders()%> required)
				<% } %></td>
			<td class="col-sm-2"><%= g.getLevel().name() %></td>
			<td class="col-sm-2">
				<a href="multiplayer/games?attacker=1&id=<%= g.getId() %>">Join as Attacker</a><br>
				<a href="multiplayer/games?defender=1&id=<%= g.getId() %>">Join as Defender</a>
			</td>
		</tr>
		<%
				} // for (MultiplayerGame g : games) %>
	 <% } // if (games.isEmpty())
		%>
	</table>

	<a href="/multiplayer/games/create">Create Battleground</a>
	<script>
		$('.modal').on('shown.bs.modal', function() {
			var codeMirrorContainer = $(this).find(".classPreview")[0];
			if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
				codeMirrorContainer.CodeMirror.refresh();
			} else {
				var editorDiff = CodeMirror.fromTextArea($(this).find('textarea')[0], {
					lineNumbers: false,
					mode: "diff",
					readOnly: true,
					mode: "text/x-java"
				});
				editorDiff.setSize("100%", 500);
			}
		});
	</script>


</div>
<%@ include file="/jsp/footer.jsp" %>

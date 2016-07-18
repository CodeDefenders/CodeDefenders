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
	<h2>Duels</h2>
<table class="table table-hover table-responsive table-paragraphs games-table">
	<tr>
		<th class="col-sm-2">Game ID</th>
		<th class="col-sm-2">Class</th>
		<th class="col-sm-2">Attacker</th>
		<th class="col-sm-2">Defender</th>
		<th class="col-sm-2">Level</th>
		<th class="col-sm-2">Action</th>
	</tr>
	<%
	if (games.isEmpty()) {
%>
	<tr><td colspan="7"> You are not currently in any games.</td></tr>
<%
	} else {
		%>
	<%
		for (Game g : games) {
			atkName = null;
			defName = null;

			if (g.getAttackerId() != 0) {
				atkName = DatabaseAccess.getUserForKey("User_ID", g.getAttackerId()).username;
			}

			if (g.getDefenderId() != 0) {
				defName = DatabaseAccess.getUserForKey("User_ID", g.getDefenderId()).username;
			}

			int turnId = g.getAttackerId();
			if (g.getActiveRole().equals(Role.DEFENDER))
				turnId = g.getDefenderId();

			if (atkName == null) {atkName = "Empty";}
			if (defName == null) {defName = "Empty";}

%>
	<tr>
		<td class="col-sm-2"><%= g.getId() %></td>
		<td class="col-sm-2"><%= g.getCUT().getAlias() %></td>
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

	<h2>Battlegrounds</h2>
	<table class="table table-hover table-responsive table-paragraphs games-table">
		<tr>
			<th>Game ID</th>
			<th>Owner</th>
			<th>Prize</th>
			<th>Attackers</th>
			<th>Defenders</th>
			<th>Level</th>
			<th>Starting</th>
			<th>Finishing</th>
			<th>Actions</th>
		</tr>
<%
	ArrayList<MultiplayerGame> mgames = DatabaseAccess.getMultiplayerGamesForUser(uid);
	if (mgames.isEmpty()) {
%>
<tr><td colspan="5"> You are not currently in any games. </td></tr>
<%
} else {
%>
	<%
		for (MultiplayerGame g : mgames) {
			Role role = g.getRole(uid);
	%>
	<tr>
		<td class="col-sm-2"><%= g.getId() %></td>
		<td class="col-sm-2"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).username %></td>
		<td class="col-sm-1"><%= g.getPrize() %></td>
		<td class="col-sm-1"><%= g.getAttackerIds().length %></td>
		<td class="col-sm-1"><%= g.getDefenderIds().length %></td>
		<td class="col-sm-1"><%= g.getLevel().name() %></td>
		<td class="col-sm-1"><%= g.getStartDateTime()%></td>
		<td class="col-sm-1"><%= g.getFinishDateTime()%></td>
		<td class="col-sm-2"><%
			switch(role){
				case ATTACKER:
					%>
			<a href="multiplayer/games?id=<%= g.getId() %>">Attack</a>
			<%
					break;
				case CREATOR:
			%>
			<a href="multiplayer/games?id=<%= g.getId() %>">Observe</a>
			<%

					break;
				case DEFENDER:
			%>
			<a href="multiplayer/games?id=<%= g.getId() %>">Defend</a>
			<%

					break;
				default:

					break;
			}

		%></td>
	</tr>
	<%
			} // for (MultiplayerGame g : games)
		} // if (games.isEmpty())
	%>
</table>
	<a href="/multiplayer/games/create">Create Battleground</a>

</div>
<%@ include file="/jsp/footer.jsp" %>
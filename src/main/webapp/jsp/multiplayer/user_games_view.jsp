<% String pageTitle="My Games"; %>
<a href="/multiplayer/games/create">Create Game</a>
<%@ include file="/jsp/header.jsp" %>
	<%
	int uid = (Integer)request.getSession().getAttribute("uid");
	ArrayList<MultiplayerGame> games = DatabaseAccess.getMultiplayerGamesForUser(uid);
	if (games.isEmpty()) {
%>
	<p> You are not currently in any games. </p>
<%
	} else {
		%>
<table><tr><th>Game ID</th><th>Owner</th><th>Coverage Goal</th><th>Mutant Coverage Goal</th><th>
	Prize
</th><th>Level</th></tr>
	<%
		for (MultiplayerGame g : games) {

%>
	<tr>
		<td class="col-sm-2"><%= g.getId() %></td>
		<td class="col-sm-2"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).getUsername() %></td>
		<td class="col-sm-2"><%= g.getLineCoverage() %></td>
		<td class="col-sm-2"><%= g.getMutantCoverage() %></td>
		<td class="col-sm-2"><%= g.getPrize() %></td>
		<td class="col-sm-2"><%= g.getLevel().name() %></td>
	</tr>
<%
		} // for (MultiplayerGame g : games)
	} // if (games.isEmpty())
%>
</table>
<%@ include file="/jsp/footer.jsp" %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.Role" %>
<%@ page import="org.codedefenders.*" %>
<% String pageTitle="Leaderboards"; %>
<%@ include file="/jsp/header.jsp" %>
	<%
		ArrayList<User> users = DatabaseAccess.getAllUsers();
	%>
<div class="w-100">
	<h3>Battlegrounds</h3>
	<table class="table table-hover table-responsive table-paragraphs games-table">
		<thead>
			<tr>
				<th class="col-sm-2">User</th>
				<th class="col-sm-2">Total Score</th>
				<th class="col-sm-2">Tests</th>
				<th class="col-sm-2">Mutants</th>
				<th class="col-sm-2">Defender Wins</th>
				<th class="col-sm-2">Attacker Wins</th>
			</tr>
		</thead>
		<tbody>
		<%
			for (User u : users) {
				int totalScore = 0;
				int numTests = 0;
				int numMutants = 0;
				int winsDef = 0;
				int winsAtt = 0;

				ArrayList<MultiplayerGame> mGames = DatabaseAccess.getMultiplayerGamesForUser(u.getId());
				for (MultiplayerGame mg : mGames) {
					int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(u.getId(), mg.getId());
					Role r = DatabaseAccess.getRole(u.getId(), mg.getId());

					totalScore += DatabaseAccess.getPlayerPoints(playerId);
					numTests += DatabaseAccess.getNumTestsForPlayer(playerId);
					numMutants += DatabaseAccess.getMutantsForPlayer(playerId).size();

					if(mg.getState().equals(AbstractGame.State.FINISHED))
					{
						if(r.equals(Role.DEFENDER) && r.equals(mg.getWinningTeam())) {
							winsDef ++;
						}
						if(r.equals(Role.ATTACKER) && r.equals(mg.getWinningTeam())) {
							winsAtt ++;
						}
					}

				}
		%>
		<tr>
			<td><%=u.getUsername()%></td>
			<td><%=totalScore%></td>
			<td><%=numTests%></td>
			<td><%=numMutants%></td>
			<td><%=winsDef%></td>
			<td><%=winsAtt%></td>
		</tr>
		<% } %>

		</tbody>
	</table>

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
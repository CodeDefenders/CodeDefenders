<%@ page import="org.codedefenders.game.leaderboard.Leaderboard" %>
<%@ page import="org.codedefenders.game.leaderboard.Entry" %>
<% String pageTitle="Leaderboard"; %>
<%@ include file="/jsp/header_main.jsp" %>
<div class="w-100">
	<h3>Battlegrounds</h3>
	<table id="tableMPLeaderboard" class="table table-hover table-responsive table-paragraphs games-table dataTable display">
		<thead>
			<tr>
				<th class="col-sm-2">User</th>
				<th class="col-sm-1">Mutants</th>
				<th class="col-sm-2">Attacker Score</th>
				<th class="col-sm-1">Tests</th>
				<th class="col-sm-2">Defender Score</th>
				<th class="col-sm-2">Mutants Killed</th>
				<th class="col-sm-2">Total Score</th>
			</tr>
		</thead>
		<tbody>
		<%
			for (Entry p : Leaderboard.getAll()) {
		%>
		<tr>
			<td><%=p.getUsername()%></td>
			<td><%=p.getMutantsSubmitted()%></td>
			<td><%=p.getAttackerScore()%></td>
			<td><%=p.getTestsSubmitted()%></td>
			<td><%=p.getDefenderScore()%></td>
			<td><%=p.getMutantsKilled()%></td>
			<td><%=p.getTotalPoints()%></td>
		</tr>
		<% } %>

		</tbody>
	</table>

	<script>
		$(document).ready(function() {
			$.fn.dataTable.moment( 'DD/MM/YY HH:mm' );
			$('#tableMPLeaderboard').DataTable( {
				"paging":   false,
				"searching": false,
				"order": [[ 6, "desc" ]],
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

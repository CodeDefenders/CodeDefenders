<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="org.codedefenders.game.leaderboard.Leaderboard" %>
<%@ page import="org.codedefenders.game.leaderboard.Entry" %>
<% String pageTitle="Leaderboard"; %>
<%@ include file="/jsp/header_main.jsp" %>
<div class="w-100">
	<h3>Battlegrounds</h3>
	<table id="tableMPLeaderboard" class="table table-striped table-hover table-responsive table-paragraphs games-table dataTable display">
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
				"order": [[ 6, "desc" ]],
				"columnDefs": [
					{ "searchable": false, "targets": [1,2,3,4,5,6] }
				],
				"pageLength": 50
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

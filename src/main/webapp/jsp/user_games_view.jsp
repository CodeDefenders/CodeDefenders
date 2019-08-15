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
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.model.UserMultiplayerGameInfo" %>
<% String pageTitle= null ; %>
<%@ include file="/jsp/header_main.jsp" %>
<%
    // Games active for this user (Created or joined)
    List<UserMultiplayerGameInfo> activeGames = ((List<UserMultiplayerGameInfo>) request.getAttribute("activeGames"));

    // Games open for this user (not created or joined, and enough space for one player)
    List<UserMultiplayerGameInfo> openGames = ((List<UserMultiplayerGameInfo>) request.getAttribute("openGames"));

    boolean gamesJoinable = (boolean) request.getAttribute("gamesJoinable");
    boolean gamesCreatable = (boolean) request.getAttribute("gamesCreatable");
%>

<div class="w-100">
<h2 class="full-width page-title">My Games</h2>
<table id="my-games" class="table table-striped table-hover table-responsive table-paragraphs games-table">
	<tr>
		<th>ID</th>
		<th>Type</th>
		<th>Creator</th>
		<th>Class</th>
		<th>Attack</th>
		<th>Defense</th>
		<th>Level</th>
		<th>Starting</th>
		<th>Finishing</th>
		<th></th>
	</tr>
<%
	if (activeGames.isEmpty()) {
%>
	<tr><td colspan="100%"> You are currently not active in any game.</td></tr>
<%
	} else {
		for (UserMultiplayerGameInfo info : activeGames) {
%>
	<tr id="<%="game-"+info.gameId()%>">
		<td class="col-sm-1"><%= info.gameId() %></td>
		<td class="col-sm-1">Battlegrounds</td>
		<td class="col-sm-1"><%=info.creatorName()%></td>
		<td class="col-sm-2">
			<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=info.gameId()%>"><%=info.cutAlias()%></a>
			<div id="modalCUTFor<%=info.gameId()%>" class="modal fade" role="dialog" style="text-align: left;" >
				<div class="modal-dialog">
					<!-- Modal content-->
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal">&times;</button>
							<h4 class="modal-title"><%=info.cutAlias()%></h4>
						</div>
						<div class="modal-body">
							<pre class="readonly-pre"><textarea class="readonly-textarea classPreview"
																id="sut<%=info.gameId()%>"
																name="cut<%=info.gameId()%>" cols="80"
																rows="30"><%=info.cutSource()%></textarea></pre>
						</div>
						<div class="modal-footer">
							<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
						</div>
					</div>
				</div>
			</div>
		</td>
        <%int attackers = info.attackers().size();%>
        <%int defenders = info.defenders().size();%>
        <td class="col-sm-1"><%=attackers %> of <%=info.minAttackers()%>&ndash;<%=info.maxAttackers()%></td>
        <td class="col-sm-1"><%=defenders %> of <%=info.minDefenders()%>&ndash;<%=info.maxDefenders()%></td>
		<td class="col-sm-1"><%=info.gameLevel()%></td>
		<td class="col-sm-1"><%=info.startTime()%></td>
		<td class="col-sm-1"><%=info.finishTime()%></td>
		<td class="col-sm-2">
<%
				switch(info.userRole()){
					case CREATOR:
						if (info.gameState() == GameState.CREATED) {
%>
			<form id="adminStartBtn-<%=info.gameId()%>" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post">
				<button type="submit" class="btn btn-sm btn-primary" id="startGame-<%=info.gameId()%>" form="adminStartBtn-<%=info.gameId()%>">
					Start Game
				</button>
				<input type="hidden" name="formType" value="startGame">
				<input type="hidden" name="gameId" value="<%= info.gameId() %>" />
			</form>
<%
						} else {
%>
			<a class="btn btn-sm btn-primary" id="<%="observe-"+info.gameId()%>" href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%= info.gameId() %>">
                Observe
            </a>
<%
						}
					break;
					case ATTACKER:
						if(info.gameState() != GameState.CREATED) {
%>
			<a class = "btn btn-sm btn-primary" id="<%="attack-"+info.gameId()%>" style="background-color: #884466;border-color: #772233;"
			   href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%= info.gameId() %>">Attack</a>
<%
						} else {
%>
			Joined as Attacker
			<%if (gamesJoinable) { %>
			<form id="attLeave" action="<%= request.getContextPath()  + Paths.BATTLEGROUND_SELECTION%>" method="post">
				<input class = "btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
				<input type="hidden" name="gameId" value="<%=info.gameId()%>">
				<button class="btn btn-sm btn-danger" id="<%="leave-attacker-"+info.gameId()%>" type="submit" form="attLeave" value="Leave">
					Leave
				</button>
			</form>
			<% } %>
<%
						}
					break;
					case DEFENDER:
						if(info.gameState() != GameState.CREATED) {
%>
			<a class = "btn btn-sm btn-primary" id="<%="defend-"+info.gameId()%>" style="background-color: #446688;border-color: #225577"
			   href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%= info.gameId() %>">Defend</a>
<%
						} else {
%>
			Joined as Defender
			<%if (gamesJoinable) { %>
			<form id="defLeave" action="<%= request.getContextPath()  + Paths.BATTLEGROUND_SELECTION%>" method="post">
				<input class = "btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
				<input type="hidden" name="gameId" value="<%=info.gameId()%>">
				<button class = "btn btn-sm btn-danger" id="<%="leave-defender-"+info.gameId()%>" type="submit" form="defLeave" value="Leave">
					Leave
				</button>
			</form>
			<% } %>
<%
						}
					break;
				}
%>
		</td>
	</tr>
<%
		} // Closes FOR
	} // Closes ELSE
%>

</table>
<%
%>

	<%if (gamesCreatable) { %>
	<a id="createBattleground" class = "btn btn-primary" href="<%=request.getContextPath() + Paths.BATTLEGROUND_CREATE%>">Create Battleground</a>
	<%}%>

<%if (gamesJoinable) { %>
<h2 class="full-width page-title">Open Games</h2>
<table class="table table-hover table-responsive table-paragraphs games-table">
	<tr>
		<th>ID</th>
		<th>Type</th>
		<th>Creator</th>
		<th>Class</th>
		<th>Attack</th>
		<th>Defense</th>
		<th>Level</th>
		<th>Starting</th>
		<th>Finishing</th>
		<th></th>

	</tr>
<%
	if (openGames.isEmpty()) {
%>
	<tr><td colspan="100%"> There are currently no open games. </td></tr>
<%
	} else {
        for (UserMultiplayerGameInfo info : openGames) {
%>
		<tr id="<%="game-"+info.gameId()%>">
			<td class="col-sm-1"><%= info.gameId() %></td>
			<td class="col-sm-1">Battlegrounds</td>
			<td class="col-sm-1"><%=info.creatorName()%></td>
			<td class="col-sm-2">
				<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=info.gameId()%>">
					<%=info.cutAlias()%>
				</a>
				<div id="modalCUTFor<%=info.gameId()%>" class="modal fade" role="dialog" style="text-align: left;" >
					<div class="modal-dialog">
						<!-- Modal content-->
						<div class="modal-content">
							<div class="modal-header">
								<button type="button" class="close" data-dismiss="modal">&times;</button>
								<h4 class="modal-title"><%=info.cutAlias()%></h4>
							</div>
							<div class="modal-body">
                                <pre class="readonly-pre"><textarea
										class="readonly-textarea classPreview"
										id="sut<%=info.gameId()%>"
										name="cut<%=info.gameId()%>" cols="80"
										rows="30"><%=info.cutSource()%></textarea></pre>
							</div>
							<div class="modal-footer">
								<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
							</div>
						</div>
					</div>
				</div>
			</td>
            <%int attackers = info.attackers().size();%>
            <%int defenders = info.defenders().size();%>
			<td class="col-sm-1"><%=attackers %> of <%=info.minAttackers()%>&ndash;<%=info.maxAttackers()%></td>
			<td class="col-sm-1"><%=defenders %> of <%=info.minDefenders()%>&ndash;<%=info.maxDefenders()%></td>
			<td class="col-sm-1"><%=info.gameLevel() %></td>
			<td class="col-sm-1"><%=info.startTime()%></td>
            <td class="col-sm-1"><%=info.finishTime()%></td>
			<td class="col-sm-2">
				<% if (attackers < info.maxAttackers()) { %>
				<form id="joinGameForm_attacker_<%=info.gameId()%>" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post">
					<input type="hidden" name="formType" value="joinGame">
					<input type="hidden" name="gameId" value=<%=info.gameId()%>>
					<input type="hidden" name="attacker" value=1>
					<button type="submit" id="<%="join-attacker-"+info.gameId()%>" class="btn btn-primary btn-sm" style="background-color: #884466;border-color: #772233; margin-bottom: 3px;" value="Join as Attacker">Join as Attacker</button>
				</form>
				<% } %>
				<% if (defenders < info.maxDefenders()) { %>
				<form id="joinGameForm_defender_<%=info.gameId()%>" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post">
					<input type="hidden" name="formType" value="joinGame">
					<input type="hidden" name="gameId" value=<%=info.gameId()%>>
					<input type="hidden" name="defender" value=1>
					<button type="submit" id="<%="join-defender-"+info.gameId()%>" class="btn btn-primary btn-sm" style="background-color: #446688;border-color: #225577" value="Join as Defender">Join as Defender</button>
				</form>
				<% } %>
			</td>
		</tr>
<%
        } // Closes FOR
    } // Closes ELSE
%>
	</table>
	<%}%>

	<script>
		$(document).ready(function() {
			$.fn.dataTable.moment( 'YY/MM/DD HH:mm' );
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

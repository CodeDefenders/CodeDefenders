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
<%@ page import="org.codedefenders.database.MultiplayerGameDAO" %>
<%@ page import="org.codedefenders.game.AbstractGame" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.database.UserDAO" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<% String pageTitle= null ; %>
<%@ include file="/jsp/header_main.jsp" %>
<%
	// TODO Phil 04/12/18: extract all that logic into the GamesOverview servlet
	String atkName;
	String defName;
	int atkId;
	int defId;

	// Collect all the games here
	int uid = (Integer)request.getSession().getAttribute("uid");

	// My Games
	List<MultiplayerGame> multiplayerGames = MultiplayerGameDAO.getMultiplayerGamesForUser(uid);

	List<AbstractGame> games = new ArrayList<>();
	games.addAll( multiplayerGames );

	// Open Games
	List<MultiplayerGame> openMultiplayerGames = MultiplayerGameDAO.getOpenMultiplayerGamesForUser(uid);

	List<AbstractGame> openGames = new ArrayList<>();
	openGames.addAll( openMultiplayerGames );

	boolean gamesJoinable = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_JOINING).getBoolValue();

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
	if (games.isEmpty()) {
%>
	<tr><td colspan="100%"> You are not currently playing any game. </td></tr>
<%
	} else {
		for (AbstractGame ag : games) {
			if ( ag instanceof MultiplayerGame ){
/****************************************************************************************************************************************/
				MultiplayerGame g = (MultiplayerGame) ag;
				Role role = g.getRole(uid);
                final GameClass cut = g.getCUT();%>
	<tr id="<%="game-"+g.getId()%>">
		<td class="col-sm-1"><%= g.getId() %></td>
		<td class="col-sm-1">Multiplayer</td>
		<td class="col-sm-1"><%= UserDAO.getUserById(g.getCreatorId()).getUsername() %></td>
		<td class="col-sm-2">
			<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=g.getId()%>"><%=cut.getAlias()%></a>
			<div id="modalCUTFor<%=g.getId()%>" class="modal fade" role="dialog" style="text-align: left;" >
				<div class="modal-dialog">
					<!-- Modal content-->
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal">&times;</button>
							<h4 class="modal-title"><%=cut.getAlias()%></h4>
						</div>
						<div class="modal-body">
							<pre class="readonly-pre"><textarea class="readonly-textarea classPreview"
																id="sut<%=g.getId()%>"
																name="cut<%=g.getId()%>" cols="80"
																rows="30"><%=cut.getAsHTMLEscapedString()%></textarea></pre>
						</div>
						<div class="modal-footer">
							<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
						</div>
					</div>
				</div>
			</div>
		</td>
		<td class="col-sm-1"><%= g.getAttackerIds().length %></td>
		<td class="col-sm-1"><%= g.getDefenderIds().length %></td>
		<td class="col-sm-1"><%= g.getLevel() %></td>
		<td class="col-sm-1"><%= g.getFormattedStartDateTime()%></td>
		<td class="col-sm-1"><%= g.getFormattedFinishDateTime()%></td>
		<td class="col-sm-2">
<%
    if (g.getState() == GameState.CREATED && g.getCreatorId() == uid) {
%>
            <form id="adminStartBtn-<%=g.getId()%>"
                  action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post">
                <button type="submit" class="btn btn-sm btn-primary" id="startGame-<%=g.getId()%>"
                        form="adminStartBtn-<%=g.getId()%>">
                    Start Game
                </button>
                <input type="hidden" name="formType" value="startGame">
                <input type="hidden" name="gameId" value="<%= g.getId() %>"/>
            </form>
            <%
            } else { %>
            <%
                switch (role) {
                    case ATTACKER:
                        if (!g.getState().equals(GameState.CREATED)) {
            %>
            <a class="btn btn-sm btn-primary" id="<%="attack-"+g.getId()%>"
               style="background-color: #884466;border-color: #772233;"
               href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%= g.getId() %>">Attack</a>
            <%
                        } else {
            %>
            Joined as Attacker
                            <%if (gamesJoinable) { %>
            <form id="attLeave" action="<%= request.getContextPath()  + Paths.BATTLEGROUND_SELECTION%>" method="post">
                <input class="btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
                <input type="hidden" name="gameId" value="<%=g.getId()%>">
                <button class="btn btn-sm btn-danger" id="<%="leave-attacker-"+g.getId()%>" type="submit"
                        form="attLeave" value="Leave">
                    Leave
                </button>
            </form>
                            <% } %>
            <%
                        }
                    break;
                case DEFENDER:
                    if (!g.getState().equals(GameState.CREATED)) {
            %>
            <a class="btn btn-sm btn-primary" id="<%="defend-"+g.getId()%>"
               style="background-color: #446688;border-color: #225577"
               href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%= g.getId() %>">Defend</a>
            <%
                    } else {
            %>
            Joined as Defender
                        <%if (gamesJoinable) { %>
            <form id="defLeave" action="<%= request.getContextPath()  + Paths.BATTLEGROUND_SELECTION%>" method="post">
                <input class="btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
                <input type="hidden" name="gameId" value="<%=g.getId()%>">
                <button class="btn btn-sm btn-danger" id="<%="leave-defender-"+g.getId()%>" type="submit"
                        form="defLeave" value="Leave">
                    Leave
                </button>
            </form>
                        <% } %>
            <%
                    }
                    break;
                default:
                    if (g.getCreatorId() == uid) {
            %>
            <a class="btn btn-sm btn-primary" id="<%="observe-"+g.getId()%>"
               href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%= g.getId() %>">Observe</a>

            <%
                    }
                break;
                }
            }
%>
		</td>
	</tr>
<%
/****************************************************************************************************************************************/
			}
			else {
				continue;
			}
		} // Closes FOR
	} // Closes ELSE
%>

</table>
<%
/********* OPEN GAMES *******************************************************************************************************/
%>

	<%if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_CREATION).getBoolValue()) { %>
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
		for (AbstractGame ag : openGames) {
            if ( ag instanceof MultiplayerGame ){
/****************************************************************************************************************************************/
				MultiplayerGame g = (MultiplayerGame) ag;
				Role role = g.getRole(uid);
                final GameClass cut = g.getCUT();%>
		<tr id="<%="game-"+g.getId()%>">
			<td class="col-sm-1"><%= g.getId() %></td>
			<td class="col-sm-1">Multiplayer</td>
			<td class="col-sm-1"><%= UserDAO.getUserById(g.getCreatorId()).getUsername() %></td>
			<td class="col-sm-2">
				<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=g.getId()%>">
					<%=cut.getAlias()%>
				</a>
				<div id="modalCUTFor<%=g.getId()%>" class="modal fade" role="dialog" style="text-align: left;" >
					<div class="modal-dialog">
						<!-- Modal content-->
						<div class="modal-content">
							<div class="modal-header">
								<button type="button" class="close" data-dismiss="modal">&times;</button>
								<h4 class="modal-title"><%=cut.getAlias()%></h4>
							</div>
							<div class="modal-body">
                                <pre class="readonly-pre"><textarea
										class="readonly-textarea classPreview"
										id="sut<%=g.getId()%>"
										name="cut<%=g.getId()%>" cols="80"
										rows="30"><%=cut.getAsHTMLEscapedString()%></textarea></pre>
							</div>
							<div class="modal-footer">
								<button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
							</div>
						</div>
					</div>
				</div>
			</td>
			<!-- Owner of the open game -->
			<%-- <td class="col-sm-1"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).getUsername() %></td> --%>
			<!--<td class="col-sm-1"><%/*= g.getPrize() */%></td>-->
            <%int attackers = g.getAttackerIds().length;%>
            <%int defenders = g.getDefenderIds().length;%>
			<td class="col-sm-1"><%=attackers %> of <%=g.getMinAttackers()%>&ndash;<%=g.getAttackerLimit()%></td>
			<td class="col-sm-1"><%=defenders %> of <%=g.getMinDefenders()%>&ndash;<%=g.getDefenderLimit()%></td>
			<td class="col-sm-1"><%= g.getLevel() %></td>
			<td class="col-sm-1"><%= g.getFormattedStartDateTime() %></td>
			<td class="col-sm-1"><%= g.getFormattedFinishDateTime() %></td>
			<td class="col-sm-2">
				<% if (attackers < g.getAttackerLimit()) { %>
				<form id="joinGameForm_attacker_<%=g.getId()%>" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post">
					<input type="hidden" name="formType" value="joinGame">
					<input type="hidden" name="gameId" value=<%=g.getId()%>>
					<input type="hidden" name="attacker" value=1>
					<button type="submit" id="<%="join-attacker-"+g.getId()%>" class="btn btn-primary btn-sm" style="background-color: #884466;border-color: #772233; margin-bottom: 3px;" value="Join as Attacker">Join as Attacker</button>
				</form>
				<% } %>
				<% if (defenders < g.getDefenderLimit()) { %>
				<form id="joinGameForm_defender_<%=g.getId()%>" action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>" method="post">
					<input type="hidden" name="formType" value="joinGame">
					<input type="hidden" name="gameId" value=<%=g.getId()%>>
					<input type="hidden" name="defender" value=1>
					<button type="submit" id="<%="join-defender-"+g.getId()%>" class="btn btn-primary btn-sm" style="background-color: #446688;border-color: #225577" value="Join as Defender">Join as Defender</button>
				</form>
				<% } %>
			</td>
		</tr>
<%
/****************************************************************************************************************************************/
			}
			else {
				continue;
			}
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

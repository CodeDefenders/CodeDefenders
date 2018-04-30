<%@ page import="org.codedefenders.Role" %>
<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.duel.DuelGame" %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle= null ; %>
<%@ include file="/jsp/header_main.jsp" %>
<%
	String atkName;
	String defName;
	int atkId;
	int defId;

	// Collect all the games here
	int uid = (Integer)request.getSession().getAttribute("uid");

	// My Games
	List<DuelGame> duelGames = DatabaseAccess.getGamesForUser(uid);
	List<MultiplayerGame> multiplayerGames = DatabaseAccess.getMultiplayerGamesForUser(uid);

	List<AbstractGame> games = new ArrayList<AbstractGame>();
	games.addAll( duelGames );
	games.addAll( multiplayerGames );

	// Open Games
	List<DuelGame> openDuelGames = DatabaseAccess.getOpenGames();
	List<MultiplayerGame> openMultiplayerGames = DatabaseAccess.getOpenMultiplayerGamesForUser(uid);

	List<AbstractGame> openGames = new ArrayList<AbstractGame>();
	openGames.addAll( openDuelGames );
	openGames.addAll( openMultiplayerGames );
%>

<div class="w-100">
<h2 class="full-width page-title">My Games</h2>
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
	if (games.isEmpty()) {
%>
	<tr><td colspan="100%"> You are not currently playing any game. </td></tr>
<%
	} else {
		for (AbstractGame ag : games) {
			if( ag instanceof DuelGame ){
/****************************************************************************************************************************************/
				DuelGame g = (DuelGame) ag;

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
		<td class="col-sm-1">Duel</td>
		<td class="col-sm-1"></td>
		<td class="col-sm-1">
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
		<td class="col-sm-1"><%= atkName %></td>
		<td class="col-sm-1"><%= defName %></td>
		<td class="col-sm-1"><%= g.getLevel().name() %></td>
        <td class="col-sm-1"></td>
        <td class="col-sm-1"></td>
		<td class="col-sm-3">
<%
			if (g.getState().equals(GameState.ACTIVE)) { // Can enter only if game is in progress.
				String btnLabel = "Your Turn";
				if (g.getMode().equals(GameMode.UTESTING)) {
					btnLabel = "Enter";
				}
%>
			<form id="view" action="<%= request.getContextPath() %>/games" method="post">
				<input type="hidden" name="formType" value="enterGame">
				<input type="hidden" name="game" value="<%=g.getId()%>">
				<% if (uid == turnId ) {%>
				<button class="btn btn-primary" type="submit"><%=btnLabel%></button>
				<% } else {%>
				<button  class="btn btn-default btn-sm" type="submit" value="Enter Game">Enter Game</button>
				<% }%>
			</form>

<%
			}
%>
		</td>
	</tr>
<%
/****************************************************************************************************************************************/
			} else if ( ag instanceof MultiplayerGame ){
/****************************************************************************************************************************************/
				MultiplayerGame g = (MultiplayerGame) ag;
				Role role = g.getRole(uid);
%>
	<tr>
		<td class="col-sm-1"><%= g.getId() %></td>
		<td class="col-sm-1">Multiplayer</td>
		<td class="col-sm-1"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).getUsername() %></td>
		<td class="col-sm-2">
			<a href="#" data-toggle="modal" data-target="#modalCUTFor<%=g.getId()%>"><%=g.getCUT().getAlias()%></a>
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
			<a class = "btn btn-sm btn-primary" href="<%= request.getContextPath() %>/multiplayer/games?id=<%= g.getId() %>">Observe</a>
<%
					break;
					case ATTACKER:
						if(!g.getState().equals(GameState.CREATED)) {
%>
			<a class = "btn btn-sm btn-primary"  style="background-color: #884466;border-color: #772233;"
			   href="<%= request.getContextPath() %>/multiplayer/games?id=<%= g.getId() %>">Attack</a>
<%
						} else {
%>
			Joined as Attacker
			<form id="attLeave" action="<%= request.getContextPath() %>/multiplayer/games" method="post">
				<input class = "btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
				<input type="hidden" name="game" value="<%=g.getId()%>">
				<button class = "btn btn-sm btn-danger" type="submit" form="attLeave" value="Leave">
					Leave
				</button>
			</form>
<%
						}
					break;
					case DEFENDER:
						if(!g.getState().equals(GameState.CREATED)) {
%>
			<a class = "btn btn-sm btn-primary" style="background-color: #446688;border-color: #225577"
			   href="<%= request.getContextPath() %>/multiplayer/games?id=<%= g.getId() %>">Defend</a>
<%
						} else {
%>
			Joined as Defender
			<form id="defLeave" action="<%= request.getContextPath() %>/multiplayer/games" method="post">
				<input class = "btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
				<input type="hidden" name="game" value="<%=g.getId()%>">
				<button class = "btn btn-sm btn-danger" type="submit" form="defLeave" value="Leave">Leave</button>
			</form>
<%
						}
					break;
					default:
					break;
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
	<a class = "btn btn-primary" href="<%=request.getContextPath()%>/multiplayer/games/create">Create Battleground</a>
	<a class = "btn btn-primary" href="<%=request.getContextPath()%>/games/create">Create Duel</a>
	<%}%>


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
			if( ag instanceof DuelGame ){
/****************************************************************************************************************************************/
				DuelGame g = (DuelGame) ag;
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
			<td class="col-sm-1">Duel</td>
			<td class="col-sm-1"></td>
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
			<td class="col-sm-1"></td>
			<td class="col-sm-1"></td>
			<td class="col-sm-2">
			<form id="view" action="<%=request.getContextPath() %>/games" method="post">
					<input type="hidden" name="formType" value="joinGame">
					<input type="hidden" name="game" value=<%=g.getId()%>>
					<button type="submit" class="btn btn-primary btn-sm" value="Join Game">Join Game</button>
				</form>
			</td>
		</tr>
<%
/****************************************************************************************************************************************/
			} else if ( ag instanceof MultiplayerGame ){
/****************************************************************************************************************************************/
				MultiplayerGame g = (MultiplayerGame) ag;
				Role role = g.getRole(uid);
%>
		<tr>
			<td class="col-sm-1"><%= g.getId() %></td>
			<td class="col-sm-1">Multiplayer</td>
			<td class="col-sm-1"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).getUsername() %></td>
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
			<!-- Owner of the open game -->
			<%-- <td class="col-sm-1"><%= DatabaseAccess.getUserForKey("User_ID", g.getCreatorId()).getUsername() %></td> --%>
			<!--<td class="col-sm-1"><%/*= g.getPrize() */%></td>-->
			<td class="col-sm-1"><%int attackers = g.getAttackerIds().length; %><%=attackers %> of <%=g.getMinAttackers()%>&ndash;<%=g.getAttackerLimit()%></td>
			<td class="col-sm-1"><%int defenders = g.getDefenderIds().length; %><%=defenders %> of <%=g.getMinDefenders()%>&ndash;<%=g.getDefenderLimit()%></td>
			<td class="col-sm-1"><%= g.getLevel().name() %></td>
			<td class="col-sm-1"><%= g.getStartDateTime() %></td>
			<td class="col-sm-1"><%= g.getFinishDateTime() %></td>
			<td class="col-sm-2">
				<a class="btn btn-sm btn-primary" style="background-color: #884466;border-color: #772233; margin-bottom: 3px;"
				   href="<%=request.getContextPath()%>/multiplayer/games?attacker=1&id=<%= g.getId() %>">Join as Attacker</a>
				<a class="btn btn-sm btn-primary" style="background-color: #446688;border-color: #225577"
				   href="<%=request.getContextPath()%>/multiplayer/games?defender=1&id=<%= g.getId() %>">Join as Defender</a>
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
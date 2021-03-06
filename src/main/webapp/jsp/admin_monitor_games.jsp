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
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.database.*" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.Duration" %>
<%@ page import="org.codedefenders.servlets.admin.AdminMonitorGames" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<jsp:include page="/jsp/header_main.jsp"/>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminMonitorGames"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <form id="games" action="<%=request.getContextPath() + Paths.ADMIN_MONITOR%>" method="post">
        <input type="hidden" name="formType" value="startStopGame">

        <h3>Current Multiplayer Games</h3>
        <%
            List<MultiplayerGame> multiplayerGames = MultiplayerGameDAO.getUnfinishedMultiplayerGamesCreatedBy(login.getUserId());
            if (multiplayerGames.isEmpty()) {
        %>
        <div class="panel panel-default">
            <div class="panel-body" style="    color: gray;    text-align: center;">
                There are currently no unfinished multiplayer games in the Database.
            </div>
        </div>
        <% } else { %>
        <table id="table-multiplayer"
               class="table-hover table-striped table-responsive table-paragraphs games-table display table-condensed">
            <thead>
            <tr style="border-bottom: 1px solid black">
                <th><input type="checkbox" id="selectAllGamesMultiplayer">
                </th>
                <th>ID</th>
                <th></th>
                <th>Class</th>
                <th>Creator</th>
                <th>Attackers</th>
                <th>Defenders</th>
                <th>Level</th>
                <th>
                    <a id="togglePlayersActiveMultiplayer" class="btn btn-sm btn-default" title="Show list of Players for each Game.">
                        <span id="togglePlayersActiveMultiplayerSpan" class="glyphicon glyphicon-alert"></span>
                    </a>
                </th>
            </tr>
            </thead>
            <tbody>
            <%
                for (MultiplayerGame g : multiplayerGames) {
                    GameClass cut = g.getCUT();
                    String startStopButtonIcon = g.getState().equals(GameState.ACTIVE) ?
                            "glyphicon glyphicon-stop" : "glyphicon glyphicon-play";
                    String startStopButtonClass = g.getState().equals(GameState.ACTIVE) ?
                            "btn btn-sm btn-danger" : "btn btn-sm btn-primary";
                    String startStopButtonAction = g.getState().equals(GameState.ACTIVE) ?
                            "return confirm('Are you sure you want to stop this Game?');" : "";
                    int gid = g.getId();
            %>
            <tr style="border-top: 1px solid lightgray; border-bottom: 1px solid lightgray" id="<%="game_row_"+gid%>">
                <td>
                    <input type="checkbox" name="selectedGames" id="<%="selectedGames_"+gid%>" value="<%= gid%>" onchange=
                            "document.getElementById('start_games_btn').disabled = !areAnyChecked('selectedGames');
                            document.getElementById('stop_games_btn').disabled = !areAnyChecked('selectedGames');
                            setSelectAllCheckbox('selectedGames', 'selectAllGamesMultiplayer')">
                </td>
                <td><%= gid %>
                </td>
                <td>
                    <a class="btn btn-sm btn-primary" id="<%="observe-"+g.getId()%>"
                       href="<%= request.getContextPath() + Paths.BATTLEGROUND_GAME%>?gameId=<%= gid %>">Observe</a>
                </td>
                <td class="col-sm-2">
                    <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=gid%>">
                        <%=cut.getAlias()%>
                    </a>
                    <div id="modalCUTFor<%=gid%>" class="modal fade" role="dialog" style="text-align: left;">
                        <div class="modal-dialog">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                    <h4 class="modal-title"><%=cut.getAlias()%>
                                    </h4>
                                </div>
                                <div class="modal-body">
                                    <pre class="readonly-pre"><textarea
                                            class="readonly-textarea classPreview"
                                            id="sut<%=gid%>" name="cut<%=g.getCUT().getId()%>" cols="80"
                                            rows="30"></textarea></pre>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </td>
                <td class="col-sm-1"><%= UserDAO.getUserById(g.getCreatorId()).getUsername() %>
                </td>
                <td class="col-sm-1"><%=g.getAttackerPlayers().size()%>
                </td>
                <td class="col-sm-1"><%=g.getDefenderPlayers().size()%>
                </td>
                <td><%= g.getLevel() %>
                </td>
                <td class="col-sm-1" style="padding-top:4px; padding-bottom:4px">
                    <button class="<%=startStopButtonClass%>" type="submit" value="<%=gid%>" name="start_stop_btn"
                            onclick="<%=startStopButtonAction%>" id="<%="start_stop_"+g.getId()%>">
                        <span class="<%=startStopButtonIcon%>"></span>

                    </button>
                </td>
                    <%List<List<String>> playersInfo = AdminDAO.getPlayersInfo(gid);
                if(!playersInfo.isEmpty()){%>
            <tr id="playersTableActive" hidden>
                <th colspan="3">Game Score</th>
                <th style="border-bottom: 1px solid black">Name</th>
                <th style="border-bottom: 1px solid black">Submissions</th>
                <th style="border-bottom: 1px solid black">Last Action</th>
                <th style="border-bottom: 1px solid black">Points</th>
                <th style="border-bottom: 1px solid black">Total Score</th>
                <th style="border-bottom: 1px solid black">Switch Role</th>
                <th style="border-bottom: 1px solid black"></th>
            </tr>
            <%
                }
                boolean firstAttacker = true;
                boolean firstDefender = true;

                // Compute the cumulative sum of each role score. Not sure if this is how is done in the scoreboard
                int gameScoreAttack = 0;
                int gameScoreDefense = 0;

                for (List<String> playerInfo : playersInfo) {
					if( Role.ATTACKER.equals( Role.valueOf(playerInfo.get(2)) ) ){
						gameScoreAttack = gameScoreAttack + Integer.parseInt(playerInfo.get(4));
					} else if (Role.DEFENDER.equals( Role.valueOf(playerInfo.get(2)) ) ){
						gameScoreDefense = gameScoreDefense + Integer.parseInt(playerInfo.get(4));
					}
				}

                for (List<String> playerInfo : playersInfo) {
                    int pid = Integer.parseInt(playerInfo.get(0));
                    // Do not visualize system users.
                    int userID = UserDAO.getUserForPlayer( pid ).getId();
                    // Note this does not prevent someone to forge move player requests which remove system users from the game
                    if( userID == Constants.DUMMY_ATTACKER_USER_ID || userID == Constants.DUMMY_DEFENDER_USER_ID ){
                        continue;
                    }

                    String userName = playerInfo.get(1);
                    Role role = Role.valueOf(playerInfo.get(2));
                    String ts = playerInfo.get(3);

                    String lastSubmissionTS;
                    if (ts.equalsIgnoreCase("never")) {
                        lastSubmissionTS = ts;
                    } else {
                        Instant then = Instant.ofEpochMilli(Long.parseLong(ts));
                        Instant now = Instant.now();
                        Duration duration = Duration.between(then, now);
                        lastSubmissionTS = String.format("%02dh %02dm %02ds",
                                duration.toHours(), duration.toMinutes() % 60, duration.getSeconds() % 60);
                    }

                    int totalScore = Integer.parseInt(playerInfo.get(4));
                    int submissionsCount = Integer.parseInt(playerInfo.get(5));
                    String color = role == Role.ATTACKER ? "#edcece" : "#ced6ed";
                    int gameScore = AdminMonitorGames.getPlayerScore(g, pid);
            %>
            <tr style="height: 3px;" id="playersTableActive" hidden></tr>
            <tr id="playersTableActive" hidden>
            <%	if ( firstAttacker && role.equals( Role.ATTACKER ) ) {%>
                <td colspan = "3"><%= gameScoreAttack %></td>
            <%	firstAttacker = false;
				}
				else if ( firstDefender && role.equals( Role.DEFENDER ) ) {%>
                <td colspan = "3"><%= gameScoreDefense %></td>
            <%  firstDefender = false;
                } else { %>
                <td></td>
                <td></td>
                <td></td>
            <%  } %>
                <td style="background: <%= color %>; border-top-left-radius: 7px;border-bottom-left-radius: 7px;">
                    <%= userName %>
                </td>
                <td style="background: <%= color %>"><%= submissionsCount %>
                </td>
                <td style="background: <%= color %>"><%= lastSubmissionTS %>
                </td>
                <td style="background: <%= color %>"><%= gameScore %>
                </td>
                <td style="background: <%= color %>"><%= totalScore %>
                </td>
                <td style="background: <%= color %>">

                    <button class="btn btn-sm btn-danger" value="<%=pid + "-" + gid + "-" + role%>"
                            onclick="return confirm('Are you sure you want to permanently remove this player? \n' +
                             'This will also delete ALL of his tests, mutants and claimed equivalences ' +
                              'and might create inconsistencies in the Game.');"
                            id="<%="switch_player_"+pid+"_game_"+gid%>"
                            name="activeGameUserSwitchButton">
                        <span class="glyphicon glyphicon-transfer"></span>
                    </button>
                </td>
                <td style="background: <%= color %>; border-top-right-radius: 7px;border-bottom-right-radius: 7px;">

                    <button class="btn btn-sm btn-danger" value="<%=pid + "-" + gid%>"
                            onclick="return confirm('Are you sure you want to permanently remove this player? \n' +
                             'This will also delete ALL of his tests, mutants and claimed equivalences ' +
                              'and might create inconsistencies in the Game.');"
                            id="<%="remove_player_"+pid+"_game_"+gid%>"
                            name="activeGameUserRemoveButton">Remove
                    </button>
                </td>
            </tr>
            <% } %>
            <% } %>
            </tbody>
        </table>
        <br/>

        <% } %>

        <%-- ------------------------------------------------------------------------------------------------------ --%>

        <h3>Current Melee Games</h3>
        <%
            List<MeleeGame> meleeGames = MeleeGameDAO.getUnfinishedMeleeGamesCreatedBy(login.getUserId());
            if (meleeGames.isEmpty()) {
        %>
        <div class="panel panel-default">
            <div class="panel-body" style="    color: gray;    text-align: center;">
                There are currently no unfinished multiplayer games in the Database.
            </div>
        </div>
        <% } else { %>
        <table id="table-melee"
               class="table-hover table-striped table-responsive table-paragraphs games-table display table-condensed">
            <thead>
            <tr style="border-bottom: 1px solid black">
                <th><input type="checkbox" id="selectAllGamesMelee">
                </th>
                <th>ID</th>
                <th></th>
                <th>Class</th>
                <th>Creator</th>
                <th>Players</th>
                <th>Level</th>
                <th>
                    <a id="togglePlayersActiveMelee" class="btn btn-sm btn-default" title="Show list of Players for each Game.">
                        <span id="togglePlayersActiveMeleeSpan" class="glyphicon glyphicon-alert"></span>
                    </a>
                </th>
            </tr>
            </thead>
            <tbody>
            <%
                for (MeleeGame g : meleeGames) {
                    GameClass cut = g.getCUT();
                    String startStopButtonIcon = g.getState().equals(GameState.ACTIVE) ?
                            "glyphicon glyphicon-stop" : "glyphicon glyphicon-play";
                    String startStopButtonClass = g.getState().equals(GameState.ACTIVE) ?
                            "btn btn-sm btn-danger" : "btn btn-sm btn-primary";
                    String startStopButtonAction = g.getState().equals(GameState.ACTIVE) ?
                            "return confirm('Are you sure you want to stop this Game?');" : "";
                    int gid = g.getId();
            %>
            <tr style="border-top: 1px solid lightgray; border-bottom: 1px solid lightgray" id="<%="game_row_"+gid%>">
                <td>
                    <input type="checkbox" name="selectedGames" id="<%="selectedGames_"+gid%>" value="<%= gid%>" onchange=
                            "document.getElementById('start_games_btn').disabled = !areAnyChecked('selectedGames');
                            document.getElementById('stop_games_btn').disabled = !areAnyChecked('selectedGames');
                            setSelectAllCheckbox('selectedGames', 'selectAllGamesMelee')">
                </td>
                <td><%= gid %>
                </td>
                <td>
                    <a class="btn btn-sm btn-primary" id="<%="observe-"+g.getId()%>"
                       href="<%= request.getContextPath() + Paths.MELEE_GAME%>?gameId=<%= gid %>">Observe</a>
                </td>
                <td class="col-sm-2">
                    <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=gid%>">
                        <%=cut.getAlias()%>
                    </a>
                    <div id="modalCUTFor<%=gid%>" class="modal fade" role="dialog" style="text-align: left;">
                        <div class="modal-dialog">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                    <h4 class="modal-title"><%=cut.getAlias()%>
                                    </h4>
                                </div>
                                <div class="modal-body">
                                    <pre class="readonly-pre"><textarea
                                            class="readonly-textarea classPreview"
                                            id="sut<%=gid%>" name="cut<%=g.getCUT().getId()%>" cols="80"
                                            rows="30"></textarea></pre>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </td>
                <td class="col-sm-1"><%= UserDAO.getUserById(g.getCreatorId()).getUsername() %>
                </td>
                <td class="col-sm-1"><%=g.getPlayers().size()%>
                </td>
                <td><%= g.getLevel() %>
                </td>
                <td class="col-sm-1" style="padding-top:4px; padding-bottom:4px">
                    <button class="<%=startStopButtonClass%>" type="submit" value="<%=gid%>" name="start_stop_btn"
                            onclick="<%=startStopButtonAction%>" id="<%="start_stop_"+g.getId()%>">
                        <span class="<%=startStopButtonIcon%>"></span>

                    </button>
                </td>
                    <%List<List<String>> playersInfo = AdminDAO.getPlayersInfo(gid);
                if(!playersInfo.isEmpty()){%>
            <tr id="playersTableActive" hidden>
                <th colspan="3">Game Score</th>
                <th style="border-bottom: 1px solid black">Name</th>
                <th style="border-bottom: 1px solid black">Submissions</th>
                <th style="border-bottom: 1px solid black">Last Action</th>
                <th style="border-bottom: 1px solid black">Points</th>
                <th style="border-bottom: 1px solid black">Total Score</th>
                <th style="border-bottom: 1px solid black"></th>
            </tr>
            <%
                }
                boolean firstPlayer = true;

                // Compute the cumulative sum of each role score. Not sure if this is how is done in the scoreboard
                int gameScore = 0;

                for (List<String> playerInfo : playersInfo) {
                    if (Role.DEFENDER.equals(Role.valueOf(playerInfo.get(2)))) {
                        gameScore = gameScore + Integer.parseInt(playerInfo.get(4));
                    }
                }

                for (List<String> playerInfo : playersInfo) {
                    int pid = Integer.parseInt(playerInfo.get(0));
                    // Do not visualize system users.
                    int userID = UserDAO.getUserForPlayer( pid ).getId();
                    // Note this does not prevent someone to forge move player requests which remove system users from the game
                    if( userID == Constants.DUMMY_ATTACKER_USER_ID || userID == Constants.DUMMY_DEFENDER_USER_ID ){
                        continue;
                    }

                    String userName = playerInfo.get(1);
                    Role role = Role.valueOf(playerInfo.get(2));
                    String ts = playerInfo.get(3);

                    String lastSubmissionTS;
                    if (ts.equalsIgnoreCase("never")) {
                        lastSubmissionTS = ts;
                    } else {
                        Instant then = Instant.ofEpochMilli(Long.parseLong(ts));
                        Instant now = Instant.now();
                        Duration duration = Duration.between(then, now);
                        lastSubmissionTS = String.format("%02dh %02dm %02ds",
                                duration.toHours(), duration.toMinutes() % 60, duration.getSeconds() % 60);
                    }

                    int totalScore = Integer.parseInt(playerInfo.get(4));
                    int submissionsCount = Integer.parseInt(playerInfo.get(5));
            %>
            <tr style="height: 3px;" id="playersTableActive" hidden></tr>
            <tr id="playersTableActive" hidden>
                <%	if (firstPlayer && role.equals(Role.PLAYER)) { %>
                <td colspan = "3"><%= gameScore %></td>
                <%
                    firstPlayer = false;
                    } else {
                %>
                <td></td>
                <td></td>
                <td></td>
                <%  } %>
                <td style="border-top-left-radius: 7px;border-bottom-left-radius: 7px;">
                    <%= userName %>
                </td>
                <td><%= submissionsCount %>
                </td>
                <td><%= lastSubmissionTS %>
                </td>
                <td>
                </td>
                <td><%= totalScore %>
                </td>
                <td style="border-top-right-radius: 7px;border-bottom-right-radius: 7px;">

                    <button class="btn btn-sm btn-danger" value="<%=pid + "-" + gid%>"
                            onclick="return confirm('Are you sure you want to permanently remove this player? \n' +
                             'This will also delete ALL of his tests, mutants and claimed equivalences ' +
                              'and might create inconsistencies in the Game.');"
                            id="<%="remove_player_"+pid+"_game_"+gid%>"
                            name="activeGameUserRemoveButton">Remove
                    </button>
                </td>
            </tr>
            <% } %>
            <% } %>
            </tbody>
        </table>
        <br/>

        <% } %>

        <% if (!multiplayerGames.isEmpty() || !meleeGames.isEmpty()) { %>
        <button class="btn btn-md btn-primary" type="submit" name="games_btn" id="start_games_btn"
                disabled value="Start Games">
            Start Games
        </button>
        <button class="btn btn-md btn-danger" type="submit" name="games_btn" id="stop_games_btn"
                onclick="return confirm('Are you sure you want to stop the selected Games?');"
                disabled value="Stop Games">
            Stop Games
        </button>
        <% } %>
    </form>


        <script>
            $('#selectAllGamesMultiplayer').click(function () {
                $(this).closest('table')
                        .find('tbody')
                        .find(':checkbox')
                        .prop('checked', this.checked);
                document.getElementById('start_games_btn').disabled = !areAnyChecked('selectedGames');
                document.getElementById('stop_games_btn').disabled = !areAnyChecked('selectedGames');
            });

            $('#selectAllGamesMelee').click(function () {
                $(this).closest('table')
                        .find('tbody')
                        .find(':checkbox')
                        .prop('checked', this.checked);
                document.getElementById('start_games_btn').disabled = !areAnyChecked('selectedGames');
                document.getElementById('stop_games_btn').disabled = !areAnyChecked('selectedGames');
            });

            $('#togglePlayersActiveMultiplayer').click(function () {
                var showPlayers = localStorage.getItem("showActivePlayersMultiplayer") === "true";
                localStorage.setItem("showActivePlayersMultiplayer", showPlayers ? "false" : "true");
                $('#table-multiplayer').find("[id=playersTableActive]").toggle();
                setActivePlayersSpan()
            });

            $('#togglePlayersActiveMelee').click(function () {
                var showPlayers = localStorage.getItem("showActivePlayersMelee") === "true";
                localStorage.setItem("showActivePlayersMelee", showPlayers ? "false" : "true");
                $('#table-melee').find("[id=playersTableActive]").toggle();
                setActivePlayersSpan()
            });

            function setActivePlayersSpan() {
                var showPlayers = localStorage.getItem("showActivePlayersMultiplayer") === "true";
                var buttonClass = showPlayers ? "glyphicon glyphicon-eye-close" : "glyphicon glyphicon-eye-open";
                var span = document.getElementById("togglePlayersActiveMultiplayerSpan");
                if (span) {
                    span.setAttribute("class", buttonClass);
                }

                showPlayers = localStorage.getItem("showActivePlayersMelee") === "true";
                buttonClass = showPlayers ? "glyphicon glyphicon-eye-close" : "glyphicon glyphicon-eye-open";
                span = document.getElementById("togglePlayersActiveMeleeSpan");
                if (span) {
                    span.setAttribute("class", buttonClass);
                }
            }

            /* Check only in the local table if all checkboxes are checked. */
            function setSelectAllCheckbox(checkboxesName, selectAllCheckboxId) {
                var checkboxes = $(this).closest('table')
                        .find('tbody')
                        .find("[name='" + checkboxesName + "']");
                var allChecked = false;
                checkboxes.each(function (index, element) {
                    allChecked = allChecked && element.checked;
                });
                document.getElementById(selectAllCheckboxId).checked = allChecked;
            }

            /* Check in both tables if any checkboxes are checked. */
            function areAnyChecked(name) {
                var checkboxes = $("[name='" + name + "']");
                var anyChecked = false;
                checkboxes.each(function (index, element) {
                    anyChecked = anyChecked || element.checked;
                });
                return anyChecked;
            }

            $(document).ready(function () {
                if (localStorage.getItem("showActivePlayersMultiplayer") === "true") {
                    $('#table-multiplayer').find("[id=playersTableActive]").show();
                }
                if (localStorage.getItem("showActivePlayersMelee") === "true") {
                    $('#table-melee').find("[id=playersTableActive]").show();
                }

                setActivePlayersSpan();
            });

            $('.modal').on('shown.bs.modal', function () {
                let codeMirrorContainer = $(this).find(".CodeMirror")[0];
                if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                    codeMirrorContainer.CodeMirror.refresh();
                } else {
                    let textarea = $(this).find('textarea')[0];
                    let editor = CodeMirror.fromTextArea(textarea, {
                        lineNumbers: false,
                        readOnly: true,
                        mode: "text/x-java"
                    });
                    editor.setSize("100%", 500);
                    ClassAPI.getAndSetEditorValue(textarea, editor);
                }
            });
        </script>

</div>
<%@ include file="/jsp/footer.jsp" %>

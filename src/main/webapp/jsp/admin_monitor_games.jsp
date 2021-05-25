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
<%@ page import="java.util.Map" %>
<%@ page import="java.util.stream.Collectors" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<jsp:include page="/jsp/header_main.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminMonitorGames"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <form id="games" action="<%=request.getContextPath() + Paths.ADMIN_MONITOR%>" method="post">
        <input type="hidden" name="formType" value="startStopGame">

        <h3 class="mb-3">Current Multiplayer Games</h3>
        <%
            List<MultiplayerGame> multiplayerGames = MultiplayerGameDAO.getUnfinishedMultiplayerGamesCreatedBy(login.getUserId());
            if (multiplayerGames.isEmpty()) {
        %>
            <div class="card">
                <div class="card-body text-muted text-center">
                    There are currently no unfinished multiplayer games.
                </div>
            </div>
        <%
            } else {
        %>
            <table id="table-multiplayer" class="table">
                <thead>
                    <tr>
                        <th>
                            <div class="form-check">
                                <input type="checkbox" class="form-check-input" id="selectAllGamesMultiplayer">
                            </div>
                        </th>
                        <th>ID</th>
                        <th></th>
                        <th>Class</th>
                        <th>Creator</th>
                        <th>Attackers</th>
                        <th>Defenders</th>
                        <th>Level</th>
                        <th>
                            <input type="checkbox" id="togglePlayersActiveMultiplayer" class="btn-check" autocomplete="off">
                            <label for="togglePlayersActiveMultiplayer" class="btn btn-sm btn-outline-secondary"
                                   title="Show list of Players for each Game.">
                                <i class="fa fa-eye"></i>
                            </label>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        for (MultiplayerGame g : multiplayerGames) {
                            GameClass cut = g.getCUT();
                            String startStopButtonIcon = g.getState().equals(GameState.ACTIVE) ?
                                    "fa fa-stop" : "fa fa-play";
                            String startStopButtonClass = g.getState().equals(GameState.ACTIVE) ?
                                    "btn btn-sm btn-danger" : "btn btn-sm btn-primary";
                            String startStopButtonAction = g.getState().equals(GameState.ACTIVE) ?
                                    "return confirm('Are you sure you want to stop this Game?');" : "";
                            int gid = g.getId();
                    %>
                        <tr id="<%="game_row_"+gid%>">
                            <td>
                                <div class="form-check">
                                    <input type="checkbox" name="selectedGames" id="<%="selectedGames_"+gid%>" value="<%=gid%>" class="form-check-input"
                                           onchange="document.getElementById('start_games_btn').disabled = !areAnyChecked('selectedGames');
                                               document.getElementById('stop_games_btn').disabled = !areAnyChecked('selectedGames');
                                               setSelectAllCheckbox('selectedGames', 'selectAllGamesMultiplayer')">
                                </div>
                            </td>
                            <td><%=gid%></td>
                            <td>
                                <a class="btn btn-sm btn-primary" id="<%="observe-"+g.getId()%>"
                                   href="<%=request.getContextPath() + Paths.BATTLEGROUND_GAME%>?gameId=<%=gid%>">
                                    Observe
                                </a>
                            </td>
                            <td>
                                <a href="#" data-bs-toggle="modal" data-bs-target="#modalCUTFor<%=gid%>">
                                    <%=cut.getAlias()%>
                                </a>
                                <div id="modalCUTFor<%=gid%>" class="modal fade" tabindex="-1">
                                    <div class="modal-dialog">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <h5 class="modal-title"><%=cut.getAlias()%></h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <pre class="readonly-pre"><textarea
                                                        class="readonly-textarea classPreview"
                                                        id="sut<%=gid%>" name="cut<%=g.getCUT().getId()%>" cols="80"
                                                        rows="30"></textarea></pre>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td><%=UserDAO.getUserById(g.getCreatorId()).getUsername()%></td>
                            <td><%=g.getAttackerPlayers().size()%></td>
                            <td><%=g.getDefenderPlayers().size()%></td>
                            <td><%=g.getLevel()%></td>
                            <td>
                                <button class="<%=startStopButtonClass%>" type="submit" value="<%=gid%>" name="start_stop_btn"
                                        onclick="<%=startStopButtonAction%>" id="<%="start_stop_"+g.getId()%>">
                                    <i class="<%=startStopButtonIcon%>"></i>
                                </button>
                            </td>
                        </tr>
                        <%
                            List<List<String>> playerInfos = AdminDAO.getPlayersInfo(gid);
                            Map<Integer, Integer> userIdByPlayerId = playerInfos.stream().collect(Collectors.toMap(
                                    info -> Integer.parseInt(info.get(0)),
                                    info -> UserDAO.getUserForPlayer(Integer.parseInt(info.get(0))).getId()
                            ));
                            if (!userIdByPlayerId.values().stream().anyMatch(
                                    userId -> userId != Constants.DUMMY_ATTACKER_USER_ID && userId != Constants.DUMMY_DEFENDER_USER_ID)) {
                        %>
                            <tr class="players-table" hidden>
                                <td colspan="9" class="text-muted ps-5">
                                    There are no players active in this game.
                                </td>
                            </tr>
                        <%
                            } else {
                        %>
                            <tr class="players-table" hidden>
                                <td colspan="9" class="p-0 ps-5">
                                    <table class="table m-0">
                                        <thead>
                                            <tr>
                                                <th>Game Score</th>
                                                <th>Name</th>
                                                <th>Submissions</th>
                                                <th>Last Action</th>
                                                <th>Points</th>
                                                <th>Total Score</th>
                                                <th>Switch Role</th>
                                                <th>Remove Player</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <%
                                                boolean firstAttacker = true;
                                                boolean firstDefender = true;

                                                // Compute the cumulative sum of each role score. Not sure if this is how is done in the scoreboard
                                                int gameScoreAttack = 0;
                                                int gameScoreDefense = 0;

                                                for (List<String> playerInfo : playerInfos) {
                                                    if (Role.ATTACKER.equals(Role.valueOf(playerInfo.get(2)))) {
                                                        gameScoreAttack = gameScoreAttack + Integer.parseInt(playerInfo.get(4));
                                                    } else if (Role.DEFENDER.equals(Role.valueOf(playerInfo.get(2)))) {
                                                        gameScoreDefense = gameScoreDefense + Integer.parseInt(playerInfo.get(4));
                                                    }
                                                }

                                                for (List<String> playerInfo : playerInfos) {
                                                    int playerId = Integer.parseInt(playerInfo.get(0));
                                                    int userID = userIdByPlayerId.get(playerId);

                                                    // Do not visualize system users.
                                                    // Note this does not prevent someone to forge move player requests which remove system users from the game
                                                    if (userID == Constants.DUMMY_ATTACKER_USER_ID || userID == Constants.DUMMY_DEFENDER_USER_ID) {
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
                                                    int gameScore = AdminMonitorGames.getPlayerScore(g, playerId);
                                            %>
                                                <tr>
                                                    <%
                                                        if (firstAttacker && role.equals(Role.ATTACKER)) {
                                                            firstAttacker = false;
                                                    %>
                                                        <td style="background: <%=color%>;"><%=gameScoreAttack%></td>
                                                    <%
                                                        } else if (firstDefender && role.equals(Role.DEFENDER)) {
                                                            firstDefender = false;
                                                    %>
                                                        <td style="background: <%=color%>;"><%=gameScoreDefense%></td>
                                                    <%
                                                        } else {
                                                    %>
                                                        <td style="border: none;"></td>
                                                    <%
                                                        }
                                                    %>
                                                    <td style="background: <%=color%>;"><%=userName%></td>
                                                    <td style="background: <%=color%>;"><%=submissionsCount%></td>
                                                    <td style="background: <%=color%>;"><%=lastSubmissionTS%></td>
                                                    <td style="background: <%=color%>;"><%=gameScore%></td>
                                                    <td style="background: <%=color%>;"><%=totalScore%></td>
                                                    <td style="background: <%=color%>;">
                                                        <button class="btn btn-sm btn-danger" value="<%=playerId + "-" + gid + "-" + role%>"
                                                                onclick="return confirm('Are you sure you want to permanently remove this player? \n' +
                                                                    'This will also delete ALL of his tests, mutants and claimed equivalences ' +
                                                                    'and might create inconsistencies in the Game.');"
                                                                id="<%="switch_player_"+playerId+"_game_"+gid%>"
                                                                name="activeGameUserSwitchButton">
                                                            <i class="fa fa-exchange"></i>
                                                        </button>
                                                    </td>
                                                    <td style="background: <%=color%>;">
                                                        <button class="btn btn-sm btn-danger" value="<%=playerId + "-" + gid%>"
                                                                onclick="return confirm('Are you sure you want to permanently remove this player? \n' +
                                                                    'This will also delete ALL of his tests, mutants and claimed equivalences ' +
                                                                    'and might create inconsistencies in the Game.');"
                                                                id="<%="remove_player_"+playerId+"_game_"+gid%>"
                                                                name="activeGameUserRemoveButton">
                                                            Remove
                                                        </button>
                                                    </td>
                                                </tr>
                                            <%
                                                }
                                            %>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                    <%
                            }
                        }
                    %>
                </tbody>
            </table>
        <%
            }
        %>

        <%-- ------------------------------------------------------------------------------------------------------ --%>

        <h3 class="mb-3 mt-4">Current Melee Games</h3>
        <%
            List<MeleeGame> meleeGames = MeleeGameDAO.getUnfinishedMeleeGamesCreatedBy(login.getUserId());
            if (meleeGames.isEmpty()) {
        %>
            <div class="card">
                <div class="card-body text-muted text-center">
                    There are currently no unfinished melee games.
                </div>
            </div>
        <%
            } else {
        %>
            <table id="table-melee" class="table">
                <thead>
                    <tr>
                        <th>
                            <div class="form-check">
                                <input type="checkbox" class="form-check-input" id="selectAllGamesMelee">
                            </div>
                        </th>
                        <th>ID</th>
                        <th></th>
                        <th>Class</th>
                        <th>Creator</th>
                        <th>Players</th>
                        <th>Level</th>
                        <th>
                            <input type="checkbox" id="togglePlayersActiveMelee" class="btn-check" autocomplete="off">
                            <label for="togglePlayersActiveMelee" class="btn btn-sm btn-outline-secondary"
                                   title="Show list of Players for each Game.">
                                <i class="fa fa-eye"></i>
                            </label>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        for (MeleeGame g : meleeGames) {
                            GameClass cut = g.getCUT();
                            String startStopButtonIcon = g.getState().equals(GameState.ACTIVE) ?
                                    "fa fa-stop" : "fa fa-play";
                            String startStopButtonClass = g.getState().equals(GameState.ACTIVE) ?
                                    "btn btn-sm btn-danger" : "btn btn-sm btn-primary";
                            String startStopButtonAction = g.getState().equals(GameState.ACTIVE) ?
                                    "return confirm('Are you sure you want to stop this Game?');" : "";
                            int gid = g.getId();
                    %>
                        <tr id="<%="game_row_"+gid%>">
                            <td>
                                <div class="form-check">
                                    <input type="checkbox" name="selectedGames" id="<%="selectedGames_"+gid%>" value="<%= gid%>" class="form-check-input"
                                           onchange="document.getElementById('start_games_btn').disabled = !areAnyChecked('selectedGames');
                                               document.getElementById('stop_games_btn').disabled = !areAnyChecked('selectedGames');
                                               setSelectAllCheckbox('selectedGames', 'selectAllGamesMelee')">
                                </div>
                            </td>
                            <td><%=gid%></td>
                            <td>
                                <a class="btn btn-sm btn-primary" id="<%="observe-"+g.getId()%>"
                                   href="<%=request.getContextPath() + Paths.MELEE_GAME%>?gameId=<%=gid%>">
                                    Observe
                                </a>
                            </td>
                            <td>
                                <a href="#" data-bs-toggle="modal" data-bs-target="#modalCUTFor<%=gid%>">
                                    <%=cut.getAlias()%>
                                </a>
                                <div id="modalCUTFor<%=gid%>" class="modal fade" tabindex="-1">
                                    <div class="modal-dialog">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <h5 class="modal-title"><%=cut.getAlias()%></h5>
                                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                            </div>
                                            <div class="modal-body">
                                                <pre class="readonly-pre"><textarea
                                                        class="readonly-textarea"
                                                        id="sut<%=gid%>" name="cut<%=g.getCUT().getId()%>" cols="80"
                                                        rows="30"></textarea></pre>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </td>
                            <td><%=UserDAO.getUserById(g.getCreatorId()).getUsername()%></td>
                            <td><%=g.getPlayers().size()%></td>
                            <td><%=g.getLevel()%></td>
                            <td>
                                <button class="<%=startStopButtonClass%>" type="submit" value="<%=gid%>" name="start_stop_btn"
                                        onclick="<%=startStopButtonAction%>" id="<%="start_stop_"+g.getId()%>">
                                    <i class="<%=startStopButtonIcon%>"></i>
                                </button>
                            </td>
                        </tr>
                        <%
                            List<List<String>> playerInfos = AdminDAO.getPlayersInfo(gid);
                            Map<Integer, Integer> userIdByPlayerId = playerInfos.stream().collect(Collectors.toMap(
                                    info -> Integer.parseInt(info.get(0)),
                                    info -> UserDAO.getUserForPlayer(Integer.parseInt(info.get(0))).getId()
                            ));
                            if (!userIdByPlayerId.values().stream().anyMatch(
                                    userId -> userId != Constants.DUMMY_ATTACKER_USER_ID && userId != Constants.DUMMY_DEFENDER_USER_ID)) {
                        %>
                            <tr class="players-table" hidden>
                                <td colspan="9" class="text-muted ps-5">
                                    There are no players active in this game.
                                </td>
                            </tr>
                        <%
                            } else {
                        %>
                            <tr class="players-table" hidden>
                                <td colspan="8" class="p-0 ps-5">
                                    <table class="table m-0">
                                        <thead>
                                            <tr>
                                                <th>Game Score</th>
                                                <th>Name</th>
                                                <th>Submissions</th>
                                                <th>Last Action</th>
                                                <th>Points</th>
                                                <th>Total Score</th>
                                                <th>Remove Player</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <%
                                                boolean firstPlayer = true;

                                                // Compute the cumulative sum of each role score. Not sure if this is how is done in the scoreboard
                                                int gameScore = 0;

                                                for (List<String> playerInfo : playerInfos) {
                                                    if (Role.DEFENDER.equals(Role.valueOf(playerInfo.get(2)))) {
                                                        gameScore = gameScore + Integer.parseInt(playerInfo.get(4));
                                                    }
                                                }

                                                for (List<String> playerInfo : playerInfos) {
                                                    int playerId = Integer.parseInt(playerInfo.get(0));
                                                    int userID = userIdByPlayerId.get(playerId);

                                                    // Do not visualize system users.
                                                    // Note this does not prevent someone to forge move player requests which remove system users from the game
                                                    if (userID == Constants.DUMMY_ATTACKER_USER_ID || userID == Constants.DUMMY_DEFENDER_USER_ID) {
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
                                                <tr>
                                                    <%
                                                        if (firstPlayer && role.equals(Role.PLAYER)) {
                                                            firstPlayer = false;
                                                    %>
                                                        <td><%=gameScore%></td>
                                                    <%
                                                        } else {
                                                    %>
                                                        <td style="border: none;"></td>
                                                    <%
                                                        }
                                                    %>
                                                    <td><%=userName%></td>
                                                    <td><%= submissionsCount%></td>
                                                    <td><%=lastSubmissionTS%></td>
                                                    <td></td>
                                                    <td><%=totalScore%></td>
                                                    <td>
                                                        <button class="btn btn-sm btn-danger" value="<%=playerId + "-" + gid%>"
                                                                onclick="return confirm('Are you sure you want to permanently remove this player? \n' +
                                                                    'This will also delete ALL of his tests, mutants and claimed equivalences ' +
                                                                    'and might create inconsistencies in the Game.');"
                                                                id="<%="remove_player_"+playerId+"_game_"+gid%>"
                                                                name="activeGameUserRemoveButton">
                                                            Remove
                                                        </button>
                                                    </td>
                                                </tr>
                                            <%
                                                }
                                            %>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                    <%
                            }
                        }
                    %>
                </tbody>
            </table>
        <%
            }
        %>

        <% if (!multiplayerGames.isEmpty() || !meleeGames.isEmpty()) { %>
            <div class="row g-2 mt-3">
                <div class="col-auto">
                    <button class="btn btn-md btn-primary" type="submit" name="games_btn" id="start_games_btn"
                            disabled value="Start Games">
                        Start Games
                    </button>
                </div>
                <div class="col-auto">
                    <button class="btn btn-md btn-danger" type="submit" name="games_btn" id="stop_games_btn"
                            onclick="return confirm('Are you sure you want to stop the selected Games?');"
                            disabled value="Stop Games">
                        Stop Games
                    </button>
                </div>
            </div>
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

        const showMultiplayerDetails = function (showDetails) {
            if (showDetails) {
                $('#table-multiplayer .players-table').removeAttr('hidden');
            } else {
                $('#table-multiplayer .players-table').attr('hidden', '');
            }
        };

        const showMeleeDetails = function (showDetails) {
            if (showDetails) {
                $('#table-melee .players-table').removeAttr('hidden');
            } else {
                $('#table-melee .players-table').attr('hidden', '');
            }
        };

        $('#togglePlayersActiveMultiplayer').on('change', function () {
            const showPlayers = this.checked;
            localStorage.setItem("showActivePlayersMultiplayer", JSON.stringify(showPlayers));
            showMultiplayerDetails(showPlayers);
        });

        $('#togglePlayersActiveMelee').on('change', function () {
            const showPlayers = this.checked;
            localStorage.setItem("showActivePlayersMelee", JSON.stringify(showPlayers));
            showMeleeDetails(showPlayers);
        });


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
            const showMultiplayer = localStorage.getItem("showActivePlayersMultiplayer") === "true";
            document.getElementById('togglePlayersActiveMultiplayer').checked = showMultiplayer;
            showMultiplayerDetails(showMultiplayer);

            const showMelee = localStorage.getItem("showActivePlayersMelee") === "true";
            document.getElementById('togglePlayersActiveMelee').checked = showMelee;
            showMeleeDetails(showMelee);
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
                    mode: "text/x-java",
                    autoRefresh: true
                });
                editor.setSize("100%", 500);
                ClassAPI.getAndSetEditorValue(textarea, editor);
            }
        });
    </script>

</div>

<%@ include file="/jsp/footer.jsp" %>

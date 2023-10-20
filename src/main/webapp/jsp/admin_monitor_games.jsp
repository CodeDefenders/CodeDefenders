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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.Duration" %>
<%@ page import="org.codedefenders.servlets.admin.AdminMonitorGames" %>
<%@ page import="org.codedefenders.game.multiplayer.MeleeGame" %>
<%@ page import="java.util.Map" %>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Monitor Games"); %>

<jsp:include page="/jsp/header.jsp"/>

<%
    List<MultiplayerGame> multiplayerGames = (List<MultiplayerGame>) request.getAttribute("multiplayerGames");
    Map<Integer, String> multiplayerGameCreatorNames = (Map<Integer, String>) request.getAttribute("multiplayerGameCreatorNames");
    Map<Integer, List<List<String>>> multiplayerPlayerInfoForGame = (Map<Integer, List<List<String>>>) request.getAttribute("multiplayerPlayersInfoForGame");
    Map<Integer, Integer> multiplayerUserIdForPlayerIds = (Map<Integer, Integer>) request.getAttribute("multiplayerUserIdForPlayerIds");
%>

<%!
    /* Quick fix to get striped tables to display properly without DataTables.
       Later, the tables on this page should be converted to DataTables. */
    int row = 1;
    String oddEven() {
        return row++ % 2 == 0 ? "even" : "odd";
    }
    void resetOddEven() {
        row = 1;
    }
%>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminMonitorGames"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <form id="games" action="${url.forPath(Paths.ADMIN_MONITOR)}" method="post" autocomplete="off">
        <input type="hidden" name="formType" value="startStopGame">

        <h3 class="mb-3">Multiplayer Games</h3>
        <table id="table-multiplayer" class="table table-v-align-middle table-striped">
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
                            <i class="fa fa-expand btn-check-inactive"></i>
                            <i class="fa fa-compress btn-check-active"></i>
                        </label>
                    </th>
                </tr>
            </thead>
            <tbody>
                <% resetOddEven(); %>
                <% if (multiplayerGames.isEmpty()) { %>
                    <tr>
                        <td colspan="100" class="text-center">
                            You don't control any unfinished multiplayer games at the moment.
                        </td>
                    </tr>
                <% } %>
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
                    <tr id="<%="game_row_"+gid%>" class="<%=oddEven()%>">
                        <td>
                            <div class="form-check">
                                <input type="checkbox" name="selectedGames" id="<%="selectedGames_"+gid%>" value="<%=gid%>" class="form-check-input">
                            </div>
                        </td>
                        <td><%=gid%></td>
                        <td>
                            <% if (g.getRole(login.getUserId()) != Role.NONE) { %>
                                <a class="btn btn-sm btn-primary" id="<%="observe-"+g.getId()%>"
                                   href="${url.forPath(Paths.BATTLEGROUND_GAME)}?gameId=<%=gid%>">
                                    Observe
                                </a>
                            <% } %>
                        </td>
                        <td>
                            <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gid%>">
                                <%=cut.getAlias()%>
                            </a>
                            <% pageContext.setAttribute("classId", g.getCUT().getId()); %>
                            <% pageContext.setAttribute("classAlias", cut.getAlias()); %>
                            <% pageContext.setAttribute("gameId", gid); %>
                            <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-for-game-${gameId}"/>
                        </td>
                        <td><%=multiplayerGameCreatorNames.get(gid)%></td>
                        <td><%=g.getAttackerPlayers().size()%></td>
                        <td><%=g.getDefenderPlayers().size()%></td>
                        <td><%=g.getLevel()%></td>
                        <td>
                            <div class="d-flex gap-1">
                                <button class="<%=startStopButtonClass%>" type="submit" value="<%=gid%>" name="start_stop_btn"
                                        onclick="<%=startStopButtonAction%>" id="<%="start_stop_"+g.getId()%>"
                                        title="Start/Stop Game">
                                    <i class="<%=startStopButtonIcon%>"></i>
                                </button>
                                <button class="btn btn-sm btn-warning" type="submit" value="<%=gid%>" name="rematch_btn"
                                        onclick="return confirm('Are you sure you want to create a rematch for this game?')"
                                        id="<%="rematch_"+g.getId()%>"
                                        title="Rematch">
                                    <i class="fa fa-repeat"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                    <%
                        List<List<String>> playersInfo = multiplayerPlayerInfoForGame.get(gid);
                        if (multiplayerUserIdForPlayerIds.values().stream().noneMatch(
                                userId -> userId != Constants.DUMMY_ATTACKER_USER_ID && userId != Constants.DUMMY_DEFENDER_USER_ID)) {
                    %>
                        <tr class="players-table" hidden>
                            <td colspan="100" class="text-center">There are no players active in this game.</td>
                        </tr>
                    <%
                        } else {
                    %>
                        <tr class="players-table" hidden>
                            <td colspan="100">
                                <div class="child-row-wrapper py-0">
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

                                                for (List<String> playerInfo : playersInfo) {
                                                    if (Role.ATTACKER.equals(Role.valueOf(playerInfo.get(2)))) {
                                                        gameScoreAttack = gameScoreAttack + Integer.parseInt(playerInfo.get(4));
                                                    } else if (Role.DEFENDER.equals(Role.valueOf(playerInfo.get(2)))) {
                                                        gameScoreDefense = gameScoreDefense + Integer.parseInt(playerInfo.get(4));
                                                    }
                                                }

                                                for (List<String> playerInfo : playersInfo) {
                                                    int playerId = Integer.parseInt(playerInfo.get(0));
                                                    int userID = multiplayerUserIdForPlayerIds.get(playerId);

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
                                                    String color = role == Role.ATTACKER ? "bg-attacker-light" : "bg-defender-light";
                                                    int gameScore = AdminMonitorGames.getPlayerScore(g, playerId);
                                            %>
                                                <tr>
                                                    <%
                                                        if (firstAttacker && role.equals(Role.ATTACKER)) {
                                                            firstAttacker = false;
                                                    %>
                                                        <td class="<%=color%>"><%=gameScoreAttack%></td>
                                                    <%
                                                        } else if (firstDefender && role.equals(Role.DEFENDER)) {
                                                            firstDefender = false;
                                                    %>
                                                        <td class="<%=color%>"><%=gameScoreDefense%></td>
                                                    <%
                                                        } else {
                                                    %>
                                                        <td style="border: none;"></td>
                                                    <%
                                                        }
                                                    %>
                                                    <td class="<%=color%>"><%=userName%></td>
                                                    <td class="<%=color%>"><%=submissionsCount%></td>
                                                    <td class="<%=color%>"><%=lastSubmissionTS%></td>
                                                    <td class="<%=color%>"><%=gameScore%></td>
                                                    <td class="<%=color%>"><%=totalScore%></td>
                                                    <td class="<%=color%>">
                                                        <button class="btn btn-sm btn-danger" value="<%=playerId + "-" + gid + "-" + role%>"
                                                                onclick="return confirm('Are you sure you want to change the role of this player? \n' +
                                                                    'This will keep the score, but deletes ALL of his tests, mutants and claimed equivalences ' +
                                                                    'and might create inconsistencies in the game.');"
                                                                id="<%="switch_player_"+playerId+"_game_"+gid%>"
                                                                name="activeGameUserSwitchButton">
                                                            <i class="fa fa-exchange"></i>
                                                        </button>
                                                    </td>
                                                    <td class="<%=color%>">
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
                                </div>
                            </td>
                        </tr>
                <%
                        }
                    }
                %>
            </tbody>
        </table>

        <%-- ------------------------------------------------------------------------------------------------------ --%>

        <h3 class="mb-3 mt-4">Melee Games</h3>
        <%
            List<MeleeGame> meleeGames = (List<MeleeGame>) request.getAttribute("meleeGames");
            Map<Integer, String> meleeGameCreatorNames = (Map<Integer, String>) request.getAttribute("meleeGameCreatorNames");
            Map<Integer, List<List<String>>> meleePlayersInfoForGame = (Map<Integer, List<List<String>>>) request.getAttribute("meleePlayersInfoForGame");
            Map<Integer, Integer> meleeUserIdForPlayerIds = (Map<Integer, Integer>) request.getAttribute("meleeUserIdForPlayerIds");
        %>
        <table id="table-melee" class="table table-v-align-middle table-striped">
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
                            <i class="fa fa-expand btn-check-inactive"></i>
                            <i class="fa fa-compress btn-check-active"></i>
                        </label>
                    </th>
                </tr>
            </thead>
            <tbody>
                <% resetOddEven(); %>
                <% if (meleeGames.isEmpty()) { %>
                    <tr>
                        <td colspan="100" class="text-center">
                            You don't control any unfinished melee games at the moment.
                        </td>
                    </tr>
                <% } %>
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
                    <tr id="<%="game_row_"+gid%>" class="<%=oddEven()%>">
                        <td>
                            <div class="form-check">
                                <input type="checkbox" name="selectedGames" id="<%="selectedGames_"+gid%>" value="<%= gid%>" class="form-check-input">
                            </div>
                        </td>
                        <td><%=gid%></td>
                        <td>
                            <% if (g.getRole(login.getUserId()) != Role.NONE) { %>
                                <a class="btn btn-sm btn-primary" id="<%="observe-"+g.getId()%>"
                                   href="${url.forPath(Paths.MELEE_GAME)}?gameId=<%=gid%>">
                                    Observe
                                </a>
                            <% } %>
                        </td>
                        <td>
                            <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gid%>">
                                <%=cut.getAlias()%>
                            </a>
                            <% pageContext.setAttribute("classId", g.getCUT().getId()); %>
                            <% pageContext.setAttribute("classAlias", cut.getAlias()); %>
                            <% pageContext.setAttribute("gameId", gid); %>
                            <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-for-game-${gameId}"/>
                        </td>
                        <td><%=meleeGameCreatorNames.get(gid)%></td>
                        <td><%=g.getPlayers().size()%></td>
                        <td><%=g.getLevel()%></td>
                        <td>
                            <div class="d-flex gap-1">
                                <button class="<%=startStopButtonClass%>" type="submit" value="<%=gid%>" name="start_stop_btn"
                                        onclick="<%=startStopButtonAction%>" id="<%="start_stop_"+g.getId()%>"
                                        title="Start/Stop Game">
                                    <i class="<%=startStopButtonIcon%>"></i>
                                </button>
                                <button class="btn btn-sm btn-warning" type="submit" value="<%=gid%>" name="rematch_btn"
                                        onclick="return confirm('Are you sure you want to create a rematch for this game?')"
                                        id="<%="rematch_"+g.getId()%>"
                                        title="Rematch">
                                    <i class="fa fa-repeat"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                    <%
                        List<List<String>> playerInfos = meleePlayersInfoForGame.get(gid);
                        if (meleeUserIdForPlayerIds.values().stream().noneMatch(
                                userId -> userId != Constants.DUMMY_ATTACKER_USER_ID && userId != Constants.DUMMY_DEFENDER_USER_ID)) {
                    %>
                        <tr class="players-table" hidden>
                            <td colspan="100" class="text-center">There are no players active in this game.</td>
                        </tr>
                    <%
                        } else {
                    %>
                        <tr class="players-table" hidden>
                            <td colspan="100">
                                <div class="child-row-wrapper py-0">
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
                                                    int userID = meleeUserIdForPlayerIds.get(playerId);

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
                                </div>
                            </td>
                        </tr>
                <%
                        }
                    }
                %>
            </tbody>
        </table>

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

    <script type="module">
        import $ from '${url.forPath("/js/jquery.mjs")}';


        $('#selectAllGamesMultiplayer').click(function () {
            $(this).closest('table')
                    .find('tbody')
                    .find(':checkbox')
                    .prop('checked', this.checked);
            const anyChecked = areAnyChecked('selectedGames');
            document.getElementById('start_games_btn').disabled = !anyChecked;
            document.getElementById('stop_games_btn').disabled = !anyChecked;
        });

        $('#selectAllGamesMelee').click(function () {
            $(this).closest('table')
                    .find('tbody')
                    .find(':checkbox')
                    .prop('checked', this.checked);
            const anyChecked = areAnyChecked('selectedGames');
            document.getElementById('start_games_btn').disabled = !anyChecked;
            document.getElementById('stop_games_btn').disabled = !anyChecked;
        });

        document.getElementById('table-multiplayer').addEventListener('change', function (event) {
            const checkbox = event.target.closest('[name="selectedGames"]');
            if (checkbox === null) return;

            const anyChecked = areAnyChecked('selectedGames');
            document.getElementById('start_games_btn').disabled = !anyChecked;
            document.getElementById('stop_games_btn').disabled = !anyChecked;
        });

        document.getElementById('table-melee').addEventListener('change', function (event) {
            const checkbox = event.target.closest('[name="selectedGames"]');
            if (checkbox === null) return;

            const anyChecked = areAnyChecked('selectedGames');
            document.getElementById('start_games_btn').disabled = !anyChecked;
            document.getElementById('stop_games_btn').disabled = !anyChecked;
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
            sessionStorage.setItem("showActivePlayersMultiplayer", JSON.stringify(showPlayers));
            showMultiplayerDetails(showPlayers);
        });

        $('#togglePlayersActiveMelee').on('change', function () {
            const showPlayers = this.checked;
            sessionStorage.setItem("showActivePlayersMelee", JSON.stringify(showPlayers));
            showMeleeDetails(showPlayers);
        });


        /* Check only in the local table if all checkboxes are checked. */
        function setSelectAllCheckbox(checkboxesName, selectAllCheckboxId) {
            const checkboxes = $(this).closest('table')
                    .find('tbody')
                    .find("[name='" + checkboxesName + "']");
            let allChecked = false;
            checkboxes.each(function (index, element) {
                allChecked = allChecked && element.checked;
            });
            document.getElementById(selectAllCheckboxId).checked = allChecked;
        }

        /* Check in both tables if any checkboxes are checked. */
        function areAnyChecked(name) {
            const checkboxes = $("[name='" + name + "']");
            let anyChecked = false;
            checkboxes.each(function (index, element) {
                anyChecked = anyChecked || element.checked;
            });
            return anyChecked;
        }

        $(document).ready(function () {
            const showMultiplayer = sessionStorage.getItem("showActivePlayersMultiplayer") === "true";
            document.getElementById('togglePlayersActiveMultiplayer').checked = showMultiplayer;
            showMultiplayerDetails(showMultiplayer);

            const showMelee = sessionStorage.getItem("showActivePlayersMelee") === "true";
            document.getElementById('togglePlayersActiveMelee').checked = showMelee;
            showMeleeDetails(showMelee);
        });
    </script>

</div>

<%@ include file="/jsp/footer.jsp" %>

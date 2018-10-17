<%@ page import="org.apache.commons.collections.ListUtils" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.leaderboard.Entry" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.servlets.admin.AdminCreateGames" %>
<%@ page import="org.codedefenders.validation.CodeValidator" %>
<%@ page import="java.util.List" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminCreateGames"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <form id="insertGames" action="admin" method="post">
        <input type="hidden" name="formType" value="insertGames"/>
        <h3>Staged Games</h3>
        <%
            List<MultiplayerGame> createdGames = (List<MultiplayerGame>) session.getAttribute(AdminCreateGames.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
            List<List<Integer>> attackerIdsList = (List<List<Integer>>) session.getAttribute(AdminCreateGames.ATTACKER_LISTS_SESSION_ATTRIBUTE);
            List<List<Integer>> defenderIdsList = (List<List<Integer>>) session.getAttribute(AdminCreateGames.DEFENDER_LISTS_SESSION_ATTRIBUTE);
            if (createdGames == null || createdGames.isEmpty()) {
        %>
        <div class="panel panel-default">
            <div class="panel-body" style="    color: gray;    text-align: center;">
                There are currently no staged multiplayer games.
            </div>
        </div>
        <%
        } else {
        %>
        <table id="tableCreatedGames"
               class="table table-striped table-hover table-responsive table-paragraphs games-table dataTable display">
            <thead>
            <tr>
                <th><input type="checkbox" id="selectAllTempGames"
                           onchange="document.getElementById('insert_games_btn').disabled = !this.checked;
                           document.getElementById('delete_games_btn').disabled = !this.checked">
                </th>
                <th>ID</th>
                <th>Class</th>
                <th>Level</th>
                <th>Starting</th>
                <th>Finishing</th>
                <th>Players
                    <div class="row">
                        <div class="col-sm-2">Name</div>
                        <div class="col-sm-4">Last Role</div>
                        <div class="col-sm-3">Score</div>
                        <a id="togglePlayersCreated" class="btn btn-sm btn-default">
                            <span id = "togglePlayersCreatedSpan" class="glyphicon glyphicon-alert"></span>
                        </a>
                    </div>
                </th>
            </tr>

            </thead>
            <tbody>
            <%
                for (int i = 0; i < createdGames.size(); ++i) {
                    MultiplayerGame g = createdGames.get(i);
                    List<Integer> attackerIds = attackerIdsList.get(i);
                    List<Integer> defenderIds = defenderIdsList.get(i);
                    GameClass cut = g.getCUT();

            %>
            <tr id="<%="temp_games_"+i%>">
                <td>
                    <input type="checkbox" name="selectedTempGames" id="<%="selectedTempGames_"+i%>" value="<%= i%>" onchange=
                            "document.getElementById('insert_games_btn').disabled = !areAnyChecked('selectedTempGames');
                            document.getElementById('delete_games_btn').disabled = !areAnyChecked('selectedTempGames');
                            setSelectAllCheckbox('selectedTempGames', 'selectAllTempGames');">
                </td>
                <td><%=i%>
                </td>
                <td class="col-sm-2">
                    <a href="#" data-toggle="modal" data-target="#modalCUTFor<%=g.getId()%>">
                        <%=cut.getAlias()%>
                    </a>
                    <div id="modalCUTFor<%=g.getId()%>" class="modal fade" role="dialog" style="text-align: left;">
                        <div class="modal-dialog">
                            <!-- Modal content-->
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                    <h4 class="modal-title"><%=cut.getAlias()%>
                                    </h4>
                                </div>
                                <div class="modal-body">
                                    <pre class="readonly-pre"><textarea
                                            class="readonly-textarea classPreview"
                                            id="sut<%=g.getId()%>" name="cut<%=g.getId()%>" cols="80"
                                            rows="30"><%=cut.getAsHTMLEscapedString()%></textarea></pre>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </td>
                <td><%= g.getLevel().name() %>
                </td>
                <td class="col-sm-2"><%= g.getStartDateTime() %>
                </td>
                <td class="col-sm-2"><%= g.getFinishDateTime() %>
                </td>
                <td class="col-sm-4">
                    <div id="playersTableHidden" style="color: lightgray;"> (hidden)</div>
                    <table id="playersTableCreated" hidden>
                        <%
                            List<Integer> attackerAndDefenderIds = ListUtils.union(attackerIds, defenderIds);
                            for (int id : attackerAndDefenderIds) {
                                String userName = DatabaseAccess.getUser(id).getUsername();
                                //Timestamp ts = AdminDAO.getLastLogin(aid);
                                Role lastRole = AdminDAO.getLastRole(id);
                                Entry score = AdminDAO.getScore(id);
                                int totalScore = score.getTotalPoints();
                                String color = attackerIds.contains(id) ? "#edcece" : "#ced6ed";
                        %>
                        <tr style="background: <%= color %>">
                            <td class="col-sm-1"><%= userName %>
                            </td>
                            <td class="col-sm-1"><%= lastRole %>
                            </td>
                            <td class="col-sm-1"><%= totalScore %>
                            </td>
                            <td>
                                <button class="btn btn-sm btn-primary"
                                        value="<%=String.valueOf(i) + "-" + String.valueOf(id)%>"
                                        id="<%="switch_player_"+id+"_game_"+i%>"
                                        name="tempGameUserSwitchButton">
                                    <span class="glyphicon glyphicon-transfer"></span>
                                </button>
                            </td>
                            <td>
                                <button class="btn btn-sm btn-danger"
                                        value="<%=String.valueOf(i) + "-" + String.valueOf(id)%>"
                                        id="<%="remove_player_"+id+"_game_"+i%>"
                                        name="tempGameUserRemoveButton">
                                    <span class="glyphicon glyphicon-trash"></span>
                                </button>
                            </td>
                        </tr>
                        <% } %>
                    </table>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <button class="btn btn-md btn-primary" type="submit" name="games_btn" id="insert_games_btn"
                disabled value="insert Games">
            Create games
        </button>
        <button class="btn btn-md btn-danger" type="submit" name="games_btn" id="delete_games_btn"
                onclick="return confirm('Are you sure you want to discard the selected Games?');"
                disabled value="delete Games">
            Discard games
        </button>
        <% }
        %>

    </form>
    <form id="users" action="admin" method="post">
        <input type="hidden" name="formType" value="createGame">

        <h3>Unassigned Users</h3>
        <table id="tableAddUsers"
               class="table table-striped table-hover table-responsive table-paragraphs games-table dataTable display">
            <thead>
            <tr>
                <th><input type="checkbox" id="selectAllUsers"
                           onchange="document.getElementById('submit_users_btn').disabled = !this.checked">
                </th>
                <th>User ID</th>
                <th>User</th>
                <th>Last Role</th>
                <th>Total Score</th>
                <th>Last Login</th>
                <th>Add to existing Game</th>
            </tr>
            </thead>
            <tbody>

            <%
                List<MultiplayerGame> availableGames = AdminDAO.getAvailableGames();
                createdGames = (List<MultiplayerGame>) session.getAttribute(AdminCreateGames.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
                List<List<String>> unassignedUsersInfo = AdminCreateGames.getUnassignedUsers(attackerIdsList, defenderIdsList);
                if (unassignedUsersInfo.isEmpty()) {
            %>

            <div class="panel panel-default">
                <div class="panel-body" style="    color: gray;    text-align: center;">
                    There are currently no created unassigned users.
                </div>
            </div>

            <%
            } else {
                int currentUserID = (Integer) session.getAttribute("uid");
                for (List<String> userInfo : unassignedUsersInfo) {
                    int uid = Integer.valueOf(userInfo.get(0));
                    String username = userInfo.get(1);
                    String lastLogin = userInfo.get(3);
                    String lastRole = userInfo.get(4);
                    String totalScore = userInfo.get(5);
            %>

            <tr id="<%="user_row_"+uid%>">
                <td>
                    <% if (uid != currentUserID) { %>
                    <input type="checkbox" name="selectedUsers" id="selectedUsers" value="<%= uid%>" onchange =
                            "updateCheckbox(this.value, this.checked);">
                    <%}%>
                </td>
                <td><%= uid%>
                    <input type="hidden" name="added_uid" value=<%=uid%>>
                </td>
                <td><%= username %>
                </td>
                <td><%= lastRole %>
                </td>
                <td><%= totalScore %>
                </td>
                <td><%= lastLogin %>
                </td>
                <td id="<%="addToExistingGameTd_"+uid%>" style="padding-top:3px; padding-bottom:3px; ">
                    <div id="<%="game_"+uid%>" style="max-width: 150px; float: left;">
                        <select name="<%="game_" + uid%>" class="form-control selectpicker" data-size="small"
                                id="game">
                            <% for (MultiplayerGame g : availableGames) { %>
                            <option value="<%=g.getId()%>"><%=String.valueOf(g.getId()) + ": " + g.getCUT().getAlias()%>
                            </option>
                            <%
                                }
                                if (createdGames != null) {
                                    for (int gameIndex = 0; gameIndex < createdGames.size(); ++gameIndex) {
                                        String classAlias = createdGames.get(gameIndex).getCUT().getAlias();
                            %>
                            <option style="color:gray"
                                    value=<%="T" + String.valueOf(gameIndex)%>><%="T" + String.valueOf(gameIndex)
                                    + ": " + classAlias%>
                            </option>
                            <%}%>
                            <%}%>
                        </select>
                    </div>
                    <div id="<%="role_"+uid%>" style="float: left; max-width: 120px; margin-left:2px">
                        <select name="<%="role_" + uid%>" class="form-control selectpicker" data-size="small"
                                id="role">
                            <option value="<%=Role.ATTACKER%>">Attacker</option>
                            <option value="<%=Role.DEFENDER%>">Defender</option>
                        </select>
                    </div>
                    <button class="btn btn-sm btn-primary" type="submit" value="<%=uid%>" name="userListButton"
                            style="margin: 2px; float:left"
                            id="<%="add_"+uid%>"
                            <%=availableGames.isEmpty() && (createdGames == null || createdGames.isEmpty()) ? "disabled" : ""%>>
                        <span class="glyphicon glyphicon-plus"></span>
                    </button>
                </td>
            </tr>

            <%
                    }
                }
            %>
            </tbody>
        </table>

        <input type="text" class="form-control" id="hidden_user_id_list" name="hidden_user_id_list" hidden>

        <div class="form-group">
            <label for="user_name_list">User Names</label>
            <a data-toggle="collapse" href="#demo" style="color:black">
                <span class="glyphicon glyphicon-question-sign"></span>
            </a>
            <div id="demo" class="collapse">
                Newline seperated list of usernames or emails.
                <br/>The union of these users and the users selected in the table above will be used to create games.
                <br/>Only unassigned users are taken into account.
            </div>
            <textarea class="form-control" rows="5" id="user_name_list" name="user_name_list"
                      oninput="document.getElementById('submit_users_btn').disabled =
                        !(areAnyChecked('selectedUsers') || containsText('user_name_list')) || (document.getElementById('cut_select').selectedIndex != 0)"></textarea>
        </div>


        <div class="row">
            <div class="col-sm-2" id="cutDiv">
                <label for="cut_select" class="label-normal">CUT</label>
                <select name="class" class="form-control selectpicker" data-size="large" id="cut_select">
                    <% for (GameClass c : DatabaseAccess.getAllClasses()) { %>
                    <option value="<%=c.getId()%>"><%=c.getAlias()%>
                    </option>
                    <%}%>
                </select>
                <br/>
                <a href="<%=request.getContextPath()%>/games/upload?fromAdmin=true"> Upload Class </a>
            </div>
            <div class="col-sm-1"></div>
            <div class="col-sm-2">
                <label for="roles_group" class="label-normal">Role Assignment</label>
                <div id="roles_group">
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="roles"
                                                           value="<%=AdminCreateGames.RoleAssignmentMethod.RANDOM%>"
                                                           checked="checked"/>
                            Random
                        </label>
                    </div>
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="roles"
                                                           VALUE="<%=AdminCreateGames.RoleAssignmentMethod.OPPOSITE%>"/>
                            Opposite Role
                        </label>
                    </div>
                </div>
            </div>
            <div class="col-sm-3">
                <label for="teams_group" class="label-normal">Team Assignment</label>
                <div id="teams_group">
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="teams"
                                                           value="<%=AdminCreateGames.TeamAssignmentMethod.RANDOM%>"
                                                           checked="checked"/>Random</label>
                    </div>
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="teams"
                                                           VALUE="<%=AdminCreateGames.TeamAssignmentMethod.SCORE_DESCENDING%>"/>
                            Scores descending
                        </label>
                    </div>
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="teams"
                                                           VALUE="<%=AdminCreateGames.TeamAssignmentMethod.SCORE_SHUFFLED%>"/>
                            Scores block shuffled
                        </label>
                    </div>
                </div>
            </div>
            <div class="col-sm-2">
                <label for="attackers" class="label-normal">Attackers per Game</label>
                <input type="number" value="3" id="attackers" name="attackers" min="1" class="form-control"/>
            </div>
            <div class="col-sm-2">
                <label for="defenders" class="label-normal">Defenders per Game</label>
                <input type="number" value="3" id="defenders" name="defenders" min="1" class="form-control"/>
            </div>
        </div>
        <div class="row">
            <div class="col-sm-2">
                <label for="level_group" class="label-normal">Games Level</label>
                <div id="level_group">
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="gamesLevel"
                                                           VALUE="<%=GameLevel.HARD%>" checked="checked"/>
                            Hard</label>
                    </div>
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="gamesLevel"
                                                           value="<%=GameLevel.EASY%>"/>
                            Easy</label>
                    </div>
                </div>
            </div>
            <div class="col-sm-1">
            </div>
            <div class="col-sm-2">
                <label for="state_group" class="label-normal">Games State</label>
                <div id="state_group">
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="gamesState"
                                                           VALUE="<%=GameState.CREATED%>" checked="checked"/>
                            Created</label>
                    </div>
                    <div class="radio">
                        <label class="label-normal"><input TYPE="radio" name="gamesState"
                                                           value="<%=GameState.ACTIVE%>"/>
                            Active</label>
                    </div>
                </div>
            </div>
            <div class="col-sm-3">
            </div>
            <div class="col-sm-2">
                <label for="startTime" class="label-normal">Start Time</label>
                <br/>
                <input type="hidden" id="startTime" name="startTime"/>
                <input class="form-control" name="start_dateTime" id="start_dateTime"
                       data-toggle="popover"
                       data-content="Invalid date or format (expected: YYYY/MM/DD)"
                       data-placement="top"
                       data-trigger="manual"/>
                <div>
                    <input class="ws-2" type="text" name="start_hours" id="start_hours"
                           style="text-align: center;" min="0" max="59"
                           data-toggle="popover"
                           data-content="Hours must be a number between 0 and 23"
                           data-placement="bottom"
                           data-trigger="manual"/>
                    <span>:</span>
                    <input class="ws-2" type="text" name="start_minutes" id="start_minutes"
                           style="text-align: center;" min="0" max="59"
                           data-toggle="popover"
                           data-content="Minutes must be a number between 0 and 59"
                           data-placement="bottom"
                           data-trigger="manual"/>
                </div>
                <span id="finishBeforeStartWarning" data-toggle="popover"
                      data-content="Finish time must be later than selected start time!"
                      data-placement="bottom"
                      data-trigger="manual"></span>
            </div>

            <script>
                $(document).ready(function () {
                    var initialStartDate = new Date();
                    $("#startTime").val(initialStartDate.getTime());
                    $("#start_dateTime").datepicker({dateFormat: "yy/mm/dd"});
                    $("#start_dateTime").datepicker("setDate", initialStartDate);
                    $("#start_hours").val(initialStartDate.getHours());
                    var mins = initialStartDate.getMinutes();
                    var hours = initialStartDate.getHours();
                    if (mins < 10) {
                        // add leading zero to minute representation
                        mins = "0" + mins;
                    }
                    if (hours < 10) {
                        // add leading zero to minute representation
                        hours = "0" + hours;
                    }
                    $("#start_minutes").val(mins);
                    $("#start_hours").val(hours);
                });

                $("#start_dateTime").on("change", function () {
                    var date = ($("#start_dateTime")).val();

                    if (isValidDate(date)) {
                        updateStartTimestamp();
                    } else {
                        document.getElementById("submit_users_btn").disabled = true;
                        $("#start_dateTime").popover("show");
                        setTimeout(function () {
                            $("#start_dateTime").popover("hide")
                        }, 6000);
                        document.getElementById("finishTimeWarning").style.display = "none";
                    }
                });

                $("#start_hours").on("change", function () {
                    var hours = $("#start_hours").val();

                    if (hours < 0 || hours > 23 || hours === "" || isNaN(hours)) {
                        document.getElementById("submit_users_btn").disabled = true;
                        $("#start_hours").popover("show");
                        setTimeout(function () {
                            $("#start_hours").popover("hide");
                        }, 6000);
                    } else {
                        if (hours < 10) {
                            // add leading zero to hour representation
                            $("#start_hours").val("0" + hours);
                        }
                        updateStartTimestamp();
                    }
                });

                $("#start_minutes").on("change", function () {
                    var mins = $("#start_minutes").val();

                    // check invalid input to show error popover and disable submit button
                    if (mins === "" || isNaN(mins) || (mins < 0 || mins > 59)) {
                        document.getElementById("submit_users_btn").disabled = true;
                        $("#start_minutes").popover("show");
                        setTimeout(function () {
                            $("#start_minutes").popover("hide");
                        }, 6000);
                    } else {
                        if (mins < 10) {
                            // add leading zero to minute representation
                            $("#start_minutes").val("0" + mins);
                        }
                        updateStartTimestamp();
                    }
                });

                // update the input of hidden startTime field with selected timestamp
                var updateStartTimestamp = function () {
                    var startDate = $("#start_dateTime").val();
                    var hours = $("#start_hours").val();
                    var mins = $("#start_minutes").val();

                    // update hidden start timestamp only if whole input is valid
                    if (isValidDate(startDate)
                        && !(hours < 0 || hours > 23 || hours === "" || isNaN(hours))
                        && !(mins === "" || isNaN(mins) || (mins < 0 || mins > 59))) {
                        var newStartTime = new Date(startDate).getTime();
                        newStartTime += parseInt(hours * 60 * 60 * 1000);
                        newStartTime += parseInt(mins * 60 * 1000);
                        var finishTime = parseInt($("#finishTime").val());

                        var finishHours = $("#finish_hours").val();
                        var finishMins = $("#finish_minutes").val();

                        if (finishTime > newStartTime) {

                            if (isValidDate($("#finish_dateTime").val())
                                && !(finishHours < 0 || finishHours > 23 || finishHours === "" || isNaN(finishHours))
                                && !(finishMins === "" || isNaN(finishMins) || (finishMins < 0 || finishMins > 59))) {
                                document.getElementById("submit_users_btn").disabled = false;
                            }
                        } else {
                            if (isValidDate($("#finish_dateTime").val())
                                && !(finishHours < 0 || finishHours > 23 || finishHours === "" || isNaN(finishHours))
                                && !(finishMins === "" || isNaN(finishMins) || (finishMins < 0 || finishMins > 59))) {
                                $("#finishBeforeStartWarning").popover("show");
                                setTimeout(function () {
                                    $("#finishBeforeStartWarning").popover("hide");
                                }, 6000);                                    }
                            document.getElementById("submit_users_btn").disabled = true;
                        }
                        $("#startTime").val(newStartTime);
                    }
                };

                // date validation used in start and finish date
                function isValidDate(dateString) {
                    // check pattern for YYYY/MM/DD
                    if (!/^\d{4}\/\d{1,2}\/\d{1,2}$/.test(dateString))
                        return false;

                    // parse the date parts to integers
                    var parts = dateString.split("/");
                    var year = parseInt(parts[0], 10);
                    var month = parseInt(parts[1], 10);
                    var day = parseInt(parts[2], 10);

                    // check the ranges of month and year
                    if (year < 1000 || year > 3000 || month === 0 || month > 12)
                        return false;

                    var monthLength = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

                    // check for leap years
                    if (year % 400 === 0 || (year % 100 !== 0 && year % 4 === 0)) {
                        monthLength[1] = 29;
                    }

                    // check the range of the day
                    return day > 0 && day <= monthLength[month - 1];
                };

            </script>
            <div class="col-sm-2">
                <label for="finishTime" class="label-normal">Finish Time</label>
                <input type="hidden" id="finishTime" name="finishTime"/>
                <input class="form-control" name="finish_dateTime" id="finish_dateTime"
                       data-toggle="popover" data-content="Invalid date or format (expected: YYYY/MM/DD)"
                       data-placement="top"
                       data-trigger="manual"/>
                <div>
                    <input class="ws-2" type="text" name="finish_hours" id="finish_hours"
                           style="text-align: center;" min="0" max="59"
                           data-toggle="popover"
                           data-content="Hours must be a number between 0 and 23"
                           data-placement="bottom"
                           data-trigger="manual"/>
                    <span>:</span>
                    <input class="ws-2" type="text" name="finish_minutes" id="finish_minutes"
                           style="text-align: center;" min="0" max="59"
                           data-toggle="popover"
                           data-content="Minutes must be a number between 0 and 59"
                           data-placement="bottom"
                           data-trigger="manual"/>
                </div>
            </div>
            <script>
                $(document).ready(function () {
                    var initialFinishDate = new Date();
                    // add default 3 days to initial finish date
                    initialFinishDate.setDate(initialFinishDate.getDate() + 3);
                    $("#finishTime").val(initialFinishDate.getTime());
                    $("#finish_dateTime").datepicker({dateFormat: "yy/mm/dd"});
                    $("#finish_dateTime").datepicker("setDate", initialFinishDate);
                    $("#finish_hours").val(initialFinishDate.getHours());
                    var mins = initialFinishDate.getMinutes();
                    var hours = initialFinishDate.getHours();
                    if (mins < 10) {
                        mins = "0" + mins;
                    }
                    if (hours < 10) {
                        hours = "0" + hours;
                    }
                    $("#finish_minutes").val(mins);
                    $("#finish_hours").val(hours);
                });

                $("#finish_dateTime").on("change", function () {
                    var date = ($("#finish_dateTime")).val();

                    if (isValidDate(date)) {
                        updateFinishTimestamp();
                    } else {
                        document.getElementById("submit_users_btn").disabled = true;
                        $("#finish_dateTime").popover("show");
                        setTimeout(function () {
                            $("#finish_dateTime").popover("hide")
                        }, 6000);
                        document.getElementById("finishTimeWarning").style.display = "none";
                    }
                });

                $("#finish_hours").on("change", function () {
                    var hours = $("#finish_hours").val();

                    if (hours < 0 || hours > 23 || hours === "" || isNaN(hours)) {
                        document.getElementById("submit_users_btn").disabled = true;
                        $("#finish_hours").popover("show");
                        setTimeout(function () {
                            $("#finish_hours").popover("hide");
                        }, 6000);
                    } else {
                        if (hours < 10) {
                            // add leading zero to hour representation
                            $("#finish_hours").val("0" + hours);
                        }
                        updateFinishTimestamp();
                    }
                });

                $("#finish_minutes").on("change", function () {
                    var mins = $("#finish_minutes").val();

                    if (mins === "" || isNaN(mins) || (mins < 0 || mins > 59)) {
                        document.getElementById("submit_users_btn").disabled = true;
                        $("#finish_minutes").popover("show");
                        setTimeout(function () {
                            $("#finish_minutes").popover("hide");
                        }, 6000);
                    } else {
                        if (mins < 10) {
                            // add leading zero to minute representation
                            $("#finish_minutes").val("0" + mins);
                        }
                        updateFinishTimestamp();
                    }
                });

                var updateFinishTimestamp = function () {
                    var finishDate = $("#finish_dateTime").val();
                    var finishHours = $("#finish_hours").val();
                    var finishMins = $("#finish_minutes").val();

                    if (isValidDate(finishDate)
                        && !(finishHours < 0 || finishHours > 23 || finishHours === "" || isNaN(finishHours))
                        && !(finishMins === "" || isNaN(finishMins) || (finishMins < 0 || finishMins > 59))) {
                        var newFinishTime = new Date($("#finish_dateTime").val()).getTime();
                        newFinishTime += parseInt($("#finish_hours").val() * 60 * 60 * 1000);
                        newFinishTime += parseInt($("#finish_minutes").val() * 60 * 1000);
                        var startTime = parseInt($("#startTime").val());

                        var startHours = $("#start_hours").val();
                        var startMins = $("#start_minutes").val();

                        if (newFinishTime > startTime) {
                            if (isValidDate($("#start_dateTime").val())
                                && !(startHours < 0 || startHours > 23 || startHours === "" || isNaN(startHours))
                                && !(startMins === "" || isNaN(startMins) || (startMins < 0 || startMins > 59))) {
                                document.getElementById("submit_users_btn").disabled = false;
                            }
                        } else {
                            if (isValidDate($("#start_dateTime").val())
                                && !(startHours < 0 || startHours > 23 || startHours === "" || isNaN(startHours))
                                && !(startMins === "" || isNaN(startMins) || (startMins < 0 || startMins > 59))) {
                                $("#finishBeforeStartWarning").popover("show");
                                setTimeout(function () {
                                    $("#finishBeforeStartWarning").popover("hide");
                                }, 6000);                                    }
                            document.getElementById("submit_users_btn").disabled = true;
                        }
                        $("#finishTime").val(newFinishTime);
                    }
                };
            </script>
        </div>
        <div class="row">
            <div class="col-sm-2" id="mutantValidatorLevelDiv">
                <label class="label-normal" title="Click the question sign for more information on the levels"
                       for="mutantValidatorLevel">
                    Mutant validator
                    <a data-toggle="collapse" href="#validatorExplanation" style="color:black">
                        <span class="glyphicon glyphicon-question-sign"></span>
                    </a>
                </label>
                <select id="mutantValidatorLevel" name="mutantValidatorLevel" class="form-control selectpicker"
                        data-size="medium">
                    <%for (CodeValidator.CodeValidatorLevel cvl : CodeValidator.CodeValidatorLevel.values()) {%>
                    <option value=<%=cvl.name()%> <%=cvl.equals(CodeValidator.CodeValidatorLevel.MODERATE) ? "selected" : ""%>>
                        <%=cvl.name().toLowerCase()%>
                    </option>
                    <%}%>
                </select>
            </div>
            <div class="col-sm-1">
            </div>
            <div class="col-sm-2" id="chatEnabledDiv">
                <label class="label-normal" title="Players can chat with their team and with all players in the game"
                       for="chatEnabled">
                    Enable Game Chat
                </label>
                <input type="checkbox" id="chatEnabled" name="chatEnabled"
                       class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                       data-onstyle="primary" data-offstyle="" checked>
            </div>
            <div class="col-sm-3" id="markUncoveredDiv">
                <label class="label-normal" title="Attackers can mark uncovered lines as equivalent"
                       for="markUncovered">
                    Mark uncovered lines as equivalent
                </label>
                <input type="checkbox" id="markUncovered" name="markUncovered"
                       class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                       data-onstyle="primary" data-offstyle="">
            </div>
            <div class="col-sm-2">
                <label for="maxAssertionsPerTest" class="label-normal"
                       title="Maximum number of assertions per test. Increase this for difficult to test classes.">Max.
                    Assertions per Test</label>
                <br/>
                <input class="form-control" type="number" value="2" name="maxAssertionsPerTest"
                       id="maxAssertionsPerTest" min=1 required/>
            </div>
        </div>
        <br>
        <div class="row">
            <div class="col-sm-5">
                <div id="validatorExplanation" class="collapse panel panel-default" style="font-size: 12px;">
                    <%@ include file="/jsp/validator_explanation.jsp" %>
                </div>
            </div>
        </div>
        <button class="btn btn-md btn-primary" type="submit" name="submit_users_btn" id="submit_users_btn" disabled>
            Stage Games
        </button>

        <p>
            If you just want to create a single open game without assigning players, you can also use the <a href="<%=request.getContextPath()%>/multiplayer/games/create?fromAdmin=true"> Create game</a> interface.
        </p>

            <script>
            $('#selectAllUsers').click(function () {
                var checkboxes = document.getElementsByName('selectedUsers');
                var isChecked = document.getElementById('selectAllUsers').checked;
                checkboxes.forEach(function (element) {
                    if(element.checked !== isChecked)
                        element.click();
                });
            });

            $('#selectAllTempGames').click(function () {
                $(this.form.elements).filter(':checkbox').prop('checked', this.checked);
            });

            $('#selectAllGames').click(function () {
                $(this.form.elements).filter(':checkbox').prop('checked', this.checked);
            });

            $('#togglePlayersCreated').click(function () {
                var showPlayers = localStorage.getItem("showCreatedPlayers") === "true";
                localStorage.setItem("showCreatedPlayers", showPlayers ? "false" : "true");
                $("[id=playersTableCreated]").toggle();
                $("[id=playersTableHidden]").toggle();
                setCreatedPlayersSpan()
            });

            function setCreatedPlayersSpan() {
                var showPlayers = localStorage.getItem("showCreatedPlayers") === "true";
                var buttonClass = showPlayers ? "glyphicon glyphicon-eye-close" : "glyphicon glyphicon-eye-open";
                document.getElementById("togglePlayersCreatedSpan").setAttribute("class", buttonClass);
            }

            function setSelectAllCheckbox(checkboxesName, selectAllCheckboxId) {
                var checkboxes = document.getElementsByName(checkboxesName);
                var allChecked = true;
                checkboxes.forEach(function (element) {
                    allChecked = allChecked && element.checked;
                });
                document.getElementById(selectAllCheckboxId).checked = allChecked;
            }

            function areAnyChecked(name) {
                var checkboxes = document.getElementsByName(name);
                var anyChecked = false;
                checkboxes.forEach(function (element) {
                    anyChecked = anyChecked || element.checked;
                });
                return anyChecked;
            }

            function containsText(id) {
                return document.getElementById(id).value.trim() !== "";
            }

            function updateCheckbox(checkboxVal, isChecked) {
                document.getElementById('submit_users_btn').disabled =
                    !(areAnyChecked('selectedUsers') || containsText('user_name_list')) || (document.getElementById('cut_select').selectedIndex != 0);
                setSelectAllCheckbox('selectedUsers', 'selectAllUsers');
                var hiddenIdList = document.getElementById('hidden_user_id_list');
                if (isChecked) {
                    hiddenIdList.value = hiddenIdList.value.trim() + '<' + checkboxVal + '>,';
                } else {
                    hiddenIdList.value = hiddenIdList.value.replace('<' + checkboxVal + '>,', '');
                }
            }

            $('#tableAddUsers').on('draw.dt', function () {
                setSelectAllCheckbox('selectedUsers', 'selectAllUsers');
            });


            $(document).ready(function () {
                if (localStorage.getItem("showActivePlayers") === "true") {
                    $("[id=playersTableActive]").show();
                }

                if (localStorage.getItem("showCreatedPlayers") === "true") {
                    $("[id=playersTableCreated]").show();
                    $("[id=playersTableHidden]").hide();
                }
                $('#tableAddUsers').DataTable({
                    pagingType: "full_numbers",
                    "lengthChange": false,
                    "searching": true,
                    "order": [[5, "desc"]],
                    "columnDefs": [{
                        "targets": 0,
                        "orderable": false
                    }, {
                        "targets": 6,
                        "orderable": false
                    }]
                });

                $('#tableCreatedGames').DataTable({
                    pagingType: "full_numbers",
                    lengthChange: false,
                    searching: false,
                    order: [[3, "desc"]],
                    "columnDefs": [{
                        "targets": 0,
                        "orderable": false
                    }, {
                        "targets": 6,
                        "orderable": false
                    }]
                });

                setCreatedPlayersSpan();
            });

            $('.modal').on('shown.bs.modal', function () {
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

    </form>
</div>
<%@ include file="/jsp/footer.jsp" %>

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
<%@ page import="org.apache.commons.collections.ListUtils" %>
<%@ page import="org.codedefenders.database.GameClassDAO" %>
<%@ page import="org.codedefenders.database.UserDAO" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.leaderboard.Entry" %>
<%@ page import="org.codedefenders.servlets.admin.AdminCreateGames" %>
<%@ page import="org.codedefenders.validation.code.CodeValidatorLevel" %>
<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.database.MultiplayerGameDAO" %>
<%@ page import="org.codedefenders.database.AdminDAO" %>
<% String pageTitle = null; %>
<%@ include file="/jsp/header_main.jsp" %>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminCreateGames"); %>
    <%@ include file="/jsp/admin_navigation.jsp" %>

    <form id="insertGames" action="<%=request.getContextPath() + Paths.ADMIN_PAGE%>" method="post">
        <input type="hidden" name="formType" value="insertGames"/>
        <h3>Staged Games</h3>
        <%
			List<MultiplayerGame> availableGames = MultiplayerGameDAO.getAvailableMultiplayerGames();
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
                <th class="col-md-6">Players
                    <div class="row">
                        <div class="col-sm-2" style="padding-top: 10px">Name</div>
                        <div class="col-sm-3" style="padding-top: 10px">Last Role</div>
                        <div class="col-sm-1" style="padding-top: 10px">Score</div>
                        <a id="togglePlayersCreated" class="btn btn-sm btn-default" style="float: right">
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
                <td>
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
                <td><%= g.getLevel() %>
                </td>
                <td>
                    <div id="playersTableHidden" style="color: lightgray;"> (hidden)</div>
                    <table id="playersTableCreated" hidden>
                        <%
                            List<Integer> attackerAndDefenderIds = ListUtils.union(attackerIds, defenderIds);
                            for (int id : attackerAndDefenderIds) {
                                String userName = UserDAO.getUserById(id).getUsername();
                                //Timestamp ts = AdminDAO.getLastLogin(aid);
                                Role lastRole = UserDAO.getLastRoleOfUser(id);
                                Entry score = AdminDAO.getScore(id);
                                int totalScore = score.getTotalPoints();
                                String color = attackerIds.contains(id) ? "#edcece" : "#ced6ed";
                        %>
                        <tr style="background: <%= color %>">
                            <td class="col-md-2"><%= userName %>
                            </td>
                            <td class="col-md-3"><%= lastRole %>
                            </td>
                            <td class="col-md-1"><%= totalScore %>
                            </td>
                            <td class="col-md-1">
                                <button class="btn btn-sm btn-primary"
                                        value="<%=String.valueOf(i) + "-" + String.valueOf(id)%>"
                                        id="<%="switch_player_"+id+"_game_"+i%>"
                                        name="tempGameUserSwitchButton">
                                    <span class="glyphicon glyphicon-transfer"></span>
                                </button>
                            </td>
                            <td class="col-md-1">
                                <button class="btn btn-sm btn-danger"
                                        value="<%=String.valueOf(i) + "-" + String.valueOf(id)%>"
                                        id="<%="remove_player_"+id+"_game_"+i%>"
                                        name="tempGameUserRemoveButton">
                                    <span class="glyphicon glyphicon-trash"></span>
                                </button>
                            </td>
                            <%-- ------------------ --%>
                            <%-- Show moving to game UI only if there's more than 1 game --%>
                            <%-- ------------------ --%>
                            <% if( createdGames.size() + availableGames.size() > 1 ) {%>
                            <td class="col-md-3" style="padding-top:3px; padding-bottom:3px;">
                            <%-- create the select and fill it with the available games except the current one --%>
								<div id="<%="game_"+id%>" style="max-width: 100px; float: left;">
									<select name="<%="game_" + id%>" class="form-control selectpicker" data-size="small" id="game" style="float: right">
										<%-- List created games --%>
										<% for (MultiplayerGame availableGame : availableGames) { %>
											<option value="<%=availableGame.getId()%>"><%=String.valueOf(availableGame.getId()) + ": " + availableGame.getCUT().getAlias()%>
											</option>
										<% } %>
										<%-- List the staged games --%>
										<% if (createdGames != null) {
												for (int gameIndex = 0; gameIndex < createdGames.size(); ++gameIndex) {
													// Do not list the current game in the select
													if( gameIndex == i ) { continue; }
													String classAlias = createdGames.get(gameIndex).getCUT().getAlias();
										%>
											<option style="color:gray" value=<%="T" + String.valueOf(gameIndex)%>><%="T" + String.valueOf(gameIndex) + ": " + classAlias%>
											</option>
										<%
												}
											}
										%>
									</select>

								</div>
							<%-- Keep the role of the user also in the target game --%>
								<input type="hidden" name="<%="role_" + id%>" value="<%= (attackerIds.contains(new Integer(id))) ? Role.ATTACKER : Role.DEFENDER %>"/>
							</td>
                            <%-- Create the button to move it --%>
                            <td class="col-md-1">
                                <button name="tempGameUserMoveToButton" class="btn btn-sm btn-primary" type="submit" value="<%="move_player_"+id+"_from_game_T"+i%>" name="userListButton" style="margin: 2px; float:left">
                                    <span class="glyphicon glyphicon-arrow-right"></span>
                                </button>
                            </td>
							<% }%>
                            <%-- ------------------ --%>
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
    <form id="users" action="<%=request.getContextPath() + Paths.ADMIN_PAGE%>" method="post">
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
                <th class="col-md-4">Add to existing Game</th>
            </tr>
            </thead>
            <tbody>

            <%
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
                            style="margin: 2px; float:right"
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
                    <% for (GameClass c : GameClassDAO.getAllPlayableClasses()) { %>
                    <option value="<%=c.getId()%>"><%=c.getAlias()%>
                    </option>
                    <%}%>
                </select>
                <br/>
                <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>?fromAdmin=true"> Upload Class </a>
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
                <label for="" class="label-normal">Include predefined mutants (if available)</label>
                <input type="checkbox" id="withMutants" name="withMutants"
                       class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                       data-onstyle="primary" data-offstyle="">
                <br/>
                <label for="" class="label-normal">Include predefined tests (if available)</label>
                <input type="checkbox" id="withTests" name="withTests"
                       class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                       data-onstyle="primary" data-offstyle="">
            </div>
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
                    <%for (CodeValidatorLevel cvl : CodeValidatorLevel.values()) {%>
                    <option value=<%=cvl.name()%> <%=cvl.equals(CodeValidatorLevel.MODERATE) ? "selected" : ""%>>
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
            <div class="col-sm-2" id="capturePlayersIntentionDiv">
                <label class="label-normal" title="Enable Capturing Player Intention"
                       for="capturePlayersIntention">
                    Capture Players Intention
                </label>
                <input type="checkbox" id="capturePlayersIntention" name="capturePlayersIntention"
                       class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                       data-onstyle="primary" data-offstyle="">
            </div>
            <div class="col-sm-1"></div>
            <div class="col-sm-2">
                <label for="maxAssertionsPerTest" class="label-normal"
                       title="Maximum number of assertions per test. Increase this for difficult to test classes.">Max.
                    Assertions per Test</label>
                <br/>
                <input class="form-control" type="number" value="2" name="maxAssertionsPerTest"
                       id="maxAssertionsPerTest" min=1 required/>
            </div>
            <div class="col-sm-2">
                <label for="automaticEquivalenceTrigger" class="label-normal"
                       title="Threshold for automatically triggering equivalence duels. If the value is 0 the trigger is disabled.">
                       Automatic Equivalence Threshold</label>
                <br/>
                <input class="form-control" type="number" value="0" name="automaticEquivalenceTrigger"
                       id="automaticEquivalenceTrigger" min=0 required/>
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
            If you just want to create a single open game without assigning players, you can also use the <a href="<%=request.getContextPath() + Paths.BATTLEGROUND_CREATE%>?fromAdmin=true"> Create game</a> interface.
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

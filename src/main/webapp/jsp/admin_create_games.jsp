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
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.game.Role" %>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<jsp:include page="/jsp/header_main.jsp"/>

<%-- Workaround for the stupid <div class="nest"> in header_main. Remove this once that div is gone --%>
</div></div></div></div></div><div class="container">

<%
    boolean displayingStagedGames;
    boolean displayingUnassignedUsers;
%>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminCreateGames"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <form id="insertGames" action="<%=request.getContextPath() + Paths.ADMIN_PAGE%>" method="post" style="margin-top: 25px;">
        <input type="hidden" name="formType" value="insertGames"/>
        <%
			List<MultiplayerGame> availableGames = MultiplayerGameDAO.getAvailableMultiplayerGames();
            List<MultiplayerGame> createdGames = (List<MultiplayerGame>) session.getAttribute(AdminCreateGames.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
            List<List<Integer>> attackerIdsList = (List<List<Integer>>) session.getAttribute(AdminCreateGames.ATTACKER_LISTS_SESSION_ATTRIBUTE);
            List<List<Integer>> defenderIdsList = (List<List<Integer>>) session.getAttribute(AdminCreateGames.DEFENDER_LISTS_SESSION_ATTRIBUTE);
            if (createdGames == null || createdGames.isEmpty()) {
                displayingStagedGames = false;
        %>

        <div class="panel panel-default">
            <div class="panel-heading">
                Staged Games
            </div>
            <div class="panel-body" style="color: gray; text-align: center;">
                There are currently no staged multiplayer games.
            </div>
        </div>

        <%
            } else {
                displayingStagedGames = true;
        %>

        <div class="panel panel-default">
            <div class="panel-heading">
                Staged Games
                <div style="float: right;">
                    <input type="search" id="search-staged-games" class="form-control" placeholder="Search" style="height: .65em; width: 10em; display: inline;">
                </div>
            </div>
            <div class="panel-body">

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
                            <th>Players (Name, Last Role, Score)</th>
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
                                                        id="sut<%=g.getId()%>" name="cut<%=g.getCUT().getId()%>" cols="80"
                                                        rows="30"></textarea></pre>
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
                                            String lastRoleStr = lastRole != null
                                                    ? lastRole.getFormattedString()
                                                    : "<span style=\"color: gray;\">none<span>";
                                            Entry score = AdminDAO.getScore(id);
                                            int totalScore = score.getTotalPoints();
                                            String color = attackerIds.contains(id) ? "#edcece" : "#ced6ed";
                                    %>
                                    <tr style="background: <%= color %>">
                                        <td class="col-md-2"><%= userName %>
                                        </td>
                                        <td class="col-md-3"><%= lastRoleStr %>
                                        </td>
                                        <td class="col-md-1"><%= totalScore %>
                                        </td>
                                        <td class="col-md-1">
                                            <button class="btn btn-sm btn-primary"
                                                    value="<%=i + "-" + id%>"
                                                    id="<%="switch_player_"+id+"_game_"+i%>"
                                                    name="tempGameUserSwitchButton">
                                                <span class="glyphicon glyphicon-transfer"></span>
                                            </button>
                                        </td>
                                        <td class="col-md-1">
                                            <button class="btn btn-sm btn-danger"
                                                    value="<%=i + "-" + id%>"
                                                    id="<%="remove_player_"+id+"_game_"+i%>"
                                                    name="tempGameUserRemoveButton">
                                                <span class="glyphicon glyphicon-trash"></span>
                                            </button>
                                        </td>
                                        <%-- ------------------ --%>
                                        <%-- Show moving to game UI only if there's more than 1 game --%>
                                        <%-- ------------------ --%>
                                        <% if (createdGames.size() + availableGames.size() > 1) {%>
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
                                                        <option value=<%="T" + String.valueOf(gameIndex)%>><%="T" + String.valueOf(gameIndex) + ": " + classAlias%>
                                                        </option>
                                                    <%
                                                            }
                                                        }
                                                    %>
                                                </select>

                                            </div>
                                        <%-- Keep the role of the user also in the target game --%>
                                            <input type="hidden" name="<%="role_" + id%>" value="<%= (attackerIds.contains(id)) ? Role.ATTACKER.name() : Role.DEFENDER.name() %>"/>
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

                <br/>

                <button class="btn btn-md btn-primary" type="submit" name="games_btn" id="insert_games_btn"
                        disabled value="insert Games">
                    Create games
                </button>
                <button class="btn btn-md btn-danger" type="submit" name="games_btn" id="delete_games_btn"
                        onclick="return confirm('Are you sure you want to discard the selected Games?');"
                        disabled value="delete Games">
                    Discard games
                </button>

            </div>
        </div>

        <% } %>

    </form>

    <form id="users" action="<%=request.getContextPath() + Paths.ADMIN_PAGE%>" method="post">
        <input type="hidden" name="formType" value="createGame">

        <%
            createdGames = (List<MultiplayerGame>) session.getAttribute(AdminCreateGames.CREATED_GAMES_LISTS_SESSION_ATTRIBUTE);
            List<List<String>> unassignedUsersInfo = AdminCreateGames.getUnassignedUsers(attackerIdsList, defenderIdsList);
            if (unassignedUsersInfo.isEmpty()) {
                displayingUnassignedUsers = false;
        %>

        <div class="panel panel-default">
            <div class="panel-heading">
                Unassigned Users
            </div>
            <div class="panel-body" style="color: gray; text-align: center;">
                There are currently no created unassigned users.
            </div>
        </div>

        <%
            } else {
                displayingUnassignedUsers = true;
        %>

        <div class="panel panel-default">
            <div class="panel-heading">
                Unassigned Users
                <div style="float: right;">
                    <input type="search" id="search-unassigned-users" class="form-control" placeholder="Search" style="height: .65em; width: 10em; display: inline;">
                </div>
            </div>
            <div class="panel-body">

                <table id="tableAddUsers"
                       class="table table-striped table-hover table-responsive table-paragraphs games-table dataTable display">
                    <thead>
                        <tr>
                            <th>
                                <input type="checkbox" id="selectAllUsers" onchange="document.getElementById('submit_users_btn').disabled = document.getElementById('cut_select').selectedIndex == -1 || ! this.checked;">
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
                        for (List<String> userInfo : unassignedUsersInfo) {
                            int uid = Integer.valueOf(userInfo.get(0));
                            String username = userInfo.get(1);
                            String lastLogin = userInfo.get(3);
                            lastLogin = lastLogin != null
                                    ? lastLogin
                                    : "<span style=\"color: gray;\">never<span>";
                            String lastRole = userInfo.get(4) != null
                                    ? Role.valueOf(userInfo.get(4)).getFormattedString()
                                    : "<span style=\"color: gray;\">none<span>";
                            String totalScore = userInfo.get(5);
                    %>

                        <tr id="<%="user_row_"+uid%>">
                            <td>
                                <% if (uid != login.getUserId()) { %>
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
                                        <option value=<%="T" + gameIndex%>><%="T" + gameIndex + ": " + classAlias%></option>
                                        <%}%>
                                        <%}%>
                                    </select>
                                </div>
                                <div id="<%="role_"+uid%>" style="float: left; max-width: 120px; margin-left:2px">
                                    <select name="<%="role_" + uid%>" class="form-control selectpicker" data-size="small"
                                            id="role">
                                        <option value="<%=Role.ATTACKER.name()%>">Attacker</option>
                                        <option value="<%=Role.DEFENDER.name()%>">Defender</option>
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

                    <% } %>

                    </tbody>
                </table>
            </div>
        </div>

        <% } %>

        <input type="text" class="form-control" id="hidden_user_id_list" name="hidden_user_id_list" hidden>

        <div class="panel panel-default">
            <div class="panel-heading">
                User Names
                <a data-toggle="collapse" href="#demo" style="color:black">
                    <span class="glyphicon glyphicon-question-sign"></span>
                </a>
            </div>
            <div class="panel-body">
                <div id="demo" class="collapse panel panel-default" style="margin-top: 5px;">
                    <div class="panel-body" style="padding: 10px;">
                        Newline separated list of usernames or emails.
                        These names, as well as the names selected in the above table, will be assigned to created games.
                        Only unassigned users are taken into account.
                    </div>
                </div>
                <textarea class="form-control" rows="5" id="user_name_list" name="user_name_list"
                          oninput="document.getElementById('submit_users_btn').disabled =
                            !(areAnyChecked('selectedUsers') || containsText('user_name_list')) || (document.getElementById('cut_select').selectedIndex != 0)"></textarea>
            </div>
        </div>

        <div class="row">

            <div class="col-sm-12 col-md-6" style="padding-left: 2em; padding-right: 2em;">
                <h3>Game Settings</h3>

                <div class="form-group" id="cutDiv">
                    <label for="cut_select" class="label-normal">CUT</label>
                    <select name="class" class="form-control selectpicker" data-size="large" id="cut_select">
                        <% for (GameClass c : GameClassDAO.getAllPlayableClasses()) { %>
                        <option value="<%=c.getId()%>"><%=c.getAlias()%>
                        </option>
                        <%}%>
                    </select>
                    <br/>
                    <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>?fromAdmin=true">Upload Class</a>
                </div>

                <div class="form-group">
                    <label for="withMutants" class="label-normal">Include predefined mutants (if available)</label>
                    <br/>
                    <input type="checkbox" id="withMutants" name="withMutants"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </div>

                <div class="form-group">
                    <label for="withTests" class="label-normal">Include predefined tests (if available)</label>
                    <br/>
                    <input type="checkbox" id="withTests" name="withTests"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </div>

                <div class="form-group">
                    <label for="maxAssertionsPerTest" class="label-normal"
                           title="Maximum number of assertions per test. Increase this for difficult to test classes.">Max.
                        Assertions per Test</label>
                    <br/>
                    <input class="form-control" type="number" value="2" name="maxAssertionsPerTest"
                           id="maxAssertionsPerTest" min=1 required/>
                </div>

                <div class="form-group" id="mutantValidatorLevelDiv">
                    <label class="label-normal" title="Click the question sign for more information on the levels"
                           for="mutantValidatorLevel">
                        Mutant validator
                        <a data-toggle="modal" href="#validatorExplanation" style="color:black">
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

                <div class="form-group" id="chatEnabledDiv">
                    <label class="label-normal" title="Players can chat with their team and with all players in the game"
                           for="chatEnabled">
                        Enable Game Chat
                    </label>
                    <br/>
                    <input type="checkbox" id="chatEnabled" name="chatEnabled"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                           data-onstyle="primary" data-offstyle="" checked>
                </div>

                <div class="form-group" id="capturePlayersIntentionDiv">
                    <label class="label-normal" title="Enable Capturing Player Intention"
                           for="capturePlayersIntention">
                        Capture Players Intention
                    </label>
                    <br/>
                    <input type="checkbox" id="capturePlayersIntention" name="capturePlayersIntention"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </div>

                <div class="form-group">
                    <label for="automaticEquivalenceTrigger" class="label-normal"
                           title="Threshold for triggering equivalence duels automatically (use 0 to deactivate)">
                        Threshold for triggering equivalence duels automatically

                        <a data-toggle="modal" href="#automaticEquivalenceTriggerExplanation" style="color:black">
                            <span class="glyphicon glyphicon-question-sign"></span>
                        </a>
                    </label>
                    <input class="form-control" type="number" value="0" name="automaticEquivalenceTrigger"
                           id="automaticEquivalenceTrigger" min=0 required/>
                </div>
                <div class="form-group">
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

            </div> <%-- column --%>

            <div class="col-sm-12 col-md-6" style="padding-left: 2em; padding-right: 2em;">
                <h3>Game Management Settings</h3>

                <div class="form-group">
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

                <div class="form-group">
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

                <div class="form-group">
                    <label for="attackers" class="label-normal">Attackers per Game</label>
                    <input type="number" value="3" id="attackers" name="attackers" min="1" class="form-control"/>
                </div>

                <div class="form-group">
                    <label for="defenders" class="label-normal">Defenders per Game</label>
                    <input type="number" value="3" id="defenders" name="defenders" min="1" class="form-control"/>
                </div>

                <div class="form-group">
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

            </div> <%-- column --%>

        </div> <%-- row --%>

        <button class="btn btn-md btn-primary" type="submit" name="submit_users_btn" id="submit_users_btn" disabled style="margin-top: 1em">
            Stage Games
        </button>
        <p style="margin-top: .5em">
            If you just want to create a single open game without assigning players,
            you can also use the <a href="<%=request.getContextPath() + Paths.BATTLEGROUND_CREATE%>?fromAdmin=true"> Create game</a> interface.
        </p>
    </form>

    <div class="modal fade" id="validatorExplanation" role="dialog"
        aria-labelledby="validatorExplanation" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">Mutant Validator Explanation</h4>
                </div>

                <div class="modal-body">
                    <%@ include file="/jsp/validator_explanation.jsp"%>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>
    <div class="modal fade" id="automaticEquivalenceTriggerExplanation"
        role="dialog"
        aria-labelledby="automaticEquivalenceTriggerExplanation"
        aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">Threshold for Triggering Equivalence
                        Duels Automatically Explanation</h4>
                </div>

                <div class="modal-body">
                    <%@ include file="/jsp/automatic_duels_explanation.jsp"%>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <script>
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

        $(document).ready(function () {
            <% if (displayingUnassignedUsers) { %>
                $('#selectAllUsers').click(function () {
                    var checkboxes = document.getElementsByName('selectedUsers');
                    var isChecked = document.getElementById('selectAllUsers').checked;
                    checkboxes.forEach(function (element) {
                        if (element.checked !== isChecked) {
                            element.click();
                        }
                    });
                });

                $('#tableAddUsers').on('draw.dt', function () {
                    setSelectAllCheckbox('selectedUsers', 'selectAllUsers');
                });

                const tableAddUsers = $('#tableAddUsers').DataTable({
                    order: [[5, "desc"]],
                    columnDefs: [{
                        targets: 0,
                        orderable: false
                    }, {
                        targets: 6,
                        orderable: false
                    }],
                    scrollY: '400px',
                    scrollCollapse: true,
                    paging: false,
                    dom: 't',
                    language: {emptyTable: 'There are currently no unassigned users.'}
                });
                $('#search-unassigned-users').on('keyup', function () { setTimeout(() => tableAddUsers.search(this.value).draw(), 0); });
            <% } %>

            <% if (displayingStagedGames) { %>
                $('#selectAllTempGames').click(function () {
                    $(this.form.elements).filter(':checkbox').prop('checked', this.checked);
                });

                $('#togglePlayersCreated').click(function () {
                    var showPlayers = localStorage.getItem("showCreatedPlayers") === "true";
                    localStorage.setItem("showCreatedPlayers", showPlayers ? "false" : "true");
                    $("[id=playersTableCreated]").toggle();
                    $("[id=playersTableHidden]").toggle();
                    setCreatedPlayersSpan()
                });

                if (localStorage.getItem("showActivePlayers") === "true") {
                    $("[id=playersTableActive]").show();
                }

                if (localStorage.getItem("showCreatedPlayers") === "true") {
                    $("[id=playersTableCreated]").show();
                    $("[id=playersTableHidden]").hide();
                }

                const tableStagedGames = $('#tableCreatedGames').DataTable({
                    order: [[1, "desc"]],
                    columnDefs: [{
                        targets: 0,
                        orderable: false
                    }, {
                        targets: 3,
                        orderable: false
                    }],
                    scrollY: '400px',
                    scrollCollapse: true,
                    paging: false,
                    dom: 't',
                    language: {emptyTable: 'There are currently no staged multiplayer games.'}
                });
                $('#search-staged-games').on('keyup', function () { setTimeout(() => tableStagedGames.search(this.value).draw(), 0); });

                setCreatedPlayersSpan();
            <% } %>

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
        });
    </script>
</div>

<%-- Workaround for the stupid <div class="nest"> in header_main. Remove this once that div is gone --%>
</div><div></div><div><div><div><div>

<%@ include file="/jsp/footer.jsp" %>

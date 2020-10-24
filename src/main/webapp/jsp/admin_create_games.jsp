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
<%@ page import="org.codedefenders.database.GameClassDAO" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.validation.code.CodeValidatorLevel" %>
<%@ page import="org.codedefenders.game.GameClass" %>
<%@ page import="org.codedefenders.beans.admin.AdminCreateGamesBean" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="adminCreateGames" type="org.codedefenders.beans.admin.AdminCreateGamesBean"--%>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<jsp:include page="/jsp/header_main.jsp"/>

<%-- Workaround for the stupid <div class="nest"> in header_main. Remove this once that div is gone --%>
</div></div></div></div></div><div class="container">

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminCreateGames"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div style="height: 25px;" class="spacing"></div>

    <div class="panel panel-default">
        <div class="panel-heading">
            Staged Games
            <div style="float: right;">
                <input type="search" id="search-staged-games" class="form-control" placeholder="Search"
                       style="height: .65em; width: 10em; display: inline;">
                <div class="btn-group" data-toggle="buttons" style="margin-left: 1em;">
                    <label class="btn btn-xs btn-default">
                        <input id="togglePlayersCreated" type="checkbox">
                        Hide Players&nbsp;<span class="glyphicon glyphicon-eye-close"></span>
                    </label>
                </div>
            </div>
        </div>
        <div class="panel-body">
            <table id="stagedGamesTable" class="table table-striped table-hover table-responsive"></table>
        </div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading">
            Unassigned Users
            <div style="float: right;">
                <input type="search" id="search-unassigned-users" class="form-control" placeholder="Search"
                       style="height: .65em; width: 10em; display: inline;">
            </div>
        </div>
        <div class="panel-body">
            <table id="usersTable" class="table table-striped table-hover table-responsive"></table>
            </div>
        </div>
    </div>

    <form>
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

                <div class="form-group" id="cutDiv"
                     title="The class the games will be played on.">
                    <label for="cut_select" class="label-normal">CUT</label>
                    <select name="class" class="form-control selectpicker" data-size="large" id="cut_select">
                        <% for (GameClass clazz : GameClassDAO.getAllPlayableClasses()) { %>
                        <option value="<%=clazz.getId()%>"><%=clazz.getAlias()%></option>
                        <%}%>
                    </select>
                    <br/>
                    <a href="<%=request.getContextPath() + Paths.CLASS_UPLOAD%>?fromAdmin=true">Upload Class</a>
                </div>

                <div class="form-group"
                     title="Include mutants uploaded together with the class.">
                    <label for="withMutants" class="label-normal">Include predefined mutants (if available)</label>
                    <br/>
                    <input type="checkbox" id="withMutants" name="withMutants"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </div>

                <div class="form-group"
                     title="Include tests uploaded together with the class.">
                    <label for="withTests" class="label-normal">Include predefined tests (if available)</label>
                    <br/>
                    <input type="checkbox" id="withTests" name="withTests"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="">
                </div>

                <div class="form-group"
                     title="Maximum number of assertions per test. Increase this for difficult to test classes.">
                    <label for="maxAssertionsPerTest" class="label-normal">Max. Assertions per Test</label>
                    <br/>
                    <input class="form-control" type="number" value="2" name="maxAssertionsPerTest"
                           id="maxAssertionsPerTest" min=1 required/>
                </div>

                <div class="form-group" id="mutantValidatorLevelDiv"
                     title="Level of restrictions for the players' mutants. Click the question mark for more information.">
                    <label class="label-normal" for="mutantValidatorLevel">
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

                <div class="form-group" id="chatEnabledDiv"
                     title="Allow players to chat within their their team and across teams.">
                    <label class="label-normal" for="chatEnabled">Enable Game Chat</label>
                    <br/>
                    <input type="checkbox" id="chatEnabled" name="chatEnabled"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                           data-onstyle="primary" data-offstyle="" checked>
                </div>

                <div class="form-group" id="capturePlayersIntentionDiv"
                     title="Force players to specify their intention before submitting a mutant or test.">
                    <label class="label-normal" for="capturePlayersIntention">Capture Players Intention</label>
                    <br/>
                    <input type="checkbox" id="capturePlayersIntention" name="capturePlayersIntention"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                           data-onstyle="primary" data-offstyle="">
                </div>

                <div class="form-group"
                     title="Threshold for triggering equivalence duels automatically. Set to 0 to deactivate this feature. Click the question mark for more information.">
                    <label for="automaticEquivalenceTrigger" class="label-normal">
                        Threshold for triggering equivalence duels automatically
                        <a data-toggle="modal" href="#automaticEquivalenceTriggerExplanation" style="color:black">
                            <span class="glyphicon glyphicon-question-sign"></span>
                        </a>
                    </label>
                    <input class="form-control" type="number" value="0" name="automaticEquivalenceTrigger"
                           id="automaticEquivalenceTrigger" min=0 required/>
                </div>
                <div class="form-group"
                    title="The level the games will be played on. Harder levels restrict the information both teams receive about the other teams mutants/tests.">
                    <label for="level_group" class="label-normal">Games Level</label>
                    <div id="level_group">
                        <div class="radio">
                            <label class="label-normal">
                                <input type="radio" name="gamesLevel"
                                       value="<%=GameLevel.HARD%>"
                                       checked="checked"/>
                                Hard
                            </label>
                        </div>
                        <div class="radio">
                            <label class="label-normal">
                                <input type="radio" name="gamesLevel"
                                       value="<%=GameLevel.EASY%>"/>
                                Easy
                            </label>
                        </div>
                    </div>
                </div>

            </div> <%-- column --%>

            <div class="col-sm-12 col-md-6" style="padding-left: 2em; padding-right: 2em;">
                <h3>Game Management Settings</h3>

                <div class="form-group"
                     title="Method of assigning roles to players. Click the question mark for more information.">
                    <label for="roles_group" class="label-normal">
                        Role Assignment
                        <a data-toggle="modal" href="#roleAssignmentExplanation" style="color:black">
                            <span class="glyphicon glyphicon-question-sign"></span>
                        </a>
                    </label>
                    <div id="roles_group">
                        <div class="radio">
                            <label class="label-normal">
                                <input type="radio" name="roles"
                                       value="<%=AdminCreateGamesBean.RoleAssignmentMethod.RANDOM%>"
                                       checked="checked"/>
                                Random
                            </label>
                        </div>
                        <div class="radio">
                            <label class="label-normal">
                                <input type="radio" name="roles"
                                       value="<%=AdminCreateGamesBean.RoleAssignmentMethod.OPPOSITE%>"/>
                                Opposite Role
                            </label>
                        </div>
                    </div>
                </div>

                <div class="form-group"
                     title="Method of assigning players to teams. Click the question mark for more information.">
                    <label for="teams_group" class="label-normal">
                        Team Assignment
                        <a data-toggle="modal" href="#teamAssignmentExplanation" style="color:black">
                            <span class="glyphicon glyphicon-question-sign"></span>
                        </a>
                    </label>
                    <div id="teams_group">
                        <div class="radio">
                            <label class="label-normal">
                                <input type="radio" name="teams"
                                       value="<%=AdminCreateGamesBean.TeamAssignmentMethod.RANDOM%>"
                                       checked="checked"/>
                                Random
                            </label>
                        </div>
                        <div class="radio">
                            <label class="label-normal">
                                <input type="radio" name="teams"
                                       value="<%=AdminCreateGamesBean.TeamAssignmentMethod.SCORE_DESCENDING%>"/>
                                Scores descending
                            </label>
                        </div>
                    </div>
                </div>

                <div class="form-group"
                     title="Number of attackers per game.">
                    <label for="attackers" class="label-normal">Attackers per Game</label>
                    <input type="number" value="3" id="attackers" name="attackers" min="1" class="form-control"/>
                </div>

                <div class="form-group"
                     title="Number of defenders per game.">
                    <label for="defenders" class="label-normal">Defenders per Game</label>
                    <input type="number" value="3" id="defenders" name="defenders" min="1" class="form-control"/>
                </div>

                <div class="form-group"
                     title="Start games once they are created.">
                    <label class="label-normal" for="startGames">Start Games</label>
                    <br/>
                    <input type="checkbox" id="startGames" name="startGames"
                           class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                           data-onstyle="primary" data-offstyle="" checked>
                </div>

            </div> <%-- column --%>

        </div> <%-- row --%>

        <button class="btn btn-md btn-primary" type="submit" name="submit_users_btn" id="submit_users_btn" disabled style="margin-top: 1em">
            Stage Games
        </button>
        <p style="margin-top: .5em">
            If you just want to create a single open game without assigning players,
            you can also use the <a href="<%=request.getContextPath() + Paths.BATTLEGROUND_CREATE%>?fromAdmin=true"> Create Game</a> page.
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
                    <h4 class="modal-title">Equivalence Duel Threshold Explanation</h4>
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

    <div class="modal fade" id="roleAssignmentExplanation"
         role="dialog"
         aria-labelledby="roleAssignmentExplanation"
         aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">Role Assignment Explanation</h4>
                </div>

                <div class="modal-body">
                    <p>Specifies how roles are assigned to players:</p>
                    <ul>
                        <li>
                            <b>Random:</b>
                            Players are assigned roles randomly.
                        </li>
                        <li>
                            <b>Opposite:</b>
                            Players are assigned the opposite of their last played role, if possible.
                        </li>
                    </ul>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="teamAssignmentExplanation"
         role="dialog"
         aria-labelledby="teamAssignmentExplanation"
         aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">Team Assignment Explanation</h4>
                </div>

                <div class="modal-body">
                    <p>Specifies how players are assigned to teams:</p>
                    <ul>
                        <li>
                            <b>Random:</b>
                            Players are assigned to teams randomly.
                        </li>
                        <li>
                            <b>Scores Descending:</b>
                            Players are assigned to teams based on their total score in past games.
                            The players with the highest scores are assigned to the first games,
                            the players with the lowest scores are assigned to the last games.
                        </li>
                        <li>
                            <b>Scores block shuffled:</b>
                            Players are grouped on their total score in past games,
                            then block shuffled and assigned to teams.
                        </li>
                    </ul>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        const stagedGames = new Map(JSON.parse('${adminCreateGames.stagedGamesAsJSON}'));
        const userInfos = new Map(JSON.parse('${adminCreateGames.userInfosAsJSON}'));

        const stagedGamesData = [...stagedGames.values()].sort((a, b) => a.id - b.id);
        const userInfosData = [...userInfos.values()].sort((a, b) => a.user.id - b.user.id);

        const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        const dateFormat = Intl.DateTimeFormat([], {
            year: '2-digit',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        <%-- datatables.net/plug-ins/sorting/custom-data-source/dom-checkbox --%>
        $.fn.dataTable.ext.order['dom-checkbox'] = function(settings, col) {
            return this.api().column(col, {order:'index'}).nodes().map(function (td, i) {
                return $('input', td).prop('checked') ? 0 : 1;
            });
        };

        $.fn.dataTable.ext.order['data-lastLogin'] = function (settings, col) {
            return this.api().column(col, {order:'index'}).nodes().map(function (td, i) {
                const date = this.row(td).data().lastLogin;
                return date === null ? Number.MAX_SAFE_INTEGER : Number.MAX_SAFE_INTEGER - date;
            });
        };

        $.fn.dataTable.ext.order['data-lastRole'] = function (settings, col) {
            return this.api().column(col, {order:'index'}).nodes().map(function (td, i) {
                const role = this.row(td).data().lastRole;
                return role === null ? 'z' : role;
            });
        };

        const genUserId = row => {
            return row.user.id;
        };

        const genUserName = row => {
            return row.user.username;
        };

        const genUserLastRole = row => {
            const span = document.createElement('span');
            switch (row.lastRole) {
                case null:
                    span.style.color = 'gray';
                    span.textContent = 'none';
                    break;
                case 'ATTACKER':
                    span.textContent = 'Attacker';
                    break;
                case 'DEFENDER':
                    span.textContent = 'Defender';
                    break;
                case 'PLAYER':
                    span.textContent = 'Player';
                    break;
                default:
                    span.textContent = 'Unknown Role';
                    break;
            }
            return span.outerHTML;
        };

        const genUserTotalScore = row => {
            return row.totalScore;
        };

        const genUserLastLogin = row => {
            const span = document.createElement('span');
            if (row.lastLogin === null) {
                span.style.color = 'gray';
                span.textContent = 'never';
            } else {
                span.title = 'Dates are converted to you local timezone: ' + timezone + '.';
                span.textContent = dateFormat.format(row.lastLogin);
            }
            return span.outerHTML;
        };

        const genUserAddToGame = row => {
            return 'TODO';
        };

        const genStagedGameId = row => {
            return row.id;
        };

        const genStagedGameClass = row => {
            const name = row.gameSettings.cut.name;
            const alias = row.gameSettings.cut.alias;
            if (name === alias) {
                return name;
            } else {
                return name + ' (alias ' + alias + ')';
            }
        };

        const genStagedGameType = row => {
            switch (row.gameSettings.gameType) {
                case 'MULTIPLAYER':
                    return 'Multiplayer';
                case 'MELEE':
                    return 'Melee';
                default:
                    return 'Unknown game type';
            }
        };

        const genStagedGameSettings = row => {
            return 'TODO';
        };

        const genStagedGamePlayers = row => {
            return 'TODO';
        }

        $(document).ready(function () {
            const stagedGamesTable = $('#stagedGamesTable').DataTable({
                data: stagedGamesData,
                columns: [
                    { data: null, orderDataType: 'dom-checkbox', defaultContent: '<input type="checkbox">', title: '' },
                    { data: genStagedGameId, title: 'ID' },
                    { data: genStagedGameClass, title: 'Class' },
                    { data: genStagedGameType, title: 'Game Type' },
                    { data: genStagedGameSettings, title: 'Settings' },
                    { data: genStagedGamePlayers, title: 'Players' },
                ],
                order: [[1, 'asc']],
                scrollY: '800px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'There are currently no staged multiplayer games.'}
            });
            $('#search-staged-games').on('keyup', function () {
                setTimeout(() => stagedGamesTable.search(this.value).draw(), 0);
            });

            const usersTable = $('#usersTable').DataTable({
                data: userInfosData,
                columns: [
                    { data: null, orderDataType: 'dom-checkbox', defaultContent: '<input type="checkbox">', title: '' },
                    { data: genUserId, title: 'ID' },
                    { data: genUserName, title: 'Name' },
                    { data: genUserLastRole, orderDataType: 'data-lastRole', title: 'Last Role' },
                    { data: genUserTotalScore, title: 'Total Score' },
                    { data: genUserLastLogin, orderDataType: 'data-lastLogin', title: 'Last Login' },
                    { data: genUserAddToGame, title: 'Add to existing game' }
                ],
                order: [[5, 'asc']],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'There are currently no unassigned users.'}
            });
            $('#search-unassigned-users').on('keyup', function () {
                setTimeout(() => usersTable.search(this.value).draw(), 0);
            });

            /*
            $('#togglePlayersCreated').on('change', function () {
                const checked = $(this).is(':checked');
                if (checked) {
                    $("[id=playersTableCreated]").hide();
                    $("[id=playersTableHidden]").show();
                } else {
                    $("[id=playersTableHidden]").hide();
                    $("[id=playersTableCreated]").show();
                }
            });
            */
        });
    </script>
</div>

<%-- Workaround for the stupid <div class="nest"> in header_main. Remove this once that div is gone --%>
</div><div><div><div><div><div><div>

<%@ include file="/jsp/footer.jsp" %>

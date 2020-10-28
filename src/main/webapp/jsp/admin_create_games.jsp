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
                <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                    <label class="btn btn-xs btn-default">
                        <input id="toggle-show-players" type="checkbox">
                        Hide Players&nbsp;&nbsp;<span class="glyphicon glyphicon-eye-close"></span>
                    </label>
                </div>
                <input type="search" id="search-staged-games" class="form-control" placeholder="Search"
                       style="height: .65em; width: 10em; display: inline;">
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
                <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                    <label class="btn btn-xs btn-default">
                        <input id="toggle-show-assigned-users" type="checkbox">
                        Show Assigned Players&nbsp;&nbsp;<span class="glyphicon glyphicon-eye-open"></span>
                    </label>
                </div>
                <input type="search" id="search-users" class="form-control" placeholder="Search"
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
        const activeMultiplayerGameIds = JSON.parse('${adminCreateGames.activeMultiplayerGameIdsJSON}').sort();
        const activeMeleeGameIds = JSON.parse('${adminCreateGames.activeMeleeGameIdsJSON}').sort();

        const stagedGamesList = [...stagedGames.values()].sort((a, b) => a.id - b.id);
        const userInfosList = [...userInfos.values()].sort((a, b) => a.user.id - b.user.id);
        const activeGameIds = [...activeMultiplayerGameIds, ...activeMeleeGameIds].sort();

        const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        const dateFormat = Intl.DateTimeFormat([], {
            year: '2-digit',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        let playersHidden = false;

        /* Sort checkboxes by checked state (from datatables.net/plug-ins/sorting/custom-data-source/dom-checkbox). */
        $.fn.dataTable.ext.order['dom-checkbox'] = function (settings, col) {
            return this.api().column(col, {order:'index'}).nodes().map(function (td, i) {
                return $('input', td).prop('checked') ? 0 : 1;
            });
        };

        const renderUserLastRole = function (lastRole, type, row, meta) {
            switch (type) {
                case 'type':
                    return lastRole === null ? '' : lastRole;
                case 'sort':
                    return lastRole === null ? 'Z' : lastRole;
                case 'filter':
                    return lastRole === null ? 'none' : lastRole;
                case 'display':
                    const span = document.createElement('span');
                    switch (lastRole) {
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
            }
        };

        const renderUserLastLogin = function (lastLogin, type, row, meta) {
            switch (type) {
                case 'type':
                    return lastLogin === null ? 0 : lastLogin;
                case 'sort':
                    /* Sort users with more recent logins first, users who never logged in last. */
                    return lastLogin === null ? Number.MAX_SAFE_INTEGER : Number.MAX_SAFE_INTEGER - lastLogin;
                case 'filter':
                    return lastLogin === null ? 'never' : dateFormat.format(lastLogin);
                case 'display':
                    const span = document.createElement('span');
                    if (lastLogin === null) {
                        span.style.color = 'gray';
                        span.textContent = 'never';
                    } else {
                        span.title = 'Dates are converted to you local timezone: ' + timezone;
                        span.textContent = dateFormat.format(lastLogin);
                    }
                    return span.outerHTML;
            }
        };

        const renderUserAddToGame = function (userInfo, type, row, meta) {
            const table = document.createElement('table');
            const tr = table.insertRow();

            const gameIdCell = tr.insertCell();
            gameIdCell.style.width = '5em';

            const gameIdSelect = document.createElement('select');
            gameIdSelect.classList.add('add-player-game');
            gameIdCell.appendChild(gameIdSelect);

            for (const stagedGame of stagedGamesList) {
                const option = document.createElement('option');
                option.textContent = 'T' + stagedGame.id;
                option.value = 'T' + stagedGame.id;
                gameIdSelect.add(option);
            }
            for (const gameId of activeGameIds) {
                const option = document.createElement('option');
                option.textContent = String(gameId);
                option.value = String(gameId);
                gameIdSelect.add(option);
            }

            const roleCell = tr.insertCell();
            roleCell.style.width = '8em';

            const roleSelect = document.createElement('select');
            roleSelect.classList.add('add-player-role');
            roleCell.appendChild(roleSelect);

            const addToGameCell = tr.insertCell();
            addToGameCell.style.width = '0px';
            addToGameCell.innerHTML =
                `<button disabled class="add-player-button btn btn-sm btn-primary" title="Add player to selected game">
                     <span class="glyphicon glyphicon-plus"></span>
                 </button>`;

            return table.outerHTML;
        };

        const renderStagedGameId = function (stagedGameId, type, row, meta) {
            switch (type) {
                case 'sort':
                    return stagedGameId
                case 'type':
                case 'filter':
                case 'display':
                    return 'T' + stagedGameId;
            }
        };

        const renderStagedGameClass = function (cut, type, row, meta) {
            const name = cut.name;
            const alias = cut.alias;
            if (name === alias) {
                return name;
            } else {
                return name + ' (alias ' + alias + ')';
            }
        };

        const renderStagedGameType = function (gameType, type, row, meta) {
            switch (gameType) {
                case 'MULTIPLAYER':
                    return 'Multiplayer';
                case 'MELEE':
                    return 'Melee';
                default:
                    return 'Unknown game type';
            }
        };

        const renderStagedGamePlayers = function (stagedGame, type, row, meta) {
            const attackers = stagedGame.attackers.map(userInfos.get, userInfos)
            const defenders = stagedGame.defenders.map(userInfos.get, userInfos);
            const players = [...attackers, ...defenders];

            switch (type) {
                case 'filter':
                    return players.map(userInfo => userInfo.user.username).join(' ');
                case 'sort':
                case 'type':
                    return players.length;
                case 'display':
                    if (playersHidden) {
                        return '<span style="color: gray;">(hidden)</span>'
                    } else {
                        return createStagedGamePlayersTable(stagedGame, attackers, defenders);
                    }
            }
        }

        const createStagedGamePlayersTable = function (stagedGame, attackers, defenders) {
            const table = document.createElement('table');
            table.classList.add('staged-game-players');
            table.style.width = '100%';

            if (stagedGame.gameSettings.gameType === 'MELEE') {
                const players = [...attackers, ...defenders];
                players.sort((a, b) => a.user.id - b.user.id);

                for (const player of players) {
                    addStagedGamePlayersRow(table, stagedGame, player, 'PLAYER');
                }
            } else {
                attackers.sort((a, b) => a.user.id - b.user.id);
                defenders.sort((a, b) => a.user.id - b.user.id);

                for (const attacker of attackers) {
                    addStagedGamePlayersRow(table, stagedGame, attacker, 'ATTACKER');
                }
                for (const defender of defenders) {
                    addStagedGamePlayersRow(table, stagedGame, defender, 'DEFENDER');
                }
            }

            return table.outerHTML;
        };

        const addStagedGamePlayersRow = function (table, stagedGame, userInfo, role) {
            const tr = table.insertRow();
            tr.setAttribute('data-user-id', userInfo.user.id);
            if (role === 'ATTACKER') {
                tr.style.backgroundColor = '#EDCECE';
            } else if (role === 'DEFENDER') {
                tr.style.backgroundColor = '#CED6ED';
            }

            const userNameCell = tr.insertCell();
            userNameCell.style.paddingLeft = '1em';
            userNameCell.style.width = '20%';
            userNameCell.textContent = userInfo.user.username;

            const lastRoleCell = tr.insertCell();
            lastRoleCell.style.width = '15%';
            lastRoleCell.innerHTML = renderUserLastRole(userInfo.lastRole, 'display');

            const totalScoreCell = tr.insertCell();
            totalScoreCell.style.width = '8%';
            totalScoreCell.textContent = userInfo.totalScore;

            const switchRolesCell = tr.insertCell();
            switchRolesCell.style.width = '0px';
            switchRolesCell.innerHTML =
                    `<button class="switch-role-button btn btn-sm btn-primary" title="Switch role of player">
                         <span class="glyphicon glyphicon-transfer"></span>
                     </button>`;

            if (role === 'PLAYER') {
                switchRolesCell.firstChild.style.visibility = 'hidden';
            }

            const removeCell = tr.insertCell();
            removeCell.style.width = '0px';
            removeCell.innerHTML =
                    `<button class="remove-player-button btn btn-sm btn-danger" title="Remove player from game">
                         <span class="glyphicon glyphicon-trash"></span>
                     </button>`;

            const moveGameIdCell = tr.insertCell();
            moveGameIdCell.style.width = '5em';

            const gameIdSelect = document.createElement('select');
            gameIdSelect.classList.add('move-player-game');
            moveGameIdCell.appendChild(gameIdSelect);

            for (const otherStagedGame of stagedGamesList) {
                if (otherStagedGame.id !== stagedGame.id) {
                    const option = document.createElement('option');
                    option.textContent = 'T' + otherStagedGame.id;
                    option.value = 'T' + otherStagedGame.id;
                    gameIdSelect.add(option);
                }
            }

            const moveRoleCell = tr.insertCell();
            moveRoleCell.style.width = '8em';

            const roleSelect = document.createElement('select');
            roleSelect.classList.add('move-player-role');
            moveRoleCell.appendChild(roleSelect);

            const moveButtonCell = tr.insertCell();
            moveButtonCell.style.width = '0px';
            moveButtonCell.innerHTML =
                    `<button disabled class="move-player-button btn btn-sm btn-primary" title="Move player to selected game">
                         <span class="glyphicon glyphicon-arrow-right"></span>
                     </button>`;
        };

        const adjustFormForGame = function (roleSelect, button, gameIdStr) {
            roleSelect.innerHTML = '';
            button.disabled = true;

            let gameType;
            if (gameIdStr.startsWith('T')) {
                const gameId = Number(gameIdStr.substring(1));
                gameType = stagedGames.get(gameId).gameSettings.gameType;
            } else {
                const gameId = Number(gameIdStr);
                if (activeMultiplayerGameIds.includes(gameId)) {
                    gameType = 'MULTIPLAYER';
                } else if (activeMeleeGameIds.includes(gameId)) {
                    gameType = 'MELEE';
                }
            }

            if (gameType === 'MULTIPLAYER') {
                const attackerOption = document.createElement('option');
                attackerOption.textContent = 'Attacker';
                attackerOption.value = 'ATTACKER';
                roleSelect.appendChild(attackerOption);
                const defenderOption = document.createElement('option');
                defenderOption.textContent = 'Defender';
                defenderOption.value = 'DEFENDER';
                roleSelect.appendChild(defenderOption);
                button.disabled = false;
            } else if (gameType === 'MELEE') {
                const playerOption = document.createElement('option');
                playerOption.textContent = 'Player';
                playerOption.value = 'PLAYER';
                roleSelect.appendChild(playerOption);
                button.disabled = false;
            }
        };

        const postForm = function (params) {
            const form = document.createElement('form');
            form.method = 'post';

            for (const [name, value] of Object.entries(params)) {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = String(name);
                if (Array.isArray(value)) {
                    input.value = value.join(',');
                } else {
                    input.value = String(value);
                }
                form.appendChild(input);
            }

            document.body.appendChild(form);
            form.submit();
        };

        /* Staged games table. */
        $(document).ready(function () {
            const stagedGamesTable = $('#stagedGamesTable').DataTable({
                data: stagedGamesList,
                columns: [
                    {
                        data: null,
                        orderDataType: 'dom-checkbox',
                        defaultContent: '<input type="checkbox">',
                        type: 'html',
                        title: ''
                    },
                    {
                        data: 'id',
                        render: renderStagedGameId,
                        type: 'num-fmt',
                        title: 'ID'
                    },
                    {
                        data: 'gameSettings.cut',
                        render: renderStagedGameClass,
                        type: 'string',
                        title: 'Class'
                    },
                    {
                        data: 'gameSettings.gameType',
                        render: renderStagedGameType,
                        type: 'string',
                        title: 'Game Type'
                    },
                    {
                        data: null,
                        orderable: false,
                        defaultContent: '<span class="btn btn-xs btn-default show-settings">Show</span>',
                        type: 'html',
                        title: 'Settings'
                    },
                    {
                        data: null,
                        render: renderStagedGamePlayers,
                        type: 'html',
                        width: '55%',
                        title: 'Players (Username, Last Role, Total Score)'
                    },
                ],
                drawCallback: function () { $(this).find('select').prop('selectedIndex', -1); },
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

            $('#toggle-show-players').on('change', function () {
                playersHidden = $(this).is(':checked');
                stagedGamesTable.rows().invalidate().draw();
            });

            $(stagedGamesTable.table().node()).on('change', '.move-player-game', function () {
                const tr = $(this).closest('tr');
                const roleSelect = $(tr).find('.move-player-role').get(0);
                const button = $(tr).find('.move-player-button').get(0);
                adjustFormForGame(roleSelect, button, this.value);
            });

            $(stagedGamesTable.table().node()).on('click', '.switch-role-button', function () {
                const innerTr = $(this).parents('tr').get(0);
                const outerTr = $(this).parents('tr').get(1);
                const userId = Number(innerTr.getAttribute('data-user-id'));
                const stagedGame = stagedGamesTable.row(outerTr).data();
                let role = stagedGame.attackers.includes(userId) ? 'DEFENDER' : 'ATTACKER';
                postForm({
                    formType: 'movePlayerBetweenStagedGames',
                    userId: userId,
                    gameIdFrom: 'T' + stagedGame.id,
                    gameIdTo: 'T' + stagedGame.id,
                    role
                });
            });

            $(stagedGamesTable.table().node()).on('click', '.remove-player-button', function () {
                const innerTr = $(this).parents('tr').get(0);
                const outerTr = $(this).parents('tr').get(1);
                const stagedGame = stagedGamesTable.row(outerTr).data();
                postForm({
                    formType: 'removePlayerFromStagedGame',
                    userId: innerTr.getAttribute('data-user-id'),
                    gameId: 'T' + stagedGame.id,
                });
            });

            $(stagedGamesTable.table().node()).on('click', '.move-player-button', function () {
                const innerTr = $(this).parents('tr').get(0);
                const outerTr = $(this).parents('tr').get(1);
                const stagedGame = stagedGamesTable.row(outerTr).data();
                const gameSelect = $(innerTr).find('.move-player-game').get(0);
                const roleSelect = $(innerTr).find('.move-player-role').get(0);
                postForm({
                    formType: 'movePlayerBetweenStagedGames',
                    userId: innerTr.getAttribute('data-user-id'),
                    gameIdFrom: 'T' + stagedGame.id,
                    gameIdTo: gameSelect.value,
                    role: roleSelect.value
                });
            });
        });

        /* Users table. */
        $(document).ready(function () {
            const usersTable = $('#usersTable').DataTable({
                data: userInfosList,
                columns: [
                    {
                        data: null,
                        orderDataType: 'dom-checkbox',
                        defaultContent: '<input type="checkbox">',
                        type: 'html',
                        title: ''
                    },
                    {
                        data: 'user.id',
                        type: 'num',
                        title: 'ID'
                    },
                    {
                        data: 'user.username',
                        type: 'string',
                        title: 'Name'
                    },
                    {
                        data: 'lastRole',
                        render: renderUserLastRole,
                        type: 'html',
                        title: 'Last Role'
                    },
                    {
                        data: 'totalScore',
                        type: 'num',
                        title: 'Total Score'
                    },
                    {
                        data: 'lastLogin',
                        render: renderUserLastLogin,
                        type: 'html',
                        title: 'Last Login'
                    },
                    {
                        data: null,
                        render: renderUserAddToGame,
                        orderable: false,
                        type: 'html',
                        title: 'Add to existing game'
                    }
                ],
                drawCallback: function () { $(this).find('select').prop('selectedIndex', -1); },
                order: [[5, 'asc']],
                scrollY: '400px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'There are currently no unassigned users.'}
            });

            $('#search-users').on('keyup', function () {
                setTimeout(() => usersTable.search(this.value).draw(), 0);
            });

            $(usersTable.table().node()).on('change', '.add-player-game', function () {
                const tr = $(this).closest('tr').get(0);
                const roleSelect = $(tr).find('.add-player-role').get(0);
                const button = $(tr).find('.add-player-button').get(0);
                adjustFormForGame(roleSelect, button, this.value);
            });

            $(usersTable.table().node()).on('click', '.add-player-button', function () {
                /* Go up two levels of tr, since the form is in a table itself. */
                const tr = $(this).parents('tr').get(1);
                const userInfo = usersTable.row(tr).data();
                const gameSelect = $(tr).find('.add-player-game').get(0);
                const roleSelect = $(tr).find('.add-player-role').get(0);
                postForm({
                    formType: 'addPlayerToGame',
                    userId: userInfo.user.id,
                    gameId: gameSelect.value,
                    role: roleSelect.value
                });
            });
        });
    </script>
</div>

<%-- Workaround for the stupid <div class="nest"> in header_main. Remove this once that div is gone --%>
</div><div><div><div><div><div><div>

<%@ include file="/jsp/footer.jsp" %>

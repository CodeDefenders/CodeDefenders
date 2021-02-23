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
<%@ page import="org.codedefenders.beans.admin.StagedGameList" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="adminCreateGames" type="org.codedefenders.beans.admin.AdminCreateGamesBean"--%>

<jsp:useBean id="login" class="org.codedefenders.beans.user.LoginBean" scope="request"/>

<jsp:include page="/jsp/header_main.jsp"/>

<style>
    /* Change colors of selected table rows. */
    table.dataTable tbody > tr.selected,
    table.dataTable tbody > tr > .selected {
        background-color: #D8E0EA;
        color: inherit;
    }

    /* Initially hide melee-specific forms. */
    .melee-specific {
        display: none;
    }
</style>

<div class="full-width">
    <% request.setAttribute("adminActivePage", "adminCreateGames"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div style="height: 25px;" class="spacing"></div>

    <div class="panel panel-default">
        <div class="panel-heading">
            Staged Games
            <div style="float: right;">
                <button id="select-visible-games" class="btn btn-xs btn-default" style="margin-right: 1em;">
                    Select Visible
                </button>
                <button id="deselect-visible-games" class="btn btn-xs btn-default" style="margin-right: 1em;">
                    Deselect Visible
                </button>
                <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                    <label class="btn btn-xs btn-default">
                        <input id="toggle-hide-players" type="checkbox">
                        Hide Players&nbsp;&nbsp;<span class="glyphicon glyphicon-eye-close"></span>
                    </label>
                </div>
                <input type="search" id="search-staged-games" class="form-control" placeholder="Search"
                       style="height: .65em; width: 10em; display: inline;">
            </div>
        </div>
        <div class="panel-body">
            <table id="table-staged-games" class="table table-responsive"></table>

            <form class="form-inline" style="margin-top: 1em;">
                <div class="form-group">
                    <button class="btn btn-md btn-primary" type="button" name="create-games-button"
                            id="create-games-button" style="margin-top: 1em" disabled>
                        Create Games
                    </button>
                </div>
                <div class="form-group">
                    <button class="btn btn-md btn-danger" type="button" name="delete-games-button"
                            id="delete-games-button" style="margin-top: 1em" disabled>
                        Delete Games
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading">
            Unassigned Users
            <div style="float: right;">
                <button id="select-visible-users" class="btn btn-xs btn-default" style="margin-right: 1em;">
                    Select Visible
                </button>
                <button id="deselect-visible-users" class="btn btn-xs btn-default" style="margin-right: 1em;">
                    Deselect Visible
                </button>
                <div class="btn-group" data-toggle="buttons" style="margin-right: 1em;">
                    <label class="btn btn-xs btn-default"
                           title="Show users that are part of an existing active game.">
                        <input id="toggle-show-assigned-users" type="checkbox">
                        Show Assigned Users (Active Games)&nbsp;&nbsp;<span class="glyphicon glyphicon-eye-open"></span>
                    </label>
                </div>
                <input type="search" id="search-users" class="form-control" placeholder="Search"
                       style="height: .65em; width: 10em; display: inline;">
            </div>
        </div>
        <div class="panel-body">
            <table id="table-users" class="table table-responsive"></table>
        </div>
    </div>

    <form id="form-settings">
        <div class="row">
            <div class="col-sm-12 col-md-6">

                <div class="panel panel-default">
                    <div class="panel-heading">
                        Game Settings
                    </div>
                    <div class="panel-body">
                        <div class="form-group"
                             title="The type of game.">
                            <label for="gameType-group" class="label-normal">
                                Game Type
                            </label>
                            <div id="gameType-group">
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="gameType"
                                               value="<%=StagedGameList.GameSettings.GameType.MULTIPLAYER%>"
                                               checked/>
                                        Battleground
                                    </label>
                                </div>
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="gameType"
                                               value="<%=StagedGameList.GameSettings.GameType.MELEE%>"/>
                                        Melee
                                    </label>
                                </div>
                            </div>
                        </div>

                        <div class="form-group"
                             title="The class the games will be played on.">
                            <label for="cut" class="label-normal">CUT</label>
                            <select id="cut" name="cut" class="form-control selectpicker" data-size="large">
                                <% for (GameClass clazz : GameClassDAO.getAllPlayableClasses()) { %>
                                    <option value="<%=clazz.getId()%>"><%=clazz.getAlias()%></option>
                                <% } %>
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

                        <div class="form-group"
                             title="Level of restrictions for the players' mutants. Click the question mark for more information.">
                            <label class="label-normal" for="mutantValidatorLevel">
                                Mutant validator
                                <a data-toggle="modal" href="#validatorExplanation" style="color:black">
                                    <span class="glyphicon glyphicon-question-sign"></span>
                                </a>
                            </label>
                            <select id="mutantValidatorLevel" name="mutantValidatorLevel" class="form-control selectpicker"
                                    data-size="medium">
                                <% for (CodeValidatorLevel level : CodeValidatorLevel.values()) { %>
                                    <option value=<%=level.name()%> <%=level.equals(CodeValidatorLevel.MODERATE) ? "selected" : ""%>>
                                        <%=level.name().toLowerCase()%>
                                    </option>
                                <% } %>
                            </select>
                        </div>

                        <div class="form-group"
                             title="Allow players to chat within their their team and across teams.">
                            <label class="label-normal" for="chatEnabled">Enable Game Chat</label>
                            <br/>
                            <input type="checkbox" id="chatEnabled" name="chatEnabled"
                                   class="form-control" data-size="medium" data-toggle="toggle" data-on="On" data-off="Off"
                                   data-onstyle="primary" data-offstyle="" checked>
                        </div>

                        <div class="form-group" id="capturePlayersIntentionDiv"
                             title="Force players to specify their intention before submitting a mutant or test.">
                            <label class="label-normal" for="captureIntentions">Capture Players Intention</label>
                            <br/>
                            <input type="checkbox" id="captureIntentions" name="captureIntentions"
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
                            <label for="level-group" class="label-normal">Games Level</label>
                            <div id="level-group">
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="level"
                                               value="<%=GameLevel.HARD%>"
                                               checked/>
                                        Hard
                                    </label>
                                </div>
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="level"
                                               value="<%=GameLevel.EASY%>"/>
                                        Easy
                                    </label>
                                </div>
                            </div>
                        </div>

                        <div class="form-group"
                             title="Start games once they are created.">
                            <label class="label-normal" for="startGame">Automatically Start Game After Creation</label>
                            <br/>
                            <input type="checkbox" id="startGame" name="startGame"
                                   class="form-control" data-size="medium" data-toggle="toggle" data-on="Yes" data-off="No"
                                   data-onstyle="primary" data-offstyle="" checked>
                        </div>

                    </div>
                </div>

            </div> <%-- column --%>
            <div class="col-sm-12 col-md-6">

                <div class="panel panel-default">
                    <div class="panel-heading">
                        Create Staged Games With Users
                        <a data-toggle="modal" href="#stageGamesWithUsersExplanation" style="color:black">
                            <span class="glyphicon glyphicon-question-sign"></span>
                        </a>
                    </div>
                    <div class="panel-body">

                        <div class="form-group">
                            <label for="userNames" class="label-normal">
                                User Names
                            </label>
                            <a data-toggle="modal" href="#userNamesExplanation" style="color:black">
                                <span class="glyphicon glyphicon-question-sign"></span>
                            </a>
                            <textarea class="form-control" rows="5" id="userNames" name="userNames"></textarea>
                        </div>

                        <div class="form-group multiplayer-specific"
                             title="Method of assigning roles to players. Click the question mark for more information.">
                            <label for="roleAssignmentMethod-group" class="label-normal">
                                Role Assignment
                                <a data-toggle="modal" href="#roleAssignmentExplanation" style="color:black">
                                    <span class="glyphicon glyphicon-question-sign"></span>
                                </a>
                            </label>
                            <div id="roleAssignmentMethod-group">
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="roleAssignmentMethod"
                                               value="<%=AdminCreateGamesBean.RoleAssignmentMethod.RANDOM%>"
                                               checked/>
                                        Random
                                    </label>
                                </div>
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="roleAssignmentMethod"
                                               value="<%=AdminCreateGamesBean.RoleAssignmentMethod.OPPOSITE%>"/>
                                        Opposite Role
                                    </label>
                                </div>
                            </div>
                        </div>

                        <div class="form-group"
                             title="Method of assigning players to teams. Click the question mark for more information.">
                            <label for="teamAssignmentMethod-group" class="label-normal">
                                Team Assignment
                                <a data-toggle="modal" href="#teamAssignmentExplanation" style="color:black">
                                    <span class="glyphicon glyphicon-question-sign"></span>
                                </a>
                            </label>
                            <div id="teamAssignmentMethod-group">
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="teamAssignmentMethod"
                                               value="<%=AdminCreateGamesBean.TeamAssignmentMethod.RANDOM%>"
                                               checked/>
                                        Random
                                    </label>
                                </div>
                                <div class="radio">
                                    <label class="label-normal">
                                        <input type="radio" name="teamAssignmentMethod"
                                               value="<%=AdminCreateGamesBean.TeamAssignmentMethod.SCORE_DESCENDING%>"/>
                                        Scores Descending
                                    </label>
                                </div>
                            </div>
                        </div>

                        <div class="form-group multiplayer-specific"
                             title="Number of attackers per game.">
                            <label for="attackersPerGame" class="label-normal">Attackers per Game</label>
                            <input type="number" value="3" id="attackersPerGame" name="attackersPerGame" min="1" class="form-control"/>
                        </div>

                        <div class="form-group multiplayer-specific"
                             title="Number of defenders per game.">
                            <label for="defendersPerGame" class="label-normal">Defenders per Game</label>
                            <input type="number" value="3" id="defendersPerGame" name="defendersPerGame" min="1" class="form-control"/>
                        </div>

                        <div class="form-group melee-specific"
                             title="Players per game.">
                            <label for="playersPerGame" class="label-normal">Players per Game</label>
                            <input type="number" value="6" id="playersPerGame" name="playersPerGame" min="1" class="form-control"/>
                        </div>

                        <button class="btn btn-md btn-primary" type="button" name="stage-games-with-users-button"
                                id="stage-games-with-users-button" style="margin-top: 1em" disabled>
                            Stage Games
                        </button>

                    </div>
                </div>

                <div class="panel panel-default">
                    <div class="panel-heading">
                        Create Empty Staged Games
                        <a data-toggle="modal" href="#stageEmptyGamesExplanation" style="color:black">
                            <span class="glyphicon glyphicon-question-sign"></span>
                        </a>
                    </div>
                    <div class="panel-body">
                        <div class="form-group"
                             title="Number of staged games to create.">
                            <label for="numGames" class="label-normal">Number of Games</label>
                            <input type="number" value="1" id="numGames" name="numGames" min="1" max="100" class="form-control"/>
                        </div>

                        <button class="btn btn-md btn-primary" type="button" name="stage-games-empty-button"
                                id="stage-games-empty-button" style="margin-top: 1em">
                            Stage Games
                        </button>
                    </div>
                </div>

            </div> <%-- column --%>
        </div> <%-- row --%>
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

    <div class="modal fade" id="automaticEquivalenceTriggerExplanation" role="dialog"
        aria-labelledby="automaticEquivalenceTriggerExplanation" aria-hidden="true">
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

    <div class="modal fade" id="roleAssignmentExplanation" role="dialog"
         aria-labelledby="roleAssignmentExplanation" aria-hidden="true">
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

    <div class="modal fade" id="teamAssignmentExplanation" role="dialog"
         aria-labelledby="teamAssignmentExplanation" aria-hidden="true">
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

    <div class="modal fade" id="userNamesExplanation" role="dialog"
         aria-labelledby="userNamesExplanation" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">User Name Explanation</h4>
                </div>
                <div class="modal-body">
                    Newline-separated list of usernames or emails. The users with these names/emails, as well as the
                    users selected in the table, will be assigned to created staged games.
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="stageGamesWithUsersExplanation" role="dialog"
         aria-labelledby="stageGamesWithUsersExplanation" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">Stage Games With Users Explanation</h4>
                </div>
                <div class="modal-body">
                    Selected users from the table, as well as users entered in the text area, will be assigned to new
                    staged games. The number of games is decided by the number of users and the selected method of
                    distributing the users to teams.
                    <br/>
                    <br/>
                    The settings for the staged games are specified in the left panel.
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="stageEmptyGamesExplanation" role="dialog"
         aria-labelledby="stageEmptyGamesExplanation" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 class="modal-title">Stage Empty Games Explanation</h4>
                </div>
                <div class="modal-body">
                    Create a number staged games without users assigned to them.
                    <br/>
                    <br/>
                    The settings for the staged games are specified in the left panel.
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        /**
         *  Maps IDs to staged games.
         */
        const stagedGames = new Map(JSON.parse('${adminCreateGames.stagedGamesAsJSON}'));

        /**
         * Maps IDs to user infos.
         */
        const userInfos = new Map(JSON.parse('${adminCreateGames.userInfosAsJSON}'));

        /**
         * IDs of active (started and not finished) multiplayer games.
         */
        const activeMultiplayerGameIds = new Set(JSON.parse('${adminCreateGames.activeMultiplayerGameIdsJSON}'));

        /**
         *  IDs of active (started and not finished) melee games.
         */
        const activeMeleeGameIds = new Set(JSON.parse('${adminCreateGames.activeMeleeGameIdsJSON}'));

        /**
         * IDs of users not assigned to any active games.
         */
        const unassignedUserIds = new Set(JSON.parse('${adminCreateGames.unassignedUserIdsJSON}'));


        /**
         * IDs of users assigned to any staged game.
         */
        const assignedUserIdsStaged = new Set([...stagedGames.values()]
            .flatMap(game => [...game.attackers, ...game.defenders]));

        /**
         * IDs of active multiplayer/melee games, sorted.
         */
        const activeGameIds = [...activeMultiplayerGameIds.values(), ...activeMeleeGameIds.values()]
            .sort();

        /**
         * Staged games to be displayed in the table, sorted by ID.
         */
        const stagedGamesTableData = [...stagedGames.values()]
            .sort((a, b) => a.id - b.id);

        /**
         * User infos to be displayed in the table, sorted by ID.
         */
        const usersTableData = [...userInfos.values()]
            .filter(userInfo => !assignedUserIdsStaged.has(userInfo.user.id))
            .sort((a, b) => a.user.id - b.user.id);


        /**
         * Hide assigned players in the staged games table.
         */
        let hideStagedGamePlayers = JSON.parse(localStorage.getItem('hideStagedGamePlayers')) || false;

        /**
         * Show users assigned to active games in the users table.
         */
        let showAssignedUsers = JSON.parse(localStorage.getItem('showAssignedUsers')) || false;


        // TODO: Generate these automatically with a bean?.
        const GameType = {
            MULTIPLAYER:    {name: 'MULTIPLAYER',   display: 'Battleground'},
            MELEE:          {name: 'MELEE',         display: 'Melee'}
        };
        const CodeValidatorLevel = {
            RELAXED:        {name: 'RELAXED',       display: 'Relaxed'},
            MODERATE:       {name: 'MODERATE',      display: 'Moderate'},
            STRICT:         {name: 'STRICT',        display: 'Strict'}
        };
        const GameLevel = {
            HARD:           {name: 'HARD',          display: 'Hard'},
            MEDIUM:         {name: 'MEDIUM',        display: 'Medium'},
            EASY:           {name: 'EASY',          display: 'Easy'}
        };
        const Role = {
            ATTACKER:       {name: 'ATTACKER',      display: 'Attacker'},
            DEFENDER:       {name: 'DEFENDER',      display: 'Defender'},
            OBSERVER:       {name: 'OBSERVER',      display: 'Observer'},
            PLAYER:         {name: 'PLAYER',        display: 'Player'},
            NONE:           {name: 'NONE',          display: 'None'}
        };

        /* Timezone and date format to format the last login date for users. */
        const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        const dateFormat = Intl.DateTimeFormat([], {
            year: '2-digit',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        /* Sorting method to select DataTables rows by whether they are selected by the select extension. */
        $.fn.dataTable.ext.order['select-extension'] = function (settings, col) {
            return this.api().column(col, {order:'index'}).nodes().map(function (td, i) {
                return $(td).closest('tr').hasClass('selected') ? '0' : '1';
            });
        };

        const renderUserLastRole = function (lastRole, type, row, meta) {
            switch (type) {
                case 'type':
                    return lastRole === null ? '' : lastRole;
                case 'sort':
                    return lastRole === null ? 'ZZ' : lastRole;
                case 'filter':
                    return lastRole === null ? 'none' : lastRole;
                case 'display':
                    const span = document.createElement('span');
                    if (lastRole === null) {
                        span.style.color = 'gray';
                        span.textContent = 'none';
                    } else {
                        span.textContent = Role[lastRole].display;
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

            for (const stagedGame of stagedGamesTableData) {
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

            /* The role select is generated empty, as options are set based on the type of the selected game. */
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
            return GameType[gameType].display;
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
                    /* Sort the players column by the number of players. */
                    return players.length;
                case 'display':
                    if (hideStagedGamePlayers) {
                        return '<span style="color: gray;">(hidden)</span>'
                    } else {
                        return createStagedGamePlayersTable(stagedGame, attackers, defenders);
                    }
            }
        }

        /**
         * Creates a table displaying the players of a staged game, as well as a "form" to manipulate them.
         * @param {StagedGame} stagedGame The staged game to create the table for.
         * @return {String} The HTML of the created table.
         */
        const createStagedGamePlayersTable = function (stagedGame) {
            const attackers = stagedGame.attackers.map(userInfos.get, userInfos)
            const defenders = stagedGame.defenders.map(userInfos.get, userInfos);

            const table = document.createElement('table');
            table.classList.add('staged-game-players');
            table.style.width = '100%';

            if (stagedGame.gameSettings.gameType === GameType.MELEE.name) {
                const players = [...attackers, ...defenders];
                players.sort((a, b) => a.user.id - b.user.id);

                for (const player of players) {
                    addStagedGamePlayersRow(table, stagedGame, player, Role.PLAYER.name);
                }
            } else {
                attackers.sort((a, b) => a.user.id - b.user.id);
                defenders.sort((a, b) => a.user.id - b.user.id);

                for (const attacker of attackers) {
                    addStagedGamePlayersRow(table, stagedGame, attacker, Role.ATTACKER.name);
                }
                for (const defender of defenders) {
                    addStagedGamePlayersRow(table, stagedGame, defender, Role.DEFENDER.name);
                }
            }

            return table.outerHTML;
        };

        /**
         * Adds a row to the players table for a staged game.
         * @param {HTMLTableElement} table The table to add the row to.
         * @param {StagedGame} stagedGame The staged game the row is for.
         * @param {UserInfo} userInfo The user assigned to the staged game.
         * @param {String} role The role of the user.
         */
        const addStagedGamePlayersRow = function (table, stagedGame, userInfo, role) {
            const tr = table.insertRow();
            tr.setAttribute('data-user-id', userInfo.user.id);
            if (role === Role.ATTACKER.name) {
                tr.style.backgroundColor = '#EDCECE';
            } else if (role === Role.DEFENDER.name) {
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

            /* Hide switch role button for melee games. */
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

            for (const otherStagedGame of stagedGames.values()) {
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

        /**
         * For a "form" to add a user to an existing game, selects the correct roles for the type of the selected game
         * and enable the "submit" button.
         * @param {HTMLSelectElement} roleSelect The role select.
         * @param {HTMLButtonElement} submitButton The "submit" button.
         * @param {String} gameIdStr The formatted ID of the selected staged or existing game.
         */
        const adjustFormForGame = function (roleSelect, submitButton, gameIdStr) {
            roleSelect.innerHTML = '';
            submitButton.disabled = true;

            let gameType;
            if (gameIdStr.startsWith('T')) {
                const gameId = Number(gameIdStr.substring(1));
                gameType = stagedGames.get(gameId).gameSettings.gameType;
            } else {
                const gameId = Number(gameIdStr);
                if (activeMultiplayerGameIds.has(gameId)) {
                    gameType = GameType.MULTIPLAYER.name;
                } else if (activeMeleeGameIds.has(gameId)) {
                    gameType = GameType.MELEE.name;
                }
            }

            if (gameType === GameType.MULTIPLAYER.name) {
                const attackerOption = document.createElement('option');
                attackerOption.textContent = Role.ATTACKER.display;
                attackerOption.value = Role.ATTACKER.name;
                roleSelect.appendChild(attackerOption);
                const defenderOption = document.createElement('option');
                defenderOption.textContent = Role.DEFENDER.display;
                defenderOption.value = Role.DEFENDER.name;
                roleSelect.appendChild(defenderOption);
                submitButton.disabled = false;
            } else if (gameType === GameType.MELEE.name) {
                const playerOption = document.createElement('option');
                playerOption.textContent = Role.PLAYER.display;
                playerOption.value = Role.PLAYER.name;
                roleSelect.appendChild(playerOption);
                submitButton.disabled = false;
            }
        };

        /**
         * Generates and posts a form from the given parameters. For every "name: value" pair given in the parameters a
         * form input with the name and value is created. If the value is a list, the elements are joined with ','.
         * @param {Object} params The form parameters.
         */
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

        const createSettingsTable = function (gameSettings) {
            const table = document.createElement('table');
            table.classList.add('table', 'table-condensed');

            let tr = table.insertRow();
            tr.insertCell().textContent = 'Game Type';
            tr.insertCell().textContent = GameType[gameSettings.gameType].display;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Class';
            tr.insertCell().innerHTML = renderStagedGameClass(gameSettings.cut);

            tr = table.insertRow();
            tr.insertCell().textContent = 'Level';
            tr.insertCell().textContent = GameLevel[gameSettings.level].display;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Include Predefined Mutants';
            tr.insertCell().textContent = gameSettings.withMutants;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Include Predefined Tests';
            tr.insertCell().textContent = gameSettings.withTests;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Max Assertions Per Test';
            tr.insertCell().textContent = gameSettings.maxAssertionsPerTest;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Mutant Validator Level';
            tr.insertCell().textContent = CodeValidatorLevel[gameSettings.mutantValidatorLevel].display;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Chat Enabled';
            tr.insertCell().textContent = gameSettings.chatEnabled;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Capture Player Intentions';
            tr.insertCell().textContent = gameSettings.captureIntentions;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Equivalence Threshold';
            tr.insertCell().textContent = gameSettings.equivalenceThreshold;

            tr = table.insertRow();
            tr.insertCell().textContent = 'Start Game';
            tr.insertCell().textContent = gameSettings.startGame;

            return table.outerHTML;
        };

        /* Restore the state of show/hide toggles for the tables. */
        $(document).ready(function () {
            const setCheckboxButton = function (checkbox, checked) {
                if (checked) {
                    checkbox.checked = true;
                    checkbox.parentNode.classList.add('active');
                } else {
                    checkbox.checked = false;
                    checkbox.parentNode.classList.remove('active');
                }
            }
            setCheckboxButton($('#toggle-hide-players').get(0), hideStagedGamePlayers);
            setCheckboxButton($('#toggle-show-assigned-users').get(0), showAssignedUsers);
        });

        /* Staged games table and related components. */
        $(document).ready(function () {
            const stagedGamesTable = $('#table-staged-games').DataTable({
                data: stagedGamesTableData,
                columns: [
                    {
                        data: null,
                        title: '',
                        defaultContent: '',
                        className: 'select-checkbox',
                        orderDataType: 'select-extension'
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
                select: {
                    style: 'multi',
                    className: 'selected'
                },
                drawCallback: function () {
                    /* Select nothing in all selects in the table. */
                    $(this).find('select').prop('selectedIndex', -1);

                    /* Setup popovers to display the game settings. */
                    $(this).find('.show-settings').popover({
                        container: document.body,
                        placement: 'right',
                        trigger: 'hover',
                        html: true,
                        title: 'Game Settings',
                        content: function () {
                            const tr = $(this).closest('tr').get(0);
                            const row = stagedGamesTable.row(tr);
                            const stagedGame = row.data();
                            return createSettingsTable(stagedGame.gameSettings);
                        }
                    });
                },
                order: [[1, 'asc']],
                scrollY: '800px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'There are currently no staged multiplayer games.'}
            });

            /* Search bar. */
            $('#search-staged-games').on('keyup', function () {
                setTimeout(() => stagedGamesTable.search(this.value).draw(), 0);
            });

            /* Show/hide players assigned to staged games. */
            $('#toggle-hide-players').on('change', function () {
                hideStagedGamePlayers = $(this).is(':checked');
                localStorage.setItem('hideStagedGamePlayers', JSON.stringify(hideStagedGamePlayers));
                stagedGamesTable.rows().invalidate().draw();
            });

            /* Toggle create-games and delete-games buttons based on whether staged games are selected. */
            stagedGamesTable.on('select', function (e, dt, type, indexes) {
                $('#create-games-button').get(0).disabled = false;
                $('#delete-games-button').get(0).disabled = false;
            });
            stagedGamesTable.on('deselect', function (e, dt, type, indexes) {
                if (stagedGamesTable.rows({selected: true})[0].length === 0) {
                    $('#create-games-button').get(0).disabled = true;
                    $('#delete-games-button').get(0).disabled = true;
                }
            });

            /* Select / deselect all visible staged games. */
            $('#select-visible-games').on('click', function () {
                stagedGamesTable.rows((index, data, tr) => !data._hidden, {search: 'applied'}).select();
            })
            $('#deselect-visible-games').on('click', function () {
                stagedGamesTable.rows((index, data, tr) => !data._hidden, {search: 'applied'}).deselect();
            });

            /* Set role options according to the game type when a game id is selected. */
            $(stagedGamesTable.table().node()).on('change', '.move-player-game', function () {
                const tr = $(this).closest('tr');
                const roleSelect = $(tr).find('.move-player-role').get(0);
                const button = $(tr).find('.move-player-button').get(0);
                adjustFormForGame(roleSelect, button, this.value);
            });

            /* Switch a player's role. */
            $(stagedGamesTable.table().node()).on('click', '.switch-role-button', function () {
                const innerTr = $(this).parents('tr').get(0);
                const outerTr = $(this).parents('tr').get(1);
                const userId = Number(innerTr.getAttribute('data-user-id'));
                const stagedGame = stagedGamesTable.row(outerTr).data();
                postForm({
                    formType: 'switchRole',
                    userId: userId,
                    gameId: 'T' + stagedGame.id,
                });
            });

            /* Remove a player from a staged game. */
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

            /* Move a player between staged games. */
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

            /* Delete selected games. */
            $('#delete-games-button').on('click', function () {
                const stagedGameIds = [];
                stagedGamesTable.rows({selected: true}).every(function () {
                    const stagedGame = this.data();
                    stagedGameIds.push('T' + stagedGame.id);
                });
                postForm({
                    formType: 'deleteStagedGames',
                    stagedGameIds
                });
            });

            /* Create selected games. */
            $('#create-games-button').on('click', function () {
                const stagedGameIds = [];
                stagedGamesTable.rows({selected: true}).every(function () {
                    const stagedGame = this.data();
                    stagedGameIds.push('T' + stagedGame.id);
                });
                postForm({
                    formType: 'createStagedGames',
                    stagedGameIds
                });
            });
        });

        /* Users table and related components. */
        $(document).ready(function () {
            const usersTable = $('#table-users').DataTable({
                data: usersTableData,
                columns: [
                    {
                        data: null,
                        title: '',
                        defaultContent: '',
                        className: 'select-checkbox',
                        orderDataType: 'select-extension'
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
                select: {
                    style: 'multi',
                    className: 'selected'
                },
                drawCallback: function () {
                    /* Select nothing in all selects in the table. */
                    $(this).find('select').prop('selectedIndex', -1);

                    /* Hide hidden users. */
                    $(this).DataTable().rows().every(function () {
                        const userInfo = this.data();
                        if (showAssignedUsers || unassignedUserIds.has(userInfo.user.id)) {
                            userInfo._hidden = false;
                            $(this.node()).show();
                        } else {
                            userInfo._hidden = true;
                            $(this.node()).hide();
                        }
                    });
                },
                order: [[5, 'asc']],
                scrollY: '600px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: 'There are currently no unassigned users.'}
            });

            /* Search bar. */
            $('#search-users').on('keyup', function () {
                setTimeout(() => usersTable.search(this.value).draw(), 0);
            });

            /* Show/hide users assigned to active existing games. */
            $('#toggle-show-assigned-users').on('change', function () {
                showAssignedUsers = $(this).is(':checked');
                localStorage.setItem('showAssignedUsers', JSON.stringify(showAssignedUsers));
                usersTable.draw();
            });

            /* Select / deselect all visible users. */
            $('#select-visible-users').on('click', function () {
                usersTable.rows((index, data, tr) => !data._hidden, {search: 'applied'}).select();
            })
            $('#deselect-visible-users').on('click', function () {
                usersTable.rows((index, data, tr) => !data._hidden, {search: 'applied'}).deselect();
            });

            /* Set role options according to the game type when a game id is selected. */
            $(usersTable.table().node()).on('change', '.add-player-game', function () {
                const tr = $(this).closest('tr').get(0);
                const roleSelect = $(tr).find('.add-player-role').get(0);
                const button = $(tr).find('.add-player-button').get(0);
                adjustFormForGame(roleSelect, button, this.value);
            });

            /* Add a users to a staged game or existing active game. */
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

            /* Toggle stage games button based on whether users are selected or user names are given. */
            usersTable.on('select', function (e, dt, type, indexes) {
                $('#stage-games-with-users-button').get(0).disabled = false;
            });
            usersTable.on('deselect', function (e, dt, type, indexes) {
                if (usersTable.rows({selected: true})[0].length === 0
                        && $('#userNames').val().length === 0) {
                    $('#stage-games-with-users-button').get(0).disabled = true;
                }
            });
            $('#userNames').on('keyup', function () {
                if (usersTable.rows({selected: true})[0].length === 0
                        && $('#userNames').val().length === 0) {
                    $('#stage-games-with-users-button').get(0).disabled = true;
                } else {
                    $('#stage-games-with-users-button').get(0).disabled = false;
                }
            });

            /* Toggle multiplayer/melee specific forms based on selected game type. */
            $('#gameType-group').on('change', function (event) {
                switch (event.target.value) {
                    case 'MULTIPLAYER':
                        $('.melee-specific').hide();
                        $('.multiplayer-specific').show();
                        break;
                    case 'MELEE':
                        $('.multiplayer-specific').hide();
                        $('.melee-specific').show();
                        break;
                }
            });

            /* Create new staged games with users. */
            $('#stage-games-with-users-button').on('click', function () {
                const userIds = [];
                usersTable.rows({selected: true}).every(function () {
                    const userInfo = this.data();
                    userIds.push(userInfo.user.id);
                });
                const params = {
                    formType: 'stageGamesWithUsers',
                    userIds
                };
                for (const {name, value} of $("#form-settings").serializeArray()) {
                    params[name] = value;
                }
                postForm(params);
            });

            /* Create new empty staged games. */
            $('#stage-games-empty-button').on('click', function () {
                if (usersTable.rows({selected: true})[0].length > 0) {
                    if (!confirm("You have users selected. Are you sure you want to stage empty games?")) {
                        return;
                    }
                } else if ($('#userNames').val().length > 0) {
                    if (!confirm("You entered user names/emails. Are you sure you want to stage empty games?")) {
                        return;
                    }
                }

                const params = {
                    formType: 'stageEmptyGames',
                };
                for (const {name, value} of $("#form-settings").serializeArray()) {
                    params[name] = value;
                }
                postForm(params);
            });
        });
    </script>
</div>

<%@ include file="/jsp/footer.jsp" %>

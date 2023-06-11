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
<%@ page import="org.codedefenders.database.AdminDAO" %>
<%@ page import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ page import="org.codedefenders.validation.code.CodeValidator" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="adminCreateGames" type="org.codedefenders.beans.admin.AdminCreateGamesBean"--%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Create Games"); %>

<jsp:include page="/jsp/header.jsp"/>

<div class="container">
    <% request.setAttribute("adminActivePage", "adminCreateGames"); %>
    <jsp:include page="/jsp/admin_navigation.jsp"/>

    <div class="card mb-4">
        <div class="card-header d-flex justify-content-between flex-wrap gap-1">
            Staged Games
            <div class="d-flex flex-wrap gap-2">
                <button id="select-visible-games" class="btn btn-xs btn-secondary">Select Visible</button>
                <button id="deselect-visible-games" class="btn btn-xs btn-secondary">Deselect Visible</button>
                <div>
                    <input type="checkbox" id="toggle-hide-players" class="btn-check" autocomplete="off">
                    <label for="toggle-hide-players" class="btn btn-xs btn-outline-secondary">
                        Hide Players
                        <i class="fa fa-check btn-check-active"></i>
                    </label>
                </div>
                <input type="search" id="search-staged-games" class="form-control input-xs" placeholder="Search">
            </div>
        </div>
        <div class="card-body">
            <table id="table-staged-games" class="table"></table>

            <form>
                <div class="row g-2 mt-3">
                    <div class="col-auto">
                        <button class="btn btn-md btn-primary" type="button" name="create-games-button"
                                id="create-games-button" disabled>
                            Create Games
                        </button>
                    </div>
                    <div class="col-auto">
                        <button class="btn btn-md btn-danger" type="button" name="delete-games-button"
                                id="delete-games-button" disabled>
                            Delete Games
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <div class="card mb-4">
        <div class="card-header d-flex justify-content-between flex-wrap gap-1">
            <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist" id="table-tabs">
                <li class="nav-item" role="presentation">
                    <button class="nav-link py-1 active" id="classrooms-tab"
                            data-bs-toggle="tab"
                            data-bs-target="#classrooms-pane"
                            aria-controls="classrooms-pane"
                            type="button" role="tab" aria-selected="true">
                        Classrooms
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link py-1" id="users-tab"
                            data-bs-toggle="tab"
                            data-bs-target="#users-pane"
                            aria-controls="users-pane"
                            type="button" role="tab" aria-selected="true">
                        Unassigned Users
                    </button>
                </li>
            </ul>

            <div id="classroom-table-controls" class="d-flex flex-wrap gap-2">
                <input type="search" id="search-classrooms" class="form-control input-xs" placeholder="Search">
            </div>
            <div id="user-table-controls" class="d-flex flex-wrap gap-2" hidden>
                <button id="select-visible-users" class="btn btn-xs btn-secondary">Select Visible</button>
                <button id="deselect-visible-users" class="btn btn-xs btn-secondary">Deselect Visible</button>
                <div>
                    <input type="checkbox" id="toggle-show-assigned-users" class="btn-check" autocomplete="off">
                    <label for="toggle-show-assigned-users"
                           class="btn btn-xs btn-outline-secondary d-flex align-items-center gap-1"
                           style="height: 100%"
                           title="Show users that are part of an existing active game.">
                        Show Assigned Users (in active games)
                        <i class="fa fa-check btn-check-active"></i>
                    </label>
                </div>
                <input type="search" id="search-users" class="form-control input-xs" placeholder="Search">
            </div>
        </div>

        <div class="card-body">
            <div class="tab-content">
                <div class="tab-pane active" id="classrooms-pane" role="tabpanel">
                    <table id="table-classrooms" class="table table-v-align-middle"></table>
                </div>
                <div class="tab-pane" id="users-pane" role="tabpanel">
                    <table id="table-users" class="table table-v-align-middle"></table>
                </div>
            </div>
        </div>
    </div>

    <form id="form-settings" autocomplete="off">
        <div class="row">
            <div class="col-md-6 col-12">

                <div class="card mb-4">
                    <div class="card-header">
                        Game Settings
                    </div>
                    <div class="card-body">

                        <div class="row g-3 mb-3">
                            <div class="col-12">
                                <label class="form-label" for="gameType-group">Game Type</label>
                                <div id="gameType-group">
                                    <div class="form-check">
                                        <input type="radio" class="form-check-input" id="gameType-radio-battleground" name="gameType"
                                               value="<%=StagedGameList.GameSettings.GameType.MULTIPLAYER%>"
                                               checked>
                                        <label class="form-check-label" for="gameType-radio-battleground">Battleground</label>
                                    </div>
                                    <div class="form-check">
                                        <input type="radio" class="form-check-input" id="gameType-radio-melee" name="gameType"
                                               value="<%=StagedGameList.GameSettings.GameType.MELEE%>">
                                        <label class="form-check-label" for="gameType-radio-melee">Melee</label>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12">
                                <label for="class-select" class="form-label">Class Under Test</label>
                                <div class="input-group mb-2">
                                    <select id="class-select" name="cut" class="form-control form-select">
                                        <% for (GameClass clazz : GameClassDAO.getAllPlayableClasses()) { %>
                                            <option value="<%=clazz.getId()%>"><%=clazz.getAlias()%></option>
                                        <% } %>
                                    </select>
                                    <% if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.CLASS_UPLOAD).getBoolValue()) { %>
                                        <span class="input-group-text position-relative cursor-pointer"
                                              title="Upload a class.">
                                            <a class="stretched-link text-decoration-none"
                                               href="${url.forPath(Paths.CLASS_UPLOAD)}?origin=<%=Paths.ADMIN_GAMES%>">
                                                <i class="fa fa-upload"></i>
                                            </a>
                                        </span>
                                    <% } %>
                                </div>

                                <div class="form-check form-switch"
                                     title="Include mutants uploaded together with the class.">
                                    <input class="form-check-input" type="checkbox" id="predefined-mutants-switch" name="withMutants">
                                    <label class="form-check-label" for="predefined-mutants-switch">Include predefined mutants (if available)</label>
                                </div>

                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="predefined-tests-switch" name="withTests">
                                    <label class="form-check-label" for="predefined-tests-switch">Include predefined tests (if available)</label>
                                </div>
                            </div>

                            <div class="col-12">
                                <label class="form-label" for="level-group">Game Level</label>
                                <div id="level-group">
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" id="level-radio-hard" name="level"
                                               value="<%=GameLevel.HARD%>"
                                               checked>
                                        <label class="form-check-label" for="level-radio-hard">Hard</label>
                                    </div>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" id="level-radio-easy" name="level"
                                               value="<%=GameLevel.EASY%>">
                                        <label class="form-check-label" for="level-radio-easy">Easy</label>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12">
                                <label class="form-label" id="mutant-validator-label" for="mutant-validator-group">
                                    <a class="text-decoration-none text-reset cursor-pointer text-nowrap"
                                       data-bs-toggle="modal" data-bs-target="#validatorExplanation">
                                        Mutant Validator Level
                                        <i class="fa fa-question-circle ms-1"></i>
                                    </a>
                                </label>
                                <div id="mutant-validator-group">
                                    <% for (CodeValidatorLevel level : CodeValidatorLevel.values()) { %>
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio"
                                                   id="mutant-validator-radio-<%=level.name().toLowerCase()%>" name="mutantValidatorLevel"
                                                   value="<%=level.name()%>"
                                                   <%=level == CodeValidatorLevel.MODERATE ? "checked" : ""%>>
                                            <label class="form-check-label" for="mutant-validator-radio-<%=level.name().toLowerCase()%>">
                                                <%=level.getDisplayName()%>
                                            </label>
                                        </div>
                                    <% } %>
                                </div>
                            </div>

                            <div class="col-12"
                                 title="Maximum number of assertions per test. Increase this for difficult to test classes.">
                                <label class="form-label" id="max-assertions-label" for="max-assertions-input">
                                    Max. Assertions Per Test
                                </label>
                                <input type="number" class="form-control" id="max-assertions-input" name="maxAssertionsPerTest"
                                       value="<%=CodeValidator.DEFAULT_NB_ASSERTIONS%>" min="1" required>
                            </div>

                            <div class="col-12">
                                <label class="form-label" id="equiv-threshold-label" for="equiv-threshold-input">
                                    <a class="text-decoration-none text-reset cursor-pointer text-nowrap"
                                       data-bs-toggle="modal" data-bs-target="#automaticEquivalenceTriggerExplanation">
                                        Auto Equiv. Threshold
                                        <i class="fa fa-question-circle ms-1"></i>
                                    </a>
                                </label>
                                <input class="form-control" type="number" id="equiv-threshold-input" name="automaticEquivalenceTrigger"
                                       value="0" min="0" required>
                            </div>

                            <div class="col-12"
                                 title="Select the role you will have in the game.">
                                <label for="role-select" class="form-label">Your Role</label>
                                <select id="role-select" name="creatorRole" class="form-control form-select">
                                    <option value="OBSERVER" selected>Observer</option>
                                    <option value="ATTACKER">Attacker</option>
                                    <option value="DEFENDER">Defender</option>
                                </select>
                            </div>
                        </div>

                        <div class="row g-2 mt-2">
                            <div class="col-12"
                                 title="Forces players to specify the intentions of their mutants/tests before they can submit them.">
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="capture-intentions-switch" name="capturePlayersIntention">
                                    <label class="form-check-label" for="capture-intentions-switch">Enable Capture Players' Intentions</label>
                                </div>
                            </div>

                            <div class="col-12"
                                 title="Allows players to chat within their team and with the enemy team.">
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="chat-switch" name="chatEnabled" checked>
                                    <label class="form-check-label" for="chat-switch">Game Chat</label>
                                </div>
                            </div>

                            <div class="col-12"
                                 title="Automatically start games once they are created.">
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="start-games-switch" name="startGames">
                                    <label class="form-check-label" for="start-games-switch">Start Games</label>
                                </div>
                            </div>

                            <div class="col-12" title="The duration for how long the games will be open.">
                                <input type="hidden" name="gameDurationMinutes" id="gameDurationMinutes">
                                <%
                                    request.setAttribute("defaultDuration", AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_DEFAULT).getIntValue());
                                    request.setAttribute("maximumDuration", AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_MAX).getIntValue());
                                %>

                                <label class="form-label">Set the game's duration:</label>
                                <div class="input-group input-group-sm">
                                    <input type="number" name="days" class="form-control" id="days-input" min="0">
                                    <label for="days-input" class="input-group-text">days</label>
                                    <input type="number" name="hours" class="form-control" id="hours-input" min="0">
                                    <label for="hours-input" class="input-group-text">hours</label>
                                    <input type="number" name="minutes" class="form-control" id="minutes-input" min="0">
                                    <label for="minutes-input" class="input-group-text">minutes</label>
                                </div>
                                <small id="maxDurationInfo" class="mt-1">
                                    Maximum duration: <span id="displayMaxDuration">&hellip;</span>.
                                    If the value is greater, it will be limited to this maximum.
                                </small>

                                <script type="module">
                                    import {GameTimeValidator, formatTime} from '${url.forPath("/js/codedefenders_game.mjs")}';

                                    const gameTimeValidator = new GameTimeValidator(
                                            Number(${maximumDuration}),
                                            Number(${defaultDuration}),
                                            document.getElementById('minutes-input'),
                                            document.getElementById('hours-input'),
                                            document.getElementById('days-input'),
                                            document.getElementById('gameDurationMinutes')
                                    );


                                    document.getElementById('displayMaxDuration').innerText =
                                            formatTime(${maximumDuration});

                                    // show the max duration limit in red if an invalid duration was given
                                    const maxDurationInfo = document.getElementById('maxDurationInfo');
                                    const cn = 'text-danger';
                                    gameTimeValidator.onInvalidDuration = () => maxDurationInfo.classList.add(cn);
                                    gameTimeValidator.onValidDuration = () => maxDurationInfo.classList.remove(cn);
                                </script>
                            </div>
                        </div>

                    </div>
                </div>

            </div> <%-- column --%>
            <div class="col-md-6 col-12">

                <div class="card mb-4">
                    <div class="card-header">
                        <a class="text-decoration-none text-reset cursor-pointer"
                           data-bs-toggle="modal" data-bs-target="#stageGamesWithUsersExplanation">
                            Create Staged Games With Users
                            <i class="fa fa-question-circle ms-1"></i>
                        </a>
                    </div>
                    <div class="card-body">

                        <div class="row g-3">
                            <div class="col-12">
                                <label for="userNames" class="form-label">
                                    <a class="text-decoration-none text-reset cursor-pointer"
                                       data-bs-toggle="modal" data-bs-target="#userNamesExplanation">
                                        User Names
                                        <i class="fa fa-question-circle ms-1"></i>
                                    </a>
                                </label>
                                <textarea class="form-control" rows="5" id="userNames" name="userNames"></textarea>
                            </div>

                            <div class="col-12 multiplayer-specific"
                                 title="Method of assigning roles to players.">
                                <label class="form-label" for="roleAssignmentMethod-group">
                                    <a class="text-decoration-none text-reset cursor-pointer"
                                       data-bs-toggle="modal" data-bs-target="#roleAssignmentExplanation">
                                        Role Assignment
                                        <i class="fa fa-question-circle ms-1"></i>
                                    </a>
                                </label>
                                <div id="roleAssignmentMethod-group">
                                    <div class="form-check">
                                        <input type="radio" name="roleAssignmentMethod"
                                               class="form-check-input" id="roleAssignmentMethod-radio-random"
                                               value="<%=AdminCreateGamesBean.RoleAssignmentMethod.RANDOM%>"
                                               checked>
                                        <label class="form-check-label" for="roleAssignmentMethod-radio-random">Random</label>
                                    </div>
                                    <div class="form-check">
                                        <input type="radio" name="roleAssignmentMethod"
                                               class="form-check-input" id="roleAssignmentMethod-radio-opposite"
                                               value="<%=AdminCreateGamesBean.RoleAssignmentMethod.OPPOSITE%>">
                                        <label class="form-check-label" for="roleAssignmentMethod-radio-opposite">Opposite Role</label>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12"
                                 title="Method of assigning players to teams. Click the question mark for more information.">
                                <label class="form-label" for="teamAssignmentMethod-group">
                                    <a class="text-decoration-none text-reset cursor-pointer"
                                       data-bs-toggle="modal" data-bs-target="#teamAssignmentExplanation">
                                        Team Assignment
                                        <i class="fa fa-question-circle ms-1"></i>
                                    </a>
                                </label>
                                <div id="teamAssignmentMethod-group">
                                    <div class="form-check">
                                        <input type="radio" name="teamAssignmentMethod"
                                               class="form-check-input" id="teamAssignmentMethod-radio-random"
                                               value="<%=AdminCreateGamesBean.TeamAssignmentMethod.RANDOM%>"
                                               checked>
                                        <label class="form-check-label" for="teamAssignmentMethod-radio-random">Random</label>
                                    </div>
                                    <div class="form-check">
                                        <input type="radio" name="teamAssignmentMethod"
                                               class="form-check-input" id="teamAssignmentMethod-radio-score-descending"
                                               value="<%=AdminCreateGamesBean.TeamAssignmentMethod.SCORE_DESCENDING%>">
                                        <label class="form-check-label" for="teamAssignmentMethod-radio-score-descending">Score Descending</label>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12 multiplayer-specific"
                                 title="Number of attackers per game.">
                                <label for="attackersPerGame" class="form-label">Attackers per Game</label>
                                <input type="number" value="3" id="attackersPerGame" name="attackersPerGame" min="1" class="form-control">
                            </div>

                            <div class="col-12 multiplayer-specific"
                                 title="Number of defenders per game.">
                                <label for="defendersPerGame" class="form-label">Defenders per Game</label>
                                <input type="number" value="3" id="defendersPerGame" name="defendersPerGame" min="1" class="form-control">
                            </div>

                            <div class="col-12 melee-specific" hidden
                                 title="Players per game.">
                                <label for="playersPerGame" class="form-label">Players per Game</label>
                                <input type="number" value="6" id="playersPerGame" name="playersPerGame" min="1" class="form-control">
                            </div>

                            <div class="col-12">
                                <button class="btn btn-md btn-primary" type="button" name="stage-games-button"
                                        id="stage-games-button" disabled>
                                    Stage Games
                                </button>
                            </div>
                        </div>

                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <a class="text-decoration-none text-reset cursor-pointer"
                           data-bs-toggle="modal" data-bs-target="#stageEmptyGamesExplanation">
                            Create Empty Staged Games
                            <i class="fa fa-question-circle ms-1"></i>
                        </a>
                    </div>
                    <div class="card-body">

                        <div class="row g-3">
                            <div class="col-12"
                                 title="Number of staged games to create.">
                                <label for="numGames" class="form-label">Number of Games</label>
                                <input type="number" value="1" id="numGames" name="numGames" min="1" max="100" class="form-control">
                            </div>

                            <div class="col-12">
                                <button class="btn btn-md btn-primary" type="button" name="stage-games-empty-button"
                                        id="stage-games-empty-button">
                                    Stage Games
                                </button>
                            </div>
                        </div>

                    </div>
                </div>

            </div> <%-- column --%>
        </div> <%-- row --%>
    </form>

    <t:modal id="validatorExplanation" title="Validator Explanations">
        <jsp:attribute name="content">
            <t:validator_explanation_mutant/>
            <div class="mt-3"></div> <%-- spacing --%>
            <t:validator_explanation_test/>
        </jsp:attribute>
    </t:modal>

    <t:modal id="automaticEquivalenceTriggerExplanation" title="Equivalence Duel Threshold Explanation">
        <jsp:attribute name="content">
            <%@ include file="/jsp/automatic_duels_explanation.jsp"%>
        </jsp:attribute>
    </t:modal>

    <t:modal id="roleAssignmentExplanation" title="Role Assignment Explanation">
        <jsp:attribute name="content">
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
        </jsp:attribute>
    </t:modal>

    <t:modal id="teamAssignmentExplanation" title="Team Assignment Explanation">
        <jsp:attribute name="content">
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
        </jsp:attribute>
    </t:modal>

    <t:modal id="userNamesExplanation" title="User Names Explanation">
        <jsp:attribute name="content">
            Newline-separated list of usernames or emails. The users with these names/emails, as well as the
            users selected in the table, will be assigned to created staged games.
        </jsp:attribute>
    </t:modal>

    <t:modal id="stageGamesWithUsersExplanation" title="Stage Games With Users Explanation">
        <jsp:attribute name="content">
            <p>
                Selected users from the table, as well as users entered in the text area, will be assigned to new
                staged games. The number of games is decided by the number of users and the selected method of
                distributing the users to teams.
            </p>
            <p class="mb-0">
                The settings for the staged games are specified in the left card.
            </p>
        </jsp:attribute>
    </t:modal>

    <t:modal id="stageEmptyGamesExplanation" title="Stage Empty Games Explanation">
        <jsp:attribute name="content">
            <p>Create a number staged games without users assigned to them.</p>
            <p class="mb-0">The settings for the staged games are specified in the left card.</p>
        </jsp:attribute>
    </t:modal>

    <script type="module">
        import {Popover} from '${url.forPath("/js/bootstrap.mjs")}';
        import DataTable from '${url.forPath("/js/datatables.mjs")}';
        import $ from '${url.forPath("/js/jquery.mjs")}';
        import {formatTime} from '${url.forPath("/js/codedefenders_game.mjs")}';

        const loggedInUserId = ${login.userId};

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
         * Classrooms the user is participating in as OWNER or MODERATOR.
         */
        const classrooms = JSON.parse('${adminCreateGames.classroomsJSON}');

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
        DataTable.ext.order['select-extension'] = function (settings, col) {
            return this.api().column(col, {order:'index'}).nodes().map(function (td, i) {
                return $(td).closest('tr').hasClass('selected') ? '0' : '1';
            });
        };

        /* Search function added to DataTables to filter the users table. */
        const searchFunction = function(settings, renderedData, index, data, counter) {
            /* Let this search function only affect the users table. */
            if (settings.nTable.id !== 'table-users') {
                return true;
            }

            /* Filter out logged-in user. */
            if (data.user.id === loggedInUserId) {
                return false;
            }

            if (showAssignedUsers) {
                return true;
            }

            /* Filter out assigned users. */
            return unassignedUserIds.has(data.user.id);
        };
        DataTable.ext.search.push(searchFunction);

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
            if (stagedGamesTableData.length === 0 && activeGameIds.length === 0) {
                gameIdSelect.disabled = true;
            }

            const roleCell = tr.insertCell();
            roleCell.style.width = '8em';

            /* The role select is generated empty, as options are set based on the type of the selected game. */
            const roleSelect = document.createElement('select');
            roleSelect.classList.add('add-player-role');
            roleSelect.disabled = true;
            roleCell.appendChild(roleSelect);

            const addToGameCell = tr.insertCell();
            addToGameCell.style.width = '0px';
            addToGameCell.innerHTML =
                `<button disabled class="add-player-button btn btn-sm btn-primary" title="Add player to selected game">
                     <i class="fa fa-plus"></i>
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
            const creator = userInfos.get(loggedInUserId);
            const creatorRole = stagedGame.gameSettings.creatorRole;

            const table = document.createElement('table');
            table.classList.add('staged-game-players', 'table', 'table-sm', 'table-borderless', 'table-v-align-middle');
            table.style.width = '100%';

            if (stagedGame.gameSettings.gameType === GameType.MELEE.name) {
                const players = [...attackers, ...defenders];
                players.sort((a, b) => a.user.id - b.user.id);

                if (creatorRole === Role.PLAYER.name) {
                    addCreatorRow(table, stagedGame, creator, creatorRole);
                }
                for (const player of players) {
                    addStagedGamePlayersRow(table, stagedGame, player, Role.PLAYER.name);
                }
            } else {
                attackers.sort((a, b) => a.user.id - b.user.id);
                defenders.sort((a, b) => a.user.id - b.user.id);

                if (creatorRole === Role.ATTACKER.name) {
                    addCreatorRow(table, stagedGame, creator, Role.ATTACKER.name);
                }
                for (const attacker of attackers) {
                    addStagedGamePlayersRow(table, stagedGame, attacker, Role.ATTACKER.name);
                }
                if (creatorRole === Role.DEFENDER.name) {
                    addCreatorRow(table, stagedGame, creator, Role.DEFENDER.name);
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
        const addCreatorRow = function (table, stagedGame, userInfo, role) {
            const tr = table.insertRow();
            tr.setAttribute('data-user-id', userInfo.user.id);
            if (role === Role.ATTACKER.name) {
                tr.classList.add('bg-attacker-light');
            } else if (role === Role.DEFENDER.name) {
                tr.classList.add('bg-defender-light');
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
                    `<button class="switch-creator-role-button btn btn-sm btn-primary" title="Switch your role">
                         <i class="fa fa-exchange"></i>
                     </button>`;

            /* Hide switch role button for melee games. */
            if (role === Role.PLAYER.name) {
                switchRolesCell.firstChild.style.visibility = 'hidden';
            }

            const removeCell = tr.insertCell();
            removeCell.style.width = '0px';
            removeCell.innerHTML =
                    `<button class="remove-creator-button btn btn-sm btn-danger" title="Change to Observer">
                         <i class="fa fa-trash"></i>
                     </button>`;

            const moveGameIdCell = tr.insertCell();
            moveGameIdCell.style.width = '5em';

            const paddingCell = tr.insertCell();
            paddingCell.colSpan = 3;
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
                tr.classList.add('bg-attacker-light');
            } else if (role === Role.DEFENDER.name) {
                tr.classList.add('bg-defender-light');
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
                         <i class="fa fa-exchange"></i>
                     </button>`;

            /* Hide switch role button for melee games. */
            if (role === Role.PLAYER.name) {
                switchRolesCell.firstChild.style.visibility = 'hidden';
            }

            const removeCell = tr.insertCell();
            removeCell.style.width = '0px';
            removeCell.innerHTML =
                    `<button class="remove-player-button btn btn-sm btn-danger" title="Remove player from game">
                         <i class="fa fa-trash"></i>
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
            if (stagedGames.size === 1) {
                gameIdSelect.disabled = true;
            }

            const moveRoleCell = tr.insertCell();
            moveRoleCell.style.width = '8em';

            const roleSelect = document.createElement('select');
            roleSelect.classList.add('move-player-role');
            roleSelect.disabled = true;
            moveRoleCell.appendChild(roleSelect);

            const moveButtonCell = tr.insertCell();
            moveButtonCell.style.width = '0px';
            moveButtonCell.innerHTML =
                    `<button disabled class="move-player-button btn btn-sm btn-primary" title="Move player to selected game">
                         <i class="fa fa-arrow-right"></i>
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
            roleSelect.disabled = true;

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
                roleSelect.disabled = false;
                submitButton.disabled = false;
            } else if (gameType === GameType.MELEE.name) {
                const playerOption = document.createElement('option');
                playerOption.textContent = Role.PLAYER.display;
                playerOption.value = Role.PLAYER.name;
                roleSelect.appendChild(playerOption);
                roleSelect.disabled = false;
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
            table.classList.add('table', 'table-sm', 'table-no-last-border', 'm-0');

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
            tr.insertCell().textContent = 'Creator Role';
            tr.insertCell().textContent = Role[gameSettings.creatorRole].display;

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

            tr = table.insertRow();
            tr.insertCell().textContent = 'Game Duration';
            tr.insertCell().textContent = formatTime(gameSettings.gameDurationMinutes);

            return table;
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
            const stagedGamesTable = new DataTable('#table-staged-games', {
                data: stagedGamesTableData,
                columns: [
                    {
                        data: null,
                        title: 'Select',
                        defaultContent: '',
                        className: 'select-checkbox',
                        orderDataType: 'select-extension',
                        width: '3em'
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
                        defaultContent: '<span class="btn btn-xs btn-secondary show-settings">Show</span>',
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
                    const table = this.api().table().container();

                    /* Select nothing in all selects in the table. */
                    for (const select of table.querySelectorAll('select')) {
                        select.selectedIndex = -1;
                    }

                    /* Setup popovers to display the game settings. */
                    for (const button of table.querySelectorAll('.show-settings')) {
                        new Popover(button, {
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
                    }
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
                stagedGamesTable.rows({search: 'applied'}).select();
            })
            $('#deselect-visible-games').on('click', function () {
                stagedGamesTable.rows({search: 'applied'}).deselect();
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

            /* Switch creator role. */
            $(stagedGamesTable.table().node()).on('click', '.switch-creator-role-button', function () {
                const outerTr = $(this).parents('tr').get(1);
                const stagedGame = stagedGamesTable.row(outerTr).data();
                postForm({
                    formType: 'switchCreatorRole',
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

            /* Remove creator from staged game. */
            $(stagedGamesTable.table().node()).on('click', '.remove-creator-button', function () {
                const outerTr = $(this).parents('tr').get(1);
                const stagedGame = stagedGamesTable.row(outerTr).data();
                postForm({
                    formType: 'removeCreatorFromStagedGame',
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

        let usersTable;
        /* Users table and related components. */
        $(document).ready(function () {
            usersTable = new DataTable('#table-users', {
                data: usersTableData,
                columns: [
                    {
                        data: null,
                        title: 'Select',
                        defaultContent: '',
                        className: 'select-checkbox',
                        orderDataType: 'select-extension',
                        width: '3em'
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
                    const table = this.api().table().container();

                    /* Select nothing in all selects in the table. */
                    for (const select of table.querySelectorAll('select')) {
                        select.selectedIndex = -1;
                    }
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
                usersTable.rows({search: 'applied'}).select();
            })
            $('#deselect-visible-users').on('click', function () {
                usersTable.rows({search: 'applied'}).deselect();
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

            /* Toggle multiplayer/melee specific forms based on selected game type. */
            $('#gameType-group').on('change', function (event) {
                const roleSelect = document.getElementById('role-select');
                const observerOption = document.createElement('option');
                observerOption.value = Role.OBSERVER.name;
                observerOption.innerText = Role.OBSERVER.display;

                switch (event.target.value) {
                    case 'MULTIPLAYER':
                        for (const element of document.querySelectorAll('.melee-specific')) {
                            element.setAttribute('hidden', '');
                        }
                        for (const element of document.querySelectorAll('.multiplayer-specific')) {
                            element.removeAttribute('hidden');
                        }
                        const attackerOption = document.createElement('option');
                        attackerOption.value = Role.ATTACKER.name;
                        attackerOption.innerText = Role.ATTACKER.display;
                        const defenderOption = document.createElement('option');
                        defenderOption.value = Role.DEFENDER.name;
                        defenderOption.innerText = Role.DEFENDER.display;
                        roleSelect.replaceChildren(observerOption, attackerOption, defenderOption);
                        roleSelect.selectedIndex = 0;
                        break;
                    case 'MELEE':
                        for (const element of document.querySelectorAll('.multiplayer-specific')) {
                            element.setAttribute('hidden', '');
                        }
                        for (const element of document.querySelectorAll('.melee-specific')) {
                            element.removeAttribute('hidden');
                        }
                        const playerOption = document.createElement('option');
                        playerOption.value = Role.PLAYER.name;
                        playerOption.innerText = Role.PLAYER.display;
                        roleSelect.replaceChildren(observerOption, playerOption);
                        roleSelect.selectedIndex = 0;
                        break;
                }
            });
        });

        let classroomsTable;
        /* Classrooms table and related components. */
        $(document).ready(function () {
            classroomsTable = new DataTable('#table-classrooms', {
                data: classrooms,
                columns: [
                    {
                        data: null,
                        title: 'Select',
                        defaultContent: '',
                        className: 'select-checkbox',
                        orderDataType: 'select-extension',
                        width: '3em'
                    },
                    {
                        data: 'id',
                        type: 'number',
                        title: 'ID'
                    },
                    {
                        data: 'name',
                        type: 'string',
                        title: 'Name'
                    },
                    {
                        data: 'roomCode',
                        type: 'string',
                        title: 'Room Code'
                    }
                ],
                select: {
                    style: 'single',
                    className: 'selected'
                },
                order: [[1, 'asc']],
                scrollY: '600px',
                scrollCollapse: true,
                paging: false,
                dom: 't',
                language: {emptyTable: "You don't manage any classrooms."}
            });

            /* Search bar. */
            $('#search-classrooms').on('keyup', function () {
                setTimeout(() => classroomsTable.search(this.value).draw(), 0);
            });
        });

        /* Classrooms table and users table interaction. */
        $(document).ready(function () {
            /* Toggle stage games button based on whether users are selected or usernames are given. */
            usersTable.on('select', function (e, dt, type, indexes) {
                $('#stage-games-button').get(0).disabled = false;
            });
            classroomsTable.on('select', function (e, dt, type, indexes) {
                $('#stage-games-button').get(0).disabled = false;
            });
            usersTable.on('deselect', function (e, dt, type, indexes) {
                if (usersTable.rows({selected: true})[0].length === 0
                        && classroomsTable.rows({selected: true})[0].length === 0
                        && $('#userNames').val().length === 0) {
                    $('#stage-games-button').get(0).disabled = true;
                }
            });
            classroomsTable.on('deselect', function (e, dt, type, indexes) {
                if (usersTable.rows({selected: true})[0].length === 0
                        && classroomsTable.rows({selected: true})[0].length === 0
                        && $('#userNames').val().length === 0) {
                    $('#stage-games-button').get(0).disabled = true;
                }
            });
            $('#userNames').on('keyup', function () {
                if (usersTable.rows({selected: true})[0].length === 0
                        && classroomsTable.rows({selected: true})[0].length === 0
                        && $('#userNames').val().length === 0) {
                    $('#stage-games-button').get(0).disabled = true;
                } else {
                    $('#stage-games-button').get(0).disabled = false;
                }
            });

            /* Toggle table controls when tab is selected. */
            document.getElementById('table-tabs').addEventListener('shown.bs.tab', function (event) {
                const userControls = document.getElementById('user-table-controls');
                const classroomControls = document.getElementById('classroom-table-controls');

                if (event.target.id === 'classrooms-tab') {
                    userControls.classList.remove('d-flex');
                    userControls.classList.add('d-none');
                    classroomControls.classList.remove('d-none');
                    classroomControls.classList.add('d-flex');
                    classroomsTable.draw();
                } else if (event.target.id === 'users-tab') {
                    classroomControls.classList.remove('d-flex');
                    classroomControls.classList.add('d-none');
                    userControls.classList.remove('d-none');
                    userControls.classList.add('d-flex');
                    usersTable.draw();
                }
            });
            /* Switch the controls to the initial state. */
            const userControls = document.getElementById('user-table-controls');
            userControls.classList.remove('d-flex');
            userControls.classList.add('d-none');

            /* Create new staged games with users. */
            $('#stage-games-button').on('click', function () {
                const userIds = [];
                usersTable.rows({selected: true}).every(function () {
                    const userInfo = this.data();
                    userIds.push(userInfo.user.id);
                });

                const params = {
                    formType: 'stageGamesWithUsers',
                    userIds
                };

                if (classroomsTable.rows({selected: true})[0].length > 0) {
                    params.classroomId = classroomsTable.rows({selected: true}).data()[0].id;
                }

                for (const {name, value} of $("#form-settings").serializeArray()) {
                    params[name] = value;
                }
                postForm(params);
            });

            /* Create new empty staged games. */
            $('#stage-games-empty-button').on('click', function () {
                if (classroomsTable.rows({selected: true})[0].length > 0) {
                    if (!confirm("You have a classroom selected. Are you sure you want to stage empty games?")) {
                        return;
                    }
                } else if (usersTable.rows({selected: true})[0].length > 0) {
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

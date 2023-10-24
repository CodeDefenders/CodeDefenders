<%@ tag import="org.codedefenders.game.GameType" %>
<%@ tag import="org.codedefenders.game.GameClass" %>
<%@ tag import="org.codedefenders.database.GameClassDAO" %>
<%@ tag import="org.codedefenders.database.AdminDAO" %>
<%@ tag import="org.codedefenders.servlets.admin.AdminSystemSettings" %>
<%@ tag import="org.codedefenders.util.Paths" %>
<%@ tag import="org.codedefenders.game.GameLevel" %>
<%@ tag import="org.codedefenders.validation.code.CodeValidator" %>
<%@ tag import="org.codedefenders.validation.code.CodeValidatorLevel" %>
<%@ tag import="org.codedefenders.model.creategames.gameassignment.GameAssignmentStrategy" %>
<%@ tag import="org.codedefenders.model.creategames.roleassignment.RoleAssignmentStrategy" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="createGamesBean" value="${requestScope.createGamesBean}"/>

<div class="card mb-4">
    <div class="card-header d-flex justify-content-between flex-wrap gap-1">
        Staged Games
        <div class="d-flex flex-wrap gap-2">
            <button id="select-visible-games" class="btn btn-xs btn-secondary"
                    title="Select all games (that your search applies to)">
                Select All
            </button>
            <button id="deselect-visible-games" class="btn btn-xs btn-secondary"
                    title="Deselect all games (that your search applies to)">
                Deselect All
            </button>
            <div class="mx-2">
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
        Unassigned Users
        <div class="d-flex flex-wrap gap-2">
            <button id="select-visible-users" class="btn btn-xs btn-secondary"
                    title="Select all users (that your search applies to)">
                Select All
            </button>
            <button id="deselect-visible-users" class="btn btn-xs btn-secondary"
                    title="Deselect all users (that your search applies to)">
                Deselect All
            </button>
            <div class="mx-2">
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
        <table id="table-users" class="table table-v-align-middle"></table>
    </div>
</div>

<form id="form-settings" autocomplete="off">
    <c:if test="${createGamesBean.kind == 'CLASSROOM'}">
        <input type="hidden" name="classroomId" value="${createGamesBean.classroomId}"/>
    </c:if>

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
                                    <input type="radio" class="form-check-input" id="gameType-radio-battleground"
                                           name="gameType"
                                           value="<%=GameType.MULTIPLAYER%>"
                                           checked>
                                    <label class="form-check-label"
                                           for="gameType-radio-battleground">Battleground</label>
                                </div>
                                <div class="form-check">
                                    <input type="radio" class="form-check-input" id="gameType-radio-melee"
                                           name="gameType"
                                           value="<%=GameType.MELEE%>">
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
                            <label class="form-label" for="level-group">
                                <a class="text-decoration-none text-reset cursor-pointer text-nowrap"
                                   data-bs-toggle="modal" data-bs-target="#levelExplanation">
                                    Game Level
                                    <i class="fa fa-question-circle ms-1"></i>
                                </a>
                            </label>
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
                                import {GameTimeValidator, GameTime} from '${url.forPath("/js/codedefenders_game.mjs")}';

                                const gameTimeValidator = new GameTimeValidator(
                                        Number(${maximumDuration}),
                                        Number(${defaultDuration}),
                                        document.getElementById('minutes-input'),
                                        document.getElementById('hours-input'),
                                        document.getElementById('days-input'),
                                        document.getElementById('gameDurationMinutes')
                                );


                                document.getElementById('displayMaxDuration').innerText =
                                        GameTime.formatTime(${maximumDuration});

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
                            <label class="form-label" for="roleAssignment-group">
                                <a class="text-decoration-none text-reset cursor-pointer"
                                   data-bs-toggle="modal" data-bs-target="#roleAssignmentExplanation">
                                    Role Assignment
                                    <i class="fa fa-question-circle ms-1"></i>
                                </a>
                            </label>
                            <div id="roleAssignment-group">
                                <div class="form-check">
                                    <input type="radio" name="roleAssignmentMethod"
                                           class="form-check-input" id="roleAssignment-radio-random"
                                           value="<%=RoleAssignmentStrategy.Type.RANDOM%>"
                                           checked>
                                    <label class="form-check-label" for="roleAssignment-radio-random">Random</label>
                                </div>
                                <div class="form-check">
                                    <input type="radio" name="roleAssignmentMethod"
                                           class="form-check-input" id="roleAssignment-radio-opposite"
                                           value="<%=RoleAssignmentStrategy.Type.OPPOSITE%>">
                                    <label class="form-check-label" for="roleAssignment-radio-opposite">Opposite Role</label>
                                </div>
                            </div>
                        </div>

                        <div class="col-12"
                             title="Method of assigning players to teams. Click the question mark for more information.">
                            <label class="form-label" for="gameAssignmentMethod-group">
                                <a class="text-decoration-none text-reset cursor-pointer"
                                   data-bs-toggle="modal" data-bs-target="#gameAssignmentExplanation">
                                    Game Assignment
                                    <i class="fa fa-question-circle ms-1"></i>
                                </a>
                            </label>
                            <div id="gameAssignmentMethod-group">
                                <div class="form-check">
                                    <input type="radio" name="gameAssignmentMethod"
                                           class="form-check-input" id="gameAssignmentMethod-radio-random"
                                           value="<%=GameAssignmentStrategy.Type.RANDOM%>"
                                           checked>
                                    <label class="form-check-label" for="gameAssignmentMethod-radio-random">Random</label>
                                </div>
                                <div class="form-check">
                                    <input type="radio" name="gameAssignmentMethod"
                                           class="form-check-input" id="gameAssignmentMethod-radio-score-descending"
                                           value="<%=GameAssignmentStrategy.Type.SCORE_DESCENDING%>">
                                    <label class="form-check-label" for="gameAssignmentMethod-radio-score-descending">Score Descending</label>
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

<t:modal id="levelExplanation" title="Level Explanation">
        <jsp:attribute name="content">
            <h3>Battleground Games</h3>
            <t:level_explanation_multiplayer/>
            <h3 class="mt-3">Melee Games</h3>
            <t:level_explanation_melee/>
        </jsp:attribute>
</t:modal>

<t:modal id="validatorExplanation" title="Validator Explanation">
        <jsp:attribute name="content">
            <t:validator_explanation_mutant/>
            <div class="mt-3"></div> <%-- spacing --%>
            <t:validator_explanation_test/>
        </jsp:attribute>
</t:modal>

<t:modal id="automaticEquivalenceTriggerExplanation" title="Auto Equivalence Duel Threshold Explanation">
        <jsp:attribute name="content">
            <t:automatic_duels_explanation/>
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

<t:modal id="gameAssignmentExplanation" title="Game Assignment Explanation">
        <jsp:attribute name="content">
            <p>Specifies how players are assigned to games:</p>
            <ul>
                <li>
                    <b>Random:</b>
                    Players are assigned to games randomly.
                </li>
                <li>
                    <b>Scores Descending:</b>
                    Players are assigned to games based on their total score in past games.
                    The players with the highest scores are assigned to the first games,
                    the players with the lowest scores are assigned to the last games.
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
    import {GameTime} from '${url.forPath("/js/codedefenders_game.mjs")}';
    import {parseHTML} from '${url.forPath("/js/codedefenders_main.mjs")}';

    const loggedInUserId = ${login.userId};

    /**
     *  Maps IDs to staged games.
     */
    const stagedGames = new Map(JSON.parse('${createGamesBean.stagedGamesAsJSON}'));

    /**
     * Maps IDs to user infos.
     */
    const userInfos = new Map(JSON.parse('${createGamesBean.userInfosAsJSON}'));

    /**
     * IDs of active (started and not finished) multiplayer games.
     */
    const activeMultiplayerGameIds = new Set(JSON.parse('${createGamesBean.availableMultiplayerGameIdsJSON}'));

    /**
     *  IDs of active (started and not finished) melee games.
     */
    const activeMeleeGameIds = new Set(JSON.parse('${createGamesBean.availableMeleeGameIdsJSON}'));

    /**
     * IDs of users not assigned to any active games.
     */
    const assignedUserIds = new Set(JSON.parse('${createGamesBean.assignedUserIdsJSON}'));

    const classes = new Map(JSON.parse('${createGamesBean.usedClassesJSON}'));

    const kind = '${createGamesBean.kind}';


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
            .filter(userInfo => !assignedUserIdsStaged.has(userInfo.id))
            .sort((a, b) => a.id - b.id);


    /**
     * Hide assigned players in the staged games table.
     */
    let hideStagedGamePlayers = JSON.parse(sessionStorage.getItem('hideStagedGamePlayers')) || false;

    /**
     * Show users assigned to active games in the users table.
     */
    let showAssignedUsers = JSON.parse(sessionStorage.getItem('showAssignedUsers')) || false;


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

    /* Returns the selected entries from a data table. */
    const getSelected = function (table) {
        const entries = [];
        table.rows({selected: true}).every(function () {
            entries.push(this.data());
        });
        return entries;
    };

    /* Sorting method to select DataTables rows by whether they are selected by the select extension. */
    DataTable.ext.order['select-extension'] = function (settings, col) {
        return this.api().column(col, {order:'index'}).nodes().map(function (td, i) {
            const tr = td.closest('tr');
            return tr.classList.contains('selected') ? '0' : '1';
        });
    };

    /* Search function added to DataTables to filter the users table. */
    const searchFunction = function(settings, renderedData, index, data, counter) {
        /* Let this search function only affect the users table. */
        if (settings.nTable.id !== 'table-users') {
            return true;
        }

        /* Filter out logged-in user. */
        if (data.id === loggedInUserId) {
            return false;
        }

        if (showAssignedUsers) {
            return true;
        }

        /* Filter out assigned users. */
        return !assignedUserIds.has(data.id);
    };
    DataTable.ext.search.push(searchFunction);

    const renderClassroomRole = function(role, type, row, meta) {
        switch (type) {
            case 'type':
                return role;
            case 'sort':
                // Sort owner(s) first
                if (role === 'OWNER') {
                    return 'a';
                } else {
                    return role;
                }
            case 'filter':
            case 'display':
                // Capitalize first letter, make rest lower case
                return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
        }
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
                // lastLogin is an Epoch (seconds since 1970), dateFormat needs a Date (or milliseconds)
                return lastLogin === null ? 'never' : dateFormat.format(lastLogin * 1000);
            case 'display':
                const span = document.createElement('span');
                if (lastLogin === null) {
                    span.style.color = 'gray';
                    span.textContent = 'never';
                } else {
                    span.title = 'Dates are converted to you local timezone: ' + timezone;
                    // lastLogin is an Epoch (seconds since 1970), dateFormat needs a Date (or milliseconds)
                    span.textContent = dateFormat.format(lastLogin * 1000);
                }
                return span.outerHTML;
        }
    };

    const renderUserAddToGame = function (userInfo, type, row, meta) {
        const container = document.createElement('div');
        container.classList.add('w-100', 'd-flex', 'align-items-center', 'gap-2');

        const gameIdSelect = document.createElement('select');
        gameIdSelect.classList.add('add-player-game', 'form-select', 'form-select-sm');
        gameIdSelect.style.flex = '1 1 25%';
        container.appendChild(gameIdSelect);

        const gamePlaceholder = document.createElement('option');
        gamePlaceholder.textContent = 'Game';
        gamePlaceholder.value = '';
        gamePlaceholder.disabled = true;
        gameIdSelect.add(gamePlaceholder);

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

        /* The role select is generated empty, as options are set based on the type of the selected game. */
        const roleSelect = document.createElement('select');
        roleSelect.classList.add('add-player-role', 'form-select', 'form-select-sm');
        roleSelect.style.flex = '1 1 35%';
        roleSelect.disabled = true;
        container.appendChild(roleSelect);

        const rolePlaceholder = document.createElement('option');
        rolePlaceholder.textContent = 'Role';
        rolePlaceholder.value = '';
        rolePlaceholder.disabled = true;
        roleSelect.add(rolePlaceholder);

        const addToGameButton = parseHTML(`
            <button disabled class="add-player-button btn btn-sm btn-primary" title="Add player to selected game">
                <i class="fa fa-plus"></i>
            </button>
        `);
        container.appendChild(addToGameButton);

        return container.outerHTML;
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

    const renderStagedGameClass = function (classId, type, row, meta) {
        const cut = classes.get(classId) ?? {id: -1, name: 'unknown', alias: 'unknown'};
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
                return players.map(userInfo => userInfo.name).join(' ');
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
            players.sort((a, b) => a.id - b.id);

            if (creatorRole === Role.PLAYER.name) {
                addCreatorRow(table, stagedGame, creator, creatorRole);
            }
            for (const player of players) {
                addStagedGamePlayersRow(table, stagedGame, player, Role.PLAYER.name);
            }
        } else {
            attackers.sort((a, b) => a.id - b.id);
            defenders.sort((a, b) => a.id - b.id);

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
        tr.setAttribute('data-user-id', userInfo.id);
        if (role === Role.ATTACKER.name) {
            tr.classList.add('bg-attacker-light');
        } else if (role === Role.DEFENDER.name) {
            tr.classList.add('bg-defender-light');
        }

        const userNameCell = tr.insertCell();
        userNameCell.style.paddingLeft = '1em';
        userNameCell.style.width = '20%';
        userNameCell.textContent = userInfo.name;
        userNameCell.classList.add('truncate');

        if (kind === 'CLASSROOM') {
            const roleCell = tr.insertCell();
            roleCell.style.width = '15%';
            roleCell.textContent = userInfo.classroomRole.substring(0, 1).toUpperCase()
                    + userInfo.classroomRole.substring(1).toLowerCase();
        }

        const lastRoleCell = tr.insertCell();
        lastRoleCell.style.width = '15%';
        lastRoleCell.innerHTML = renderUserLastRole(userInfo.lastRole, 'display');

        const totalScoreCell = tr.insertCell();
        totalScoreCell.style.width = '8%';
        totalScoreCell.textContent = userInfo.totalScore;

        const controlsCell = tr.insertCell();
        const controlsContainer = document.createElement('div');
        controlsContainer.classList.add('w-100', 'd-flex', 'align-items-center', 'gap-2');
        controlsCell.appendChild(controlsContainer);

        const switchRolesButton = parseHTML(`
            <button class="switch-creator-role-button btn btn-sm btn-primary" title="Switch your role">
                <i class="fa fa-exchange"></i>
            </button>
        `);
        controlsContainer.appendChild(switchRolesButton);

        /* Hide switch role button for melee games. */
        if (role === Role.PLAYER.name) {
            switchRolesButton.style.visibility = 'hidden';
        }

        const removeButton = parseHTML(`
            <button class="remove-creator-button btn btn-sm btn-danger" title="Change to Observer">
                <i class="fa fa-trash"></i>
            </button>
        `);
        controlsContainer.appendChild(removeButton);
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
        tr.setAttribute('data-user-id', userInfo.id);
        if (role === Role.ATTACKER.name) {
            tr.classList.add('bg-attacker-light');
        } else if (role === Role.DEFENDER.name) {
            tr.classList.add('bg-defender-light');
        }

        const userNameCell = tr.insertCell();
        userNameCell.style.paddingLeft = '1em';
        userNameCell.style.width = '20%';
        userNameCell.textContent = userInfo.name;
        userNameCell.classList.add('truncate');

        if (kind === 'CLASSROOOM') {
            const roleCell = tr.insertCell();
            roleCell.style.width = '15%';
            roleCell.textContent = userInfo.classroomRole.substring(0, 1).toUpperCase()
                    + userInfo.classroomRole.substring(1).toLowerCase();
        }

        const lastRoleCell = tr.insertCell();
        lastRoleCell.style.width = '15%';
        lastRoleCell.innerHTML = renderUserLastRole(userInfo.lastRole, 'display');

        const totalScoreCell = tr.insertCell();
        totalScoreCell.style.width = '8%';
        totalScoreCell.textContent = userInfo.totalScore;

        const controlsCell = tr.insertCell();
        const controlsContainer = document.createElement('div');
        controlsContainer.classList.add('w-100', 'd-flex', 'align-items-center', 'gap-2');
        controlsCell.appendChild(controlsContainer);

        const switchRolesButton = parseHTML(`
            <button class="switch-role-button btn btn-sm btn-primary" title="Switch role of player">
                 <i class="fa fa-exchange"></i>
            </button>
        `);
        controlsContainer.appendChild(switchRolesButton);

        /* Hide switch role button for melee games. */
        if (role === Role.PLAYER.name) {
            switchRolesButton.style.visibility = 'hidden';
        }

        const removeButton = parseHTML(`
            <button class="remove-player-button btn btn-sm btn-danger" title="Remove player from game">
                <i class="fa fa-trash"></i>
            </button>
        `)
        controlsContainer.appendChild(removeButton);

        const gameIdSelect = document.createElement('select');
        gameIdSelect.classList.add('move-player-game', 'form-select', 'form-select-sm', 'ms-4');
        gameIdSelect.style.flex = '1 1 25%';
        controlsContainer.appendChild(gameIdSelect);

        const gamePlaceholder = document.createElement('option');
        gamePlaceholder.textContent = 'Game';
        gamePlaceholder.value = '';
        gamePlaceholder.disabled = true;
        gameIdSelect.add(gamePlaceholder);

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

        const roleSelect = document.createElement('select');
        roleSelect.classList.add('move-player-role', 'form-select', 'form-select-sm');
        roleSelect.style.flex = '1 1 35%';
        roleSelect.disabled = true;
        controlsContainer.appendChild(roleSelect);

        const rolePlaceholder = document.createElement('option');
        rolePlaceholder.textContent = 'Role';
        rolePlaceholder.value = '';
        rolePlaceholder.disabled = true;
        roleSelect.add(rolePlaceholder);

        const moveButton = parseHTML(`
            <button disabled class="move-player-button btn btn-sm btn-primary" title="Move player to selected game">
                <i class="fa fa-arrow-right"></i>
            </button>;
        `)
        controlsContainer.appendChild(moveButton);
    };

    /**
     * For a "form" to add a user to an existing game, selects the correct roles for the type of the selected game
     * and enable the "submit" button.
     * @param {HTMLSelectElement} roleSelect The role select.
     * @param {HTMLButtonElement} submitButton The "submit" button.
     * @param {String} gameIdStr The formatted ID of the selected staged or existing game.
     */
    const adjustFormForGame = function (roleSelect, submitButton, gameIdStr) {
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

        const rolePlaceholder = document.createElement('option');
        rolePlaceholder.textContent = 'Role';
        rolePlaceholder.value = '';
        rolePlaceholder.disabled = true;

        if (gameType === GameType.MULTIPLAYER.name) {

            const attackerOption = document.createElement('option');
            attackerOption.textContent = Role.ATTACKER.display;
            attackerOption.value = Role.ATTACKER.name;

            const defenderOption = document.createElement('option');
            defenderOption.textContent = Role.DEFENDER.display;
            defenderOption.value = Role.DEFENDER.name;

            roleSelect.replaceChildren(rolePlaceholder, attackerOption, defenderOption);

        } else if (gameType === GameType.MELEE.name) {

            const playerOption = document.createElement('option');
            playerOption.textContent = Role.PLAYER.display;
            playerOption.value = Role.PLAYER.name;

            roleSelect.replaceChildren(rolePlaceholder, playerOption);
        }

        roleSelect.selectedIndex = 1;
        roleSelect.disabled = false;
        submitButton.disabled = false;
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
        tr.insertCell().innerHTML = renderStagedGameClass(gameSettings.classId);

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
        tr.insertCell().textContent = GameTime.formatTime(gameSettings.gameDurationMinutes);

        return table;
    };

    /* Restore the state of show/hide toggles for the tables. */
    const setCheckboxButton = function (checkbox, checked) {
        if (checked) {
            checkbox.checked = true;
            checkbox.parentNode.classList.add('active');
        } else {
            checkbox.checked = false;
            checkbox.parentNode.classList.remove('active');
        }
    }
    setCheckboxButton(document.getElementById('toggle-hide-players'), hideStagedGamePlayers);
    setCheckboxButton(document.getElementById('toggle-show-assigned-users'), showAssignedUsers);

    /* Staged games table and related components. */
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
                data: 'gameSettings.classId',
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
                width: '65%',
                title: kind == 'CLASSROOM'
                    ? 'Players (Username, Classroom Role, Last Game Role, Total Score)'
                    : 'Players (Username, Last Game Role, Total Score)'
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
                select.selectedIndex = 0;
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
                        const tr = this.closest('tr');
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
        language: {
            emptyTable: 'There are currently no staged games.',
            zeroRecords: 'No matching staged games found.'
        }
    });

    /* Search bar. */
    document.getElementById('search-staged-games').addEventListener('keyup', function (event) {
        stagedGamesTable.search(this.value).draw();
    });

    /* Show/hide players assigned to staged games. */
    document.getElementById('toggle-hide-players').addEventListener('change', function (event) {
        hideStagedGamePlayers = this.checked;
        sessionStorage.setItem('hideStagedGamePlayers', JSON.stringify(hideStagedGamePlayers));
        stagedGamesTable.rows().invalidate().draw();
    });

    /* Toggle create-games and delete-games buttons based on whether staged games are selected. */
    stagedGamesTable.on('select', function (e, dt, type, indexes) {
        document.getElementById('create-games-button').disabled = false;
        document.getElementById('delete-games-button').disabled = false;
    });
    stagedGamesTable.on('deselect', function (e, dt, type, indexes) {
        if (getSelected(stagedGamesTable).length === 0) {
            document.getElementById('create-games-button').disabled = true;
            document.getElementById('delete-games-button').disabled = true;
        }
    });

    /* Select / deselect all visible staged games. */
    document.getElementById('select-visible-games').addEventListener('click', function (event) {
        stagedGamesTable.rows({search: 'applied'}).select();
    })
    document.getElementById('deselect-visible-games').addEventListener('click', function (event) {
        stagedGamesTable.rows({search: 'applied'}).deselect();
    });

    /* Set role options according to the game type when a game id is selected. */
    stagedGamesTable.table().node().addEventListener('change', function (event) {
        const select = event.target.closest('.move-player-game');
        if (select === null) return;

        const tr = select.closest('tr');
        const roleSelect = tr.querySelector('.move-player-role');
        const button = tr.querySelector('.move-player-button');
        adjustFormForGame(roleSelect, button, select.value);
    });

    stagedGamesTable.table().node().addEventListener('click', function (event) {
        const select = event.target.closest('select');
        if (select === null) return;
        event.stopPropagation();
    });

    /* Switch a player's role. */
    stagedGamesTable.table().node().addEventListener('click', function (event) {
        const button = event.target.closest('.switch-role-button');
        if (button === null) return;
        event.stopPropagation();

        const innerTr = button.closest('tr');
        const outerTr = innerTr.parentElement.closest('tr');
        const userId = Number(innerTr.dataset.userId);
        const stagedGame = stagedGamesTable.row(outerTr).data();
        postForm({
            formType: 'switchRole',
            userId: userId,
            gameId: 'T' + stagedGame.id,
        });
    });

    /* Switch creator role. */
    stagedGamesTable.table().node().addEventListener('click', function (event) {
        const button = event.target.closest('.switch-creator-role-button');
        if (button === null) return;
        event.stopPropagation();

        const outerTr = button.closest('tr').parentElement.closest('tr');
        const stagedGame = stagedGamesTable.row(outerTr).data();
        postForm({
            formType: 'switchCreatorRole',
            gameId: 'T' + stagedGame.id,
        });
    });

    /* Remove a player from a staged game. */
    stagedGamesTable.table().node().addEventListener('click', function (event) {
        const button = event.target.closest('.remove-player-button');
        if (button === null) return;
        event.stopPropagation();

        const innerTr = button.closest('tr');
        const outerTr = innerTr.parentElement.closest('tr');
        const stagedGame = stagedGamesTable.row(outerTr).data();
        postForm({
            formType: 'removePlayerFromStagedGame',
            userId: innerTr.getAttribute('data-user-id'),
            gameId: 'T' + stagedGame.id,
        });
    });

    /* Remove creator from staged game. */
    stagedGamesTable.table().node().addEventListener('click', function (event) {
        const button = event.target.closest('.remove-creator-button');
        if (button === null) return;
        event.stopPropagation();

        const outerTr = button.closest('tr').parentElement.closest('tr');
        const stagedGame = stagedGamesTable.row(outerTr).data();
        postForm({
            formType: 'removeCreatorFromStagedGame',
            gameId: 'T' + stagedGame.id,
        });
    });

    /* Move a player between staged games. */
    stagedGamesTable.table().node().addEventListener('click', function (event) {
        const button = event.target.closest('.move-player-button');
        if (button === null) return;
        event.stopPropagation();

        const innerTr = button.closest('tr');
        const outerTr = innerTr.parentElement.closest('tr');
        const stagedGame = stagedGamesTable.row(outerTr).data();
        const gameSelect = innerTr.querySelector('.move-player-game');
        const roleSelect = innerTr.querySelector('.move-player-role');
        postForm({
            formType: 'movePlayerBetweenStagedGames',
            userId: innerTr.getAttribute('data-user-id'),
            gameIdFrom: 'T' + stagedGame.id,
            gameIdTo: gameSelect.value,
            role: roleSelect.value
        });
    });

    /* Delete selected games. */
    document.getElementById('delete-games-button').addEventListener('click', function (event) {
        const stagedGameIds = getSelected(stagedGamesTable)
                .map(stagedGame => 'T' + stagedGame.id);
        postForm({
            formType: 'deleteStagedGames',
            stagedGameIds
        });
    });

    /* Create selected games. */
    document.getElementById('create-games-button').addEventListener('click', function (event) {
        const stagedGameIds = getSelected(stagedGamesTable)
                .map(stagedGame => 'T' + stagedGame.id);
        postForm({
            formType: 'createStagedGames',
            stagedGameIds
        });
    });

    /* Users table and related components. */
    const usersTable = new DataTable('#table-users', {
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
                data: 'id',
                type: 'num',
                title: 'ID'
            },
            {
                data: 'name',
                type: 'string',
                title: 'Name'
            },
            kind !== 'CLASSROOM' ? null :
                {
                    data: 'classroomRole',
                    type: 'string',
                    title: 'Classroom Role',
                    render: renderClassroomRole
                },
            {
                data: 'lastRole',
                render: renderUserLastRole,
                type: 'html',
                title: 'Last Game Role'
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
                title: 'Add to existing game',
                width: '20em'
            }
        ].filter(col => col !== null),
        select: {
            style: 'multi',
            className: 'selected'
        },
        drawCallback: function () {
            const table = this.api().table().container();

            /* Select nothing in all selects in the table. */
            for (const select of table.querySelectorAll('select')) {
                select.selectedIndex = 0;
            }
        },
        order: kind === 'CLASSROOM'
            ? [[3, 'asc']]
            : [[5, 'asc']],
        scrollY: '600px',
        scrollCollapse: true,
        paging: false,
        dom: 't',
        language: {
            emptyTable: 'No users found.',
            zeroRecords: 'No matching users found.'
        }
    });

    /* Search bar. */
    document.getElementById('search-users').addEventListener('keyup', function (event) {
        usersTable.search(this.value).draw();
    });

    /* Show/hide users assigned to active existing games. */
    document.getElementById('toggle-show-assigned-users').addEventListener('change', function (event) {
        showAssignedUsers = this.checked;
        sessionStorage.setItem('showAssignedUsers', JSON.stringify(showAssignedUsers));
        usersTable.draw();
    });

    /* Select / deselect all visible users. */
    document.getElementById('select-visible-users').addEventListener('click', function () {
        usersTable.rows({search: 'applied'}).select();
    })
    document.getElementById('deselect-visible-users').addEventListener('click', function () {
        usersTable.rows({search: 'applied'}).deselect();
    });

    /* Set role options according to the game type when a game id is selected. */
    usersTable.table().node().addEventListener('change', function (event) {
        const select = event.target.closest('.add-player-game');
        if (select === null) return;

        const tr = select.closest('tr');
        const roleSelect = tr.querySelector('.add-player-role');
        const button = tr.querySelector('.add-player-button');
        adjustFormForGame(roleSelect, button, select.value);
    });

    usersTable.table().node().addEventListener('click', function (event) {
        const select = event.target.closest('select');
        if (select === null) return;
        event.stopPropagation();
    });

    /* Add a users to a staged game or existing active game. */
    usersTable.table().node().addEventListener('click', function (event) {
        const button = event.target.closest('.add-player-button');
        if (button === null) return;
        event.stopPropagation();

        /* Go up two levels of tr, since the form is in a table itself. */
        const tr = button.closest('tr');
        const userInfo = usersTable.row(tr).data();
        const gameSelect = tr.querySelector('.add-player-game');
        const roleSelect = tr.querySelector('.add-player-role');
        postForm({
            formType: 'addPlayerToGame',
            userId: userInfo.id,
            gameId: gameSelect.value,
            role: roleSelect.value
        });
    });

    /* Toggle multiplayer/melee specific forms based on selected game type. */
    document.getElementById('gameType-group').addEventListener('change', function (event) {
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

    /* Toggle stage games button based on whether users are selected or usernames are given. */
    usersTable.on('select', function (e, dt, type, indexes) {
        document.getElementById('stage-games-button').disabled = false;
    });
    usersTable.on('deselect', function (e, dt, type, indexes) {
        const userNames = document.getElementById('userNames').value;
        const button = document.getElementById('stage-games-button');

        if (getSelected(usersTable).length === 0
                && userNames.length === 0) {
            button.disabled = true;
        }
    });
    document.getElementById('userNames').addEventListener('keyup', function (event) {
        const userNames = document.getElementById('userNames').value;
        const button = document.getElementById('stage-games-button');

        if (getSelected(usersTable).length === 0
                && userNames.length === 0) {
            button.disabled = true;
        } else {
            button.disabled = false;
        }
    });

    /* Create new staged games with users. */
    document.getElementById('stage-games-button').addEventListener('click', function (event) {
        const userIds = getSelected(usersTable)
                .map(userInfo => userInfo.id);

        const params = {
            formType: 'stageGamesWithUsers',
            userIds
        };

        const formData = new FormData(document.getElementById('form-settings'));
        for (const [name, value] of formData.entries()) {
            params[name] = value;
        }
        postForm(params);
    });

    /* Create new empty staged games. */
    document.getElementById('stage-games-empty-button').addEventListener('click', function (event) {
        const userNames = document.getElementById('userNames').value;

        if (getSelected(usersTable).length > 0) {
            if (!confirm("You have users selected. Are you sure you want to stage empty games?")) {
                return;
            }
        } else if (userNames.length > 0) {
            if (!confirm("You entered user names/emails. Are you sure you want to stage empty games?")) {
                return;
            }
        }

        const params = {
            formType: 'stageEmptyGames'
        };

        const formData = new FormData(document.getElementById('form-settings'));
        for (const [name, value] of formData.entries()) {
            params[name] = value;
        }
        postForm(params);
    });
</script>

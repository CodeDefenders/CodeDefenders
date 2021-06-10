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

<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%@ page import="org.codedefenders.model.GameInfo" %>
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="org.codedefenders.model.UserMeleeGameInfo" %>
<%@ page import="org.codedefenders.model.UserMultiplayerGameInfo" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request" />
<% pageInfo.setPageTitle("My Games"); %>

<jsp:include page="/jsp/header.jsp"/>

<%
    // Games active for this user (Created or joined)
    List<GameInfo> activeGames = new ArrayList<>();
    activeGames.addAll((List<UserMultiplayerGameInfo>) request.getAttribute("activeGames"));
    activeGames.addAll((List<UserMeleeGameInfo>) request.getAttribute("activeMeleeGames"));
    activeGames.sort(Comparator.comparingInt(GameInfo::gameId));
    pageContext.setAttribute("activeGames", activeGames);

    // Games open for this user (not created or joined, and enough space for one player)
    List<UserMultiplayerGameInfo> openMultiplayerGames
            = (List<UserMultiplayerGameInfo>) request.getAttribute("openGames");
    List<UserMeleeGameInfo> openMeleeGames = (List<UserMeleeGameInfo>) request.getAttribute("openMeleeGames");
    pageContext.setAttribute("openMultiplayerGames", openMultiplayerGames);
    pageContext.setAttribute("openMeleeGames", openMeleeGames);

    boolean gamesJoinable = (boolean) request.getAttribute("gamesJoinable");
    boolean gamesCreatable = (boolean) request.getAttribute("gamesCreatable");

    PlayerScore zeroDummyScore = new PlayerScore(-1);
    zeroDummyScore.setMutantKillInformation("0/0/0");
    zeroDummyScore.setDuelInformation("0/0/0");
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

    <h2 class="mb-3">${pageInfo.pageTitle}</h2>
    <table id="my-games" class="table table-striped table-v-align-middle">
        <thead>
            <tr>
                <th></th>
                <th>ID</th>
                <th>Creator</th>
                <th>Class</th>
                <th>Players</th>
                <th>Level</th>
                <th></th>
            </tr>
        </thead>
        <tbody>
            <% resetOddEven(); %>
            <c:choose>
                <c:when test="${empty activeGames}">
                    <tr class="<%=oddEven()%>">
                        <td colspan="100" class="text-center">You are currently not active in any games.</td>
                    </tr>
                </c:when>
                <c:otherwise>
                    <%
                        for (GameInfo info : activeGames) {
                            int gameId = info.gameId();
                            if (info instanceof UserMultiplayerGameInfo) {
                                List<Player> attackers = ((UserMultiplayerGameInfo) info).attackers();
                                List<Player> defenders = ((UserMultiplayerGameInfo) info).defenders();
                                Map<Integer, PlayerScore> attackerScores = ((UserMultiplayerGameInfo) info).getMutantScores();
                                Map<Integer, PlayerScore> defenderScores = ((UserMultiplayerGameInfo) info).getTestScores();
                    %>
                        <tr id="<%="game-"+gameId%>" class="<%=oddEven()%>">
                            <td id="toggle-game-<%=gameId%>" class="toggle-details">
                                <i class="toggle-details-icon fa fa-chevron-right"></i>
                            </td>
                            <td><%=gameId%></td>
                            <td><%=info.creatorName()%></td>
                            <td>
                                <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gameId%>">
                                    <%=info.cutAlias()%>
                                </a>
                                <% pageContext.setAttribute("classId", info.cutId()); %>
                                <% pageContext.setAttribute("classAlias", info.cutAlias()); %>
                                <% pageContext.setAttribute("gameId", gameId); %>
                                <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-for-game-${gameId}"/>
                            </td>
                            <td>
                                <span><%=attackers.size()%>&nbsp;Attackers</span>,
                                <span><%=defenders.size()%>&nbsp;Defenders</span>
                            </td>
                            <td><%=info.gameLevel().getFormattedString()%></td>
                            <td>
                                <%
                                    if (info.gameState() == GameState.CREATED && info.creatorId() == info.userId()) {
                                %>
                                    <form id="adminStartBtn-<%=gameId%>"
                                          action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>"
                                          method="post">
                                        <button type="submit" class="btn btn-sm btn-success text-nowrap" id="startGame-<%=gameId%>"
                                                form="adminStartBtn-<%=gameId%>">
                                            Start battleground game
                                        </button>
                                        <input type="hidden" name="formType" value="startGame">
                                        <input type="hidden" name="gameId" value="<%= gameId %>"/>
                                    </form>
                                <%
                                    } else {
                                        switch (info.userRole()) {
                                            case ATTACKER:
                                                if (info.gameState() != GameState.CREATED) {
                                %>
                                    <a class="btn btn-sm btn-attacker text-nowrap" id="<%="attack-"+gameId%>"
                                       href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%=gameId%>">
                                        Attack in battleground
                                    </a>
                                <%
                                                } else {
                                %>
                                    <div class="d-flex flex-column gap-1">
                                        <span>Joined as attacker</span>
                                <%
                                                    if (gamesJoinable) {
                                %>
                                        <form id="attLeave" action="<%= request.getContextPath()  + Paths.BATTLEGROUND_SELECTION%>"
                                              method="post">
                                            <input class="btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
                                            <input type="hidden" name="gameId" value="<%=gameId%>">
                                            <button class="btn btn-sm btn-danger text-nowrap" id="<%="leave-attacker-"+gameId%>" type="submit"
                                                    form="attLeave"
                                                    value="Leave">
                                                Leave battleground
                                            </button>
                                        </form>
                                <%
                                                        }
                                %>
                                    </div>
                                <%
                                                }
                                                break;
                                            case DEFENDER:
                                                if (info.gameState() != GameState.CREATED) {
                                %>
                                    <a class="btn btn-sm btn-defender text-nowrap" id="<%="defend-"+gameId%>"
                                       href="<%= request.getContextPath() + Paths.BATTLEGROUND_GAME%>?gameId=<%=gameId%>">
                                        Defend in battleground
                                    </a>
                                <%
                                                } else {
                                %>
                                    <div class="d-flex flex-column gap-1">
                                        <span>Joined as defender</span>
                                <%
                                                    if (gamesJoinable) {
                                %>
                                            <form id="defLeave" action="<%= request.getContextPath()  + Paths.BATTLEGROUND_SELECTION%>"
                                                  method="post">
                                                <input class="btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
                                                <input type="hidden" name="gameId" value="<%=gameId%>">
                                                <button class="btn btn-sm btn-danger text-nowrap" id="<%="leave-defender-"+gameId%>" type="submit"
                                                        form="defLeave"
                                                        value="Leave">
                                                    Leave battleground
                                                </button>
                                            </form>
                                <%
                                                    }
                                %>
                                    </div>
                                <%
                                            }
                                            break;
                                        case OBSERVER:
                                            if (info.creatorId() == info.userId()) {
                                %>
                                    <a class="btn btn-sm btn-primary text-nowrap" id="<%="observe-"+gameId%>"
                                       href="<%= request.getContextPath()  + Paths.BATTLEGROUND_GAME%>?gameId=<%= gameId %>">
                                        Observe battleground
                                    </a>
                                <%
                                            }
                                            break;
                                        default:
                                            break;
                                        }
                                    }
                                %>
                            </td>
                        </tr>
                        <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                            <td colspan="100">
                                <div class="child-row-wrapper">
                                    <table class="child-row-details">
                                        <thead>
                                            <tr>
                                                <th>Attacker</th>
                                                <th class="text-end">Mutants</th>
                                                <th class="text-end">Alive Mutants</th>
                                                <th class="text-end">Points</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <%
                                                if (attackers.isEmpty()) {
                                            %>
                                                <tr>
                                                    <td colspan="100" class="text-center">There are no Attackers.</td>
                                                </tr>
                                            <%
                                                } else {
                                                    for (Player attacker : attackers) {
                                                        int playerId = attacker.getId();
                                                        PlayerScore playerScores = attackerScores.getOrDefault(playerId, zeroDummyScore);
                                            %>
                                                <tr>
                                                    <td><%=attacker.getUser().getUsername()%></td>
                                                    <td class="text-end"><%=playerScores.getQuantity() %></td>
                                                    <td class="text-end"><%=playerScores.getMutantKillInformation().split("/")[0]%></td>
                                                    <td class="text-end"><%=playerScores.getTotalScore()%></td>
                                                </tr>
                                            <%
                                                    }
                                                }
                                            %>
                                        </tbody>
                                        <thead>
                                            <tr>
                                                <th>Defender</th>
                                                <th class="text-end">Tests</th>
                                                <th class="text-end">Mutants Killed</th>
                                                <th class="text-end">Points</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <%
                                                if (defenders.isEmpty()) {
                                            %>
                                                <tr>
                                                    <td colspan="100" class="text-center">There are no Defenders.</td>
                                                </tr>
                                            <%
                                                } else {
                                                    for (Player defender : defenders) {
                                                        int playerId = defender.getId();
                                                        PlayerScore playerScores = defenderScores.getOrDefault(playerId, zeroDummyScore);
                                            %>
                                                <tr>
                                                    <td><%=defender.getUser().getUsername()%></td>
                                                    <td class="text-end"><%=playerScores.getQuantity() %></td>
                                                    <td class="text-end"><%=playerScores.getMutantKillInformation()%></td>
                                                    <td class="text-end"><%=playerScores.getTotalScore()%></td>
                                                </tr>
                                            <%
                                                    }
                                                }
                                            %>
                                        </tbody>
                                    </table>
                                </div>
                            </td>
                        </tr>
                        <%
                            } else if (info instanceof UserMeleeGameInfo) {
                                List<Player> players = ((UserMeleeGameInfo) info).players();
                        %>
                            <tr id="<%="game-"+gameId%>" class="<%=oddEven()%>">
                                <td id="toggle-game-<%=gameId%>" class="toggle-details">
                                    <i class="toggle-details-icon fa fa-chevron-right"></i>
                                </td>
                                <td><%=gameId%></td>
                                <td><%=info.creatorName()%></td>
                                <td>
                                    <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gameId%>">
                                        <%=info.cutAlias()%>
                                    </a>
                                    <% pageContext.setAttribute("classId", info.cutId()); %>
                                    <% pageContext.setAttribute("classAlias", info.cutAlias()); %>
                                    <% pageContext.setAttribute("gameId", gameId); %>
                                    <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-for-game-${gameId}"/>
                                </td>
                                <td><span><%=players.size()%> Players</span></td>
                                <td><%=info.gameLevel().getFormattedString()%></td>
                                <td>
                                    <%
                                        if (info.gameState() == GameState.CREATED && info.creatorId() == info.userId()) {
                                    %>
                                        <form id="adminStartBtn-<%=gameId%>"
                                              action="<%=request.getContextPath() + Paths.MELEE_SELECTION%>"
                                              method="post">
                                            <button type="submit" class="btn btn-sm btn-success text-nowrap" id="startGame-<%=gameId%>"
                                                    form="adminStartBtn-<%=gameId%>">
                                                Start melee game
                                            </button>
                                            <input type="hidden" name="formType" value="startGame">
                                            <input type="hidden" name="gameId" value="<%= gameId %>"/>
                                        </form>
                                    <%
                                        } else {
                                            switch (info.userRole()) {
                                                case OBSERVER:
                                    %>
                                        <a class="btn btn-sm btn-primary text-nowrap" id="<%="observe-"+gameId%>"
                                           href="<%= request.getContextPath() + Paths.MELEE_GAME%>?gameId=<%= gameId %>">
                                            Observe melee game
                                        </a>
                                    <%
                                                    break;
                                                case PLAYER:
                                                    // Game is already running, the user is a player, so she can play
                                                    if (info.gameState() != GameState.CREATED) {
                                    %>
                                        <a class="btn btn-sm btn-player text-nowrap" id="<%="play-"+gameId%>"
                                           href="<%= request.getContextPath() + Paths.MELEE_GAME%>?gameId=<%=gameId%>">
                                            Play in melee game
                                        </a>
                                    <%
                                                    } else {
                                    %>
                                        <div class="d-flex flex-column gap-1">
                                            <span>Joined as player</span>
                                    <%
                                                        if (gamesJoinable) {
                                    %>
                                            <form id="leave" action="<%= request.getContextPath()  + Paths.MELEE_SELECTION%>" method="post">
                                                <input class="btn btn-sm btn-danger" type="hidden" name="formType" value="leaveGame">
                                                <input type="hidden" name="gameId" value="<%=gameId%>">
                                                <button class="btn btn-sm btn-danger text-nowrap" id="<%="leave-"+gameId%>" type="submit" form="leave"
                                                        value="Leave">
                                                    Leave Melee Game
                                                </button>
                                            </form>
                                    <%
                                                        }
                                    %>
                                        </div>
                                    <%
                                                    }
                                                    break;
                                            }
                                        }
                                    %>
                                </td>
                            </tr>
                            <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                                <td colspan="100">
                                    <div class="child-row-wrapper">
                                        <table id="game-<%=gameId%>-players" class="child-row-details">
                                            <thead>
                                                <tr>
                                                    <th>Player</th>
                                                    <th class="text-end">Points</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <% if (players.isEmpty()) { %>
                                                    <tr>
                                                        <td colspan="100" class="text-center">There are no Players.</td>
                                                    </tr>
                                                <% } %>
                                                <% for (Player player : players) { %>
                                                    <tr>
                                                        <td><%=player.getUser().getUsername()%></td>
                                                        <td class="text-end"><%=player.getPoints()%></td>
                                                    </tr>
                                                <% } %>
                                            </tbody>
                                        </table>
                                    </div>
                                </td>
                            </tr>
                    <%
                            }
                        }
                    %>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>

    <% if (gamesCreatable) { %>
        <div>
            <a id="createBattleground" class="btn btn-outline-primary me-2" href="<%=request.getContextPath() + Paths.BATTLEGROUND_CREATE%>">
                Create battleground game
            </a>
            <a id="createMelee" class="btn btn-outline-primary" href="<%=request.getContextPath() + Paths.MELEE_CREATE%>">
                Create melee game
            </a>
        </div>
    <% } %>

    <%
        if (gamesJoinable) {
    %>

        <h2 class="mt-5 mb-3">Open Battleground Games</h2>
        <table id="tableOpenBattleground" class="table table-striped table-v-align-middle">
            <thead>
                <tr>
                    <th></th>
                    <th>ID</th>
                    <th>Creator</th>
                    <th>Class</th>
                    <th>Attackers</th>
                    <th>Defenders</th>
                    <th>Level</th>
                </tr>
            </thead>
            <tbody>
                <% resetOddEven(); %>
                <c:choose>
                    <c:when test="${empty openMultiplayerGames}">
                        <tr class="<%=oddEven()%>">
                            <td colspan="100" class="text-center">There are currently no open battleground games.</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <%
                            for (UserMultiplayerGameInfo info : openMultiplayerGames) {
                                int gameId = info.gameId();
                                List<Player> attackers = info.attackers();
                                List<Player> defenders = info.defenders();
                                Map<Integer, PlayerScore> attackerScores = info.getMutantScores();
                                Map<Integer, PlayerScore> defenderScores = info.getTestScores();
                        %>
                            <tr id="game-<%=gameId%>" class="<%=oddEven()%>">
                                <td id="toggle-game-<%=gameId%>" class="toggle-details">
                                    <i class="toggle-details-icon fa fa-chevron-right"></i>
                                </td>
                                <td><%=gameId%></td>
                                <td><%=info.creatorName()%></td>
                                <td>
                                    <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gameId%>">
                                        <%=info.cutAlias()%>
                                    </a>
                                    <% pageContext.setAttribute("classId", info.cutId()); %>
                                    <% pageContext.setAttribute("classAlias", info.cutAlias()); %>
                                    <% pageContext.setAttribute("gameId", gameId); %>
                                    <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-for-game-${gameId}"/>
                                </td>
                                <td>
                                    <form id="joinGameForm_attacker_<%=gameId%>"
                                          action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>"
                                          method="post">
                                        <input type="hidden" name="formType" value="joinGame">
                                        <input type="hidden" name="gameId" value=<%=info.gameId()%>>
                                        <input type="hidden" name="attacker" value=1>

                                        <span class="text-nowrap">
                                            <%=attackers.size()%>
                                            <button type="submit" id="<%="join-attacker-"+info.gameId()%>"
                                                    class="btn btn-sm btn-attacker ms-1"
                                                    value="Join as Attacker">
                                                Join
                                            </button>
                                        </span>
                                    </form>
                                </td>
                                <td>
                                    <form id="joinGameForm_defender_<%=gameId%>"
                                          action="<%=request.getContextPath() + Paths.BATTLEGROUND_SELECTION%>"
                                          method="post">
                                        <input type="hidden" name="formType" value="joinGame">
                                        <input type="hidden" name="gameId" value=<%=gameId%>>
                                        <input type="hidden" name="defender" value=1>

                                        <span class="text-nowrap">
                                            <%=defenders.size() %>
                                            <button type="submit" id="<%="join-defender-"+gameId%>"
                                                    class="btn btn-sm btn-defender ms-1"
                                                    value="Join as Defender">
                                                Join
                                            </button>
                                        </span>
                                    </form>
                                </td>
                                <td><%=info.gameLevel().getFormattedString() %></td>
                            </tr>
                            <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                                <td colspan="100">
                                    <div class="child-row-wrapper">
                                        <table class="child-row-details">
                                            <thead>
                                                <tr>
                                                    <th>Attacker</th>
                                                    <th class="text-end">Mutants</th>
                                                    <th class="text-end">Alive Mutants</th>
                                                    <th class="text-end">Points</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <% if (attackers.isEmpty()) { %>
                                                    <tr>
                                                        <td colspan="100" class="text-center">There are no Attackers. Go Join!</td>
                                                    </tr>
                                                <%
                                                    } else {
                                                        for (Player attacker : attackers) {
                                                            int playerId = attacker.getId();
                                                            PlayerScore playerScores = attackerScores.getOrDefault(playerId, zeroDummyScore);
                                                %>
                                                    <tr>
                                                        <td><%=attacker.getUser().getUsername()%></td>
                                                        <td class="text-end"><%=playerScores.getQuantity() %></td>
                                                        <td class="text-end"><%=playerScores.getMutantKillInformation().split("/")[0]%></td>
                                                        <td class="text-end"><%=playerScores.getTotalScore()%></td>
                                                    </tr>
                                                <%
                                                        }
                                                    }
                                                %>
                                            </tbody>
                                            <thead>
                                                <tr>
                                                    <th>Defender</th>
                                                    <th class="text-end">Tests</th>
                                                    <th class="text-end">Mutants Killed</th>
                                                    <th class="text-end">Points</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <% if (defenders.isEmpty()) { %>
                                                    <tr>
                                                        <td colspan="100" class="text-center">There are no Defenders. Go Join!</td>
                                                    </tr>
                                                <%
                                                    } else {
                                                        for (Player defender : defenders) {
                                                            int playerId = defender.getId();
                                                            PlayerScore playerScores = defenderScores.getOrDefault(playerId, zeroDummyScore);
                                                %>
                                                <tr>
                                                    <td><%=defender.getUser().getUsername()%></td>
                                                    <td class="text-end"><%=playerScores.getQuantity() %></td>
                                                    <td class="text-end"><%=playerScores.getMutantKillInformation()%></td>
                                                    <td class="text-end"><%=playerScores.getTotalScore()%></td>
                                                </tr>
                                                <%
                                                        }
                                                    }
                                                %>
                                            </tbody>
                                        </table>
                                    </div>
                                </td>
                            </tr>
                        <%
                            } // Closes FOR
                        %>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>

        <h2 class="mt-5 mb-3">Open Melee Games</h2>
        <table id="tableOpenMelee" class="table table-striped table-v-align-middle">
            <thead>
                <tr>
                    <th></th>
                    <th>ID</th>
                    <th>Creator</th>
                    <th>Class</th>
                    <th>Players</th>
                    <th>Level</th>
                </tr>
            </thead>
            <tbody>
                <% resetOddEven(); %>
                <c:choose>
                    <c:when test="${empty openMeleeGames}">
                        <tr class="<%=oddEven()%>">
                            <td colspan="100" class="text-center">There are currently no open melee games.</td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <%
                            for (UserMeleeGameInfo info : openMeleeGames) {
                                int gameId = info.gameId();
                                List<Player> players = info.players();

                        %>
                            <tr id="<%="game-"+info.gameId()%>" class="<%=oddEven()%>">
                                <td id="toggle-game-<%=gameId%>" class="toggle-details">
                                    <i class="toggle-details-icon fa fa-chevron-right"></i>
                                </td>
                                <td><%=gameId%></td>
                                <td><%=info.creatorName()%></td>
                                <td>
                                    <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gameId%>">
                                        <%=info.cutAlias()%>
                                    </a>
                                    <% pageContext.setAttribute("classId", info.cutId()); %>
                                    <% pageContext.setAttribute("classAlias", info.cutAlias()); %>
                                    <% pageContext.setAttribute("gameId", gameId); %>
                                    <t:class_modal classId="${classId}" classAlias="${classAlias}" htmlId="class-modal-for-game-${gameId}"/>
                                </td>
                                <td>
                                    <form id="joinGameForm_player_<%=info.gameId()%>"
                                          action="<%=request.getContextPath() + Paths.MELEE_SELECTION%>" method="post">
                                        <input type="hidden" name="formType" value="joinGame">
                                        <input type="hidden" name="gameId" value=<%=info.gameId()%>>
                                        <input type="hidden" name="player" value=1>

                                        <span class="text-nowrap">
                                            <%=info.players().size()%>
                                            <button type="submit" id="<%="join-"+info.gameId()%>"
                                                    class="btn btn-defender btn-sm ms-1"
                                                    value="Join">
                                                Join
                                            </button>
                                        </span>
                                    </form>
                                </td>
                                <td><%=info.gameLevel().getFormattedString() %></td>
                            </tr>
                            <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                                <td colspan="100">
                                    <div class="child-row-wrapper">
                                        <table id="game-<%=gameId%>-players" class="child-row-details">
                                            <thead>
                                                <tr>
                                                    <th>Player</th>
                                                    <th class="text-end">Points</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <% if (players.isEmpty()) { %>
                                                    <tr>
                                                        <td colspan="100" class="text-center">There are no Players. Go Join!</td>
                                                    </tr>
                                                <% } %>
                                                <% for (Player player : players) { %>
                                                    <tr>
                                                        <td><%=player.getUser().getUsername()%></td>
                                                        <td class="text-end"><%=player.getPoints()%></td>
                                                    </tr>
                                                <% } %>
                                            </tbody>
                                        </table>
                                    </div>
                                </td>
                            </tr>
                        <%
                            }
                        %>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>

    <%
        }
    %>

    <script>
        (function () {

            $(document).ready(function () {
                $.fn.dataTable.moment('YY/MM/DD HH:mm');
                $('#tableMPGames').DataTable({
                    "paging": false,
                    "searching": false,
                    "order": [[5, "asc"]],
                    "language": {
                        "info": ""
                    }
                });
            });

            $('table td.toggle-details').on('click', function () {
                const details = $('.' + $(this).attr('id'));
                const icon = $(this).find('.toggle-details-icon')

                if (details.is(':visible')) {
                    icon.removeClass('fa-chevron-down');
                    icon.addClass('fa-chevron-right');
                    details.hide()
                } else {
                    icon.removeClass('fa-chevron-right');
                    icon.addClass('fa-chevron-down');
                    details.show()
                }
            });

        })();
    </script>

</div>
<%@ include file="/jsp/footer.jsp" %>

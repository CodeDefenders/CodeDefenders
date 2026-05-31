<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="i18n" type="org.xnap.commons.i18n.I18n"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.game.multiplayer.PlayerScore" %>
<%@ page import="org.codedefenders.model.Player" %>
<%@ page import="org.codedefenders.model.UserMeleeGameInfo" %>
<%@ page import="org.codedefenders.model.UserMultiplayerGameInfo" %>
<%@ page import="org.codedefenders.util.Paths" %>

<%
    List<UserMultiplayerGameInfo> games = ((List<UserMultiplayerGameInfo>) request.getAttribute("finishedBattlegroundGames"));
    List<UserMeleeGameInfo> meleeGames = ((List<UserMeleeGameInfo>) request.getAttribute("finishedMeleeGames"));

    PlayerScore zeroDummyScore = new PlayerScore(-1);
    zeroDummyScore.setMutantKillInformation("0 / 0 / 0");
    zeroDummyScore.setDuelInformation("0 / 0 / 0");
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

    <h2 class="mb-4">${i18n.tr('Game History')}</h2>

    <h3 class="mt-4 mb-3">${i18n.tr('Battlegrounds')}</h3>
    <table class="table table-striped table-v-align-middle">
        <thead>
            <tr>
                <th></th>
                <th>${i18n.tr('ID')}</th>
                <th>${i18n.tr('Owner')}</th>
                <th>${i18n.tr('Class')}</th>
                <th>${i18n.tr('Attackers')}</th>
                <th>${i18n.tr('Defenders')}</th>
                <th>${i18n.tr('Level')}</th>
                <th>${i18n.tr('Actions')}</th>
            </tr>
        </thead>
        <tbody>
            <% resetOddEven(); %>

            <%
                if (games.isEmpty()) {
            %>

                <tr class="<%=oddEven()%>">
                    <td colspan="100" class="text-center">${i18n.tr('Your battleground game history is empty.')}</td>
                </tr>

            <%
                } else {
                    for (UserMultiplayerGameInfo g : games) {
                        int gameId = g.gameId();
                        List<Player> attackers = g.attackers();
                        List<Player> defenders = g.defenders();
                        Map<Integer, PlayerScore> attackerScores = g.getMutantScores();
                        Map<Integer, PlayerScore> defenderScores = g.getTestScores();
            %>

                <tr id="game-<%=gameId%>" class="<%=oddEven()%>">
                    <td id="toggle-game-<%=gameId%>" class="toggle-details">
                        <i class="toggle-details-icon fa fa-chevron-right"></i>
                    </td>
                    <td><%=gameId%></td>
                    <td><%= g.creatorName()%></td>
                    <td>
                        <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gameId%>">
                            <%=g.cutAlias()%>
                        </a>
                        <% pageContext.setAttribute("classId", g.cutId()); %>
                        <% pageContext.setAttribute("classAlias", g.cutAlias()); %>
                        <% pageContext.setAttribute("gameId", gameId); %>
                        <t:class_modal classId="${classId}" classAlias="${classAlias}"
                                       htmlId="class-modal-for-game-${gameId}"/>
                    </td>
                    <td><%=attackers.size()%>
                    </td>
                    <td><%=defenders.size()%>
                    </td>
                    <td><%=g.gameLevel().getFormattedString()%>
                    </td>
                    <td>
                        <a class="btn btn-sm btn-secondary text-nowrap" id="<%="results_"+gameId%>"
                           href="${url.forPath(Paths.BATTLEGROUND_GAME)}?gameId=<%=gameId%>">
                            ${i18n.tr('View Results')}
                        </a>
                    </td>
                </tr>

            <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                <td colspan="100">
                    <div class="child-row-wrapper">
                        <table class="child-row-details">
                            <thead>
                                    <tr>
                                        <th class="text-end">${i18n.tr('Attacker')}</th>
                                        <th class="text-end">${i18n.tr('Mutants')}</th>
                                        <th class="text-end">${i18n.tr('Alive Mutants')}</th>
                                        <th>${i18n.tr('Points')}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <%
                                        if (attackers.isEmpty()) {
                                    %>
                                        <tr>
                                            <td colspan="100"
                                                class="text-center">${i18n.tr('There are no Attackers.')}</td>
                                        </tr>
                                    <%
                                        } else {
                                            for (Player attacker : attackers) {
                                                int playerId = attacker.getId();
                                                PlayerScore playerScore = attackerScores.getOrDefault(playerId, zeroDummyScore);
                                    %>
                                        <tr>
                                            <td><%=attacker.getUser().getUsername()%></td>
                                            <td class="text-end"><%=playerScore.getQuantity() %></td>
                                            <td class="text-end"><%=playerScore.getMutantKillInformation().split("/")[0]%></td>
                                            <td class="text-end"><%=playerScore.getTotalScore()%></td>
                                        </tr>
                                    <%
                                            }
                                        }
                                    %>
                                </tbody>
                                <thead>
                                    <tr>
                                        <th>${i18n.tr('Defender')}</th>
                                        <th class="text-end">${i18n.tr('Tests')}</th>
                                        <th class="text-end">${i18n.tr('Mutants Killed')}</th>
                                        <th class="text-end">${i18n.tr('Points')}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <%
                                        if (defenders.isEmpty()) {
                                    %>
                                        <tr>
                                            <td colspan="100"
                                                class="text-center">${i18n.tr('There are no Defenders.')}</td>
                                        </tr>
                                    <%
                                        } else {
                                            for (Player defender : defenders) {
                                                int playerId = defender.getId();
                                                PlayerScore playerScore = defenderScores.getOrDefault(playerId, zeroDummyScore);
                                    %>
                                        <tr>
                                            <td><%=defender.getUser().getUsername()%></td>
                                            <td class="text-end"><%=playerScore.getQuantity() %></td>
                                            <td class="text-end"><%=playerScore.getMutantKillInformation()%></td>
                                            <td class="text-end"><%=playerScore.getTotalScore()%></td>
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
                    }
                }
            %>
        </tbody>
    </table>

    <h3 class="mt-4 mb-3">${i18n.tr('Melee games')}</h3>
    <table class="table table-striped table-v-align-middle">
        <thead>
            <tr>
                <th></th>
                <th>${i18n.tr('ID')}</th>
                <th>${i18n.tr('Owner')}</th>
                <th>${i18n.tr('Class')}</th>
                <th>${i18n.tr('Players')}</th>
                <th>${i18n.tr('Level')}</th>
                <th>${i18n.tr('Actions')}</th>
            </tr>
        </thead>
        <tbody>
            <% resetOddEven(); %>

            <%
                if (meleeGames.isEmpty()) {
            %>

                <tr class="<%=oddEven()%>">
                    <td colspan="100" class="text-center">${i18n.tr('Your melee game history is empty.')}</td>
                </tr>

            <%
                } else {
                    for (UserMeleeGameInfo g : meleeGames) {
                        int gameId = g.gameId();
                        List<Player> players = g.players();
            %>

                <tr id="game-<%=gameId%>" class="<%=oddEven()%>">
                    <td id="toggle-game-<%=gameId%>" class="toggle-details">
                        <i class="toggle-details-icon fa fa-chevron-right"></i>
                    </td>
                    <td><%=gameId%></td>
                    <td><%=g.creatorName()%></td>
                    <td>
                        <a href="#" data-bs-toggle="modal" data-bs-target="#class-modal-for-game-<%=gameId%>">
                            <%=g.cutAlias()%>
                        </a>
                        <% pageContext.setAttribute("classId", g.cutId()); %>
                        <% pageContext.setAttribute("classAlias", g.cutAlias()); %>
                        <% pageContext.setAttribute("gameId", gameId); %>
                        <t:class_modal classId="${classId}" classAlias="${classAlias}"
                                       htmlId="class-modal-for-game-${gameId}"/>
                    </td>
                    <td><%=players.size()%>
                    </td>
                    <td><%=g.gameLevel().getFormattedString()%>
                    </td>
                    <td>
                        <a class="btn btn-sm btn-secondary text-nowrap" id="<%="results_"+gameId%>"
                           href="${url.forPath(Paths.MELEE_GAME)}?gameId=<%=gameId%>">
                            ${i18n.tr('View Results')}
                        </a>
                    </td>
                </tr>

            <tr id="game-details-<%=gameId%>" class="toggle-game-<%=gameId%>" style="display: none">
                <td colspan="100">
                    <div class="child-row-wrapper">
                        <table class="child-row-details">
                            <thead>
                                    <tr>
                                        <th>${i18n.tr('Player')}</th>
                                        <th class="text-end">${i18n.tr('Points')}</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <%
                                        if (players.isEmpty()) {
                                    %>
                                        <tr>
                                            <td colspan="100"
                                                class="text-center">${i18n.tr('There are no Players.')}</td>
                                        </tr>
                                    <%
                                        } else {
                                            for (Player player : players) {
                                    %>
                                        <tr>
                                            <td><%=player.getUser().getUsername()%></td>
                                            <td class="text-end"><%=player.getPoints()%></td>
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
                    }
                }
            %>
        </tbody>
    </table>

    <script type="module">
        import $ from '${url.forPath("/js/jquery.mjs")}';


        $('table td.toggle-details').on('click', function () {
            let id = '.' + $(this).attr('id');
            if ($(id).is(':visible')) {
                $(this).find('.toggle-details-icon').removeClass('fa-chevron-down');
                $(this).find('.toggle-details-icon').addClass('fa-chevron-right');
                $(id).hide()
            } else {
                $(this).find('.toggle-details-icon').removeClass('fa-chevron-right');
                $(this).find('.toggle-details-icon').addClass('fa-chevron-down');
                $(id).show()
            }
        });
    </script>

</div>

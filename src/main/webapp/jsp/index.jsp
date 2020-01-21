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

<%@ page import="java.util.List" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.game.GameClass" %>

<%
    List<MultiplayerGame> openGames = (List<MultiplayerGame>) request.getAttribute("openMultiplayerGames");
    Map<Integer, String> gameCreatorNames = (Map<Integer, String>) request.getAttribute("gameCreatorNames");
%>

<jsp:useBean id="pageInfo" class="org.codedefenders.beans.page.PageInfoBean" scope="request"/>
<% pageInfo.setPageTitle("Welcome to Code Defenders"); %>

<jsp:include page="/jsp/header_logout.jsp"/>

<div class="nest">
    <div class="crow fly no-gutter">
        <div id="splash" class="jumbotron masthead">
            <h2><img class="logo" href="${pageContext.request.contextPath}/"
                     src="images/logo.png" style="margin-right: 10px"/>Code Defenders: A Mutation Testing Game</h2>

<%--            <!-- Puzzle--> may be used in the future once puzzles are more sophisticated--%>
<%--            <h3 class="text-primary"--%>
<%--                style="border-top: 1px solid; border-bottom: 1px solid; padding: 10px; /*background: #d9edf7">--%>
<%--                Puzzles--%>
<%--            </h3>--%>
<%--            <p style="font-size: medium">Play one of our predefined puzzles to improve your testing skills in the--%>
<%--                following lectures:</p>--%>
<%--            <%--%>
<%--                final List<PuzzleChapter> puzzleChapters = PuzzleDAO.getPuzzleChapters();--%>
<%--            %>--%>

<%--            <table class="table table-hover table-responsive table-paragraphs games-table">--%>
<%--                <%for (PuzzleChapter chapter : puzzleChapters) {%>--%>
<%--                <tr>--%>
<%--                    <td>--%>
<%--                        <%=chapter.getTitle()%>: <%=chapter.getDescription()%>--%>
<%--                    </td>--%>
<%--                </tr>--%>
<%--                <%}%>--%>
<%--            </table>--%>

            <!-- Battleground -->
            <!--
            <h3 class="text-primary"
                style="border-top: 1px solid; border-bottom: 1px solid; padding: 10px; /*background: #d9edf7">
                Currently Active Battleground Games
            </h3>
            -->
            <p style="font-size: medium">Currently active multiplayer games:</p>

            <table class="table table-hover table-responsive table-paragraphs games-table">
                <tr>
                    <th>Creator</th>
                    <th>Class</th>
                    <th>Attackers</th>
                    <th>Defenders</th>
                    <th>Level</th>
                </tr>
                <%
                    if (openGames.isEmpty()) {
                %>
                <tr>
                    <td colspan="100%"> Currently there are no open games.</td>
                </tr>
                <%
                } else {
                    for (MultiplayerGame game : openGames) {
                    final GameClass cut = game.getCUT();
                    int attackers = game.getAttackerPlayers().size();
                    int defenders = game.getDefenderPlayers().size();
                %>
                <tr id="<%="game-"+game.getId()%>">
                    <td class="col-sm-1"><%=gameCreatorNames.get(game.getId())%></td>
                    <td class="col-sm-2">
                        <span><%=cut.getAlias()%></span>
                    </td>
                    <td class="col-sm-1"><%=attackers%>
                    </td>
                    <td class="col-sm-1"><%=defenders%>
                    </td>
                    <td class="col-sm-1"><%= game.getLevel().getFormattedString()%>
                    </td>
                </tr>
                <%
                        }
                    }
                %>
            </table>
            <a id="enter" class="btn btn-primary btn-large" href="login">Log in or sign up</a>
        </div>
    </div>
</div>

<%@ include file="/jsp/footer_logout.jsp" %>
<%@ include file="/jsp/footer.jsp" %>

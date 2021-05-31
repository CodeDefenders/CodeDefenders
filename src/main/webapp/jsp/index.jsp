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

<%-- Vertically align content if enough space is available. --%>
<div class="container py-4 h-100 d-flex flex-column justify-content-center align-items-center">

    <div class="d-flex align-items-center gap-3 mb-3">
        <img href="${pageContext.request.contextPath}/"
             src="images/logo.png" alt="Code Defenders Logo"
             width="58">
        <%-- Make the header break nicely on smaller screens. --%>
        <h1 class="d-lg-block d-flex flex-column">
            <span>Code Defenders: </span>
            <span>A Mutation Testing Game</span>
        </h1>
    </div>

    <a href="${pageContext.request.contextPath}<%=Paths.LOGIN%>"
       class="btn btn-lg btn-primary btn-highlight"
       style="margin-bottom: 5rem;">
        Log in or Sign up
    </a>

    <div class="row g-4">
        <div class="col-xl-6 col-sm-12">
            <div class="p-5 bg-light rounded-3">
                <h2 class="mb-3">Active Multiplayer Games</h2>
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th>Creator</th>
                            <th>Class</th>
                            <th>Attackers</th>
                            <th>Defenders</th>
                            <th>Level</th>
                        </tr>
                    </thead>
                    <tbody>
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
                                <td><%=gameCreatorNames.get(game.getId())%></td>
                                <td><span><%=cut.getAlias()%></span></td>
                                <td><%=attackers%></td>
                                <td><%=defenders%></td>
                                <td><%=game.getLevel().getFormattedString()%></td>
                            </tr>
                        <%
                                }
                            }
                        %>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="col-xl-6 col-sm-12">
            <div class="p-5 bg-light rounded-3">
                <h2 class="mb-3">Research</h2>
                <%@ include file="/jsp/research.jsp" %>
            </div>
        </div>
    </div>

</div>

<%@ include file="/jsp/footer.jsp" %>

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

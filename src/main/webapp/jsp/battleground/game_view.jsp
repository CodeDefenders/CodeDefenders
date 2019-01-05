<%--

    Copyright (C) 2016-2018 Code Defenders contributors

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
<%
    final Logger logger = LoggerFactory.getLogger("game_view.jsp");
    final int uid = ServletUtils.userId(request);

    MultiplayerGame game;
    String redirectURL;
    int playerId;
    {
        int gameId = ((Integer) request.getAttribute("gameId"));
        game = MultiplayerGameDAO.getMultiplayerGame(gameId);
        if (game == null){
            logger.error("Could not find multiplayer game {}", gameId);
            response.sendRedirect(request.getContextPath() + Paths.GAMES_OVERVIEW);
            return;
        }
        playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(uid, gameId);
        redirectURL = request.getContextPath() + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId;
    }
%>

<% String pageTitle="In Game"; %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.model.Event" %>
<%@ page import="org.codedefenders.model.EventStatus" %>
<%@ page import="org.codedefenders.model.EventType" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codedefenders.database.MultiplayerGameDAO" %>
<%@ page import="org.codedefenders.servlets.util.ServletUtils" %>
<%
	boolean renderMutants = true;
	boolean redirect = false;
	String codeDivName = "cut-div"; // used
	Role role = game.getRole(uid);
	HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<>();

	if (role == Role.NONE) {
        if (request.getParameter("defender") != null) {
            if (game.addPlayer(uid, Role.DEFENDER)) {
                role = Role.DEFENDER;
            }
        } else if (request.getParameter("attacker") != null) {
            if (game.addPlayer(uid, Role.ATTACKER)) {
                role = Role.ATTACKER;
            }
        } else {
            response.sendRedirect(request.getContextPath() + Paths.GAMES_OVERVIEW);
            return;
        }
    }

    if (role != Role.CREATOR && (game.getState() == GameState.CREATED || game.getState() == GameState.FINISHED)) {
        response.sendRedirect(request.getContextPath()+ Paths.GAMES_OVERVIEW);
        return;
    }

    List<Test> tests = game.getTests(true); // get executable defenders' tests

    // compute line coverage information
    for (Test t : tests) {
        for (Integer lc : t.getLineCoverage().getLinesCovered()) {
            if (!linesCovered.containsKey(lc)) {
                linesCovered.put(lc, new ArrayList<>());
            }
            linesCovered.get(lc).add(t);
        }
    }

%>
<%@ include file="/jsp/battleground/header_game.jsp" %>
<%
    messages = new ArrayList<>();
    session.setAttribute("messages", messages);

    List<Mutant> mutants = game.getMutants();

    for (Mutant m : mutants){
        m.getLines();
    }

    List<Mutant> mutantsAlive = game.getAliveMutants();

    List<Mutant> mutantsPending = game.getMutantsMarkedEquivalentPending();

    Map<Integer, List<Mutant>> mutantLines = new HashMap<>();
    Map<Integer, List<Mutant>> mutantEquivPending = new HashMap<>();
    Map<Integer, List<Mutant>> mutantKilledLines = new HashMap<>();


    // Ensure that mutants marked equivalent are drawn to display
    //mutantsAlive.addAll(mutantsEquiv);
    //mutantsAlive.addAll(mutantsPending);

    for (Mutant m : mutantsAlive) {
        for (int line : m.getLines()){
            if (!mutantLines.containsKey(line)){
                mutantLines.put(line, new ArrayList<>());
            }

            mutantLines.get(line).add(m);

        }
    }

    for (Mutant m : mutantsPending) {
        for (int line : m.getLines()){
            if (!mutantEquivPending.containsKey(line)){
                mutantEquivPending.put(line, new ArrayList<>());
            }
            mutantEquivPending.get(line).add(m);
        }
    }

    mutantsAlive.addAll(mutantsPending);

    List<Mutant> mutantsKilled = game.getKilledMutants();

    for (Mutant m : mutantsKilled) {
        for (int line : m.getLines()){
            if (!mutantKilledLines.containsKey(line)){
                mutantKilledLines.put(line, new ArrayList<>());
            }
            mutantKilledLines.get(line).add(m);
        }
    }
%>
<%@ include file="/jsp/scoring_tooltip.jsp" %>
<%@ include file="/jsp/playerFeedback.jsp" %>
    <%@ include file="/jsp/battleground/game_scoreboard.jsp" %>
<div class="crow fly no-gutter up">
    <% switch (role){
        case ATTACKER:
            Mutant equiv = null;
            for (Mutant m : mutantsPending) {
                if (m.getPlayerId() == playerId &&  m.getEquivalent() == Mutant.Equivalence.PENDING_TEST) {
                    equiv = m;
                    request.setAttribute("equivMutant", equiv);
                    break;
                }
            }

            if (equiv == null) { %>
                <%@ include file="/jsp/battleground/attacker_view.jsp" %>
            <% } else { %>
                <%@ include file="/jsp/battleground/equivalence_view.jsp" %>
            <% }

            break;
        case DEFENDER:
            %><%@ include file="/jsp/battleground/defender_view.jsp" %>
            <%
            break;
        case CREATOR:
            %><%@ include file="/jsp/battleground/creator_view.jsp" %><%
            break;
        default:
            response.sendRedirect(request.getContextPath()+ Paths.GAMES_OVERVIEW);
            return;
    }
%>
    </div>
<%
if(game.isCapturePlayersIntention() ) {
	if( Role.DEFENDER.equals(role)) {
%>
<%@ include file="/jsp/game_components/defender_intention_collector.jsp" %>
<%
	} else if( Role.ATTACKER.equals(role)) {
%>
<%@ include file="/jsp/game_components/attacker_intention_collector.jsp" %>
<%
	}
}
%>
<%@ include file="/jsp/game_notifications.jsp"%>
<%@ include file="/jsp/battleground/footer_game.jsp" %>

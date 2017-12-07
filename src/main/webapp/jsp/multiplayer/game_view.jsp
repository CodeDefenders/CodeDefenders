<%
    final Logger logger = LoggerFactory.getLogger("game_view.jsp");
    boolean redirectToGames = false;
    // Get their user id from the session.
    int uid = (Integer) session.getAttribute("uid");
    int gameId = 0;
    try {
        gameId = Integer.parseInt(request.getParameter("id"));
        session.setAttribute("mpGameId", gameId);
    } catch (NumberFormatException e) {
        logger.info("Game ID was not passed in the  Restoring from session.");
        if (session.getAttribute("mpGameId") != null) {
            gameId = (Integer) session.getAttribute("mpGameId");
        } else {
            logger.info("Don't know what game was open...");
            redirectToGames = true;
        }
    } catch (Exception e2){
        logger.error("Exception caught", e2);
        gameId = 0;
        redirectToGames = true;
    }
    MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);
    if (mg == null){
        logger.error(String.format("Could not find multiplayer game %d", gameId));
        redirectToGames = true;
    }

    if (redirectToGames){
        response.sendRedirect(request.getContextPath()+"/games/user");
        return;
    }
%>

<% String pageTitle="In Game"; %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<%@ page import="org.codedefenders.events.Event" %>
<%@ page import="org.codedefenders.events.EventType" %>
<%@ page import="org.codedefenders.events.EventStatus" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="static org.codedefenders.Constants.MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE" %>
<%
	boolean renderMutants = true;
	boolean redirect = false;
	String codeDivName = "cut-div"; // used
	Role role = mg.getRole(uid);
	HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<>();

    if ((mg.getState().equals(GameState.CREATED) || mg.getState().equals(GameState.FINISHED)) && (!role.equals(Role.CREATOR))) {
        response.sendRedirect(request.getContextPath()+"/games/user");
    }

    List<Test> tests = mg.getTests(true); // get executable defenders' tests

    // compute line coverage information
    for (Test t : tests) {
        for (Integer lc : t.getLineCoverage().getLinesCovered()) {
            if (!linesCovered.containsKey(lc)) {
                linesCovered.put(lc, new ArrayList<Test>());
            }
            linesCovered.get(lc).add(t);
        }
    }

%>
<%@ include file="/jsp/multiplayer/header_game.jsp" %>
<%
    messages = new ArrayList<>();
    session.setAttribute("messages", messages);

    int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(uid, gameId);
    List<Mutant> mutantsAlive = mg.getAliveMutants();

    List<Mutant> mutantsPending = mg.getMutantsMarkedEquivalentPending();

    if (role.equals(Role.DEFENDER) && request.getParameter("equivLine") != null &&
            (mg.getState().equals(GameState.ACTIVE) || mg.getState().equals(GameState.GRACE_ONE))){
        try {
            int equivLine = Integer.parseInt(request.getParameter("equivLine"));

            if (mg.isLineCovered(equivLine)) {
                int nClaimed = 0;
                for (Mutant m : mutantsAlive) {
                    if (m.getLines().contains(equivLine)) {
                        m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                        m.update();

                        User mutantOwner = DatabaseAccess.getUserFromPlayer(m.getPlayerId());

                        Event notifEquivFlag = new Event(-1, mg.getId(),
                                mutantOwner.getId(),
                                "One or more of your mutants is flagged equivalent.",
                                EventType.DEFENDER_MUTANT_EQUIVALENT,
                                EventStatus.NEW,
                                new Timestamp(System.currentTimeMillis()));
                        notifEquivFlag.insert();

                        DatabaseAccess.insertEquivalence(m, playerId);
                        nClaimed++;
                    }
                }

                Event notifMutant = new Event(-1, mg.getId(),
                        uid,
                        DatabaseAccess.getUser(uid)
                                .getUsername() + " flagged " + nClaimed +
                                " mutant" + (nClaimed == 1? "" : "s") +
                                " as equivalent.",
                        EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));

                notifMutant.insert();

                messages.add(String.format("Flagged %d mutant%s as equivalent", nClaimed, (nClaimed == 1 ? "" : 's')));

                response.sendRedirect("play");
            } else {
            	// equivLine is not covered, possible iff passed directly as url argument
                messages.add(MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE);
                response.sendRedirect("play");
                return;
            }
        } catch (NumberFormatException e){}

    } else if (role.equals(Role.ATTACKER) && request.getParameter("acceptEquiv") != null){
        try {
            int mutId = Integer.parseInt(request.getParameter("acceptEquiv"));

            for (Mutant m : mutantsPending){
                if (m.getPlayerId() == playerId &&  m.getId() == mutId){
                    m.kill(Mutant.Equivalence.DECLARED_YES);
                    DatabaseAccess.increasePlayerPoints(1, DatabaseAccess.getEquivalentDefenderId(m));
                    messages.add(Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE);

                    User eventUser = DatabaseAccess.getUser(uid);

                    Event notifEquiv = new Event(-1, mg.getId(),
                            uid,
                            eventUser.getUsername() +
                                    " accepts that their mutant is equivalent.",
                            EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                            new Timestamp(System.currentTimeMillis()));
                    notifEquiv.insert();

                    response.sendRedirect("play");
                }
            }
        } catch (NumberFormatException e){}
    }

    List<Mutant> mutantsEquiv =  mg.getMutantsMarkedEquivalent();
    Map<Integer, List<Mutant>> mutantLines = new HashMap<>();
    Map<Integer, List<Mutant>> mutantEquivPending = new HashMap<>();
    Map<Integer, List<Mutant>> mutantKilledLines = new HashMap<>();


    // Ensure that mutants marked equivalent are drawn to display
    //mutantsAlive.addAll(mutantsPending);

    for (Mutant m : mutantsAlive) {
        for (int line : m.getLines()){
            if (!mutantLines.containsKey(line)){
                mutantLines.put(line, new ArrayList<Mutant>());
            }

            mutantLines.get(line).add(m);

        }
    }

    for (Mutant m : mutantsPending) {
        for (int line : m.getLines()){
            if (!mutantEquivPending.containsKey(line)){
                mutantEquivPending.put(line, new ArrayList<Mutant>());
            }
            mutantEquivPending.get(line).add(m);
        }
    }

    mutantsAlive.addAll(mutantsPending);

    List<Mutant> mutantsKilled = mg.getKilledMutants();

    for (Mutant m : mutantsKilled) {
        for (int line : m.getLines()){
            if (!mutantKilledLines.containsKey(line)){
                mutantKilledLines.put(line, new ArrayList<Mutant>());
            }
            mutantKilledLines.get(line).add(m);
        }
    }
    //ArrayList<String> messages = new ArrayList<String>();
%>
    <%@ include file="/jsp/scoring_tooltip.jsp" %>
    <%@ include file="/jsp/multiplayer/game_scoreboard.jsp" %>
<div class="crow fly no-gutter up">
    <% switch (role){
        case ATTACKER:
            %><%@ include file="/jsp/multiplayer/attacker_view.jsp" %><%
            break;
        case DEFENDER:
            %><%@ include file="/jsp/multiplayer/defender_view.jsp" %><%
            break;
        case CREATOR:
            %><%@ include file="/jsp/multiplayer/creator_view.jsp" %><%
            break;
        default:
            if (request.getParameter("defender") != null){
                mg.addPlayer(uid, Role.DEFENDER);
            } else if (request.getParameter("attacker") != null){
                mg.addPlayer(uid, Role.ATTACKER);
            } else {
                response.sendRedirect(request.getContextPath()+"multiplayer/games/user");
                break;
            }
            %>
            <p>Joining Game...</p>
<%
            response.setIntHeader("Refresh", 1);
            break;
    }
%>
    </div>
<script>
<%@ include file="/jsp/multiplayer/game_highlighting.jsp" %>
</script>
<%@ include file="/jsp/game_notifications.jsp"%>
<%@ include file="/jsp/multiplayer/footer_game.jsp" %>
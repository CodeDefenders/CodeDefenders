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
        logger.info("Game ID was not passed in the request " + request.getContextPath() + request.getRequestURI() +". Restoring from session.");
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
    MultiplayerGame game = DatabaseAccess.getMultiplayerGame(gameId);
    if (game == null){
        logger.error(String.format("Could not find multiplayer game %d", gameId));
        redirectToGames = true;
    }

    if (redirectToGames){
        response.sendRedirect(request.getContextPath()+"/games/user");
        return;
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
<%@ page import="static org.codedefenders.util.Constants.MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
	boolean renderMutants = true;
	boolean redirect = false;
	String codeDivName = "cut-div"; // used
	Role role = game.getRole(uid);
	HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<>();

    if ((game.getState().equals(GameState.CREATED) || game.getState().equals(GameState.FINISHED)) && (!role.equals(Role.CREATOR))) {
        response.sendRedirect(request.getContextPath()+"/games/user");
    }

    List<Test> tests = game.getTests(true); // get executable defenders' tests

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

    List<Mutant> mutants = game.getMutants();

    for (Mutant m : mutants){
        m.getLines();
    }

    List<Mutant> mutantsAlive = game.getAliveMutants();

    List<Mutant> mutantsPending = game.getMutantsMarkedEquivalentPending();

    if (role.equals(Role.DEFENDER) && (request.getParameter("equivLine") != null || request.getParameter("equivLines") != null) &&
            (game.getState().equals(GameState.ACTIVE) || game.getState().equals(GameState.GRACE_ONE))){
        try {
            List<Integer> equivLines = new ArrayList<Integer>();
            if( request.getParameter("equivLine") != null ){
				equivLines.add(Integer.parseInt(request.getParameter("equivLine")));
            } else {
				for( String s : request.getParameter("equivLines").replaceAll("\\[|\\]", "").split(",")){
					equivLines.add(Integer.parseInt(s));
				}
            }

            int nClaimed = 0;
            boolean noneCovered = true;
            for (Integer equivLine : equivLines) {
				if (game.isLineCovered(equivLine)) {
					noneCovered = false;
					for (Mutant m : mutantsAlive) {
						if (m.getLines().contains(equivLine)) {
							m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
							// BAD
							m.update();

							User mutantOwner = DatabaseAccess.getUserFromPlayer(m.getPlayerId());

							Event notifEquivFlag = new Event(-1, game.getId(), mutantOwner.getId(),
									"One or more of your mutants is flagged equivalent.",
									EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.NEW,
									new Timestamp(System.currentTimeMillis()));
							notifEquivFlag.insert();

							DatabaseAccess.insertEquivalence(m, playerId);
							nClaimed++;
						}
					}
				}
			}

			if (noneCovered && !game.isMarkUncovered()) {
				// equivLine is not covered, possible iff passed directly as url argument
				messages.add(MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE);
				response.sendRedirect(request.getContextPath() + "/multiplayer/play");
				return;
			}

			if (nClaimed >= 1) {
				String flaggingChatMessage = DatabaseAccess.getUser(uid).getUsername() + " flagged "
						+ nClaimed + " mutant" + (nClaimed == 1 ? "" : "s") + " as equivalent.";
				Event notifMutant = new Event(-1, game.getId(), uid, flaggingChatMessage,
						EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME,
						new Timestamp(System.currentTimeMillis()));

				notifMutant.insert();
			}

			String flaggingMessage = nClaimed < 1
					? "Mutant has already been claimed as equivalent or killed!"
					: String.format("Flagged %d mutant%s as equivalent", nClaimed,
							(nClaimed == 1 ? "" : 's'));
			messages.add(flaggingMessage);
			response.sendRedirect(request.getContextPath() + "/multiplayer/play");
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

                    Event notifEquiv = new Event(-1, game.getId(),
                            uid,
                            eventUser.getUsername() +
                                    " accepts that their mutant " + m.getId() + " is equivalent.",
                            EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                            new Timestamp(System.currentTimeMillis()));
                    notifEquiv.insert();

                    response.sendRedirect(request.getContextPath()+"/multiplayer/play");
                }
            }
        } catch (NumberFormatException e){}
    }

    List<Mutant> mutantsEquiv =  game.getMutantsMarkedEquivalent();
    Map<Integer, List<Mutant>> mutantLines = new HashMap<>();
    Map<Integer, List<Mutant>> mutantEquivPending = new HashMap<>();
    Map<Integer, List<Mutant>> mutantKilledLines = new HashMap<>();


    // Ensure that mutants marked equivalent are drawn to display
    //mutantsAlive.addAll(mutantsEquiv);
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

    List<Mutant> mutantsKilled = game.getKilledMutants();

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
<%@ include file="/jsp/playerFeedback.jsp" %>
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
                game.addPlayer(uid, Role.DEFENDER);
                %><meta http-equiv="refresh" content="1" /><%
            } else if (request.getParameter("attacker") != null){
                game.addPlayer(uid, Role.ATTACKER);
                %><meta http-equiv="refresh" content="1" /><%
            } else {
                // response.sendRedirect(request.getContextPath()+"/multiplayer/games/user");
                response.sendRedirect(request.getContextPath()+"/games/user");
                break;
            }
            %>
            <p>Joining Game...</p>
<%
            break;
    }
%>
    </div>
<%@ include file="/jsp/game_notifications.jsp"%>
<%@ include file="/jsp/multiplayer/footer_game.jsp" %>
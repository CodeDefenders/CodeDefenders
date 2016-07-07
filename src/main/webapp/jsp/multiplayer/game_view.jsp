<% String pageTitle="In Game"; %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%
    // Get their user id from the session.
    int uid = (Integer) session.getAttribute("uid");
    int gameId = 0;
    try {
        gameId = Integer.parseInt(request.getParameter("id"));
        session.setAttribute("mpGameId", gameId);
    } catch (NumberFormatException e){
        try {
            gameId = (Integer) session.getAttribute("mpGameId");
        } catch (NullPointerException e2){
            response.sendRedirect("multiplayer/games/user");
        }
    }
    boolean isTests = false;
    boolean renderMutants = true;

    HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<Integer, ArrayList<Test>>();

    ArrayList<Integer> linesUncovered = new ArrayList<Integer>();

    String codeDivName = "cut-div";

    MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);
    Participance p = mg.getParticipance(uid);

    List<Test> tests = mg.getExecutableTests();
%>
<%@ include file="/jsp/multiplayer/header_game.jsp" %>
<%
    if (messages == null){
        messages = new ArrayList<String>();
    }

    ArrayList<MultiplayerMutant> mutantsAlive = mg.getAliveMutants();

    ArrayList<MultiplayerMutant> mutantsEquiv =  mg.getMutantsMarkedEquivalent();

    HashMap<Integer, ArrayList<MultiplayerMutant>> mutantLines = new HashMap<Integer, ArrayList<MultiplayerMutant>>();

    if (p.equals(Participance.DEFENDER) && request.getParameter("equivLine") != null){
        try {
            int equivLine = Integer.parseInt(request.getParameter("equivLine"));

            int equivCounter = 0;
            for (MultiplayerMutant m : mutantsAlive) {
                List<String> lines = m.getHTMLReadout();
                for (String l : lines){
                    int line = Integer.parseInt(l.split(":")[1].trim());
                    if (line == equivLine){
                        m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                        m.update();
                        equivCounter++;
                    }

                }
            }
            messages.add("Flagged " + equivCounter + " mutants as equivalent");
            mutantsAlive = mg.getAliveMutants();

        } catch (NumberFormatException e){}
    } else if (p.equals(Participance.ATTACKER) && request.getParameter("acceptEquiv") != null){
        try {
            int mutId = Integer.parseInt(request.getParameter("acceptEquiv"));

            int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(uid, gameId);

            MultiplayerMutant equiv = null;

            for (MultiplayerMutant m : mutantsEquiv){
                if (m.getPlayerId() == playerId &&  m.getId() == mutId){
                    m.setScore(0);
                    m.setEquivalent(Mutant.Equivalence.DECLARED_YES);
                    m.update();

                    break;
                }
            }
        } catch (NumberFormatException e){}
    }

    for (MultiplayerMutant m : mutantsAlive) {
        List<String> lines = m.getHTMLReadout();
        for (String l : lines){
            int line = Integer.parseInt(l.split(":")[1].trim());
            if (!mutantLines.containsKey(line)){
                mutantLines.put(line, new ArrayList<MultiplayerMutant>());
            }

            mutantLines.get(line).add(m);

        }
    }


    ArrayList<MultiplayerMutant> mutantsKilled = mg.getKilledMutants();
    //ArrayList<String> messages = new ArrayList<String>();
%>

    <%@ include file="/jsp/multiplayer/game_scoreboard.jsp" %>

    <% switch (p){
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
                mg.addUserAsDefender(uid);
            } else if (request.getParameter("attacker") != null){
                mg.addUserAsAttacker(uid);
            } else {
                response.sendRedirect("multiplayer/games/user");
                break;
            }
            %>
            <p>Joining Game...</p>
<%
            response.setIntHeader("Refresh", 1);
            break;
    }
%>
<%@ include file="/jsp/multiplayer/footer_game.jsp" %>
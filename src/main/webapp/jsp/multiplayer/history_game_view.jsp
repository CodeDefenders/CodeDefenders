<% String pageTitle="In Game"; %>
<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.game.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.util.Constants" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    // Get their user id from the session.
    int uid = ((Integer) session.getAttribute("uid"));
    String gameIdString = request.getParameter("id");
    int gameId = 0;

    if (gameIdString != null) {
        try {
            gameId = Integer.parseInt(request.getParameter("id"));
            session.setAttribute("mpGameId", gameId);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath()+"/games/history");
            return;
        }
    } else {
        response.sendRedirect(request.getContextPath()+"/games/history");
        return;
    }

    boolean renderMutants = true;

    HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<Integer, ArrayList<Test>>();

    String codeDivName = "cut-div";

    MultiplayerGame game = DatabaseAccess.getMultiplayerGame(gameId);
    Role role = game.getRole(uid);

    if ((!game.getState().equals(GameState.FINISHED))) {
        response.sendRedirect(request.getContextPath()+"/games/user");
    }

    List<Test> tests = game.getTests(); // get all executable tests

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
    if (messages == null){
        messages = new ArrayList<String>();
    }

    int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(uid, gameId);
    List<Mutant> mutantsAlive = game.getAliveMutants();
    List<Mutant> mutants = game.getMutants();

    List<Mutant> mutantsPending = game.getMutantsMarkedEquivalentPending();
    List<Mutant> mutantsEquiv =  game.getMutantsMarkedEquivalent();

    Map<Integer, List<Mutant>> mutantLines = new HashMap<>();
    Map<Integer, List<Mutant>> mutantEquivPending = new HashMap<>();
    Map<Integer, List<Mutant>> mutantKilledLines = new HashMap<>();

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

//    for (Mutant m : mutantsPending){
//        mutantsAlive.add(m);
//    }
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
    <% codeDivName = "newmut-div"; %>
    <div class="crow">
        <div class="w-45 up">
            <%@include file="/jsp/multiplayer/game_mutants.jsp"%>
            <%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
        </div>
        <div class="w-55" id="newmut-div">
                <h3>Class Under Test</h3>
                <input type="hidden" name="formType" value="createMutant">
                <input type="hidden" name="mpGameID" value="<%= game.getId() %>" />
                <%
                    String mutantCode;
                    String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                    if (previousMutantCode != null) {
                        mutantCode = previousMutantCode;
                    } else
                        mutantCode = game.getCUT().getAsString();
                %>
                <pre><textarea id="code" name="mutant" cols="80" rows="50" style="min-width: 512px;"><%= mutantCode %></textarea></pre>
            <script>
                var editorSUT = CodeMirror.fromTextArea(document.getElementById("code"), {
                    readOnly: true,
                    lineNumbers: true,
                    indentUnit: 4,
                    indentWithTabs: true,
                    matchBrackets: true,
                    mode: "text/x-java"
                });
                editorSUT.setSize("100%", 500);

                var x = document.getElementsByClassName("utest");
                var i;
                for (i = 0; i < x.length; i++) {
                    CodeMirror.fromTextArea(x[i], {
                        lineNumbers: true,
                        matchBrackets: true,
                        mode: "text/x-java",
                        readOnly: true
                    });
                }
                /* Mutants diffs */
                $('.modal').on('shown.bs.modal', function() {
                    var codeMirrorContainer = $(this).find(".CodeMirror")[0];
                    if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                        codeMirrorContainer.CodeMirror.refresh();
                    } else {
                        var editorDiff = CodeMirror.fromTextArea($(this).find('textarea')[0], {
                            lineNumbers: false,
                            mode: "diff",
                            readOnly: true /* onCursorActivity: null */
                        });
                        editorDiff.setSize("100%", 500);
                    }
                });

                $('#finishedModal').modal('show');
            </script>
        </div> <!-- col-md6 newmut -->
    </div>
    </div>
<script>
<%@ include file="/jsp/multiplayer/game_highlighting.jsp" %>
</script>
<%@ include file="/jsp/multiplayer/footer_game.jsp" %>
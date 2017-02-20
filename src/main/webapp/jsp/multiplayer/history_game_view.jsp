<% String pageTitle="In Game"; %>
<%@ page import="org.codedefenders.multiplayer.MultiplayerGame" %>
<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<%
    // Get their user id from the session.
    int uid = ((Integer) session.getAttribute("uid")).intValue();
    int gameId = 0;
    try {
        try {
            gameId = Integer.parseInt(request.getParameter("id"));
            session.setAttribute("mpGameId", new Integer(gameId));
        } catch (NumberFormatException e) {
            gameId = ((Integer) session.getAttribute("mpGameId")).intValue();
        }
    } catch (Exception e2){
        response.sendRedirect("multiplayer/games/user");
    }
    boolean renderMutants = true;

    HashMap<Integer, ArrayList<Test>> linesCovered = new HashMap<Integer, ArrayList<Test>>();

    String codeDivName = "cut-div";

    MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);
    Role role = mg.getRole(uid);

    if ((!mg.getState().equals(GameState.FINISHED))) {
        response.sendRedirect("/games/user");
    }

    List<Test> tests = mg.getTests(); // get all executable tests

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
    ArrayList<Mutant> mutantsAlive = mg.getAliveMutants();

    ArrayList<Mutant> mutantsEquiv =  mg.getMutantsMarkedEquivalent();

    HashMap<Integer, ArrayList<Mutant>> mutantLines = new HashMap<Integer, ArrayList<Mutant>>();

    HashMap<Integer, ArrayList<Mutant>> mutantKilledLines = new HashMap<Integer, ArrayList<Mutant>>();

    ArrayList<Mutant> mutantsPending = new ArrayList<Mutant>(); // assume no pending tasks

    for (Mutant m : mutantsAlive) {
        for (int line : m.getLines()){
            if (!mutantLines.containsKey(line)){
                mutantLines.put(line, new ArrayList<Mutant>());
            }

            mutantLines.get(line).add(m);

        }
    }


    ArrayList<Mutant> mutantsKilled = mg.getKilledMutants();

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

    <%@ include file="/jsp/multiplayer/game_scoreboard.jsp" %>
<div class="crow fly no-gutter up">
    <% codeDivName = "newmut-div"; %>
    <div class="crow">
        <div class="w-45 up">
            <%@include file="/jsp/multiplayer/game_mutants.jsp"%>
            <%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
        </div>
        <div class="w-55" id="newmut-div">
                <h2>Class Under Test</h2>
                <input type="hidden" name="formType" value="createMutant">
                <input type="hidden" name="mpGameID" value="<%= mg.getId() %>" />
                <%
                    String mutantCode;
                    String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                    request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                    if (previousMutantCode != null) {
                        mutantCode = previousMutantCode;
                    } else
                        mutantCode = mg.getCUT().getAsString();
                %>
                <pre><textarea id="code" name="mutant" cols="80" rows="50" style="min-width: 512px;"><%= mutantCode %></textarea></pre>
            <script>
                var editorSUT = CodeMirror.fromTextArea(document.getElementById("code"), {
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
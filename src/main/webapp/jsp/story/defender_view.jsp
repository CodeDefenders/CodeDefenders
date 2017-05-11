<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<%@ page import="javax.xml.crypto.Data" %>
<% String pageTitle = "Story Mode"; %>
<%@ include file="/jsp/header_story.jsp" %>
<%
    String testCode;
    String previousTestCode = (String) request.getSession().getAttribute("previousTest");
    StoryClass userTestCode = DatabaseAccess.getUserPuzzleTest(puzzle.getPuzzleId(), uid);
    request.getSession().removeAttribute("previousTest");
    if (previousTestCode != null) {
        testCode = previousTestCode;
    } else if (userTestCode != null ){
        testCode = userTestCode.getAsString(); // get user's latest submission
    } else {
        testCode = puzzle.getPCUT().getTestTemplate();
    }

    StoryGame sg = (StoryGame) request.getSession().getAttribute("puzzle");

    String codeDivName = "cut-div";
    HashMap<Integer, ArrayList<PuzzleTest>> linesCovered = new HashMap<Integer, ArrayList<PuzzleTest>>();
    List<PuzzleTest> tests = puzzle.getPTests();

    for (PuzzleTest t : tests) {
        for (Integer lc : t.getLineCoverage().getLinesCovered()) {
            if (!linesCovered.containsKey(lc)) {
                linesCovered.put(lc, new ArrayList<PuzzleTest>());
            }

            linesCovered.get(lc).add(t);
        }

        String tc = "";
        for (String l : t.getHTMLReadout()) {
            tc += l + "\n";
        }
    }

    List<PuzzleMutant> mutantsAlive = puzzle.getAlivePMutants();
    Map<Integer, List<PuzzleMutant>> mutantLines = new HashMap<Integer, List<PuzzleMutant>>();

    for (PuzzleMutant m : mutantsAlive) {
        for (int line : m.getLines()) {
            if (!mutantLines.containsKey(line)) {
                mutantLines.put(line, new ArrayList<PuzzleMutant>());
            }

            mutantLines.get(line).add(m);
        }
    }
%>

<div class="row-fluid">
    <div class="col-md-6">
        <h2>Task
            <a href="#" class="btn btn-info btn-game btn-right" id="btnHelp" data-toggle="modal" data-target="#helpModal">Help!</a>
            <!-- Help Modal -->
            <div id="helpModal" class="modal fade" role="dialog">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">x</button>
                            <h4 class="modal-title">Help</h4>
                        </div>
                        <div class="modal-body">
                            <p><b>Task</b>: The 'Task' area will provide instructions and any details you need to know about the puzzle!</p>
                            <p><b>Class Under Test</b>: The 'Class Under Test' is the test you are going to test.<br>
                            The mutant icon is where the mutant has been created and where you should aim your test at.</p>
                            <p><b>Write a JUnit Here</b>: The 'Write a JUnit Here' is where you write your test code</p>
                            <p><b>Stuck?</b>: If you click this button, a hint will be provided to help you pass the puzzle if you are struggling</p>
                            <p><b>Defend!</b>: Once you are happy with your test case, click this button to compile your code and find out your results!</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
                        </div>
                    </div>
                </div>
            </div> <!-- End modal -->
        </h2>
        <h4><textarea readonly id="description" name="description" cols="88" rows="50" wrap="soft" style="font-family:'Ubuntu';height:500px;background-color: white; border: solid 2px grey; padding:10px;"><%= sg.getDesc() %></textarea></h4>
    </div>
    <div class="col-md-6">
        <h2>Original Test</h2>
        <pre class="readonly-pre"><textarea class="readonly-textarea" id="originalTest" name="originalTest" cols="80" rows="30"><%= puzzle.getPTest().getAsString() %></textarea></pre>
    </div>
</div>

<div class="row-fluid">
    <div class="col-md-6" id="cut-div">
        <h2>Class Under Test</h2>
        <pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" cols="80" rows="30"><%=puzzle.getPCUT().getAsString()%></textarea></pre>
    </div>
    <div class="col-md-6" id="utest-div">
        <form id="def" action="puzzles" method="post">
            <h2>Write a JUnit here
                <a href="#" class="btn btn-primary btn-game btn-right" id="btnHint" data-toggle="modal" data-target="#hintModal">Stuck?</a>
                <!-- Hint modal -->
                <div id="hintModal" class="modal fade" role="dialog">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                                <h4 class="modal-title">Hint</h4>
                            </div>
                            <div class="modal-body">
                                <p><%= sg.getHint() %></p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
                            </div>
                        </div><!-- /.modal-content -->
                    </div><!-- /.modal-dialog -->
                </div><!-- /.modal -->
            </h2>
            <pre><textarea id="code" name="test" cols="88" rows="30"><%= testCode %></textarea></pre>
            <input type="hidden" name="formType" value="createTest">
        </form>
        <input type="button" value="Defend!" class="btn btn-primary btn-game btn-right" form="def" onClick="this.form.submit(); this.disabled = true; this.value = 'Defending...';">
    </div>
</div>

<script>
    var editor = CodeMirror.fromTextArea(document.getElementById("code"), {
        lineNumbers: true,
        indentUnit: 4,
        indentWithTabs: true,
        matchBrackets: true,
        mode: "text/x-java"
    });
    editor.setSize("100%", 500);

    editor.on("viewportChange", function(){
        showMutants();
        showKilledMutants();
        highlightCoverage();
    });
    $(document).ready(function(){
        showMutants();
        showKilledMutants();
        highlightCoverage();
    });

    var editor2 = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        indentUnit: 4,
        indentWithTabs: true,
        matchBrackets: true,
        mode: "text/x-java"
    });

    editor2.setSize("100%", 500);

    var editor3 = CodeMirror.fromTextArea(document.getElementById("originalTest"), {
        lineNumbers: true,
        indentUnit: 4,
        indentWithTabs: true,
        matchBrackets: true,
        mode: "text/x-java"
    });

    editor3.setSize("100%", 500);

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

    highlightCoverage = function(){
        highlightLine([<% for (Integer i : linesCovered.keySet()){%>
            [<%=i%>, <%=((float)linesCovered.get(i).size() / (float) tests.size())%>],
            <% } %>], COVERED_COLOR, "<%="#" + codeDivName%>");
    };

    showMutants = function(){
        mutantLine([
            <% for (Integer line : mutantLines.keySet()) {
            %>
            [<%= line %>,
                <%= mutantLines.get(line).size() %>, [
                <% for(PuzzleMutant mm : mutantLines.get(line)){%>
                <%= mm.getMutantId() %>,
                <%
                    }
                    session.setAttribute("numberMutants", mutantLines.keySet().size());
                %>
            ]],
            <%
                } %>
        ],"<%="#" + codeDivName%>", false);
    };

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

</script>
<%@ include file="/jsp/footer_game.jsp" %>

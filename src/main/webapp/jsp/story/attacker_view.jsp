<%@ page import="org.codedefenders.*" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle="Story Mode"; %>
<%@ include file="/jsp/header_story.jsp" %>

<%
    StoryGame sg = (StoryGame) request.getSession().getAttribute("puzzle");

%>

<div class="row-fluid">
    <div class="col-lg-3" id="mutants-div">
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
                            <p><b>Test Class</b>: The 'Test Class' is the test that the mutant you are going to create will be tested against!<br>
                            It's to give you an idea on where to create the mutant to pass the puzzle!</p>
                            <p><b>Create a Mutant Here</b>: The 'Create a Mutant Here' is where you create your mutant</p>
                            <p><b>Stuck?</b>: If you click this button, a hint will be provided to help you pass the puzzle if you are struggling</p>
                            <p><b>Attack!</b>: Once you are happy with your mutant, click this button to compile your code and find out your results!</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
                        </div>
                    </div>
                </div>
            </div> <!-- End Help modal -->
        </h2>
        <h4><textarea readonly id="description" name="description" cols="80" rows="50" wrap="soft" style="font-family:'Ubuntu';height:500px;background-color:white;border:solid 2px grey;padding:10px;"><%=sg.getDesc()%></textarea></h4>
    </div> <!-- col-md6 mutants -->
    <div class="col-md-5" id="test-div">
        <h2>Test Class</h2>
        <pre class="readonly-pre"><textarea class="readonly-textarea" id="test" name="test" cols="80" rows="30"><%= puzzle.getPTest().getAsString() %></textarea></pre>
    </div>
    <div class="col-md-4" id="newmut-div">
        <form id="atk" action="puzzles" method="post">
            <h2>
                Create a Mutant Here
                <a href="#" class="btn btn-primary btn-game btn-right" id="btnHint" data-toggle="modal" data-target="#hintModal">Stuck?</a>
                <!-- Hint Modal -->
                <div id="hintModal" class="modal fade" role="dialog">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">x</button>
                                <h4 class="modal-title">Hint</h4>
                            </div>
                            <div class="modal-body">
                                <p><%=sg.getHint()%></p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div> <!-- End Hint modal -->
            </h2>
            <input type="hidden" name="formType" value="createMutant">
            <%
                String mutantCode;
                String previousMutantCode = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                StoryClass userPuzzleMutant = DatabaseAccess.getUserPuzzleMutant(puzzle.getPuzzleId(), uid);
                request.getSession().removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                if (previousMutantCode != null) {
                    mutantCode = previousMutantCode;
                } else if (userPuzzleMutant != null) {
                    mutantCode = userPuzzleMutant.getAsString(); // get user's latest submission
                } else {
                    mutantCode = puzzle.getPCUT().getAsString();
                }
            %>
            <pre><textarea id="code" name="mutant" cols="80" rows="50"><%= mutantCode %></textarea></pre>
        </form>
        <input type="button" class="btn btn-primary btn-game btn-right" value="Attack!" form="atk" onClick="this.form.submit(); this.disabled=true; this.value='Attacking...';">
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

    var editor2 = CodeMirror.fromTextArea(document.getElementById("test"), {
        lineNumbers: true,
        indentUnit: 4,
        indentWithTabs: true,
        matchBrackets: true,
        mode: "text/x-java"
    });
    editor2.setSize("100%", 500);

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

    function resetCode(textId, resetCode) {

        var textCode = document.getElementById(textId).value = '';
        textCode.value = resetCode;

    }

</script>
<%@ include file="/jsp/footer_game.jsp" %>
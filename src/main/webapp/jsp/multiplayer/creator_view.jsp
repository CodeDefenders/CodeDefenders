<%
    codeDivName = "cut-div";
%>
<div class="crow">
    <div class="w-45 up">
    <%@include file="/jsp/multiplayer/game_mutants.jsp"%>
    <%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
    </div>
    <div class="w-55"  id="cut-div">

        <h2>Class Under Test</h2>
		<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" cols="80" rows="30">
<%=mg.getCUT().getAsString()%>
		</textarea></pre>

        <div class="admin-panel">
            <h2>Admin</h2>
            <form id="adminEndBtn" action="<%=request.getContextPath() %>/multiplayer/move" method="post">

                    <button type="submit" class="btn btn-primary btn-game btn-right" form="adminEndBtn"
                            <% if (!mg.getState().equals(GameState.ACTIVE)) { %> disabled <% } %>>
                        End Game
                    </button>
                    <input type="hidden" name="formType" value="endGame">
                    <input type="hidden" name="mpGameID" value="<%= mg.getId() %>" />
            </form>
            <form id="adminStartBtn" action="<%=request.getContextPath() %>/multiplayer/move" method="post">
                    <button type="submit" class="btn btn-primary btn-game btn-right" form="adminStartBtn"
                            <% if (!mg.getState().equals(GameState.CREATED)) { %> disabled <% } %>>
                        Start Game
                    </button>
                    <input type="hidden" name="formType" value="startGame">
                    <input type="hidden" name="mpGameID" value="<%= mg.getId() %>" />
            </form>
        </div>
    </div>
    <div>
        <script>
            var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
                lineNumbers: true,
                matchBrackets: true,
                mode: "text/x-java",
                readOnly: true
            });
            editorSUT.setSize("100%", 500);

            /* Submitted tests */
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
    </div>
</div>

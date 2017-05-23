<%@ page import="org.codedefenders.PuzzleMode" %>
<%@ page import="org.codedefenders.story.StoryPuzzle" %>
<%@ page import="org.codedefenders.PuzzleClass" %>
<%@ page import="org.codedefenders.StoryClass" %>
%>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<%@ page import="javax.xml.crypto.Data" %>
<% String pageTitle = null; %>
<%@ include file ="/jsp/header.jsp" %>

<%
    int cid = (Integer) request.getAttribute("editClassId");
    StoryPuzzle details = DatabaseAccess.getLevelDetails(cid); // get list of levels
    List<String> unavailable = DatabaseAccess.getUnavailablePuzzles();
    StoryClass editCut = DatabaseAccess.getStoryForKey(cid);
%>

<div class="container-fluid">
    <h2>Editing '${classAlias}'</h2>
    <form id="formEdit" action="editpuzzles" class="form-upload form-horizontal" method="post">
        <input type="hidden" name="formType" value="sendEdit">
        <input type="hidden" name="editClassId" value="${editClassId}">
        <div class="form-group">
            <label class="control-label" for="levelNumber">Level:</label>
            <span>
                <select id="levelNumber" name="levelNumber" class="form-control" style="width:50px;">
                    <%
                        List<PuzzleClass> levels = DatabaseAccess.getLevels();
                        if (levels.isEmpty()) {
                    %>
                    <option>No levels added yet!</option>
                    <% } else {
                            // adding list of available level numbers to choose from
                            for (PuzzleClass l : levels) {
                                if (details.getLevelNum() == l.getLevelNum()) {
                    %>
                    <option selected><%= l.getLevelNum() %></option>
                    <% } else { %>
                    <option><%= l.getLevelNum() %></option>
                <%
                            }
                        }
                    }
                %>
                </select>
            </span>
        </div>
        <div class="form-group">
            <label class="control-label" for="puzzleNumber">Puzzle number:</label>
            <span>
                <%
                    String puzzleNum = String.valueOf(details.getPuzzleNum());
                    if (details.getPuzzleNum() == 0) {
                        puzzleNum = "";
                    }
                %>
                <input id="puzzleNumber" name="puzzleNumber" type="text" style="width:50px;" class="form-control" placeholder="<%= puzzleNum %>", value="<%= puzzleNum %>">
                <a href="#" class="btn btn-primary" id="btnPuzzles" data-toggle="modal" data-target="#puzzleModal">Check Unavailable Puzzle Numbers</a>
                <div id="puzzleModal" class="modal fade" role="dialog">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">x</button>
                                <h4 class="modal-title">Unavailable puzzle numbers</h4>
                            </div>
                            <div class="modal-body">
                                <p>
                                    <%
                                        for (String s : unavailable) {
                                    %>
                                            <%=s%>
                                    <%
                                        }
                                    %>
                                </p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div> <!-- End Unavailable puzzle modal -->
            </span>
            <input type="hidden" name="pNum" value="<%= details.getPuzzleNum() %>">
        </div>
        <div class="form-group">
            <label class="control-label" for="classAlias">Puzzle name:</label>
            <span>
                <%
                    // placeholder and leaving empty if database retrieved value is null
                    String puzzleText, puzzleNameText;
                    if (details.getPuzzleName() == null) {
                        puzzleText = "";
                        puzzleNameText = "Please enter a puzzle name";
                    } else {
                        puzzleText = details.getPuzzleName();
                        puzzleNameText = puzzleText;
                    }
                %>
                <input id="classAlias" name="classAlias" type="text" class="form-control" placeholder="<%= puzzleNameText %>" value="<%= puzzleText %>">
        </div>
        <!-- TODO: SQL injection -->
        <div class="form-group">
            <label class="control-label" for="puzzleHint">Hint:</label>
            <span>
                <%
                    // hint placeholder and display text
                    String hintText;
                    if (details.getHint() == null) {
                        hintText = "";
                    } else {
                        hintText = details.getHint();
                    }
                %>
                <pre><textarea id="puzzleHint" name="puzzleHint" rows="3" class="form-control" style="font-family:'Ubuntu';"><%= hintText %></textarea></pre>
            </span>
        </div>
        <div class="form-group">
            <label class="control-label" for="puzzleDesc">Description:</label>
            <%
                // description placeholder and display text
                String descText;
                if (details.getDesc() == null) {
                    descText = "";
                } else {
                    descText = details.getDesc();
                }
            %>
            <span>
                <pre><textarea id="puzzleDesc" name="puzzleDesc" rows="10" class="form-control" style="font-family:'Ubuntu';"><%= descText %></textarea></pre>
            </span>
        </div>
        <div class="form-group">
            <label class="control-label" for="pMode">Puzzle mode:</label>
            <span>
                <select id="pMode" name="pMode" class="form-control" style="width:150px;">
                    <%
                        if (details.getMode().equals(PuzzleMode.ATTACKER)) {
                    %>
                    <!--original was attacker -->
                    <option selected>Attacker</option>
                    <option>Defender</option>
                    <% } else { %>
                    <!-- origianl was defender -->
                    <option>Attacker</option>
                    <option selected>Defender</option>
                    <%
                        }
                    %>
                </select>
            </span>
        </div>
        <div class="form-group">
            <a class="btn btn-danger" href="story/mypuzzles" style="float:right;">Cancel</a>
            <input class="btn btn-primary" type="submit" value="Submit changes" onClick="getPuzzleMode()">
            <input type="hidden" id="puzzleMode" name="puzzleMode" value="">
        </div>
        <input type="hidden" id="puzzleId" name="puzzleId" value="<%=details.getPuzzleId()%>">
    </form>

    <script>
        // converts from String to State
        function getPuzzleMode() {
            var puzzleMode;
            var selected = document.getElementById("pMode");
            if (selected[selected.selectedIndex].value == "Attacker") {
                puzzleMode = "ATTACKER";
            } else {
                puzzleMode = "DEFENDER";
            }
            document.getElementById("puzzleMode").value = puzzleMode;
        }

    </script>

</div>


<%@ include file="/jsp/footer.jsp" %>
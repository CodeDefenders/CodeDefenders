<%@ page import="org.codedefenders.story.StoryGame" %>
<%@ page import="org.codedefenders.StoryState" %>
<%@ page import="org.codedefenders.PuzzleMode" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle="Select a puzzle"; %>
<%@ include file="/jsp/header.jsp" %>
    <%
        int uid = (Integer)request.getSession().getAttribute("uid");
        List<StoryGame> puzzles = DatabaseAccess.getPuzzles(); //list of puzzles
    %>

<div class="w-50">
<a href="/story/achievements">Achievements</a>
<a href="/puzzle/upload" style="float:right;">Add Puzzle</a>
    <span style="float:right;">&nbsp;|&nbsp;</span>
<a href="/story/mypuzzles" style="float:right;">Puzzle Editor</a>
<p></p>
<table class="table table-hover table-responsive table-paragraphs games-table">
    <tr>
        <th class="col-lg-1">Level</th>
        <th class="col-lg-1">Puzzle</th>
        <th class="col-lg-2">Puzzle name</th>
        <th class="col-lg-2">Points obtained</th>
        <th class="col-lg-2">Mode</th>
        <th class="col-lg-2">State</th>
        <th class="col-lg-2"></th>
    </tr>
    <%
    if (puzzles.isEmpty()) {
    %>
    <tr><td colspan="100%">No puzzles added yet!</td></tr>
    <%
    } else {
        boolean locked = false; // to assign locked status
        boolean unlocked = false; // to assign one after completed is not locked
        for (StoryGame p : puzzles ) {

            StoryState temp = p.getStoryState();
            String storyState;
            switch (temp) {
                case UNATTEMPTED: storyState = "Unattempted";
                    break;
                case LOCKED: storyState = "Locked";
                    break;
                case IN_PROGRESS: storyState = "In progress";
                    break;
                case COMPLETED: storyState = "Completed";
                    break;
                default: storyState = "Error";
                    break;
            }
            if (locked) {
                storyState = "Locked";
                p.updateState(StoryState.LOCKED);
            }

            // for repeat of completed levels
            if (!storyState.equals("Completed")) {
                if (unlocked) {
                    storyState = "Unattempted";
                    p.updateState(StoryState.UNATTEMPTED);
                }
            }

            PuzzleMode temp2 = p.getStoryMode();
            String storyMode;
            switch (temp2) {
                case ATTACKER: storyMode = "Attacker";
                    break;
                case DEFENDER: storyMode = "Defender";
                    break;
                default: storyMode = "Error";
                    break;
            }
            if (p.getPuzzleName() != null && p.getDesc() != null) {
                if (p.getId() == uid) {
    %>
    <tr>
        <td class="col-lg-1"><%= p.getLevelNum() %></td>
        <td class="col-lg-1"><%= p.getPuzzle() %></td>
        <td class="col-lg-2"><%= p.getPuzzleName() %></td>
        <td class="col-lg-2"><%= p.getPoints() %></td>
        <td class="col-lg-2"><%= storyMode %></td>
        <td class="col-lg-2"><%= storyState %></td>
        <td class="col-lg-2">
            <form id="view" action="puzzle" method="post">
                <input type="hidden" name="formType" value="enterPuzzle">
                <input type="hidden" name="puzzleId" value="<%= p.getPuzzleId() %>">
                <input type="hidden" name="puzzleName" value="<%= p.getPuzzleName() %>">
                <% if (locked) { %>
                    <input class="btn btn-primary disabled" type="submit" value="Enter Puzzle">
                <% } else { %>
                    <input class="btn btn-primary" type="submit" value="Enter Puzzle">
                <% }
                if (!storyState.equals("Completed")) {
                    locked = true;
                    unlocked = false;
                } else if (storyState.equals("Completed")) {
                    unlocked = true; // unlock next level
                }
                %>
            </form>
        </td>
    </tr>
    <%
                    }
            } // if puzzleName is not null
        } // for loop
    } // if puzzles aren't empty
    %>

</table>
</div>

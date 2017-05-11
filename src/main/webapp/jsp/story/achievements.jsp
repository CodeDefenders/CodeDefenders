<%@ page import="org.codedefenders.Achievements" %>
<%@ page import="org.codedefenders.util.DatabaseAccess" %>
<% String pageTitle = "Achievements"; %>
<%@ include file="/jsp/header.jsp" %>

<%
    int uid = (Integer) request.getSession().getAttribute("uid");
    int points = DatabaseAccess.getOverallPoints(uid);
    List<Achievements> achv = DatabaseAccess.getAchievements();
    int allPuzzles = DatabaseAccess.getAllPuzzleCount(uid);
    int allCompleted = DatabaseAccess.getCompletedPuzzleCount(uid);
    int progress = (allCompleted*100)/allPuzzles; // work out percentage progress
    int count = 0;
    for (Achievements a : achv) {
        if (a.getUserId() == uid && a.getAchieved() == 1) {
            count ++; // counts number of achievements user has obtained
        }
    }
%>

<div class="w-50">
    <a href="story/view">Back to Story Mode</a>
    <p></p>
    <table class="table table-hover table-responsive w-50">
        <tr>
            <th class="col-lg-4 control-label">Overall points:</th>
            <td class="col-lg-1"><%= points %></td>
        </tr>
        <tr>
            <th class="col-lg-4 control-label">Progress:</th>
            <td class="col-lg-1"><%= progress %>% (<%=allCompleted%>/<%=allPuzzles%>)</td>
        </tr>
        <tr>
            <th class="col-lg-4 control-label">Achievements unlocked:</th>
            <td class="col-lg-1"><%= count %></td>
        </tr>
        <tr>
            <th class="col-lg-4 control-label">Total achievements:</th>
            <td class="col-lg-1"><%= achv.size() %></td>
        </tr>
    </table>
    <table class="table table-hover table-responsive table-paragraphs games-table">
        <tr>
            <th class="col-lg-4">Achievement</th>
            <th class="col-lg-6">Description</th>
            <th class="col-lg-2"></th>
        </tr>
        <%
            if (achv.isEmpty()) { %>
                <tr><td colspan="100%">No Achievements added yet!</td></tr>
            <% } else {
                for (Achievements a : achv) {
                    if (a.getUserId() == uid && a.getAchieved() == 1) {
            %>
                    <tr>
                        <td class="col-lg-4"><%= a.getAchvName() %></td>
                        <td class="col-lg-6"><%= a.getAchvDesc() %></td>
                        <td class="col=lg-2"><img style="height:15%;width:15%;"src="images/achieved.png"></td>
                    </tr>
            <%
                    } else {
            %>
                    <tr>
                        <td class="col-lg-4" style="color:#666666;"><%= a.getAchvName() %></td>
                        <td class="col-lg-6" style="color:#666666;"><%= a.getAchvDesc() %></td>
                        <td class="col-lg-2"></td>
                    </tr>

        <%
                    }
                }
            }
        %>
    </table>
</div>
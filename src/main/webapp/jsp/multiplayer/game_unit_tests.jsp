<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="org.codedefenders.game.Mutant" %>
<%@ page import="java.util.Set" %>

<% if (role.equals(Role.DEFENDER) || role.equals(Role.CREATOR) || mg.getLevel().equals(GameLevel.EASY) || mg.getState().equals(GameState.FINISHED)){
%>
<div class="ws-12">
    <h3>JUnit tests </h3>
    <div class="slider single-item">
        <%
            for (Test t : tests) {
                String tc = "";
                for (String l : t.getHTMLReadout()) { tc += l + "\n"; }
                User creator = DatabaseAccess.getUserFromPlayer(t.getPlayerId());
                final Set<Mutant> coveredMutants = t.getCoveredMutants();
                final Set<Mutant> killedMutants = t.getKilledMutants();
        %>
        <div>
            <ul>
                <li style=" display: inline-block; "><h4>Test <%= t.getId() %>
                </h4></li>
                <li style=" display: inline-block; "><h4> | Creator: <%= creator.getUsername() %>
                   <!-- [UID: <%= creator.getId() %>] -->
                </h4></li>
                <li style=" display: inline-block;"><h4> |
                    <% if(!coveredMutants.isEmpty()) { %>
                        <a href="javascript:void(0);" data-toggle="tooltip" title="<%= coveredMutants.stream().map(mutant-> String.valueOf(mutant.getId())).collect(Collectors.joining(", ")) %>">Covered: <%= coveredMutants.size()%></a>
                    <% } else { %>
                        Covered: 0
                    <% } %>
                </h4></li>
                <li style=" display: inline-block;"><h4> |
                    <% if(!killedMutants.isEmpty()) { %>
                        <a href="javascript:void(0);" data-toggle="tooltip" title="<%= killedMutants.stream().map(mutant-> String.valueOf(mutant.getId())).collect(Collectors.joining(", ")) %>">Killed: <%= killedMutants.size()%></a>
                    <% } else { %>
                        Killed: 0
                <% } %>
                </h4></li>
                <li style=" display: inline-block;"><h4> | Points: <%= t.getScore() %>
                </h4></li>

            </ul>
            <pre class="readonly-pre"><textarea class="utest" cols="20" rows="10"><%=tc%></textarea></pre>
        </div>
        <%
            }
            if (tests.isEmpty()) {%>
        <div><h3></h3><p> There are currently no tests </p></div>
        <%}
        %>
    </div> <!-- slider single-item -->
</div> <!-- col-md-6 left bottom -->
<% } %>
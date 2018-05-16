<%@ page import="org.codedefenders.database.DatabaseAccess" %>
<%@ page import="org.codedefenders.game.GameLevel" %>
<%@ page import="org.codedefenders.game.GameState" %>
<%@ page import="org.codedefenders.game.Role" %>
<%@ page import="org.codedefenders.game.Test" %>
<%@ page import="org.codedefenders.model.User" %>
<%@ page import="java.util.stream.Collectors" %>

<% if (role.equals(Role.DEFENDER) || role.equals(Role.CREATOR) || mg.getLevel().equals(GameLevel.EASY) || mg.getState().equals(GameState.FINISHED)){
%>
<div class="ws-12">
    <h2> JUnit tests </h2>
    <div class="slider single-item">
        <%
            for (Test t : tests) {
                String tc = "";
                for (String l : t.getHTMLReadout()) { tc += l + "\n"; }
                User creator = DatabaseAccess.getUserFromPlayer(t.getPlayerId());
        %>
        <div>
            <ul>
                <li style=" display: inline-block; "><h4>Test <%= t.getId() %>
                </h4></li>
                <li style=" display: inline-block; "><h4> | Creator: <%= creator.getUsername() %>
                    [UID: <%= creator.getId() %>] </h4></li>
                <li style=" display: inline-block; float:right"><h4>points: <%= t.getScore() %>
                </h4></li>
            <% if(!t.getCoveredMutants().isEmpty()) { %>
                <br/><li style=" display: inline-block;"><h4>Covered mutants: <%= t.getCoveredMutants().stream().map(Mutant::getId).map(Object::toString).collect(Collectors.joining(", ")) %>
            </li></h4>
            <% } %>
            <% if(!t.getKilledMutants().isEmpty()) { %>
                <br/><li style=" display: inline-block;"><h4>Killed mutants: <%= t.getKilledMutants().stream().map(Mutant::getId).map(Object::toString).collect(Collectors.joining(", ")) %>
            </li></h4>
            <% } %>
            </ul>
            <pre class="readonly-pre"><textarea class="utest" cols="20" rows="10"><%=tc%></textarea></pre>
        </div>
        <%
            }
            if (tests.isEmpty()) {%>
        <div><h2></h2><p> There are currently no tests </p></div>
        <%}
        %>
    </div> <!-- slider single-item -->
</div> <!-- col-md-6 left bottom -->
<% } %>
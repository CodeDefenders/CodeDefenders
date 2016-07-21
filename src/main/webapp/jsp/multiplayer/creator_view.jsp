<div class="crow">
    <div class="w-45 up">
        <%@include file="/jsp/multiplayer/game_mutants.jsp"%>
        <%@include file="/jsp/multiplayer/game_unit_tests.jsp"%>
    </div>
    <div class="w-55 up">
        <h2>Class Under Test</h2>
        <%
            String cutText = mg.getCUT().getAsString();
        %>
        <pre><%= cutText %></pre>
    </div>
</div>
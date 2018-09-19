<%--
    Parameters:
    AbstractGame game
--%>

<%@ page import="org.codedefenders.game.AbstractGame" %>

<% { %>

<%-- TODO Rename to "game" once the global game vairable has been removed --%>
<% AbstractGame gameTODORENAME = (AbstractGame) request.getAttribute("game");  %>

<h3>Class Under Test</h3>
<pre class="readonly-pre">
    <textarea class="readonly-textarea" id="sut" name="cut" cols="80" rows="30">
        <%= gameTODORENAME.getCUT().getAsString() %>
    </textarea>
</pre>

<script>
    var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        matchBrackets: true,
        mode: "text/x-java",
        readOnly: true
    });
    editorSUT.setSize("100%", 500);
</script>

<% } %>

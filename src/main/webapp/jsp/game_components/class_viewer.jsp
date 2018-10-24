<%--
    Displays the class code in a read-only CodeMirror textarea.

    @param String classCode
        The source code of the class to display.
--%>

<% { %>

<%
    String classCode = (String) request.getAttribute("classCode");
%>

<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" title="cut" cols="80" rows="30"><%= classCode %></textarea></pre>

<script>
    var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        matchBrackets: true,
        mode: "text/x-java",
        readOnly: true,
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });
    editorSUT.setSize("100%", 500);
</script>

<% } %>

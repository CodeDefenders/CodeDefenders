<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %><%--
    Displays the class code for a class under test and its dependencies in a read-only CodeMirror textarea.

    @param String classCode
        The source code of the class to display.
    @param Map<String, String> dependencies
        The source code of the dependencies to display.
--%>

<%
{
    final String className = (String) request.getAttribute("className");
    final String classCode = (String) request.getAttribute("classCode");

    final Map<String, String> dependencies = (HashMap<String, String>) request.getAttribute("dependencies");
%>
<%
    if (!dependencies.isEmpty()) {
%>
<script>
    classes = {};
    classes['<%=className%>'] = '<%=StringEscapeUtils.escapeJavaScript(classCode)%>'
</script>
<div class="nav nav-tabs">
    <button class="tab-link" onclick="updateEditor(event, '<%=className%>')" id="defaultDisplayedClass"><%=className%></button>
<%
        for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
%>
    <button class="tab-link" onclick="updateEditor(event, '<%=dependency.getKey()%>')"><%=dependency.getKey()%></button>
    <script>
        classes['<%=dependency.getKey()%>'] = '<%=StringEscapeUtils.escapeJavaScript(dependency.getValue())%>'
    </script>
<%
        }
%>
</div>

<script>
    var updateEditor = function(event, className) {
        editorSUT.setValue(classes[className]);
        editorSUT.save();
    }
</script>

<%
    }
%>
<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" title="cut" cols="80"
    rows="30"><%=classCode%></textarea></pre>
<script>
    var editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        matchBrackets: true,
        mode: "text/x-java",
        readOnly: true,
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });
    editorSUT.setSize("100%", 500);

    document.getElementById("defaultDisplayedClass").click();
</script>
<%
}
%>


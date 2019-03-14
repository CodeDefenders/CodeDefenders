<%--

    Copyright (C) 2016-2019 Code Defenders contributors

    This file is part of Code Defenders.

    Code Defenders is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Code Defenders is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%--
    Displays the class code for a class under test and its dependencies in a read-only CodeMirror textarea.

    @param String classCode
        The source code of the class to display, but not {@code null}.
    @param String className
        The class name of the CUT, but not {@code null}.
    @param Map<String, String> dependencies
        A mapping between a class name and the content of the CUT dependencies.
        Can be empty, but must not be {@code null}.
--%>

<%
{
    final String className = (String) request.getAttribute("className");
    final String classCode = (String) request.getAttribute("classCode");

    final Map<String, String> dependencies = (HashMap<String, String>) request.getAttribute("dependencies");
%>
<%
    if (dependencies.isEmpty()) { // no dependencies -> no tabs
%>
<pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" title="cut" cols="80"
                                    rows="30"><%=classCode%></textarea></pre>
<script>
    let editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        matchBrackets: true,
        mode: "text/x-java",
        readOnly: true,
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });
    editorSUT.setSize("100%", 500);

    autocompletedClasses = {
        '<%=className%>': editorSUT.getTextArea().value
    }
</script>
<%
    } else { // dependencies exist -> tab system
%>
<div>
    <ul class="nav nav-tabs">
        <li role="presentation" class="active"><a href="#<%=className%>" aria-controls="<%=className%>" role="tab" data-toggle="tab"><%=className%></a></li>
        <%
            for (String depName : dependencies.keySet()) {
        %>
        <li role="presentation"><a href="#<%=depName%>" aria-controls="<%=depName%>" role="tab" data-toggle="tab"><%=depName%></a></li>
        <%
            }
        %>
    </ul>

    <div class="tab-content">
        <div role="tabpanel" class="tab-pane active" id="<%=className%>" data-toggle="tab">
            <pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" title="cut" cols="80"
                                                rows="30"><%=classCode%></textarea></pre>
            <script>
                let editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
                    lineNumbers: true,
                    matchBrackets: true,
                    mode: "text/x-java",
                    readOnly: true,
                    gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
                });
                editorSUT.setSize("100%", 457); // next to the test editor the cm editor would be too big

                autocompletedClasses = {
                    '<%=className%>': editorSUT.getTextArea().value
                }
            </script>
        </div>
        <%
            for (Map.Entry<String, String> dependency : dependencies.entrySet()) {
                String depName = dependency.getKey();
                String depCode = dependency.getValue();
        %>
        <div role="tabpanel" class="tab-pane active hideAfterRendering" id="<%=depName%>" data-toggle="tab">
            <pre class="readonly-pre"><textarea class="readonly-textarea" id="text-<%=depName%>"
                                                name="text-<%=depName%>"
                                                title="text-<%=depName%>" cols="80"
                                                rows="30"><%=depCode%></textarea></pre>
            <script>
                let editor<%=depName%> = CodeMirror.fromTextArea(document.getElementById("text-<%=depName%>"), {
                    lineNumbers: true,
                    matchBrackets: true,
                    mode: "text/x-java",
                    readOnly: true
                });
                editor<%=depName%>.setSize("100%", 457); // next to the test editor the cm editor would be too big
                editor<%=depName%>.refresh();

                Object.assign(autocompletedClasses, {
                    '<%=depName%>': editor<%=depName%>.getTextArea().value
                });
            </script>
        </div>
        <%
            }
        %>
    </div>
    <script>
        <%-- Without the hideAfterRendering class attribute the editor is only rendered
             when the editor is displayed and actively clicked on.--%>
        $('.hideAfterRendering').each(function () {
            $(this).removeClass('active')
        });
    </script>
</div>
<%
    }
}
%>


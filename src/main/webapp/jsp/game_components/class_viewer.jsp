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
<%@ page import="java.util.Map" %>

<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>

<link href="css/codemirror_customize.css" rel="stylesheet" type="text/css" />


<%-- no dependencies -> no tabs --%>
<% if (!classViewer.hasDependencies()) { %>

    <pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" title="cut" cols="80"
                                        rows="30">${classViewer.classCode}</textarea></pre>

<%-- dependencies exist -> tab system --%>
<% } else { %>

    <div>
        <ul class="nav nav-tabs">
            <li role="presentation" class="active"><a href="#${classViewer.className}" aria-controls="${classViewer.className}" role="tab" data-toggle="tab">${classViewer.className}</a></li>
            <% for (String depName : classViewer.getDependencies().keySet()) { %>
                <li role="presentation"><a href="#<%=depName%>" aria-controls="<%=depName%>" role="tab" data-toggle="tab"><%=depName%></a></li>
            <% } %>
        </ul>

        <div class="tab-content">
            <div role="tabpanel" class="tab-pane active" id="${classViewer.className}" data-toggle="tab">
                <pre class="readonly-pre"><textarea class="readonly-textarea" id="sut" name="cut" title="cut" cols="80"
                                                    rows="30">${classViewer.classCode}</textarea></pre>
            </div>
            <% for (Map.Entry<String, String> dependency : classViewer.getDependencies().entrySet()) {
                    String depName = dependency.getKey();
                    String depCode = dependency.getValue(); %>
                <div role="tabpanel" class="tab-pane active hideAfterRendering" id="<%=depName%>" data-toggle="tab">
                    <pre class="readonly-pre"><textarea class="readonly-textarea" id="text-<%=depName%>"
                                                        name="text-<%=depName%>"
                                                        title="text-<%=depName%>" cols="80"
                                                        rows="30"><%=depCode%></textarea></pre>
                </div>
            <% } %>
        </div>
    </div>

<% } %>



<script>
(function () {

    let editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        matchBrackets: true,
        mode: "text/x-java",
        readOnly: true,
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons']
    });
    editorSUT.setSize("100%", 500);

    /* If global autocompletedClasses exists, get it, otherwise, create it. */
    const autocompletedClasses = window.autocompletedClasses = window.autocompletedClasses || {};
    autocompletedClasses['${classViewer.className}'] = editorSUT.getTextArea().value;

    <%-- dependencies exist -> tab system --%>
    <% if (classViewer.hasDependencies()) { %>

        <% for (Map.Entry<String, String> dependency : classViewer.getDependencies().entrySet()) {
                String depName = dependency.getKey(); %>
            let editor<%=depName%> = CodeMirror.fromTextArea(document.getElementById("text-<%=depName%>"), {
                lineNumbers: true,
                matchBrackets: true,
                mode: "text/x-java",
                readOnly: true
            });
            editor<%=depName%>.setSize("100%", 457); // next to the test editor the cm editor would be too big
            editor<%=depName%>.refresh();

            autocompletedClasses['<%=depName%>'] =  editor<%=depName%>.getTextArea().value;
        <% } %>

    <% } %>

    <%-- Without the hideAfterRendering class attribute the editor is only rendered
         when the editor is displayed and actively clicked on.--%>
    $('.hideAfterRendering').each(function () {
        $(this).removeClass('active')
    });

})();
</script>


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

<div class="card codemirror-card">

    <%-- no dependencies -> no tabs --%>
    <% if (!classViewer.hasDependencies()) { %>

        <div class="card-body p-0">
            <pre class="m-0"><textarea id="sut" name="cut" title="cut" readonly>${classViewer.classCode}</textarea></pre>
        </div>

    <%-- dependencies exist -> tab system --%>
    <% } else { %>

        <div class="card-header">
            <ul class="nav nav-pills nav-fill gap-1" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link px-2 py-1 active" data-bs-toggle="tab"
                            id="${classViewer.className}-tab"
                            data-bs-target="#${classViewer.className}"
                            aria-controls="${classViewer.className}"
                            type="button" role="tab" aria-selected="true">
                        ${classViewer.className}
                    </button>
                </li>
                <% for (String depName : classViewer.getDependencies().keySet()) { %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link px-2 py-1" data-bs-toggle="tab"
                                id="<%=depName%>-tab"
                                data-bs-target="#<%=depName%>"
                                aria-controls="<%=depName%>"
                                type="button" role="tab" aria-selected="true">
                            <%=depName%>
                        </button>
                    </li>
                <% } %>
            </ul>
        </div>

        <div class="card-body p-0">
            <div class="tab-content">
                <div class="tab-pane active"
                     id="${classViewer.className}"
                     aria-labelledby="${classViewer.className}-tab"
                     role="tabpanel">
                    <pre class="m-0"><textarea id="sut" name="cut" title="cut" readonly>${classViewer.classCode}</textarea></pre>
                </div>
                <% for (Map.Entry<String, String> dependency : classViewer.getDependencies().entrySet()) {
                        String depName = dependency.getKey();
                        String depCode = dependency.getValue(); %>
                    <div class="tab-pane"
                         id="<%=depName%>"
                         aria-labelledby="<%=depName%>-tab"
                         role="tabpanel">
                         <pre class="m-0"><textarea id="text-<%=depName%>"
                                                    name="text-<%=depName%>"
                                                    title="text-<%=depName%>"
                                                    readonly><%=depCode%></textarea></pre>
                    </div>
                <% } %>
            </div>
        </div>

    <% } %>

    <div class="card-footer">
        <jsp:include page="/jsp/game_components/mutant_explanation.jsp"/>
    </div>

</div>


<script>
(function () {

    let editorSUT = CodeMirror.fromTextArea(document.getElementById("sut"), {
        lineNumbers: true,
        matchBrackets: true,
        mode: "text/x-java",
        readOnly: 'nocursor',
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-mutantIcons'],
        autoRefresh: true
    });

    /* If global autocompletedClasses exists, get it, otherwise, create it. */
    const autocompletedClasses = window.autocompletedClasses = window.autocompletedClasses || {};
    autocompletedClasses['${classViewer.className}'] = editorSUT.getTextArea().value;

    <%-- dependencies exist -> tab system --%>
    <% if (classViewer.hasDependencies()) { %>

        <% for (Map.Entry<String, String> dependency : classViewer.getDependencies().entrySet()) {
                String depName = dependency.getKey(); %>
            let editor = CodeMirror.fromTextArea(document.getElementById("text-<%=depName%>"), {
                lineNumbers: true,
                matchBrackets: true,
                mode: "text/x-java",
                readOnly: 'nocursor',
                autoRefresh: true
            });

            autocompletedClasses['<%=depName%>'] =  editor.getTextArea().value;
        <% } %>

    <% } %>

})();
</script>

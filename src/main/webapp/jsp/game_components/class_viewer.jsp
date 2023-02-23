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
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<jsp:useBean id="classViewer" class="org.codedefenders.beans.game.ClassViewerBean" scope="request"/>

<div class="card game-component-resize">

    <%-- no dependencies -> no tabs --%>
    <%
        if (!classViewer.hasDependencies()) {
    %>

        <div class="card-body p-0 codemirror-fill loading">
            <pre class="m-0"><textarea id="sut" name="cut" title="cut" readonly>${classViewer.classCode}</textarea></pre>
        </div>

    <%-- dependencies exist -> tab system --%>
    <%
        } else {
            int currentId = 0;
    %>

        <div class="card-header">
            <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link py-1 active" data-bs-toggle="tab"
                            id="class-viewer-tab-<%=currentId%>"
                            data-bs-target="#class-viewer-pane-<%=currentId%>"
                            aria-controls="class-viewer-pane-<%=currentId%>"
                            type="button" role="tab" aria-selected="true">
                        ${classViewer.className}
                    </button>
                </li>
                <%
                    for (String depName : classViewer.getDependencies().keySet()) {
                        currentId++;
                %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link py-1" data-bs-toggle="tab"
                                id="class-viewer-tab-<%=currentId%>"
                                data-bs-target="#class-viewer-pane-<%=currentId%>"
                                aria-controls="class-viewer-pane-<%=currentId%>"
                                type="button" role="tab" aria-selected="true">
                            <%=depName%>
                        </button>
                    </li>
                <%
                    }
                %>
            </ul>
        </div>

        <%
            currentId = 0;
        %>

        <div class="card-body p-0 codemirror-fill loading">
            <div class="tab-content">
                <div class="tab-pane active"
                     id="class-viewer-pane-<%=currentId%>"
                     aria-labelledby="class-viewer-tab-<%=currentId%>"
                     role="tabpanel">
                    <pre class="m-0"><textarea id="sut" name="cut" title="cut" readonly>${classViewer.classCode}</textarea></pre>
                </div>

                <%
                    for (String depCode : classViewer.getDependencies().values()) {
                        currentId++;
                %>
                    <div class="tab-pane"
                         id="class-viewer-pane-<%=currentId%>"
                         aria-labelledby="class-viewer-tab-<%=currentId%>"
                         role="tabpanel">
                             <pre class="m-0"><textarea id="class-viewer-code-<%=currentId%>"
                                                        name="class-viewer-code-<%=currentId%>"
                                                        title="class-viewer-code-<%=currentId%>"
                                                        readonly><%=depCode%></textarea></pre>
                    </div>
                <%
                    }
                %>
            </div>
        </div>

    <% } %>

    <div class="card-footer">
        <jsp:include page="/jsp/game_components/mutant_explanation.jsp"/>
    </div>

</div>

<script type="module">
    import {objects} from '${url.forPath("/js/codedefenders_main.mjs")}';
    import {ClassViewer} from '${url.forPath("/js/codedefenders_game.mjs")}';


    const numDependencies = ${classViewer.hasDependencies() ? classViewer.dependencies.size() : 0};

    const editorElement = document.getElementById('sut');
    const dependencyEditorElements = [];
    for (let i = 1; i <= numDependencies; i++) {
        dependencyEditorElements.push(document.getElementById(`class-viewer-code-\${i}`));
    }

    const classViewer = new ClassViewer(editorElement, dependencyEditorElements);


    objects.register('classViewer', classViewer);
</script>

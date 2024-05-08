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

<%--
    Displays the mutant code in a CodeMirror textarea.
--%>

<jsp:useBean id="login" type="org.codedefenders.auth.CodeDefendersAuth" scope="request"/>
<jsp:useBean id="mutantEditor" class="org.codedefenders.beans.game.MutantEditorBean" scope="request"/>

<div class="card game-component-resize">

    <%-- no dependencies -> no tabs --%>
    <%
        if (!mutantEditor.hasDependencies()) {
    %>

        <div class="card-body p-0 codemirror-fill loading">
            <pre class="m-0"><textarea id="mutant-code" name="mutant" title="mutant">${mutantEditor.mutantCode}</textarea></pre>
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
                            id="mutant-editor-tab-<%=currentId%>"
                            data-bs-target="#mutant-editor-pane-<%=currentId%>"
                            aria-controls="mutant-editor-pane-<%=currentId%>"
                            type="button" role="tab" aria-selected="true">
                        ${mutantEditor.className}
                    </button>
                </li>
                <%
                    for (String depName : mutantEditor.getDependencies().keySet()) {
                        currentId++;
                %>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link py-1" data-bs-toggle="tab"
                                id="mutant-editor-tab-<%=currentId%>"
                                data-bs-target="#mutant-editor-pane-<%=currentId%>"
                                aria-controls="mutant-editor-pane-<%=currentId%>"
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
                     id="mutant-editor-pane-<%=currentId%>"
                     aria-labelledby="mutant-editor-tab-<%=currentId%>"
                     role="tabpanel">
                    <pre class="m-0"><textarea id="mutant-code" name="mutant" title="mutant">${mutantEditor.mutantCode}</textarea></pre>
                </div>
                <%
                    for (String depCode : mutantEditor.getDependencies().values()) {
                        currentId++;
                %>
                    <div class="tab-pane"
                         id="mutant-editor-pane-<%=currentId%>"
                         aria-labelledby="mutant-editor-tab-<%=currentId%>"
                         role="tabpanel">
                             <pre class="m-0"><textarea id="mutant-editor-code-<%=currentId%>"
                                                        name="mutant-editor-code-<%=currentId%>"
                                                        title="mutant-editor-code-<%=currentId%>"
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
    import {MutantEditor} from '${url.forPath("/js/codedefenders_game.mjs")}';


    const editableLinesStart = ${mutantEditor.hasEditableLinesStart() ? mutantEditor.editableLinesStart : "null"};
    const editableLinesEnd = ${mutantEditor.hasEditableLinesEnd() ? mutantEditor.editableLinesEnd : "null"};
    const keymap = '${login.user.keyMap.CMName}';
    const numDependencies = ${mutantEditor.hasDependencies() ? mutantEditor.dependencies.size() : 0};

    const editorElement = document.getElementById('mutant-code');
    const dependencyEditorElements = [];
    for (let i = 1; i <= numDependencies; i++) {
        dependencyEditorElements.push(document.getElementById(`mutant-editor-code-\${i}`));
    }

    const mutantEditor = new MutantEditor(
            editorElement,
            dependencyEditorElements,
            editableLinesStart,
            editableLinesEnd,
            keymap);


    objects.register('mutantEditor', mutantEditor);
</script>

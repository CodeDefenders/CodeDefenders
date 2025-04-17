<%--

    Copyright (C) 2016-2025 Code Defenders contributors

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
<%--@elvariable id="login" type="org.codedefenders.auth.CodeDefendersAuth"--%>
<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="mutantEditor" type="org.codedefenders.beans.game.MutantEditorBean"--%>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%--
    Displays the mutant code in a CodeMirror textarea.
--%>

<div class="card game-component-resize">
    <c:choose>
        <c:when test="${!mutantEditor.hasDependencies()}">
            <%-- no dependencies -> no tabs --%>
            <div class="card-body p-0 codemirror-fill loading">
                <pre class="m-0"><textarea id="mutant-code" name="mutant" title="mutant">${mutantEditor.mutantCode}</textarea></pre>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card-header">
                <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link py-1 active" data-bs-toggle="tab"
                                id="mutant-editor-tab-0"
                                data-bs-target="#mutant-editor-pane-0"
                                aria-controls="mutant-editor-pane-0"
                                type="button" role="tab" aria-selected="true">
                            ${mutantEditor.className}
                        </button>
                    </li>
                    <c:forEach var="depName" items="${mutantEditor.dependencies.keySet()}" varStatus="s">
                        <c:set var="id" value="${s.index + 1}"/>

                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1" data-bs-toggle="tab"
                                    id="mutant-editor-tab-${id}"
                                    data-bs-target="#mutant-editor-pane-${id}"
                                    aria-controls="mutant-editor-pane-${id}"
                                    type="button" role="tab" aria-selected="true">
                                ${depName}
                            </button>
                        </li>
                    </c:forEach>
                </ul>
            </div>

            <div class="card-body p-0 codemirror-fill loading">
                <div class="tab-content">
                    <div class="tab-pane active"
                         id="mutant-editor-pane-0"
                         aria-labelledby="mutant-editor-tab-0"
                         role="tabpanel">
                        <pre class="m-0"><textarea id="mutant-code" name="mutant" title="mutant">${mutantEditor.mutantCode}</textarea></pre>
                    </div>
                    <c:forEach var="depCode" items="${mutantEditor.dependencies.values()}" varStatus="s">
                        <c:set var="id" value="${s.index + 1}"/>

                        <div class="tab-pane"
                             id="mutant-editor-pane-${id}"
                             aria-labelledby="mutant-editor-tab-${id}"
                             role="tabpanel">
                                 <pre class="m-0"><textarea id="mutant-editor-code-${id}"
                                                            name="mutant-editor-code-${id}"
                                                            title="mutant-editor-code-${id}"
                                                            readonly>${depCode}</textarea></pre>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:otherwise>
    </c:choose>

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
    const readonly = Boolean(${mutantEditor.readonly ? 1 : 0});

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
            keymap,
            readonly);


    objects.register('mutantEditor', mutantEditor);
</script>

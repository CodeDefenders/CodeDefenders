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
<%--@elvariable id="classViewer" type="org.codedefenders.beans.game.ClassViewerBean"--%>


<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<div class="card game-component-resize">

    <c:choose>
        <c:when test="${!classViewer.hasDependencies()}">
            <%-- no dependencies -> no tabs --%>
            <div class="card-body p-0 codemirror-fill loading">
                <pre class="m-0"><textarea id="sut" name="cut" title="cut" readonly>${classViewer.classCode}</textarea></pre>
            </div>
        </c:when>
        <c:otherwise>
            <%-- dependencies exist -> tab system --%>
            <div class="card-header">
                <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link py-1 active" data-bs-toggle="tab"
                                id="class-viewer-tab-0"
                                data-bs-target="#class-viewer-pane-0"
                                aria-controls="class-viewer-pane-0"
                                type="button" role="tab" aria-selected="true">
                                ${classViewer.className}
                        </button>
                    </li>
                    <c:forEach var="depName" items="${classViewer.dependencies.keySet()}" varStatus="s">
                        <c:set var="id" value="${s.index + 1}"/>

                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1" data-bs-toggle="tab"
                                    id="class-viewer-tab-${id}"
                                    data-bs-target="#class-viewer-pane-${id}"
                                    aria-controls="class-viewer-pane-${id}"
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
                         id="class-viewer-pane-0"
                         aria-labelledby="class-viewer-tab-0"
                         role="tabpanel">
                        <pre class="m-0"><textarea id="sut" name="cut" title="cut" readonly>${classViewer.classCode}</textarea></pre>
                    </div>

                    <c:forEach var="depCode" items="${classViewer.dependencies.values()}" varStatus="s">
                        <c:set var="id" value="${s.index + 1}"/>

                        <div class="tab-pane"
                             id="class-viewer-pane-${id}"
                             aria-labelledby="class-viewer-tab-${id}"
                             role="tabpanel">
                                 <pre class="m-0"><textarea id="class-viewer-code-${id}"
                                                            name="class-viewer-code-${id}"
                                                            title="class-viewer-code-${id}"
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

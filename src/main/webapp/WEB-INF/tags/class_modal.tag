<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="classViewer" type="org.codedefenders.beans.game.ClassViewerBean"--%>

<%@ attribute name="classId" required="true" %>
<%@ attribute name="classAlias" required="true" %>
<%@ attribute name="htmlId" required="true" %>
<%@ tag import="org.codedefenders.util.FileUtils" %>
<%
    request.setAttribute("number_of_dependencies", FileUtils.getNumberOfDependencies(Integer.parseInt(classId)));//TODO
%>

<div>
    <t:modal title="${classAlias}" id="${htmlId}"
             modalDialogClasses="modal-dialog-responsive"
             modalBodyClasses="loading loading-bg-gray loading-height-200">

        <jsp:attribute name="content">
                    <div class="card">
                        <div class="card-header">
                            <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                                <li class="nav-item" role="presentation">
                                    <button class="nav-link py-1 active" data-bs-toggle="tab"
                                            id="cut-header-${classId}"
                                            data-bs-target="#cut-body-${classId}"
                                            aria-controls="cut-body-${classId}"
                                            type="button" role="tab" aria-selected="true">
                                    </button>
                                </li>
                                <c:forEach var="i" begin="1" end="${number_of_dependencies}">
                                    <li class="nav-item" role="presentation">
                                        <button class="nav-link py-1" data-bs-toggle="tab"
                                                id="class-header-${classId}-${i}"
                                                data-bs-target="#class-body-${classId}-${i}"
                                                aria-controls="class-body-${classId}-${i}"
                                                type="button" role="tab" aria-selected="false">
                                        </button>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                        <div class="card-body p-0 codemirror-expand codemirror-class-modal-size">
                            <div class="tab-content">
                                <div class="tab-pane active"
                                     id="cut-body-${classId}"
                                     aria-labelledby="cut-header-${classId}"
                                     role="tabpanel">
                                    <pre class="m-0"><textarea id="sut" name="cut" title="cut" readonly></textarea></pre>
                                </div>
                                <c:forEach var="i" begin="1" end="${number_of_dependencies}">
                                    <div class="tab-pane"
                                         id="class-body-${classId}-${i}"
                                         aria-labelledby="class-header-${classId}-${i}"
                                         role="tabpanel">
                                        <pre class="m-0"><textarea id="dependency-area-${i}" name="dependency-${i}" title="dependency-${i}" readonly></textarea></pre>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
            </jsp:attribute>
    </t:modal>

    <script>
        (function () {
            const modal = document.currentScript.parentElement.querySelector('.modal');
            const cutTextArea = document.currentScript.parentElement.querySelector('textarea[id="sut"]');
            const cutTitle = document.currentScript.parentElement.querySelector('button[id^="cut-header"]');
            const dependencyTextAreas = document.currentScript.parentElement.querySelectorAll('textarea[id^="dependency-area-"]');
            const dependencyHeaders = document.currentScript.parentElement.querySelectorAll('button[id^="class-header-"]');
            const classId = <%= classId %>;

            modal.addEventListener('shown.bs.modal', async function () {
                const codeMirrorContainer = this.querySelector('.CodeMirror');
                if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                    codeMirrorContainer.CodeMirror.refresh();
                    return;
                }

                const {default: CodeMirror} = await import('${url.forPath("/js/codemirror.mjs")}');
                const {InfoApi, LoadingAnimation} = await import('${url.forPath("/js/codedefenders_main.mjs")}');

                const editor = CodeMirror.fromTextArea(cutTextArea, {
                    lineNumbers: true,
                    readOnly: true,
                    mode: 'text/x-java',
                    autoRefresh: true
                });
                editor.getWrapperElement().classList.add('codemirror-readonly');

                await InfoApi.setClassEditorValue(editor, classId);

                const classInfo = await InfoApi.getClassInfo(classId, true);
                cutTitle.textContent = classInfo.name;

                await dependencyHeaders.forEach((header, index) => {
                    header.textContent = classInfo.dependency_names[index];
                });

                await dependencyTextAreas.forEach((textarea, index) => {
                    const dependencyEditor = CodeMirror.fromTextArea(textarea, {
                        lineNumbers: true,
                        readOnly: true,
                        mode: 'text/x-java',
                        autoRefresh: true
                    });
                    dependencyEditor.getWrapperElement().classList.add('codemirror-readonly');
                    InfoApi.setDependencyEditorValue(dependencyEditor, classId, index);
                });
                const codeMirrorContainers = this.querySelectorAll('.CodeMirror'); //refresh all CodeMirror instances
                if (codeMirrorContainers.length > 0 && codeMirrorContainers[0].CodeMirror) {
                    codeMirrorContainers.forEach(container => container.CodeMirror.refresh());
                }
                LoadingAnimation.hideAnimation(cutTextArea);
            });
        })();
    </script>
</div>

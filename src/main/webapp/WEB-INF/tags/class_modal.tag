<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ attribute name="classId" required="true" %>
<%@ attribute name="classAlias" required="true" %>
<%@ attribute name="htmlId" required="true" %>
<%@ tag import="org.codedefenders.util.FileUtils" %>
<%@ tag import="java.util.List" %>
<%@ tag import="com.google.gson.Gson" %>
<%
    List<String> deps = FileUtils.getCodeFromDependencies(Integer.parseInt(classId));
    String numberOfDeps = String.valueOf(deps.size() - 1);
    String depsJson = new Gson().toJson(deps);
%>

<div>
    <t:modal title="${classAlias}" id="${htmlId}"
             modalDialogClasses="modal-dialog-responsive"
             modalBodyClasses="loading loading-bg-gray loading-height-200">
        <jsp:attribute name="content">
            <div class="card">
                <div class="card-body p-0 codemirror-expand codemirror-class-modal-size">
                    <pre class="m-0"><textarea></textarea></pre>
                </div>
            </div>
            <c:forEach var="i" begin="0" end="2">
                    <div class="card">
                        <div class="card-body p-0 codemirror-expand codemirror-class-modal-size">
                            <pre class="m-0"><textarea id="dependency-area-${i}"></textarea></pre>
                        </div>
                    </div>
            </c:forEach>
        </jsp:attribute>
    </t:modal>

    <script>
        (function () {
            const modal = document.currentScript.parentElement.querySelector('.modal');
            const cutTextArea = document.currentScript.parentElement.querySelector('textarea');
            const dependencyTextAreas = document.currentScript.parentElement.querySelectorAll('textarea[id^="dependency-area-"]');

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

                await InfoApi.setClassEditorValue(editor, ${classId});
                LoadingAnimation.hideAnimation(cutTextArea);

                await dependencyTextAreas.forEach(setContentForTextarea);
                const codeMirrorContainers = this.querySelectorAll('.CodeMirror'); //refresh all CodeMirror instances
                if (codeMirrorContainers.length > 0 && codeMirrorContainers[0].CodeMirror) {
                    codeMirrorContainers.forEach(container => container.CodeMirror.refresh());
                    return;
                }
            });
        })();

        async function setContentForTextarea(textarea, index) {
            const deps = JSON.parse('<%= depsJson%>');
            const dependencyCode = deps[index]; //TODO schauen, dass das nicht jedes Mal gemacht wird
            const {default: CodeMirror} = await import('${url.forPath("/js/codemirror.mjs")}');
            const {InfoApi, LoadingAnimation} = await import('${url.forPath("/js/codedefenders_main.mjs")}');


            const dependencyEditor = CodeMirror.fromTextArea(textarea, {
                lineNumbers: true,
                readOnly: true,
                mode: 'text/x-java',
                autoRefresh: true
            });
            dependencyEditor.getWrapperElement().classList.add('codemirror-readonly');

            dependencyEditor.setValue(dependencyCode);
        }
    </script>
</div>

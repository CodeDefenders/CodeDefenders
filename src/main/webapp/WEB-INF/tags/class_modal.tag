<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<%@ attribute name="classId" required="true" %>
<%@ attribute name="classAlias" required="true" %>
<%@ attribute name="htmlId" required="true" %>

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
        </jsp:attribute>
    </t:modal>

    <script>
        (function () {
            const modal = document.currentScript.parentElement.querySelector('.modal');
            const textarea = document.currentScript.parentElement.querySelector('textarea');

            modal.addEventListener('shown.bs.modal', async function () {
                const codeMirrorContainer = this.querySelector('.CodeMirror');
                if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                    codeMirrorContainer.CodeMirror.refresh();
                    return;
                }

                const {default: CodeMirror} = await import('${url.forPath("/js/codemirror.mjs")}');
                const {InfoApi, LoadingAnimation} = await import('${url.forPath("/js/codedefenders_main.mjs")}');

                const editor = CodeMirror.fromTextArea(textarea, {
                    lineNumbers: true,
                    readOnly: true,
                    mode: 'text/x-java',
                    autoRefresh: true
                });
                editor.getWrapperElement().classList.add('codemirror-readonly');

                await InfoApi.setClassEditorValue(editor, ${classId});
                LoadingAnimation.hideAnimation(textarea);
            });
        })();
    </script>
</div>

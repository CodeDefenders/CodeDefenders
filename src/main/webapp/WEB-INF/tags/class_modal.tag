<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="classId" required="true" %>
<%@ attribute name="classAlias" required="true" %>
<%@ attribute name="htmlId" required="true" %>

<div>
    <t:modal title="${classAlias}" id="${htmlId}" modalDialogClasses="modal-dialog-responsive">
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

            modal.addEventListener('shown.bs.modal', function () {
                const codeMirrorContainer = this.querySelector('.CodeMirror');
                if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                    codeMirrorContainer.CodeMirror.refresh();
                } else {
                    const editor = CodeMirror.fromTextArea(textarea, {
                        lineNumbers: true,
                        readOnly: true,
                        mode: 'text/x-java',
                        autoRefresh: true
                    });
                    editor.getWrapperElement().classList.add('codemirror-readonly');
                    CodeDefenders.InfoApi.setClassEditorValue(editor, ${classId});
                }
            })
        })();
    </script>
</div>

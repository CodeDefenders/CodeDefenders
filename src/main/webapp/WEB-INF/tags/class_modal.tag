<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>
<%--@elvariable id="classViewer" type="org.codedefenders.beans.game.ClassViewerBean"--%>

<%@ attribute name="classId" required="true" %>
<%@ attribute name="classAlias" required="true" %>
<%@ attribute name="htmlId" required="true" %>

<div>
    <t:modal title="${classAlias}" id="${htmlId}"
             modalDialogClasses="modal-dialog-responsive">

        <jsp:attribute name="content">
            <div class="card loading loading-bg-gray loading-height-200" id="class-modal-card-${classId}">
                <div class="card-header" hidden>
                    <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1 active" data-bs-toggle="tab"
                                    id="cut-header-${classId}"
                                    data-bs-target="#cut-body-${classId}"
                                    aria-controls="cut-body-${classId}"
                                    type="button" role="tab" aria-selected="true">
                            </button>
                        </li>
                    </ul>
                </div>
                <div class="card-body p-0 codemirror-expand codemirror-class-modal-size">
                    <div class="tab-content">
                        <div class="tab-pane active"
                             id="cut-body-${classId}"
                             aria-labelledby="cut-header-${classId}"
                             role="tabpanel">
                            <pre class="m-0"><textarea id="cut-area" name="cut" title="cut"
                                                       readonly></textarea></pre>
                        </div>
                    </div>
                </div>
            </div>
        </jsp:attribute>
    </t:modal>

    <script>
        (function () {
            const modal = document.currentScript.parentElement.querySelector('.modal');
            const classId = ${classId};

            modal.addEventListener('shown.bs.modal', async function () {
                const codeMirrorContainer = this.querySelector('.CodeMirror');
                if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                    codeMirrorContainer.CodeMirror.refresh();
                    return;
                }

                const {DynamicClassViewer} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
                const card = modal.querySelector('#class-modal-card-${classId}');
                await DynamicClassViewer.show_code(classId, card);
            });
        })();
    </script>
</div>

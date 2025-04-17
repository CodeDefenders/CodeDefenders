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

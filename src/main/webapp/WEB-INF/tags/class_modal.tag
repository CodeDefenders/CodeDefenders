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
             modalDialogClasses="modal-dialog-responsive"
             modalBodyClasses="loading loading-bg-gray loading-height-200">

        <jsp:attribute name="content">
            <div class="card">
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
            const cutArea = document.currentScript.parentElement.querySelector('textarea[id="cut-area"]');
            const cutHeader = document.currentScript.parentElement.querySelector('button[id^="cut-header"]');
            const cardHeader = document.currentScript.parentElement.querySelector('.card-header');
            const classId = <%= classId %>;

            modal.addEventListener('shown.bs.modal', async function () {

                const codeMirrorContainer = this.querySelector('.CodeMirror');
                if (codeMirrorContainer && codeMirrorContainer.CodeMirror) {
                    codeMirrorContainer.CodeMirror.refresh();
                    return;
                }

                const {default: CodeMirror} = await import('${url.forPath("/js/codemirror.mjs")}');
                const {InfoApi, LoadingAnimation} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
                const classInfo = await InfoApi.getClassInfo(classId, true);
                const hasDependencies = classInfo.dependency_names.length > 0;

                const cutEditor = CodeMirror.fromTextArea(cutArea, {
                    lineNumbers: true,
                    readOnly: true,
                    mode: 'text/x-java',
                    autoRefresh: true
                });
                cutEditor.getWrapperElement().classList.add('codemirror-readonly');
                try {
                    cutEditor.setValue(classInfo.source)
                    if (hasDependencies) {
                        cutHeader.textContent = classInfo.name;
                    }
                } catch (e) {
                    cutEditor.setValue("Could not fetch class.\nPlease try again later.");
                    if (hasDependencies) {
                        cutHeader.textContent = "Error";
                    }
                }

                const headerNav = this.querySelector('.nav');
                const bodyContent = this.querySelector('.tab-content');
                await classInfo.dependency_names.forEach((name, index) => {
                    const dependencyTitle = document.createElement("li");
                    dependencyTitle.classList.add("nav-item");
                    dependencyTitle.role = "presentation";
                    dependencyTitle.innerHTML = `
                        <button class="nav-link py-1" data-bs-toggle="tab"
                        ` +
                                "id=\"class-header-${classId}-" + index + "\""
                                + " data-bs-target=\"#class-body-${classId}-" + index + "\""
                                + " aria-controls=\"class-body-${classId}-" + index + "\""
                                + " type=\"button\" role=\"tab\" aria-selected=\"false\">"
                            + name
                            + "</button>"

                    headerNav.appendChild(dependencyTitle);

                    const dependencyContent = document.createElement("div");
                    dependencyContent.classList.add("tab-pane");
                    dependencyContent.id = "class-body-${classId}-" + index;
                    dependencyContent.setAttribute("aria-labelledby", "class-header-${classId}-" + index);
                    dependencyContent.role = "tabpanel";

                    const dependencyPre = document.createElement("pre");
                    dependencyPre.classList.add("m-0");

                    const dependencyTextArea = document.createElement("textarea");
                    dependencyTextArea.readonly = true;

                    dependencyPre.appendChild(dependencyTextArea);
                    dependencyContent.appendChild(dependencyPre);
                    bodyContent.appendChild(dependencyContent);

                    const dependencyEditor = CodeMirror.fromTextArea(dependencyTextArea, {
                        lineNumbers: true,
                        readOnly: true,
                        mode: 'text/x-java',
                        autoRefresh: true
                    });
                    dependencyEditor.getWrapperElement().classList.add('codemirror-readonly');
                    try {
                        dependencyEditor.setValue(classInfo.dependency_code[index])
                    } catch (e) {
                        dependencyEditor.setValue("Could not fetch dependency \n Please try again later.");
                    }


                });

                const codeMirrorContainers = this.querySelectorAll('.CodeMirror'); //refresh all CodeMirror instances
                if (codeMirrorContainers.length > 0 && codeMirrorContainers[0].CodeMirror) {
                    codeMirrorContainers.forEach(container => container.CodeMirror.refresh());
                }
                if (hasDependencies) {
                    cardHeader.removeAttribute("hidden")
                }

                LoadingAnimation.hideAnimation(cutArea);
            });
        })();
    </script>
</div>

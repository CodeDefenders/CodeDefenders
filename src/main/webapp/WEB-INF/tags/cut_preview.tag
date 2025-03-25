<%--
  ~ Copyright (C) 2021 Code Defenders contributors
  ~
  ~ This file is part of Code Defenders.
  ~
  ~ Code Defenders is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or (at
  ~ your option) any later version.
  ~
  ~ Code Defenders is distributed in the hope that it will be useful, but
  ~ WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  ~ General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
  --%>
<%@ tag pageEncoding="UTF-8" %>

<%--@elvariable id="url" type="org.codedefenders.util.URLUtils"--%>

<div id="create-game-cut-preview" class="w-100 h-100">
    <div class="card loading loading-bg-gray loading-height-200"
         style="height: 100%; min-height: 200px; resize: vertical; overflow: auto;">
        <div class="card-header" hidden>
            <ul class="nav nav-pills nav-fill card-header-pills gap-1" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link py-1 active" data-bs-toggle="tab"
                            id="cut-header"
                            data-bs-target="#cut-body"
                            aria-controls="cut-body"
                            type="button" role="tab" aria-selected="true">
                    </button>
                </li>
            </ul>
        </div>
        <div class="card-body p-0 codemirror-expand codemirror-class-modal-size">
            <div class="tab-content">
                <div class="tab-pane active"
                     id="cut-body"
                     aria-labelledby="cut-header"
                     role="tabpanel">
                            <pre class="m-0"><textarea id="cut-area" name="cut" title="cut"
                                                       readonly></textarea></pre>
                </div>
            </div>
        </div>
    </div>

    <script type="module">
        import CodeMirror from '${url.forPath("/js/codemirror.mjs")}';

        import {InfoApi} from '${url.forPath("/js/codedefenders_main.mjs")}';


        const cutPreview = document.querySelector('#create-game-cut-preview')

        const classSelector = document.querySelector('#class-select');

        const updatePreview = async function () {
            const classId = Number(classSelector.value);
            const cutArea = cutPreview.querySelector('textarea[id="cut-area"]');
            const cutHeader = cutPreview.querySelector('button[id="cut-header"]');
            const cardHeader = cutPreview.querySelector('.card-header');
            const cutBody = cutPreview.querySelector('#cut-body');
            const card = cutPreview.querySelector('.card');


            //---------------------------------------------- Reset

            //Change active tab to CuT
            document.querySelectorAll(".nav-item").forEach(header =>
                    header.classList.remove("active")
            );
            cutHeader.classList.add("active");
            document.querySelectorAll(".tab-pane").forEach(tab =>
                    tab.classList.remove("active")
            );
            cutBody.classList.add("active");

            cardHeader.setAttribute("hidden", "true");
            card.setAttribute("class", "card loading loading-bg-gray loading-height-200");

            //Remove all dependency tabs
            const headerNav = cutPreview.querySelector('.nav');
            while (headerNav.children.length > 1) {
                headerNav.removeChild(headerNav.children[1]);
            }

            //Reset the CuT editor, if it already exists, otherwise create a new one
            let cutEditorWrapper = cutPreview.querySelector('#cut-editor');
            let cutEditor;
            if (cutEditorWrapper && cutEditorWrapper.CodeMirror) {
                cutEditor = cutEditorWrapper.CodeMirror;
                cutEditor.setValue("");
            } else {
                const {default: CodeMirror} = await import('${url.forPath("/js/codemirror.mjs")}');

                cutEditor = CodeMirror.fromTextArea(cutArea, {
                    lineNumbers: true,
                    readOnly: true,
                    mode: 'text/x-java',
                    autoRefresh: true
                });
                cutEditor.getWrapperElement().classList.add('codemirror-readonly');
                cutEditor.getWrapperElement().id = "cut-editor";
            }


            //----------------------------------------------


            const {InfoApi, LoadingAnimation} = await import('${url.forPath("/js/codedefenders_main.mjs")}');
            const classInfo = await InfoApi.getClassInfo(classId, true);
            const hasDependencies = classInfo.dependency_names.length > 0;

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

            const bodyContent = cutPreview.querySelector('.tab-content');
            classInfo.dependency_names.forEach((name, index) => {
                const dependencyTitle = document.createElement("li");
                dependencyTitle.classList.add("nav-item");
                dependencyTitle.role = "presentation";
                dependencyTitle.innerHTML = `
                        <button class="nav-link py-1" data-bs-toggle="tab"
                                id="class-header-\${classId}-\${index}"
                                data-bs-target="#class-body-\${classId}-\${index}"
                                aria-controls="class-body-\${classId}-\${index}"
                                type="button" role="tab" aria-selected="false">
                            \${name}
                        </button>
                    `;

                headerNav.appendChild(dependencyTitle);

                const dependencyContent = document.createElement("div");
                dependencyContent.classList.add("tab-pane");
                dependencyContent.id = `class-body-\${classId}-\${index}`;
                dependencyContent.setAttribute("aria-labelledby", `class-header-\${classId}-\${index}`);
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
            LoadingAnimation.hideAnimation(card);
        };

        // Load initial selected class
        document.addEventListener("DOMContentLoaded", updatePreview);

        classSelector.addEventListener('change', updatePreview);
    </script>
</div>

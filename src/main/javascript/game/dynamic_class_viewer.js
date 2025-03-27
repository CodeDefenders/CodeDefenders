import {LoadingAnimation} from "../main";
//import CodeMirror from '../thirdparty/codemirror';

/**
 * This class is intended for use in overview pages outside the game, where code of many different classes
 * might be displayed.
 */
class DynamicClassViewer {

    /**
     * Fills an existing card with header and body elements for the class under test and its dependencies.
     * Code will be displayed using CodeMirrors in read-only mode.
     * The card has to follow a specific format, see {@code cut_preview.tag} for an example.
     *
     * @param classId The ID of the class to display.
     * @param card The card element to fill.
     * @returns {Promise<void>}
     */
    static async show_code(classId, card) {

        const cardHeader = card.querySelector('.card-header');
        const cutHeader = cardHeader.querySelector('button');

        const cardBody = card.querySelector('.card-body');
        const cutBody = cardBody.querySelector('.tab-pane');

        //---------------------------------------------- Reset

        //Change active tab to CuT
        cardHeader.querySelectorAll(".nav-item").forEach(header =>
            header.classList.remove("active")
        );
        cutHeader.classList.add("active");
        cardBody.querySelectorAll(".tab-pane").forEach(tab =>
            tab.classList.remove("active")
        );
        cutBody.classList.add("active");

        cardHeader.setAttribute("hidden", "true");
        card.setAttribute("class", "card loading loading-bg-gray loading-height-200");

        //Remove all dependency tabs
        const headerNav = cardHeader.querySelector('.nav');
        while (headerNav.children.length > 1) {
            headerNav.removeChild(headerNav.children[1]);
        }

        //Remove all dependency content
        cardBody.querySelectorAll("div[id^='class-body']").forEach(tab => {
            tab.parentElement.removeChild(tab);
        })

        //Reset the CuT editor, if it already exists, otherwise create a new one
        let cutEditorWrapper = cutBody.querySelector('.CodeMirror');
        let cutEditor;
        if (cutEditorWrapper) {
            cutEditor = cutEditorWrapper.CodeMirror;
            cutEditor.setValue("");
        } else {
            const {default: CodeMirror} = await import('../thirdparty/codemirror.js');

            cutEditor = CodeMirror(cutBody, {
                lineNumbers: true,
                readOnly: true,
                mode: 'text/x-java',
                autoRefresh: true
            });
            cutEditor.getWrapperElement().classList.add('codemirror-readonly');
        }



        //----------------------------------------------

        const {InfoApi, LoadingAnimation} = await import('../main/index.js');
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

        const bodyContent = cardBody.querySelector('.tab-content');

        classInfo.dependency_names.forEach((name, index) => {
            const dependencyTitle = document.createElement("li");
            dependencyTitle.classList.add("nav-item");
            dependencyTitle.role = "presentation";
            const dependencyButton = document.createElement("button");
            dependencyButton.classList.add("nav-link",  "py-1");
            dependencyButton.dataset.bsToggle = "tab";
            dependencyButton.id = `class-header-${classId}-${index}`;
            dependencyButton.dataset.bsTarget = `#class-body-${classId}-${index}`;
            dependencyButton.setAttribute("aria-controls", `class-body-${classId}-${index}`);
            dependencyButton.type = "button";
            dependencyButton.role = "tab";
            dependencyButton.setAttribute("aria-selected", "false");
            dependencyButton.textContent = name;

            dependencyTitle.appendChild(dependencyButton);

            headerNav.appendChild(dependencyTitle);

            const dependencyContent = document.createElement("div");
            dependencyContent.classList.add("tab-pane");
            dependencyContent.id = `class-body-${classId}-${index}`;
            dependencyContent.setAttribute("aria-labelledby", `class-header-${classId}-${index}`);
            dependencyContent.role = "tabpanel";

            bodyContent.appendChild(dependencyContent);

            const dependencyEditor = CodeMirror(dependencyContent, {
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
        const codeMirrorContainers = cardBody.querySelectorAll('.CodeMirror'); //refresh all CodeMirror instances
        if (codeMirrorContainers.length > 0 && codeMirrorContainers[0].CodeMirror) {
            codeMirrorContainers.forEach(container => container.CodeMirror.refresh());
        }
        if (hasDependencies) {
            cardHeader.removeAttribute("hidden")
        }
        LoadingAnimation.hideAnimation(card);
    };
}

export default DynamicClassViewer;

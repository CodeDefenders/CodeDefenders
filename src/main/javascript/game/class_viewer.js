import {LoadingAnimation} from "../main";
import CodeMirror from '../thirdparty/codemirror';


class ClassViewer {

    /**
     * @param {HTMLTextAreaElement} editorElement The text area to use as editor.
     * @param {HTMLTextAreaElement[]} dependencyEditorElements Text areas to use as dependency editors.
     */
    constructor (editorElement, dependencyEditorElements) {
        /**
         * The text area element of the editor.
         * @type {HTMLTextAreaElement}
         */
        this.editorElement = editorElement;
        /**
         * The CodeMirror instance used for the mutant editor.
         * @type {CodeMirror}
         */
        this.editor = null;


        /**
         * Text area elements for displaying dependencies.
         * @type {HTMLTextAreaElement[]}
         */
        this.dependencyEditorElements = dependencyEditorElements;
        /**
         * CodeMirror editors displaying dependencies.
         * @type {CodeMirror[]}
         */
        this.dependencyEditors = [];


        this._init();
    }

    /** @private */
    _init () {
        /* Create the editor. */
        this.editor = CodeMirror.fromTextArea(this.editorElement, {
            lineNumbers: true,
            matchBrackets: true,
            mode: 'text/x-java',
            readOnly: true,
            gutters: [
                'CodeMirror-linenumbers',
                'CodeMirror-mutantIcons'
            ],
            autoRefresh: true
        });
        this.editor.getWrapperElement().classList.add('codemirror-readonly');

        /* Refresh editor when resized. */
        if (window.hasOwnProperty('ResizeObserver')) {
            new ResizeObserver(() => this.editor.refresh())
                    .observe(this.editor.getWrapperElement());
        }

        this._initDependencies();

        LoadingAnimation.hideAnimation(this.editorElement);
    }

    /** @private */
    _initDependencies () {
        for (const element of this.dependencyEditorElements) {
            const editor = CodeMirror.fromTextArea(element, {
                lineNumbers: true,
                matchBrackets: true,
                mode: 'text/x-java',
                readOnly: true,
                autoRefresh: true
            });
            editor.getWrapperElement().classList.add('codemirror-readonly');

            if (window.hasOwnProperty('ResizeObserver')) {
                new ResizeObserver(() => editor.refresh()).observe(editor.getWrapperElement());
            }

            this.dependencyEditors.push(editor);
        }
    }

    /**
     * Scrolls the given line into view.
     * @param {number} line The given line (1-indexed).
     */
    jumpToLine (line) {
        line -= 1; // Subtract 1 because CodeMirror's lines are 0-indexed.
        this.editor.scrollIntoView({line}, 200);
    }
}


export default ClassViewer;

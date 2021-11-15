/* Wrap in a function to avoid polluting the global scope. */
(function () {

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
            readOnly: 'nocursor',
            gutters: [
                'CodeMirror-linenumbers',
                'CodeMirror-mutantIcons'
            ],
            autoRefresh: true
        });

        /* Refresh editor when resized. */
        if (window.hasOwnProperty('ResizeObserver')) {
            new ResizeObserver(() => this.editor.refresh())
                    .observe(this.editor.getWrapperElement());
        }

        this._initDependencies();
    }

    /** @private */
    _initDependencies () {
        for (const element of this.dependencyEditorElements) {
            const editor = CodeMirror.fromTextArea(element, {
                lineNumbers: true,
                matchBrackets: true,
                mode: 'text/x-java',
                readOnly: 'nocursor',
                autoRefresh: true
            });

            if (window.hasOwnProperty('ResizeObserver')) {
                new ResizeObserver(() => editor.refresh()).observe(editor.getWrapperElement());
            }

            this.dependencyEditors.push(editor);
        }
    }
}

CodeDefenders.classes.ClassViewer ??= ClassViewer;

})();

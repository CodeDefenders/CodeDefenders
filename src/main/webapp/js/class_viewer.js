/* Wrap in a function to avoid polluting the global scope. */
(function () {

class ClassViewer {

    constructor (editorElement, dependencyEditorElements) {
        this.editorElement = editorElement;
        this.editor = null;

        this.dependencyEditorElements = dependencyEditorElements;
        this.dependencyEditors = [];

        this._init();
    }

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

CodeDefenders.classes.ClassViewer = ClassViewer;

})();

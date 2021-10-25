/* Wrap in a function to avoid polluting the global scope. */
(function () {

class ErrorHighlighting {
    /**
     * @param errorLines
     *      Given by [JSON.parse('${mutantErrorHighlighting.errorLinesJSON}')]
     */
    constructor (errorLines) {
        /**
         * The line numbers of lines containing errors.
         * @type {number[]}
         */
        this.errorLines = errorLines;

        /**
         * The CodeMirror editor to highlight errors on.
         * @type {CodeMirror}
         */
        this.editor = null;
        if (CodeDefenders.objects.classViewer != null) {
            this.editor = CodeDefenders.objects.classViewer.editor;
        } else if (CodeDefenders.objects.mutantEditor != null) {
            this.editor = CodeDefenders.objects.mutantEditor.editor;
        }
    }

    /**
     * Highlights errors on the given CodeMirror instance.
     * See: https://creativewebspecialist.co.uk/2013/07/15/highlight-errors-in-codemirror/
     */
    highlightErrors () {
        for (const errorLine of this.errorLines) {
            this.editor.addLineClass(errorLine - 1, 'background', 'line-error');
        }
    }

}

CodeDefenders.classes.ErrorHighlighting ??= ErrorHighlighting;

})();

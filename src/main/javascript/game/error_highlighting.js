class ErrorHighlighting {
    /**
     * @param {number[]} errorLines
     *      Given by [JSON.parse('${mutantErrorHighlighting.errorLinesJSON}')]
     * @param {CodeMirror} editor
     *      The CodeMirror editor to apply the error highlighting on.
     */
    constructor (errorLines, editor) {
        /**
         * The line numbers of lines containing errors.
         * @type {number[]}
         */
        this._errorLines = errorLines;

        /**
         * The CodeMirror editor to highlight errors on.
         * @type {CodeMirror}
         */
        this.editor = editor;
    }

    /**
     * Highlights errors on the given CodeMirror instance.
     * See: https://creativewebspecialist.co.uk/2013/07/15/highlight-errors-in-codemirror/
     */
    highlightErrors () {
        for (const errorLine of this._errorLines) {
            this.editor.addLineClass(errorLine - 1, 'background', 'line-error');
        }
    }

}


export default ErrorHighlighting;

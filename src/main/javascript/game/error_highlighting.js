/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
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

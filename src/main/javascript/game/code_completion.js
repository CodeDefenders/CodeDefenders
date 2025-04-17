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
import CodeMirror from '../thirdparty/codemirror';


class CodeCompletion {
    constructor () {
        /**
         * Keep multiple pools of completions by name, so pools can be updated individually when something changes.
         * @type {Map<string, Set<string>>}
         */
        this._completionPools = new Map();
    }

    /**
     * Regex to filter out comments.
     * @type {RegExp}
     */
    static _COMMENT_REGEX = /(\/\*([\s\S]*?)\*\/)|(\/\/(.*)$)/gm;

    /**
     * Regex to find valid Java identifiers.
     * (from https://stackoverflow.com/a/35578805/9360382)
     * @type {RegExp}
     */
    static _IDENTIFIER_REGEX = /(?:\b[_a-zA-Z]|\B\$)[_$a-zA-Z0-9]*/g;

    /**
     * Registers a code completion command for the editor under the given name.
     * @param editor The editor to complete the code on.
     * @param commandName The name given to the code completion command, to be used in the CodeMirror extraKeys option.
     */
    registerCodeCompletionCommand (editor, commandName) {
        CodeMirror.commands[commandName] = this._complete.bind(this, editor);
    }

    /**
     * Extracts possible completions from java files.
     * @param {string[]} texts The java files.
     * @return {Set<string>} The extracted completions.
     */
    getCompletionsForJavaFiles (texts) {
        const completions = new Set();

        for (let text of texts) {
            text = text.replace(CodeCompletion._COMMENT_REGEX, ' ');
            for (const match of text.matchAll(CodeCompletion._IDENTIFIER_REGEX)) {
                completions.add(match[0]);
            }
        }

        return completions;
    }

    /**
     * Sets a completion pool to the given completions.
     * Multiple named pools are kept so individual sets of completions can easily be updated.
     * The text of the editor itself does not have to be added to a pool, as it is added to the completions at the time
     * of code completion.
     * @param {string} poolName The name of the pool.
     * @param {Set<string>} completions The completions for the pool.
     */
    setCompletionPool (poolName, completions) {
        this._completionPools.set(poolName, completions);
    }

    /**
     * The code completion function.
     * Adds the current editor text to the completions, then displays possible completions in the editor.
     * @param editor The editor to code complete on.
     * @private
     */
    _complete (editor) {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        editor.showHint({
            hint: function (editor) {
                const cursor = editor.getCursor();
                const line = editor.getLine(cursor.line);
                const {word, index} = self._getIdentifierUnderCursor(line, cursor.ch);

                /* Add the current editor text (without the word under cursor) to the completions. */
                const lines = editor.getValue().split(/\r?\n/);
                lines[cursor.line] = line.substring(0, index) + ' ' + line.substring(index + word.length, line.length);
                const editorCompletions = self.getCompletionsForJavaFiles([lines.join('\n')]);
                self.setCompletionPool('', editorCompletions);

                const list = self._getCompletionsForWord(word);

                return {
                    list,
                    from: CodeMirror.Pos(cursor.line, index),
                    to: CodeMirror.Pos(cursor.line, index + word.length)
                };
            }
        });
    }

    /**
     * Computes the word under cursor for a cursor index.
     * A word is considered under cursor if the cursor index describes any of its characters or the character after the
     * word.
     * @param {string} line The string to find the word it.
     * @param {number} index The cursor index, 0 to (including) str.length.
     * @returns {{word: string, index: number}}
     *      <ul>
     *          <li>word: the word under cursor</li>
     *          <li>index: index of the first character</li>
     *      </ul>
     *      If there is no word under the cursor, {word: '', index: cursor index} will be returned.
     * @private
     */
    _getIdentifierUnderCursor (line, index) {
        for (const match of line.matchAll(CodeCompletion._IDENTIFIER_REGEX)) {
            if (index >= match.index && index <= match.index + match[0].length) {
                return {word: match[0], index: match.index}
            }
        }
        return {word: '', index};
    }

    /**
     * Goes through all pools of completions and searches for possible completions of the given word.
     * @param {string} word The word to complete.
     * @return {string[]} Possible completions.
     * @private
     */
    _getCompletionsForWord (word) {
        const foundCompletions = new Set();
        const lowercaseWord = word.toLowerCase();

        for (const pool of this._completionPools.values()) {
            for (const item of pool) {
                if (item.toLowerCase().startsWith(lowercaseWord)) {
                    foundCompletions.add(item);
                }
            }
        }

        return Array.from(foundCompletions).sort();
    }

}


export default CodeCompletion;

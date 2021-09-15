/* Wrap in a function to avoid polluting the global scope. */
(function () {

class TestEditor {
    constructor (editorElement, editableLinesStart, editableLinesEnd, mockingEnabled, keymap) {
        this.editorElement = editorElement;
        this.editor = null;

        this.editableLinesStart = editableLinesStart ?? 1;
        this.editableLinesEnd = editableLinesEnd; // Does nothing as of now.
        this.mockingEnabled = mockingEnabled;
        this.keymap = keymap;

        this.codeCompletionList = [];

        this._init();
    }

    static COMMENT_REGEX = /(\/\*([\s\S]*?)\*\/)|(\/\/(.*)$)/gm;

    /* from https://stackoverflow.com/a/35578805/9360382 */
    static IDENTIFIER_REGEX = /(?:\b[_a-zA-Z]|\B\$)[_$a-zA-Z0-9]*/g;


    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        this._registerCodeCompletion();

        /* Create the editor. */
        this.editor = CodeMirror.fromTextArea(this.editorElement, {
            lineNumbers: true,
            indentUnit: 4,
            smartIndent: true,
            matchBrackets: true,
            mode: "text/x-java",
            autoCloseBrackets: true,
            styleActiveLine: true,
            extraKeys: {
                "Ctrl-Space": "autocompleteTest",
                "Tab": "insertSoftTab"
            },
            keyMap: this.keymap,
            gutters: [
                    'CodeMirror-linenumbers',
                    'CodeMirror-mutantIcons'
            ],
            autoRefresh: true
        });

        if (window.hasOwnProperty('ResizeObserver')) {
            new ResizeObserver(() => this.editor.refresh())
                    .observe(this.editor.getWrapperElement());
        }

        this.editor.on('beforeChange', function (cm, change) {
            const numLines = cm.lineCount();
            if (change.from.line < (self.editableLinesStart - 1)
                    || change.to.line > (numLines - 3)) {
                change.cancel();
            }
        });

        this.editor.on('focus', function () {
            self._updateCodeCompletion();
        });

        this.editor.on('keyHandled', function (cm, name, event) {
            // 9 == Tab, 13 == Enter
            if ([9, 13].includes(event.keyCode)) {
                self._updateCodeCompletion();
            }
        });
    }

    /**
     * Computes the word under cursor for a cursor index.
     * A word is considered under cursor if the cursor index describes any of its character or the character after the
     * word.
     *
     * @param {string} line The string to find the word it.
     * @param {number} index The cursor index, 0 to (including) str.length.
     *
     * @returns {{word: string, index: number}}
     *      <ul>
     *          <li>start: start of the word (inclusive)</li>
     *          <li>index: index of the first character</li>
     *      </ul>
     *      If there is no word under the cursor, {word: '', index: cursor index} will be returned.
     */
    _getIdentifierUnderCursor (line, index) {
        for (const match of line.matchAll(TestEditor.IDENTIFIER_REGEX)) {
            if (index >= match.index && index <= match.index + match[0].length) {
                return {word: match[0], index: match.index}
            }
        }
        return {word: '', index};
    }

    _registerCodeCompletion () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        CodeMirror.commands.autocompleteTest = function (cm) {
            cm.showHint({
                hint: function (editor) {
                    const cursor = editor.getCursor();
                    const line = editor.getLine(cursor.line);

                    const {word, index} = self._getIdentifierUnderCursor(line, cursor.ch);

                    let list = self.codeCompletionList;
                    if (word !== '') {
                        const lowerWord = word.toLowerCase();
                        list = list
                                .filter(item => item.toLowerCase().startsWith(lowerWord))
                                .sort();
                    }

                    return {
                        list,
                        from: CodeMirror.Pos(cursor.line, index),
                        to: CodeMirror.Pos(cursor.line, index + word.length)
                    };
                }
            });
        };
    }

    _updateCodeCompletion () {
        // If you make changes to the autocompletion, change it for an attacker too.
        let testMethods = [
                "assertArrayEquals",
                "assertEquals",
                "assertTrue",
                "assertFalse",
                "assertNull",
                "assertNotNull",
                "assertSame",
                "assertNotSame",
                "fail"
        ];

        if (this.mockingEnabled) {
            let mockitoMethods = [
                    "mock",
                    "when",
                    "then",
                    "thenThrow",
                    "doThrow",
                    "doReturn",
                    "doNothing"
            ];
            // Answer object handling is currently not included (Mockito.doAnswer(), OngoingStubbing.then/thenAnswer
            // Calling real methods is currently not included (Mockito.doCallRealMethod / OngoingStubbing.thenCallRealMethod)
            // Behavior verification is currently not implemented (Mockito.verify)
            testMethods = testMethods.concat(mockitoMethods);
        }

        let testClass = this.editor.getValue().split("\n");
        testClass.slice(this.editableLinesStart, testClass.length - 2);
        testClass = testClass.join("\n");
        let texts = [testClass];

        const autocompletedClasses = window.autocompletedClasses;
        if (typeof autocompletedClasses !== 'undefined') {
            Object.getOwnPropertyNames(autocompletedClasses).forEach(function(key) {
                texts.push(autocompletedClasses[key]);
            });
        }

        let codeCompletionList = new Set(testMethods);
        for (let text of texts) {
            text = text.replace(TestEditor.COMMENT_REGEX, '');
            for (const match of text.matchAll(TestEditor.IDENTIFIER_REGEX)) {
                codeCompletionList.add(match[0]);
            }
        }

        this.codeCompletionList = Array.from(codeCompletionList);
    }

}

CodeDefenders.classes.TestEditor = TestEditor;

})();

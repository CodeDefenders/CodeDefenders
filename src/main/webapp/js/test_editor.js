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

    _getWordAt (str, index) {
        const regex = /[a-zA-Z][a-zA-Z0-9]*/;

        let start = index;
        let end = index;

        while (end < str.length && regex.test(str.charAt(end))) {
            end++;
        }

        while (start > 0 && regex.test(str.charAt(start - 1))) {
            start--;
        }

        const word = str.slice(start, end);
        return {start, end, word};
    }

    _registerCodeCompletion () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        CodeMirror.commands.autocompleteTest = function (cm) {
            cm.showHint({
                hint: function (editor) {
                    const cursor = editor.getCursor();
                    const line = editor.getLine(cursor.line);

                    const {start, end, word} = self._getWordAt(line, cursor.ch);
                    const regex = new RegExp('^' + word, 'i');

                    let list = self.codeCompletionList;
                    if (word !== '') {
                        list = list
                                .filter(item => item.match(regex))
                                .sort();
                    }

                    return {
                        list,
                        from: CodeMirror.Pos(cursor.line, start),
                        to: CodeMirror.Pos(cursor.line, end)
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

        const filterOutComments = function(text) {
            let commentRegex = /(\/\*([\s\S]*?)\*\/)|(\/\/(.*)$)/gm;
            return text.replace(commentRegex, "");
        };

        let wordRegex = /[a-zA-Z][a-zA-Z0-9]*/gm;
        let set = new Set(testMethods);

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

        texts.forEach(function (text) {
            text = filterOutComments(text);
            let m;
            while ((m = wordRegex.exec(text)) !== null) {
                if (m.index === wordRegex.lastIndex) {
                    wordRegex.lastIndex++;
                }
                m.forEach(function (match) {
                    set.add(match)
                });
            }
        });

        this.codeCompletionList = Array.from(set);
    }

}

CodeDefenders.classes.TestEditor = TestEditor;

})();

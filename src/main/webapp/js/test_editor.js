/* Wrap in a function to avoid polluting the global scope. */
(function () {

class TestEditor {
    constructor (editorElement, editableLinesStart, editableLinesEnd, mockingEnabled, keymap) {
        this.editorElement = editorElement;
        this.editor = null;

        this.editableLinesStart = editableLinesStart;
        this.editableLinesEnd = editableLinesEnd;
        this.initialNumLines = null;

        this.mockingEnabled = mockingEnabled;
        this.keymap = keymap;

        this.codeCompletion = null;

        this._init();
    }

    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Create the editor. */
        this.editor = CodeMirror.fromTextArea(this.editorElement, {
            lineNumbers: true,
            indentUnit: 4,
            smartIndent: true,
            matchBrackets: true,
            mode: 'text/x-java',
            autoCloseBrackets: true,
            styleActiveLine: true,
            extraKeys: {
                'Ctrl-Space': 'completeTest',
                'Tab': "insertSoftTab"
            },
            keyMap: this.keymap,
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

        /* Prevent changes in readonly lines. */
        this.initialNumLines = this.editor.lineCount();
        this.editableLinesStart = this.editableLinesStart ?? 1;
        this.editableLinesEnd = this.editableLinesEnd ?? this.initialNumLines - 2;
        this.editor.on('beforeChange', function (editor, change) {
            if (change.from.line < self.editableLinesStart - 1
                    || change.to.line > self.editableLinesEnd - 1 + (editor.lineCount() - self.initialNumLines)) {
                change.cancel();
            }
        });

        this._initCodeCompletion();
    }

    _initCodeCompletion() {
        this.codeCompletion = new CodeDefenders.classes.CodeCompletion();

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
            // Answer object handling is currently not included (Mockito.doAnswer(), OngoingStubbing.then/thenAnswer
            // Calling real methods is currently not included (Mockito.doCallRealMethod / OngoingStubbing.thenCallRealMethod)
            // Behavior verification is currently not implemented (Mockito.verify)

            let mockitoMethods = [
                "mock",
                "when",
                "then",
                "thenThrow",
                "doThrow",
                "doReturn",
                "doNothing"
            ];

            testMethods = testMethods.concat(mockitoMethods);
        }

        this.codeCompletion.setCompletionPool('testMethods', new Set(testMethods));

        if (typeof window.autocompletedClasses !== 'undefined') {
            const texts = Array.from(Object.values(window.autocompletedClasses));
            const completions = this.codeCompletion.getCompletionsForJavaFiles(texts);
            this.codeCompletion.setCompletionPool('classes', completions);
        }

        this.codeCompletion.registerCodeCompletionCommand(this.editor, 'completeTest');
    }
}

CodeDefenders.classes.TestEditor = TestEditor;

})();

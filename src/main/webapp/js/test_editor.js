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

        /* Set editable lines to sensible values. */
        this.initialNumLines = this.editor.lineCount();
        this.editableLinesStart = this.editableLinesStart ?? 1;
        this.editableLinesEnd = this.editableLinesEnd ?? this.initialNumLines - 2;

        /* Prevent changes in readonly lines. */
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
        this.codeCompletion.registerCodeCompletionCommand(this.editor, 'completeTest');

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

        /* Gather classes to autocomplete. */
        const texts = [];
        /* Get classes from class viewer. */
        if (CodeDefenders.objects.hasOwnProperty('classViewer')) {
            const classViewer = CodeDefenders.objects.classViewer;
            texts.push(classViewer.editor.getValue());
            for (const dependencyEditor of classViewer.dependencyEditors) {
                texts.push(dependencyEditor.getValue());
            }
        }
        /* Get classes from mutant editor. */
        if (CodeDefenders.objects.hasOwnProperty('mutantEditor')) {
            const mutantEditor = CodeDefenders.objects.mutantEditor;
            texts.push(mutantEditor.editor.getValue());
            for (const dependencyEditor of mutantEditor.dependencyEditors) {
                texts.push(dependencyEditor.getValue());
            }
        }

        /* Add words from classes to completion. */
        const completions = this.codeCompletion.getCompletionsForJavaFiles(texts);
        this.codeCompletion.setCompletionPool('classes', completions);
    }
}

CodeDefenders.classes.TestEditor = TestEditor;

})();

import CodeMirror from '../thirdparty/codemirror'
import CodeCompletion from './code_completion';
import {LoadingAnimation, objects} from '../main';


class TestEditor {

    /**
     * @param {HTMLTextAreaElement} editorElement The text area to use as editor.
     * @param {?number} editableLinesStart
     *      Given by [${mutantEditor.hasEditableLinesStart() ? mutantEditor.editableLinesStart : "null"}]
     *      Null if the text should be editable from the start.
     * @param {?number} editableLinesEnd
     *      Given by [${mutantEditor.hasEditableLinesEnd() ? mutantEditor.editableLinesEnd : "null"}]
     *      Null if the text should be editable to the end.
     * @param {boolean} mockingEnabled
     *      Given by [${testEditor.mockingEnabled}]
     * @param {string} keymap
     *      Given by [${login.user.keyMap.CMName}]
     */
    constructor (editorElement, editableLinesStart, editableLinesEnd, mockingEnabled, keymap) {
        /**
         * The text area element of the editor.
         * @type {HTMLTextAreaElement}
         */
        this.editorElement = editorElement;
        /**
         * The CodeMirror instance used for the test editor.
         * @type {CodeMirror}
         */
        this.editor = null;

        /**
         * The first editable line (1-indexed).
         * @type {?number}
         */
        this.editableLinesStart = editableLinesStart;
        /**
         * The last editable line (1-indexed).
         * @type {?number}
         */
        this.editableLinesEnd = editableLinesEnd;
        /**
         * Initial number of lines. Used to calculate the editable lines after the text has been edited.
         * @type {?number}
         */
        this.initialNumLines = null;


        /**
         * Whether to include mocking keywords in the code completion.
         * @type {boolean}
         */
        this._mockingEnabled = mockingEnabled;
        /**
         * Name of the keymap to be used in the editor.
         * @type {string}
         */
        this._keymap = keymap;
        /**
         * Code completion instance to handle the completion command.
         * @type {CodeCompletion}
         */
        this._codeCompletion = null;


        this._init();
    }

    /** @private */
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
            keyMap: this._keymap,
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

        LoadingAnimation.hideAnimation(this.editorElement);
    }

    /** @private */
    _initCodeCompletion() {
        this._codeCompletion = new CodeCompletion();
        this._codeCompletion.registerCodeCompletionCommand(this.editor, 'completeTest');

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

        if (this._mockingEnabled) {
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

        this._codeCompletion.setCompletionPool('testMethods', new Set(testMethods));

        /* Add class viewer code to completions. */
        objects.await('classViewer').then(classViewer => {
            const texts = [classViewer.editor.getValue()];
            for (const dependencyEditor of classViewer.dependencyEditors) {
                texts.push(dependencyEditor.getValue());
            }
            const completions = this._codeCompletion.getCompletionsForJavaFiles(texts);
            this._codeCompletion.setCompletionPool('classViewer', completions);
        });

        /* Add mutant editor code to completions. */
        objects.await('mutantEditor').then(mutantEditor => {
            const texts = [mutantEditor.editor.getValue()];
            for (const dependencyEditor of mutantEditor.dependencyEditors) {
                texts.push(dependencyEditor.getValue());
            }
            const completions = this._codeCompletion.getCompletionsForJavaFiles(texts);
            this._codeCompletion.setCompletionPool('mutantEditor', completions);
        });
    }

    /**
     * Scrolls the given line into view.
     * @param {number} line The given line (1-indexed).
     */
    jumpToLine (line) {
        line -= 1; // Subtract 1 because CodeMirror's lines are 0-indexed.
        this.editor.scrollIntoView({line}, 200);
    }
}


export default TestEditor;

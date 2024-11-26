import CodeMirror from '../thirdparty/codemirror'
import CodeCompletion from './code_completion';
import {LoadingAnimation} from '../main';


class MutantEditor {

    /**
     * @param {HTMLTextAreaElement} editorElement The text area to use as editor.
     * @param {HTMLTextAreaElement[]} dependencyEditorElements Text areas to use as dependency editors.
     * @param {?number} editableLinesStart
     *      Given by [${mutantEditor.hasEditableLinesStart() ? mutantEditor.editableLinesStart : "null"}]
     *      Null if the text should be editable from the start.
     * @param {?number} editableLinesEnd
     *      Given by [${mutantEditor.hasEditableLinesEnd() ? mutantEditor.editableLinesEnd : "null"}]
     *      Null if the text should be editable to the end.
     * @param {string} keymap
     *      Given by [${login.user.keyMap.CMName}]
     * @param {boolean} readonly
     *      Whether the editor is readonly.
     */
    constructor (editorElement, dependencyEditorElements, editableLinesStart, editableLinesEnd, keymap, readonly) {
        /**
         * The text area element of the editor.
         * @type {HTMLTextAreaElement}
         */
        this.editorElement = editorElement;
        /**
         * The CodeMirror instance used for the mutant editor.
         * @type {CodeMirror}
         */
        this.editor = null;


        /**
         * Text area elements for displaying dependencies.
         * @type {HTMLTextAreaElement[]}
         */
        this.dependencyEditorElements = dependencyEditorElements;
        /**
         * CodeMirror editors displaying dependencies.
         * @type {CodeMirror[]}
         */
        this.dependencyEditors = [];


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
         * Name of the keymap to be used in the editor.
         * @type {string}
         */
        this._keymap = keymap;
        /**
         * Code completion instance to handle the completion command.
         * @type {CodeCompletion}
         */
        this._codeCompletion = null;

        /**
         * Whether the editor should be readonly.
         * @type {boolean}
         */
        this._readonly = readonly;

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
                'Ctrl-Space': 'completeMutant',
                'Tab': "insertSoftTab"
            },
            keyMap: this._keymap,
            gutters: [
                'CodeMirror-linenumbers',
                'CodeMirror-mutantIcons'
            ],
            autoRefresh: true,
            readOnly: this._readonly
        });
        if (this._readonly) {
            this.editor.getWrapperElement().classList.add('codemirror-readonly');
        }

        /* Refresh editor when resized. */
        if (window.hasOwnProperty('ResizeObserver')) {
            new ResizeObserver(() => this.editor.refresh())
                    .observe(this.editor.getWrapperElement());
        }

        /* Set editable lines to sensible values. */
        this.initialNumLines = this.editor.lineCount();
        this.editableLinesStart = this.editableLinesStart ?? 1;
        this.editableLinesEnd = this.editableLinesEnd ?? this.initialNumLines;

        /* Prevent changes in readonly lines. */
        this.editor.on('beforeChange', function (editor, change) {
            if (change.from.line < self.editableLinesStart - 1
                    || change.to.line > self.editableLinesEnd - 1 + (editor.lineCount() - self.initialNumLines)) {
                change.cancel();
            }
        });

        this._initDependencies();
        this._initCodeCompletion();
        this._greyOutReadOnlyLines();

        LoadingAnimation.hideAnimation(this.editorElement);
    }

    /** @private */
    _initCodeCompletion () {
        this._codeCompletion = new CodeCompletion();
        this._codeCompletion.registerCodeCompletionCommand(this.editor, 'completeMutant');

        /* Gather classes to autocomplete. */
        const texts = [];
        texts.push(this.editor.getValue());
        for (const dependencyEditor of this.dependencyEditors) {
            texts.push(dependencyEditor.getValue());
        }

        /* Add words from classes to completion. */
        const completions = this._codeCompletion.getCompletionsForJavaFiles(texts);
        this._codeCompletion.setCompletionPool('classes', completions);
    }

    /** @private */
    _initDependencies () {
        for (const element of this.dependencyEditorElements) {
            const editor = CodeMirror.fromTextArea(element, {
                lineNumbers: true,
                matchBrackets: true,
                mode: 'text/x-java',
                readOnly: true,
                autoRefresh: true
            });
            editor.getWrapperElement().classList.add('codemirror-readonly');

            if (window.hasOwnProperty('ResizeObserver')) {
                new ResizeObserver(() => editor.refresh()).observe(editor.getWrapperElement());
            }

            this.dependencyEditors.push(editor);
        }
    }

    /** @private */
    _greyOutReadOnlyLines () {
        const lineCount = this.editor.lineCount();

        for (let lineNum = 0;
             lineNum < this.editableLinesStart - 1;
             lineNum++) {
            this.editor.addLineClass(lineNum, 'text', 'readonly-line');
        }

        for (let lineNum = this.editableLinesEnd + (this.editor.lineCount() - this.initialNumLines);
             lineNum < lineCount;
             lineNum++) {
            this.editor.addLineClass(lineNum, 'text', 'readonly-line');
        }
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


export default MutantEditor;

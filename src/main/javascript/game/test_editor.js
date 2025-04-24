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
     * @param {string} assertionLibrary
     *      Given by [${testEditor.assertionLibrary}]
     * @param {string} keymap
     *      Given by [${login.user.keyMap.CMName}]
     * @param {boolean} readonly
     *      Whether the editor is readonly.
     */
    constructor (editorElement, editableLinesStart, editableLinesEnd, mockingEnabled, assertionLibrary, keymap, readonly) {
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
         * The assertion library for the class. Used to determine code completions.
         * @type {string}
         */
        this._assertionLibrary = assertionLibrary;
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
                'Ctrl-Space': 'completeTest',
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
    _getTestMethods() {
        let testMethods = [];

        if (this._assertionLibrary.includes('JUNIT4')) {
            testMethods = testMethods.concat([
                    'assertArrayEquals',
                    'assertEquals',
                    'assertFalse',
                    'assertNotEquals',
                    'assertNotNull',
                    'assertNotSame',
                    'assertNull',
                    'assertSame',
                    // 'assertThat',
                    'assertThrows',
                    'assertTrue',
                    'fail'
            ]);
        }

        if (this._assertionLibrary.includes('JUNIT5')) {
            testMethods = testMethods.concat([
                    'assertAll',
                    'assertArrayEquals',
                    'assertDoesNotThrow',
                    'assertEquals',
                    'assertFalse',
                    'assertInstanceOf',
                    'assertIterableEquals',
                    'assertLinesMatch',
                    'assertNotEquals',
                    'assertNotNull',
                    'assertNotSame',
                    'assertNull',
                    'assertSame',
                    'assertThrows',
                    'assertThrowsExactly',
                    // 'assertTimeout',
                    // 'assertTimeoutPreemptively',
                    'assertTrue',
                    'fail'
            ]);
        }

        if (this._assertionLibrary.includes('HAMCREST') || this._assertionLibrary.includes('GOOGLE_TRUTH')) {
            testMethods.push('assertThat');
        }

        if (this._mockingEnabled) {
            // Answer object handling is currently not included (Mockito.doAnswer(), OngoingStubbing.then/thenAnswer
            // Calling real methods is currently not included (Mockito.doCallRealMethod / OngoingStubbing.thenCallRealMethod)
            // Behavior verification is currently not implemented (Mockito.verify)

            testMethods = testMethods.concat([
                    'mock',
                    'when',
                    'then',
                    'thenThrow',
                    'doThrow',
                    'doReturn',
                    'doNothing'
            ]);
        }

        return testMethods;
    }


    /** @private */
    _initCodeCompletion() {
        this._codeCompletion = new CodeCompletion();
        this._codeCompletion.registerCodeCompletionCommand(this.editor, 'completeTest');

        this._codeCompletion.setCompletionPool('testMethods', new Set(this._getTestMethods()));

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

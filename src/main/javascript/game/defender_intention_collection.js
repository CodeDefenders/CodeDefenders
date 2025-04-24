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
import {objects} from '../main';


class DefenderIntentionCollection {

    constructor (previouslySelectedLine) {
        /**
         * An note telling the user to select a line.
         * @type {HTMLElement}
         */
        this._lineChooseNote = document.getElementById('line-choose-note');

        /**
         * The selected line number, or null if no line is selected.
         * @type {?number}
         */
        this._selectedLine = null;

        /**
         * The (hidden) input to submit the selected line number with.
         * @type {HTMLInputElement}
         */
        this._selectedLineInput = document.getElementById('selected_lines');

        /**
         * The "Defend" button used to submit tests.
         * @type {HTMLElement}
         */
        this._submitTestButton = document.getElementById('submitTest');

        /**
         * The line that was selected for the last (failed) submission, if any.
         * @type {?number}
         */
        this._previouslySelectedLine = previouslySelectedLine;
    }

    async initAsync () {
        /**
         * The class viewer CodeMirror instance.
         * @type {CodeMirror}
         */
        this._classEditor = (await Promise.race([
            objects.await('classViewer'),
            objects.await('mutantEditor')
        ])).editor;

        /**
         * The test editor CodeMirror instance.
         * @type {CodeMirror}
         */
        this._testEditor = (await objects.await('testEditor')).editor;

        this._init();

        return this;
    }

    _init () {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Set the editor styling. */
        this._testEditor.getWrapperElement().classList.add('codemirror-readonly-toggle');

        /* Select line on class viewer gutter click. */
        this._classEditor.on('gutterClick', function (cm, line) {
            line++; // Make line 1-indexed.
            if (line !== self._selectedLine) {
                self.selectLine(line);
            } else {
                self.unselectLine();
            }
        });

        /* If there was a problem with the player's last submission, select previously selected line again,
           otherwise, initialize without a selected line. */
        if (this._previouslySelectedLine !== null) {
            self.selectLine(this._previouslySelectedLine);
        } else {
            self.unselectLine();
        }
    }

    unselectLine () {
        const previousSelectedLine = this._selectedLine;
        this._selectedLine = null;

        this._setInputValue('');
        this._lockTestEditor();
        this._removeLineMarker(previousSelectedLine);
        this._showLineChooseNote();
        this._setButtonText('Defend');
    }

    selectLine (line) {
        if (this._selectedLine !== null) {
            this._removeLineMarker(this._selectedLine);
        }

        if (line > this._classEditor.lineCount()) {
            console.log('Selected line is out of bounds.');
            this.unselectLine();
            return;
        }

        this._selectedLine = line;

        this._setInputValue(String(line));
        this._unlockTestEditor();
        this._setLineMarker(line);
        this._hideLineChooseNote();
        this._setButtonText(`Defend Line ${line}`);
    }

    /** @private */
    _setLineMarker (line) {
        const marker = document.createElement('div');
        marker.style.color = "#002cae";
        marker.classList.add('text-center');
        marker.innerHTML = '<i class="fa fa-arrow-right marker ps-1"></i>';

        this._classEditor.setGutterMarker(line - 1, 'CodeMirror-linenumbers', marker);
    }

    /** @private */
    _removeLineMarker (line) {
        this._classEditor.setGutterMarker(line - 1, 'CodeMirror-linenumbers', null);
    }

    /** @private */
    _lockTestEditor() {
        this._testEditor.setOption('readOnly', true);
        this._testEditor.getWrapperElement().classList.add('codemirror-readonly');
        this._submitTestButton.disabled = true;
    }

    /** @private */
    _unlockTestEditor() {
        this._testEditor.setOption('readOnly', false);
        this._testEditor.getWrapperElement().classList.remove('codemirror-readonly');
        this._submitTestButton.disabled = false;
    }

    /** @private */
    _showLineChooseNote() {
        this._lineChooseNote.removeAttribute('hidden');
    }

    /** @private */
    _hideLineChooseNote() {
        this._lineChooseNote.setAttribute('hidden', '');
    }

    /** @private */
    _setInputValue(value) {
        this._selectedLineInput.value = value;
    }

    /** @private */
    _setButtonText(text) {
        this._submitTestButton.innerText = text;
    }
}


export default DefenderIntentionCollection;

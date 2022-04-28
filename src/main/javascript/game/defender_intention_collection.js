import {objects} from '../main';


class DefenderIntentionCollection {

    constructor (previouslySelectedLine) {
        /**
         * An note telling the user to select a line.
         * @type {HTMLElement}
         */
        this.lineChooseNote = document.getElementById('line-choose-note');

        /**
         * The selected line number, or null if no line is selected.
         * @type {?number}
         */
        this.selectedLine = null;

        /**
         * The (hidden) input to submit the selected line number with.
         * @type {HTMLInputElement}
         */
        this.selectedLineInput = document.getElementById('selected_lines');

        /**
         * The "Defend" button used to submit tests.
         * @type {HTMLElement}
         */
        this.submitTestButton = document.getElementById('submitTest');

        this._init(previouslySelectedLine);
    }

    async initAsync () {
        /**
         * The class viewer CodeMirror instance.
         * @type {CodeMirror}
         */
        this.classEditor = (await Promise.race([
            objects.await('classViewer'),
            objects.await('mutantEditor')
        ])).editor;

        /**
         * The test editor CodeMirror instance.
         * @type {CodeMirror}
         */
        this.testEditor = (await objects.await('testEditor')).editor;
    }

    _init (previouslySelectedLine) {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Set the editor styling. */
        this.testEditor.getWrapperElement().classList.add('codemirror-readonly-toggle');

        /* Select line on class viewer gutter click. */
        this.classEditor.on('gutterClick', function (cm, line) {
            line++; // Make line 1-indexed.
            if (line !== self.selectedLine) {
                self.selectLine(line);
            } else {
                self.unselectLine();
            }
        });

        /* If there was a problem with the player's last submission, select previously selected line again,
           otherwise, initialize without a selected line. */
        if (previouslySelectedLine !== null) {
            self.selectLine(previouslySelectedLine);
        } else {
            self.unselectLine();
        }
    }

    unselectLine () {
        const previousSelectedLine = this.selectedLine;
        this.selectedLine = null;

        this._setInputValue('');
        this._lockTestEditor();
        this._removeLineMarker(previousSelectedLine);
        this._showLineChooseNote();
        this._setButtonText('Defend');
    }

    selectLine (line) {
        if (this.selectedLine !== null) {
            this._removeLineMarker(this.selectedLine);
        }

        if (line > this.classEditor.lineCount()) {
            console.log('Selected line is out of bounds.');
            this.unselectLine();
            return;
        }

        this.selectedLine = line;

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

        this.classEditor.setGutterMarker(line - 1, 'CodeMirror-linenumbers', marker);
    }

    /** @private */
    _removeLineMarker (line) {
        this.classEditor.setGutterMarker(line - 1, 'CodeMirror-linenumbers', null);
    }

    /** @private */
    _lockTestEditor() {
        this.testEditor.setOption('readOnly', true);
        this.testEditor.getWrapperElement().classList.add('codemirror-readonly');
        this.submitTestButton.disabled = true;
    }

    /** @private */
    _unlockTestEditor() {
        this.testEditor.setOption('readOnly', false);
        this.testEditor.getWrapperElement().classList.remove('codemirror-readonly');
        this.submitTestButton.disabled = false;
    }

    /** @private */
    _showLineChooseNote() {
        this.lineChooseNote.removeAttribute('hidden');
    }

    /** @private */
    _hideLineChooseNote() {
        this.lineChooseNote.setAttribute('hidden', '');
    }

    /** @private */
    _setInputValue(value) {
        this.selectedLineInput.value = value;
    }

    /** @private */
    _setButtonText(text) {
        this.submitTestButton.innerText = text;
    }
}


export default DefenderIntentionCollection;

/* Wrap in a function to avoid polluting the global scope. */
(function () {

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
         * The class viewer CodeMirror instance.
         * @type {CodeMirror}
         */
        this.classViewer = null;
        if (CodeDefenders.objects.classViewer !== null) {
            this.classViewer = CodeDefenders.objects.classViewer.editor; // Battleground mode
        } else if (CodeDefenders.objects.mutantEditor !== null) {
            this.classViewer = CodeDefenders.objects.mutantEditor.editor; // Melee mode
        }

        /**
         * The test editor CodeMirror instance.
         * @type {CodeMirror}
         */
        this.testEditor = CodeDefenders.objects.testEditor.editor;

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

    _init (previouslySelectedLine) {
        /* Bind "this" to safely use it in callback functions. */
        const self = this;

        /* Set the editor styling. */
        this.testEditor.getWrapperElement().classList.add('codemirror-readonly-toggle');

        /* Select line on class viewer gutter click. */
        this.classViewer.on('gutterClick', function (cm, line) {
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

        if (line > this.classViewer.lineCount()) {
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

        this.classViewer.setGutterMarker(line - 1, 'CodeMirror-linenumbers', marker);
    }

    /** @private */
    _removeLineMarker (line) {
        this.classViewer.setGutterMarker(line - 1, 'CodeMirror-linenumbers', null);
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

CodeDefenders.classes.DefenderIntentionCollection ??= DefenderIntentionCollection;

})();

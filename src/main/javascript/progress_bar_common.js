/* Wrap in a function to avoid polluting the global scope. */
(function () {

class ProgressBar {
    constructor(progressElement) {
        /**
         * The container element of the progress bar.
         * @type {HTMLDivElement}
         */
        this.progressElement = progressElement;

        /**
         * The variable-width bar element of the progress bar.
         * @type {HTMLDivElement}
         */
        this.barElement = progressElement.children[0];
    }

    /**
     * Hides the progress bar.
     */
    hide () {
        this.progressElement.setAttribute('hidden', '');
    }

    /**
     * Sets the progress of the progress bar.
     * @param {number} value The new width of the progress bar, as a number between 0 and 100.
     * @param {string} text The text to display on the progress bar.
     */
    setProgress (value, text) {
        this.progressElement.removeAttribute('hidden');
        this.barElement.setAttribute('aria-valuenow', String(value));
        this.barElement.style.width = value + '%';
        this.barElement.textContent = text;
    }
}

CodeDefenders.classes.ProgressBar ??= ProgressBar;

})();

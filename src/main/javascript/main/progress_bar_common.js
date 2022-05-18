class ProgressBar {
    constructor(progressElement) {
        /**
         * The container element of the progress bar.
         * @type {HTMLDivElement}
         */
        this._progressElement = progressElement;

        /**
         * The variable-width bar element of the progress bar.
         * @type {HTMLDivElement}
         */
        this._barElement = progressElement.children[0];
    }

    /**
     * Hides the progress bar.
     */
    hide () {
        this._progressElement.setAttribute('hidden', '');
    }

    /**
     * Sets the progress of the progress bar.
     * @param {number} value The new width of the progress bar, as a number between 0 and 100.
     * @param {string} text The text to display on the progress bar.
     */
    setProgress (value, text) {
        this._progressElement.removeAttribute('hidden');
        this._barElement.setAttribute('aria-valuenow', String(value));
        this._barElement.style.width = value + '%';
        this._barElement.textContent = text;
    }
}


export default ProgressBar;

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

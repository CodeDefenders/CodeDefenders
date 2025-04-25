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
class GameTimeManager {

    /**
     * @param {String} selector the selector of the elements holding the start time and duration data. The remaining
     *                          time will be rendered in this element.
     * @param {Number} updateInterval the time in seconds between each render
     */
    constructor(selector, updateInterval) {
        const elements = [...document.querySelectorAll(selector)];
        elements.forEach(this.renderElement.bind(this)); // initial render
        this._renderer = elements.filter(this.needsUpdating.bind(this));
        this._interval = setInterval(this.render.bind(this), updateInterval * 1e3);
    }

    /**
     * Checks whether the element needs to be updated periodically.
     *
     * @param element {HTMLElement} the element
     * @returns {boolean} whether the elements needs to be updated periodically
     */
    needsUpdating(element) {
         return element.dataset.type === 'remaining' || element.dataset.type === 'progress'
             || element.dataset.type === 'end' && element.dataset.start === '-1';
    }

    /**
     * Updates all elements
     */
    render() {
        this._renderer.forEach(this.renderElement.bind(this));
    }

    /**
     * Updates the given element
     *
     * @param element {HTMLElement} the element to render. Has to contain the necessary data
     */
    renderElement(element) {
        const duration = Number(element.dataset.duration);
        const type = element.dataset.type;

        let start = Number(element.dataset.start);
        if (start === -1) {
            start = Date.now() / 1e3;
        }

        switch (type) {
            case 'total':
                this.renderTotal(element, duration);
                break;
            case 'remaining':
                this.renderRemaining(element, start, duration);
                break;
            case 'end':
                this.renderEnd(element, start, duration);
                break;
            case 'elapsed':
                this.renderElapsed(element, start);
                break;
            case 'progress':
                this.renderProgress(element, start, duration);
                break;
        }
    }

    /**
     * Renders the total duration in the target element.
     */
    renderTotal(element, duration) {
        element.innerText = GameTime.formatTime(duration);
    }

    /**
     * Renders the remaining duration in the target element.
     */
    renderRemaining(element, start, duration) {
        const remainingMinutes = GameTime.calculateRemainingTime(start, duration);
        element.innerText = GameTime.formatTime(remainingMinutes);
    }

    /**
     * Renders the end date in the target element. If the end date is today, only the time is rendered.
     */
    renderEnd(element, start, duration) {
        const endDate = GameTime.calculateEndDate(start, duration);
        element.innerText = GameTime.formatDate(endDate);
    }

    /**
     * Renders the elapsed duration in the target element.
     */
    renderElapsed(element, start) {
        const elapsedMinutes = GameTime.calculateElapsedMinutes(start);
        element.innerText = GameTime.formatTime(elapsedMinutes);
    }

    /**
     * Sets the width of the target element according to the percentage of the game duration that has passed.
     */
    renderProgress(element, start, duration) {
        const percentage = GameTime.calculateElapsedPercentage(start, duration) * 100;
        element.style.width = `${percentage}%`;
        element.ariaValueNow = percentage;
    }
}

class GameTimeValidator {

    /**
     * Maps a time unit to the according query-selector
     * @callback inputSelector
     * @param {string} unit
     * @return {string}
     */

    /**
     * Calculate the elapsed minutes for the current game
     * @callback calculateElapsedMinutes
     * @return {Number}
     */

    /**
     *
     * @param {Number} maxDurationMinutes
     * @param {?Number} defaultDurationMinutes
     * @param {HTMLInputElement} minutesInput
     * @param {HTMLInputElement} hoursInput
     * @param {HTMLInputElement} daysInput
     * @param {HTMLInputElement} totalField
     */
    constructor(maxDurationMinutes, defaultDurationMinutes,
                minutesInput, hoursInput, daysInput, totalField) {
        this.maxDurationMinutes = maxDurationMinutes;
        this.defaultDurationMinutes = defaultDurationMinutes;

        this.minutesInput = minutesInput;
        this.hoursInput = hoursInput;
        this.daysInput = daysInput;
        this.inputs = [minutesInput, hoursInput, daysInput];

        this.totalField = totalField;

        // init hooks
        const doNothing = () => {};
        this._onInvalidDuration = doNothing;
        this._onValidDuration = doNothing;

        if (this.defaultDurationMinutes !== null) {
            this.setDefaults();
        }

        for (const input of this.inputs) {
            input.addEventListener('input', this.validateAndSetDuration.bind(this));
        }
        this.validateAndSetDuration();
    }

    setDefaults() {
        let minutes = this.defaultDurationMinutes;
        let hours = 0;
        let days = 0;

        if (minutes >= 60) {
            hours = Math.floor(minutes / 60);
            minutes %= 60;
        }

        if (hours >= 24) {
            days = Math.floor(hours / 24);
            hours %= 24;
        }

        if (days > 0) this.daysInput.value = days;
        if (hours > 0) this.hoursInput.value = hours;
        if (minutes > 0) this.minutesInput.value = minutes;
    }

    setValidity(customValidity) {
        this.minutesInput.setCustomValidity(customValidity);
        this.hoursInput.setCustomValidity(customValidity);
        this.daysInput.setCustomValidity(customValidity);
    }

    validateAndSetDuration() {
        const hasValue = this.inputs.some(input => input.value.length > 0);
        if (!hasValue) {
            this.setValidity('missing-value');
            return;
        }

        const days = Number(this.daysInput.value);
        const hours = Number(this.hoursInput.value);
        const minutes = Number(this.minutesInput.value);

        const newRemainingDuration = ((days * 24) + hours) * 60 + minutes;
        this.totalField.value = String(newRemainingDuration);

        if (newRemainingDuration < 0 || newRemainingDuration > this.maxDurationMinutes) {
            this.setValidity('invalid-value');
            this._onInvalidDuration();
            return;
        }

        this.setValidity('');
        this._onValidDuration();
    }

    set onInvalidDuration(callback) {
        this._onInvalidDuration = callback;
    }

    set onValidDuration(callback) {
        this._onValidDuration = callback;
    }
}

class GameTime {
    /**
     * Calculates the remaining time in seconds
     *
     * @param start {Number} the start time in seconds since epoch (Unix timestamp)
     * @param duration {Number} the games duration in minutes
     * @returns {number} the remaining time in minutes (rounded and always >= 0)
     */
    static calculateRemainingTime(start, duration) {
        const currentSeconds = Date.now() / 1e3;
        const deltaSeconds = currentSeconds - start;
        const remainingSeconds = (duration * 60) - deltaSeconds;
        const remainingMinutes = Math.round(remainingSeconds / 60);
        return Math.max(0, remainingMinutes);
    }

    /**
     * Calculates the end date of the game.
     *
     * @param start {Number} the start time in seconds since epoch (Unix timestamp)
     * @param duration {Number} the games duration in minutes
     * @returns {Date} the end date of the game
     */
    static calculateEndDate(start, duration) {
        const durationSecs = duration * 60;
        return new Date((start + durationSecs) * 1e3);
    }

    /**
     * Calculates how much of the time has passed.
     *
     * @param start {Number} the start time in seconds since epoch (Unix timestamp)
     * @returns {number} how much of the time has passed in minutes
     */
    static calculateElapsedMinutes(start) {
        const currentSeconds = Date.now() / 1e3;
        const elapsedSeconds = currentSeconds - start;
        return elapsedSeconds / 60;
    }

    /**
     * Calculates how much of the time has passed.
     *
     * @param start {Number} the start time in seconds since epoch (Unix timestamp)
     * @param duration {Number} the games duration in minutes
     * @returns {number} how much of the time has passed, as a percentage between 0.0 and 1.0
     */
    static calculateElapsedPercentage(start, duration) {
        const percentage = GameTime.calculateElapsedMinutes(start) / duration;
        return Math.min(1.0, Math.max(0.0, percentage));
    }

    /**
     * Renders the given date. If the date is today, only the time is rendered. Otherwise, the whole
     * date is rendered.
     *
     * @param date {Date} the date to format
     * @returns {string} the formatted string
     */
    static formatDate(date) {
        const now = new Date();
        const timeString = date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
        if (date.getFullYear() === now.getFullYear()
                && date.getMonth() === now.getMonth()
                && date.getDate() === now.getDate()) {
            return timeString;
        } else {
            const dateString = date.toLocaleDateString();
            return `${dateString}, ${timeString}`;
        }
    }

    /**
     * Converts the given duration in minutes to a string with the format "Xd Yh Zm" or "Yh Zm" or "Zm".
     *
     * @param pMinutes {Number} the duration in minutes
     * @returns {string} the string representation
     */
    static formatTime(pMinutes) {
        let minutes = pMinutes;
        if (minutes <= 0) {
            return '0min';
        }

        let hours = 0;
        let days = 0;

        if (minutes >= 60) {
            hours = Math.floor(minutes / 60);
            minutes %= 60;
        }

        if (hours >= 24) {
            days = Math.floor(hours / 24);
            hours %= 24;
        }

        let result = [];
        if (days > 0) result.push(days + 'd');
        if (hours > 0) result.push(hours + 'h');
        if (minutes > 0) result.push(Math.round(minutes) + 'min');
        return result.join(' ');
    }
}

export {
    GameTimeManager,
    GameTimeValidator,
    GameTime
};

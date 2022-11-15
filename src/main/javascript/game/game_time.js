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
        element.innerText = toMixedUnitString(duration);
    }

    /**
     * Renders the remaining duration in the target element.
     */
    renderRemaining(element, start, duration) {
        const remainingMinutes = this.calculateRemainingTime(start, duration);
        element.innerText = toMixedUnitString(remainingMinutes);
    }

    /**
     * Renders the end date in the target element. If the end date is today, only the time is rendered.
     */
    renderEnd(element, start, duration) {
        const endDate = this.calculateEndDate(start, duration);
        element.innerText = this.formatDate(endDate);
    }

    /**
     * Renders the elapsed duration in the target element.
     */
    renderElapsed(element, start) {
        const elapsedMinutes = this.calculateElapsedMinutes(start);
        element.innerText = toMixedUnitString(elapsedMinutes);
    }

    /**
     * Sets the width of the target element according to the percentage of the game duration that has passed.
     */
    renderProgress(element, start, duration) {
        const percentage = this.calculateElapsedPercentage(start, duration) * 100;
        element.style.width = `${percentage}%`;
        element.ariaValueNow = percentage;
    }

    /**
     * Calculates the remaining time in seconds
     *
     * @param start {Number} the start time in seconds since epoch (Unix timestamp)
     * @param duration {Number} the games duration in minutes
     * @returns {number} the remaining time in minutes (rounded and always >= 0)
     */
    calculateRemainingTime(start, duration) {
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
    calculateEndDate(start, duration) {
        const durationSecs = duration * 60;
        return new Date((start + durationSecs) * 1e3);
    }

    /**
     * Calculates how much of the time has passed.
     *
     * @param start {Number} the start time in seconds since epoch (Unix timestamp)
     * @returns {number} how much of the time has passed in minutes
     */
    calculateElapsedMinutes(start) {
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
    calculateElapsedPercentage(start, duration) {
        const percentage = this.calculateElapsedMinutes(start) / duration;
        return Math.min(1.0, Math.max(0.0, percentage));
    }

    /**
     * Renders the given date. If the date is today, only the time is rendered. Otherwise, the whole
     * date is rendered.
     *
     * @param date {Date} the date to format
     * @returns {string} the formatted string
     */
    formatDate(date) {
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
}

class GameTimeValidator {

    /**
     * Maps a time unit to the according query-selector
     * @callback inputSelector
     * @param {string} unit
     * @return {string}
     */

    /**
     *
     * @param {Number} MAXIMUM_DURATION_MINUTES
     * @param {Number} DEFAULT_DURATION_MINUTES
     * @param {Array<string>=} units
     * @param {inputSelector=} inputSelector
     * @param {string=} totalInputSelector
     * @param {string=} maxDurationOutputSelector
     */
    constructor(MAXIMUM_DURATION_MINUTES, DEFAULT_DURATION_MINUTES, units = ['days', 'hours', 'minutes'],
                inputSelector = unit => `#${unit}-input`, totalInputSelector = '#gameDurationMinutes',
                maxDurationOutputSelector = '#displayMaxDuration') {
        this.MAXIMUM_DURATION_MINUTES = MAXIMUM_DURATION_MINUTES;
        this.DEFAULT_DURATION_MINUTES = DEFAULT_DURATION_MINUTES;
        /** @type Array<string> */
        this.units = units;
        this.inputs = {};
        this.units.forEach(unit => this.inputs[unit] = document.querySelector(inputSelector(unit)));
        this.totalInput = document.querySelector(totalInputSelector);
        document.querySelector(maxDurationOutputSelector).innerText = toMixedUnitString(this.MAXIMUM_DURATION_MINUTES);
        this.setDefaults();
        this.units.forEach(u => this.inputs[u].addEventListener('input', this.validateAndSetDuration.bind(this)));
        this.validateAndSetDuration();
    }

    setDefaults() {
        let minutes = this.DEFAULT_DURATION_MINUTES;
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

        if (days > 0) this.inputs.days.value = days;
        if (hours > 0) this.inputs.hours.value = hours;
        if (minutes > 0) this.inputs.minutes.value = minutes;
    }

    setValidity(customValidity) {
        this.units.forEach(u => this.inputs[u].setCustomValidity(customValidity));
    }

    validateAndSetDuration() {
        const hasValue = this.units.some(u => this.inputs[u].value.length > 0);
        if (!hasValue) {
            this.setValidity('missing-value');
            return;
        }

        const days = Number(this.inputs.days.value);
        const hours = Number(this.inputs.hours.value);
        const minutes = Number(this.inputs.minutes.value);
        const total = ((days * 24) + hours) * 60 + minutes;

        this.totalInput.value = total;

        if (total <= 0 || total > this.MAXIMUM_DURATION_MINUTES) {
            this.setValidity('invalid-value');
            return;
        }

        this.setValidity('');
    }

}

/**
 * Converts the given duration in minutes to a string with the format "Xd Yh Zm" or "Yh Zm" or "Zm".
 *
 * @param pMinutes {Number} the duration in minutes
 * @returns {string} the string representation
 */
function toMixedUnitString(pMinutes) {
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

    let result = '';
    if (days > 0) result += days + 'd ';
    if (hours > 0) result += hours + 'h ';
    if (minutes > 0) result += Math.round(minutes) + 'min';
    return result;
}

export {GameTimeManager, GameTimeValidator};

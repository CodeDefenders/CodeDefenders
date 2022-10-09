class GameTimeManager {

    /**
     *
     * @param {String} selector the selector of the element holding the start time and duration data. The remaining time
     *                          will be rendered in this element.
     * @param {Number} updateInterval the time in seconds between each render
     */
    constructor(selector, updateInterval) {
        this._renderer = [...document.querySelectorAll(selector)];
        this._interval = setInterval(this.render.bind(this), updateInterval * 1e3);
        this.render();
        this.addToolTip();
    }

    /**
     * Adds the end time as a tooltip to the elements.
     */
    addToolTip() {
        this._renderer.forEach((element) => {
            if (element.dataset.showTitle === 'false') return;
            const end = this.calculateEndDate(element).toLocaleString();
            const prefix = element.dataset.titlePrefix || '';
            element.title = `${prefix} Game ends at ${end}`.trim();
        });
    }

    /**
     * Calculates the end date of the game using the data attributes of the given element
     *
     * @param element {HTMLElement} the element to get the data attributes from
     * @returns {Date} the end date of the game
     */
    calculateEndDate(element) {
        const start = Number(element.dataset.startTime);
        const duration = Number(element.dataset.totalMin) * 60;
        return new Date((start + duration) * 1e3);
    }

    /**
     * Updates the remaining time in all elements
     */
    render() {
        this._renderer.forEach(this.renderElement.bind(this));
    }

    /**
     * Renders the remaining time in the given element
     *
     * @param element {HTMLElement} the element to render the remaining time in. Has to contain the start time and duration data
     */
    renderElement(element) {
        let renderedTime;
        if (element.dataset.renderOnlyEndTime === 'true') {
            renderedTime = this.calculateEndDate(element).toLocaleString();
        } else {
            const start = Number(element.dataset.startTime);
            const duration = Number(element.dataset.totalMin);
            const remainingMinutes = this.calculateRemainingTime(start, duration);
            const showMixedUnits = element.dataset.showMixedUnits !== 'false';
            renderedTime = showMixedUnits ? this.toMixedUnitString(remainingMinutes) : remainingMinutes;
        }

        const prefix = element.dataset.innerTextPrefix || '';
        const suffix = element.dataset.innerTextSuffix || '';
        element.innerText = `${prefix} ${renderedTime} ${suffix}`.trim();
    }

    /**
     * Calculates the remaining time in minutes
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
        return remainingMinutes < 0 ? 0 : remainingMinutes;
    }

    /**
     * Converts the given duration in minutes to a string with the format "Xd Yh Zm" or "Yh Zm" or "Zm".
     *
     * @param pMinutes {Number} the duration in minutes
     * @returns {string} the string representation
     */
    toMixedUnitString(pMinutes) {
        let minutes = pMinutes;
        let hours = 0;
        let days = 0;

        if (minutes > 60) {
            hours = Math.floor(minutes / 60);
            minutes %= 60;
        }

        if (hours > 24) {
            days = Math.floor(hours / 24);
            hours %= 24;
        }

        const daysString = days > 0 ? `${days}d ` : '';
        const hoursString = hours > 0 ? `${hours}h ` : '';
        const minutesString = minutes > 0 ? `${minutes}min` : '0';
        return `${daysString}${hoursString}${minutesString}`;
    }
}

export default GameTimeManager;
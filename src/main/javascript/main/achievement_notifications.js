/**
 * @typedef {Object} Achievement
 * @property {Number} achievementId
 * @property {Number} level
 * @property {String} name
 * @property {String} description
 * @property {String} progressText
 * @property {Number} metricForCurrentLevel
 * @property {Number} metricForNextLevel
 * @property {Number} metricCurrent
 */

/**
 * @typedef {Object} AchievementNotification
 * @property {Achievement} achievement
 * @property {HTMLElement} element
 * @property {Timeout} timer
 */

/**
 * Shows notifications for achievements.
 */
class AchievementNotifications {

    /**
     * @param {String} achievementIconPath
     * @param {String} profilePath
     * @param {Number} notificationsVisibleTime time in seconds
     */
    constructor(achievementIconPath, profilePath, notificationsVisibleTime = 8) {
        /** @type {HTMLElement} */
        this._notificationContainer = document.createElement('ul');
        this._notificationContainer.classList.add('achievement-notifications');
        // this._notificationContainer.style.top = document.getElementById('header').offsetHeight + 'px';
        document.body.appendChild(this._notificationContainer);

        /** @type {AchievementNotification[]} */
        this._currentNotifications = [];

        /** @type {Number} time in ms */
        this._notificationsVisibleTime = notificationsVisibleTime * 1e3;

        /** @type {String} */
        this._achievementIconPath = achievementIconPath;
        /** @type {String} */
        this._profilePath = profilePath;
    }

    /**
     * @public
     * @param {Achievement} achievement
     * @returns {AchievementNotification} the notification that is shown
     */
    showAchievementNotification(achievement) {
        const notification = {
            achievement: achievement,
            element: this._createNotificationElement(achievement),
            timer: null
        };
        this._currentNotifications.push(notification);
        this._notificationContainer.appendChild(notification.element);
        notification.timer = setTimeout(
            this._removeNotification.bind(this, notification),
            this._notificationsVisibleTime
        );
        notification.element.querySelector('.btn-close').addEventListener(
            'click',
            this._removeNotification.bind(this, notification)
        );
        return notification;
    }

    /**
     * @private
     * @param {Achievement} achievement
     * @returns {HTMLElement}
     */
    _createNotificationElement(achievement) {
        const element = document.createElement('li');
        element.classList.add('achievement-notification');
        element.innerHTML = `
            <a href="${this._profilePath}" class="achievement-notification__link" target="_blank" rel="noopener" 
                    title="Show all achievements on the profile page">
                <div class="achievement-notification__icon">
                    <img src="${this._achievementIconPath}codedefenders_achievements_${achievement.achievementId}_lvl_${achievement.level}.png" 
                         alt="Icon ${achievement.name} (Level ${achievement.level})">
                </div>
                <div class="achievement-notification__text">
                    <p class="achievement-notification__title">New Achievement Unlocked:<br>${achievement.name} (Level ${achievement.level})</p>
                    <!--p class="achievement-notification__description">${this._getDescription(achievement)}</p-->
                    <p class="achievement-notification__progress">${this._getProgressText(achievement)}</p>
                </div>
            </a>
            <button type="button" class="btn-close" aria-label="Close"></button>
        `;
        return element;
    }

    /**
     * @private
     * @param {Achievement} achievement
     * @return {string} the progress text with the current and next level metric inserted
     */
    _getProgressText(achievement) {
        return achievement.progressText
            .replace('{0}', achievement.metricCurrent.toString())
            .replace('{1}', achievement.metricForNextLevel.toString());
    }

    /**
     * @private
     * @param {Achievement} achievement
     * @return {string} the description with the current level metric inserted
     */
    _getDescription(achievement) {
        return achievement.description.replace('{0}', achievement.metricForCurrentLevel.toString());
    }

    /**
     * @private
     * @param {AchievementNotification} notification
     */
    _removeNotification(notification) {
        this._notificationContainer.removeChild(notification.element);
        this._currentNotifications.splice(this._currentNotifications.indexOf(notification), 1);
        clearTimeout(notification.timer);
    }
}

export default AchievementNotifications;
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

import show_toasts from "./show_toasts";

/**
 * Shows notifications for achievements.
 */
class AchievementNotifications {

    /**
     * @public
     * @param {Achievement} achievement
     * @param {String} achievementPath
     * @param {String} profilePath
     */
    static showAchievementNotification(achievement, achievementPath, profilePath) {
        const progressText = achievement.progressText
                .replace('{0}', achievement.metricCurrent.toString())
                .replace('{1}', achievement.metricForNextLevel.toString());

        show_toasts.showToast({
            colorClass: "",
            title: "New Achievement Unlocked:\n" + achievement.name,
            secondary: "Level " + achievement.level,
            body: progressText,
            icon:  achievementPath + "codedefenders_achievements_" + achievement.achievementId
                    + "_lvl_" + achievement.level + ".png",
            link: profilePath,
            longTimeout: true
        })
    }
}

export default AchievementNotifications;

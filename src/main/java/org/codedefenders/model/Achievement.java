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
package org.codedefenders.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.xnap.commons.i18n.I18n;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Achievement implements Serializable {
    @Expose
    @SerializedName("achievementId")
    private final AchievementType achievementType;
    @Expose
    private final int level;
    @Expose
    private final int index;
    @Expose
    private String name;
    @Expose
    private String description;
    @Expose
    private String progressText;
    @Expose
    private final int metricForCurrentLevel;
    @Expose
    private final Integer metricForNextLevel;
    @Expose
    private int metricCurrent;

    /**
     * Constructs an Achievement from its type, current level, and the user's current metric.
     * The name, description, progressText, and metric thresholds are derived from the {@link AchievementType}.
     *
     * @param type          the achievement type
     * @param level         the user's current level for this achievement
     * @param metricCurrent the user's current metric value
     */
    public Achievement(AchievementType type, int level, int metricCurrent) {
        AchievementLevel currentLevel = type.getLevel(level);
        AchievementLevel nextLevel = type.getLevel(level + 1);

        this.achievementType = type;
        this.level = level;
        this.index = type.getIndex();
        this.name = currentLevel.name();
        this.description = currentLevel.description();
        this.progressText = currentLevel.progressText();
        this.metricForCurrentLevel = currentLevel.metric();
        this.metricForNextLevel = nextLevel != null ? nextLevel.metric() : null;
        this.metricCurrent = metricCurrent;
    }

    public AchievementType getType() {
        return achievementType;
    }

    public int getLevel() {
        return level;
    }

    public int getIndex() {
        return index;
    }

    public String getName(I18n i18n) {
        return i18n.tr(name, metricForCurrentLevel);
    }

    public String getDescription(I18n i18n) {
        return i18n.tr(description, metricForCurrentLevel);
    }

    public String getProgressText(I18n i18n) {
        return format(progressText, i18n);
    }

    private String format(String text, I18n i18n) {
        List<Integer> args = new ArrayList<>();
        args.add(metricCurrent);
        getNumMetricNeededForNextLevel().ifPresent(args::add);
        return i18n.tr(text, args.toArray());
    }

    public int getMetricForCurrentLevel() {
        return metricForCurrentLevel;
    }

    public Optional<Integer> getNumMetricNeededForNextLevel() {
        return Optional.ofNullable(metricForNextLevel);
    }

    public int getMetricCurrent() {
        return metricCurrent;
    }

    public Optional<Float> getProgress() {
        return getNumMetricNeededForNextLevel().map(metricForNextLevel -> 100f * metricCurrent / metricForNextLevel);
    }

    public boolean updateCurrentMetric(int metricChange) {
        metricCurrent += metricChange;
        return reachedNextLevel();
    }

    public boolean reachedNextLevel() {
        return getNumMetricNeededForNextLevel()
                .map(metricForNextLevel -> metricCurrent >= metricForNextLevel)
                .orElse(false);
    }

    public boolean isMaxLevel() {
        return getNumMetricNeededForNextLevel().isEmpty();
    }

    public Achievement translate(I18n i18n) {
        var n = new Achievement(achievementType, level, metricCurrent);
        n.name = i18n.tr(n.name);
        n.description = i18n.tr(n.description);
        n.progressText = i18n.tr(n.progressText);
        return n;
    }
}

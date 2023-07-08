package org.codedefenders.model;

import java.io.Serializable;

public class Achievement implements Serializable {
    private final int achievementId;
    private final int level;
    private final String name;
    private final int metricForNextLevel;
    private int metricCurrent = 0;

    public Achievement(int achievementId, int level, String name, int metricForNextLevel) {
        this.achievementId = achievementId;
        this.level = level;
        this.name = name;
        this.metricForNextLevel = metricForNextLevel;
    }

    public Achievement(int achievementId, int level, String name, int metricForNextLevel, int metricCurrent) {
        this(achievementId, level, name, metricForNextLevel);
        this.metricCurrent = metricCurrent;
    }

    public int getId() {
        return achievementId;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public int getNumMetricNeededForNextLevel() {
        return metricForNextLevel;
    }

    public boolean updateCurrentMetric(int metricChange) {
        metricCurrent += metricChange;
        return reachedNextLevel();
    }

    public boolean reachedNextLevel() {
        return metricCurrent >= metricForNextLevel;
    }

}

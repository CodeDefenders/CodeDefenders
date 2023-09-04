package org.codedefenders.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Achievement implements Serializable {
    private final Id achievementId;
    private final int level;
    private final String name;
    private final String description;
    private final int metricForCurrentLevel;
    private final Optional<Integer> metricForNextLevel;
    private int metricCurrent = 0;

    public Achievement(Id achievementId, int level, String name, String description, int metricForCurrentLevel,
                       Optional<Integer> metricForNextLevel) {
        this.achievementId = achievementId;
        this.level = level;
        this.name = name;
        this.description = MessageFormat.format(description, metricForCurrentLevel);
        this.metricForCurrentLevel = metricForCurrentLevel;
        this.metricForNextLevel = metricForNextLevel;
    }

    public Achievement(Id achievementId, int level, String name, String description, int metricForCurrentLevel,
                       Optional<Integer> metricForNextLevel, int metricCurrent) {
        this(achievementId, level, name, description, metricForCurrentLevel, metricForNextLevel);
        this.metricCurrent = metricCurrent;
    }

    public Id getId() {
        return achievementId;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getMetricForCurrentLevel() {
        return metricForCurrentLevel;
    }

    public Optional<Integer> getNumMetricNeededForNextLevel() {
        return metricForNextLevel;
    }

    public int getMetricCurrent() {
        return metricCurrent;
    }

    public Optional<Float> getProgress() {
        return metricForNextLevel.map(metricForNextLevel -> 100f * metricCurrent / metricForNextLevel);
    }

    public boolean updateCurrentMetric(int metricChange) {
        metricCurrent += metricChange;
        return reachedNextLevel();
    }

    public boolean reachedNextLevel() {
        return metricForNextLevel.map(metricForNextLevel -> metricCurrent >= metricForNextLevel).orElse(false);
    }

    public boolean isMaxLevel() {
        return metricForNextLevel.isEmpty();
    }

    public enum Id {
        PLAY_GAMES(0),
        PLAY_AS_ATTACKER(1),
        PLAY_AS_DEFENDER(2),
        PLAY_MELEE_GAMES(3),
        WRITE_TESTS(4),
        CREATE_MUTANTS(5);

        private final int id;
        private static final Map<Integer, Id> MAP = new HashMap<>();

        static {
            Arrays.stream(Id.values()).forEach(
                    achievementId -> MAP.put(achievementId.getAsInt(), achievementId));
        }

        Id(int id) {
            this.id = id;
        }

        public int getAsInt() {
            return id;
        }

        public static Id fromInt(int id) {
            return MAP.get(id);
        }
    }

}

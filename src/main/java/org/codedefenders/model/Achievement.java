package org.codedefenders.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Achievement implements Serializable {
    @Expose
    private final Id achievementId;
    @Expose
    private final int level;
    @Expose
    private final int index;
    @Expose
    private final String name;
    @Expose
    private final String description;
    @Expose
    private final String progressText;
    @Expose
    private final int metricForCurrentLevel;
    @Expose
    private final Integer metricForNextLevel;
    @Expose
    private int metricCurrent = 0;

    public Achievement(Id achievementId, int level, int index, String name, String description, String progressText,
                       int metricForCurrentLevel, Optional<Integer> metricForNextLevel) {
        this.achievementId = achievementId;
        this.level = level;
        this.index = index;
        this.name = name;
        this.description = MessageFormat.format(description, metricForCurrentLevel);
        this.progressText = progressText;
        this.metricForCurrentLevel = metricForCurrentLevel;
        this.metricForNextLevel = metricForNextLevel.orElse(null);
    }

    public Achievement(Id achievementId, int level, int index, String name, String description, String progressText,
                       int metricForCurrentLevel, Optional<Integer> metricForNextLevel, int metricCurrent) {
        this(achievementId, level, index, name, description, progressText, metricForCurrentLevel, metricForNextLevel);
        this.metricCurrent = metricCurrent;
    }

    public Id getId() {
        return achievementId;
    }

    public int getLevel() {
        return level;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getProgressText() {
        List<Integer> args = new ArrayList<>();
        args.add(metricCurrent);
        getNumMetricNeededForNextLevel().ifPresent(args::add);
        return MessageFormat.format(progressText, args.toArray());
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

    public enum Id {
        @SerializedName("0")
        PLAY_GAMES(0),
        @SerializedName("1")
        PLAY_AS_ATTACKER(1),
        @SerializedName("2")
        PLAY_AS_DEFENDER(2),
        @SerializedName("3")
        PLAY_MELEE_GAMES(3),
        @SerializedName("4")
        WRITE_TESTS(4),
        @SerializedName("5")
        CREATE_MUTANTS(5),
        @SerializedName("6")
        WIN_GAMES(6),
        @SerializedName("7")
        WIN_GAMES_AS_ATTACKER(7),
        @SerializedName("8")
        WIN_GAMES_AS_DEFENDER(8),
        @SerializedName("9")
        SOLVE_PUZZLES(9),
        ; // format for better git diffs

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

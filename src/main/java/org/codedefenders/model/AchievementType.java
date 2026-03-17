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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xnap.commons.i18n.I18n;

import com.google.gson.annotations.SerializedName;

/**
 * Defines all achievement types with their level data.
 * Replaces the old {@code achievements} database table.
 */
public enum AchievementType {
    @SerializedName("0")
    PLAY_GAMES(0, 0),
    @SerializedName("1")
    PLAY_AS_ATTACKER(1, 100),
    @SerializedName("2")
    PLAY_AS_DEFENDER(2, 200),
    @SerializedName("3")
    PLAY_MELEE_GAMES(3, 300),
    @SerializedName("4")
    WRITE_TESTS(4, 800),
    @SerializedName("5")
    CREATE_MUTANTS(5, 900),
    @SerializedName("6")
    WIN_GAMES(6, 400),
    @SerializedName("7")
    WIN_GAMES_AS_ATTACKER(7, 500),
    @SerializedName("8")
    WIN_GAMES_AS_DEFENDER(8, 600),
    @SerializedName("9")
    SOLVE_PUZZLES(9, 700),
    @SerializedName("10")
    WRITE_CLEAN_TESTS(10, 1000),
    @SerializedName("11")
    KILL_MUTANTS(11, 1100),
    @SerializedName("12")
    TOTAL_COVERAGE(12, 1200),
    @SerializedName("13")
    MAX_COVERAGE(13, 1300),
    @SerializedName("14")
    WIN_EQUIVALENCE_DUELS(14, 1400),
    @SerializedName("15")
    WIN_EQUIVALENCE_DUELS_AS_ATTACKER(15, 1500),
    @SerializedName("16")
    WIN_EQUIVALENCE_DUELS_AS_DEFENDER(16, 1600),
    @SerializedName("17")
    MAX_TESTS_IN_SHORT_TIME(17, 1700),
    @SerializedName("18")
    MAX_MUTANTS_IN_SHORT_TIME(18, 1800),
    @SerializedName("19")
    PUZZLES_SOLVED_ON_FIRST_TRY(19, 750),
    ; // format for better git diffs

    private final int id;
    private final int index;
    private final Map<Integer, AchievementLevel> levels = new LinkedHashMap<>();
    private static final Map<Integer, AchievementType> BY_ID = new HashMap<>();

    AchievementType(int id, int index) {
        this.id = id;
        this.index = index;
    }

    public int getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public AchievementLevel getLevel(int level) {
        return levels.get(level);
    }

    public int getMaxLevel() {
        return levels.keySet().stream().mapToInt(i -> i).max().orElse(0);
    }

    public static AchievementType fromInt(int id) {
        return BY_ID.get(id);
    }


    private AchievementType setLevel(int level, AchievementLevel levelInfo) {
        levels.put(level, levelInfo);
        return this;
    }

    private AchievementType setLevel(int level, String name, String description, String progressText, int metric) {
        return setLevel(level, new AchievementLevel(name, description, progressText, metric));
    }

    static {
        Arrays.stream(values()).forEach(type -> BY_ID.put(type.id, type));

        PLAY_GAMES
                .setLevel(0,
                        I18n.marktr("No games played yet"),
                        I18n.marktr("Play your first game to unlock this achievement"),
                        I18n.marktr("{0} of {1} games played"), 0)
                .setLevel(1,
                        I18n.marktr("Newbie"),
                        I18n.marktr("Play your first game!"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Player"),
                        I18n.marktr("Play {0} games"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("Silver Player"),
                        I18n.marktr("Play {0} games"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("Gold Player"),
                        I18n.marktr("Play {0} games"),
                        I18n.marktr("{0} games played, max level reached"), 50);

        PLAY_AS_ATTACKER
                .setLevel(0,
                        I18n.marktr("No game played as attacker yet"),
                        I18n.marktr("Play as attacker to unlock this achievement"),
                        I18n.marktr("{0} of {1} games played"), 0)
                .setLevel(1,
                        I18n.marktr("Prepare to Attack"),
                        I18n.marktr("Play your first multiplayer game as attacker"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Attacker"),
                        I18n.marktr("Play {0} games as attacker"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("Silver Attacker"),
                        I18n.marktr("Play {0} games as attacker"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("Gold Attacker"),
                        I18n.marktr("Play {0} games as attacker"),
                        I18n.marktr("{0} games played, max level reached"), 50);

        PLAY_AS_DEFENDER
                .setLevel(0,
                        I18n.marktr("No game played as defender yet"),
                        I18n.marktr("Play as defender to unlock this achievement"),
                        I18n.marktr("{0} of {1} games played"), 0)
                .setLevel(1,
                        I18n.marktr("Prepare Your Defenses"),
                        I18n.marktr("Play your first multiplayer game as defender"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Defender"),
                        I18n.marktr("Play {0} games as defender"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("Silver Defender"),
                        I18n.marktr("Play {0} games as defender"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("Gold Defender"),
                        I18n.marktr("Play {0} games as defender"),
                        I18n.marktr("{0} games played, max level reached"), 50);

        PLAY_MELEE_GAMES
                .setLevel(0,
                        I18n.marktr("No melee game played yet"),
                        I18n.marktr("Try the melee mode to unlock this achievement"),
                        I18n.marktr("{0} of {1} games played"), 0)
                .setLevel(1,
                        I18n.marktr("Melee Starter"),
                        I18n.marktr("Play your first melee game"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Melee Bronze"),
                        I18n.marktr("Play {0} melee games"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("Melee Silver"),
                        I18n.marktr("Play {0} melee games"),
                        I18n.marktr("{0} of {1} games played to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("Melee Gold"),
                        I18n.marktr("Play {0} melee games"),
                        I18n.marktr("{0} games played, max level reached"), 50);

        WRITE_TESTS
                .setLevel(0,
                        I18n.marktr("No tests written yet"),
                        I18n.marktr("Write tests to unlock this achievement"),
                        I18n.marktr("{0} of {1} test written"), 0)
                .setLevel(1,
                        I18n.marktr("The First Test"),
                        I18n.marktr("Write your first test"),
                        I18n.marktr("{0} of {1} tests written to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Test Writer"),
                        I18n.marktr("Write {0} tests"),
                        I18n.marktr("{0} of {1} tests written to reach the next level"), 10)
                .setLevel(3,
                        I18n.marktr("Silver Test Writer"),
                        I18n.marktr("Write {0} tests"),
                        I18n.marktr("{0} of {1} tests written to reach the next level"), 50)
                .setLevel(4,
                        I18n.marktr("Gold Test Writer"),
                        I18n.marktr("Write {0} tests"),
                        I18n.marktr("{0} tests written, max level reached"), 200);

        CREATE_MUTANTS
                .setLevel(0,
                        I18n.marktr("No mutants created yet"),
                        I18n.marktr("Create mutants to unlock this achievement"),
                        I18n.marktr("{0} of {1} mutant created"), 0)
                .setLevel(1,
                        I18n.marktr("The First Mutant"),
                        I18n.marktr("Create your first mutant"),
                        I18n.marktr("{0} of {1} mutants created to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Mutant Creator"),
                        I18n.marktr("Create {0} mutants"),
                        I18n.marktr("{0} of {1} mutants created to reach the next level"), 10)
                .setLevel(3,
                        I18n.marktr("Silver Mutant Creator"),
                        I18n.marktr("Create {0} mutants"),
                        I18n.marktr("{0} of {1} mutants created to reach the next level"), 50)
                .setLevel(4,
                        I18n.marktr("Gold Mutant Creator"),
                        I18n.marktr("Create {0} mutants"),
                        I18n.marktr("{0} mutants created, max level reached"), 200);

        WIN_GAMES
                .setLevel(0,
                        I18n.marktr("No games won"),
                        I18n.marktr("Win games to unlock this achievement"),
                        I18n.marktr("{0} of {1} games won"), 0)
                .setLevel(1,
                        I18n.marktr("First Victory"),
                        I18n.marktr("Win your first game"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Winner"),
                        I18n.marktr("Win {0} games"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("Silver Winner"),
                        I18n.marktr("Win {0} games"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("Gold Winner"),
                        I18n.marktr("Win {0} games"),
                        I18n.marktr("{0} games won, max level reached"), 50);

        WIN_GAMES_AS_ATTACKER
                .setLevel(0,
                        I18n.marktr("No games won as attacker"),
                        I18n.marktr("Win games as attacker to unlock this achievement"),
                        I18n.marktr("{0} of {1} games won"), 0)
                .setLevel(1,
                        I18n.marktr("First Attacker Victory"),
                        I18n.marktr("Win your first game as attacker"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Good Attacker"),
                        I18n.marktr("Win {0} games as attacker"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("Advanced Attacker"),
                        I18n.marktr("Win {0} games as attacker"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 8)
                .setLevel(4,
                        I18n.marktr("Master Attacker"),
                        I18n.marktr("Win {0} games as attacker"),
                        I18n.marktr("{0} games won, max level reached"), 25);

        WIN_GAMES_AS_DEFENDER
                .setLevel(0,
                        I18n.marktr("No games won as defender"),
                        I18n.marktr("Win games as defender to unlock this achievement"),
                        I18n.marktr("{0} of {1} games won"), 0)
                .setLevel(1,
                        I18n.marktr("First Defender Victory"),
                        I18n.marktr("Win your first game as defender"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Good Defender"),
                        I18n.marktr("Win {0} games as defender"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("Advanced Defender"),
                        I18n.marktr("Win {0} games as defender"),
                        I18n.marktr("{0} of {1} games won to reach the next level"), 8)
                .setLevel(4,
                        I18n.marktr("Master Defender"),
                        I18n.marktr("Win {0} games as defender"),
                        I18n.marktr("{0} games won, max level reached"), 25);

        SOLVE_PUZZLES
                .setLevel(0,
                        I18n.marktr("No puzzles solved yet"),
                        I18n.marktr("Solve puzzles to unlock this achievement"),
                        I18n.marktr("{0} of {1} puzzles solved"), 0)
                .setLevel(1,
                        I18n.marktr("The First Puzzle"),
                        I18n.marktr("Solve your first puzzle"),
                        I18n.marktr("{0} of {1} puzzles solved to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Puzzle Solver"),
                        I18n.marktr("Solve {0} puzzles"),
                        I18n.marktr("{0} of {1} puzzles solved to reach the next level"), 5)
                .setLevel(3,
                        I18n.marktr("Silver Puzzle Solver"),
                        I18n.marktr("Solve {0} puzzles"),
                        I18n.marktr("{0} of {1} puzzles solved to reach the next level"), 15)
                .setLevel(4,
                        I18n.marktr("Puzzle Expert"),
                        I18n.marktr("Solve {0} puzzles"),
                        I18n.marktr("{0} puzzles solved, max level reached"), 25);

        WRITE_CLEAN_TESTS
                .setLevel(0,
                        I18n.marktr("No smell free tests written"),
                        I18n.marktr("Write a test without test smells to unlock this achievement"),
                        I18n.marktr("{0} of {1} smell free tests written"), 0)
                .setLevel(1,
                        I18n.marktr("This smells good"),
                        I18n.marktr("Write the first test without smells"),
                        I18n.marktr("{0} of {1} smell free tests written to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Perfumer"),
                        I18n.marktr("Write {0} smell free tests"),
                        I18n.marktr("{0} of {1} smell free tests written to reach the next level"), 10)
                .setLevel(3,
                        I18n.marktr("Silver Perfumer"),
                        I18n.marktr("Write {0} smell free tests"),
                        I18n.marktr("{0} of {1} smell free tests written to reach the next level"), 25)
                .setLevel(4,
                        I18n.marktr("Perfume Expert"),
                        I18n.marktr("Write {0} smell free tests"),
                        I18n.marktr("{0} smell free tests written, max level reached"), 100);

        KILL_MUTANTS
                .setLevel(0,
                        I18n.marktr("No mutants killed"),
                        I18n.marktr("Kill a mutant to unlock this achievement"),
                        I18n.marktr("{0} of {1} mutant killed"), 0)
                .setLevel(1,
                        I18n.marktr("Get the mutants!"),
                        I18n.marktr("Kill the first mutant with a test"),
                        I18n.marktr("{0} of {1} mutants killed to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Mutant Killer"),
                        I18n.marktr("Kill {0} mutants with tests"),
                        I18n.marktr("{0} of {1} mutants killed to reach the next level"), 10)
                .setLevel(3,
                        I18n.marktr("Silver Mutant Killer"),
                        I18n.marktr("Kill {0} mutants with tests"),
                        I18n.marktr("{0} of {1} mutants killed to reach the next level"), 50)
                .setLevel(4,
                        I18n.marktr("Mutant Executioner"),
                        I18n.marktr("Kill {0} mutants with tests"),
                        I18n.marktr("{0} mutants killed, max level reached"), 200);

        TOTAL_COVERAGE
                .setLevel(0,
                        I18n.marktr("No lines covered"),
                        I18n.marktr("Cover at least one line of code using tests to unlock this achievement"),
                        I18n.marktr("{0} of {1} lines covered"), 0)
                .setLevel(1,
                        I18n.marktr("Cover the first line"),
                        I18n.marktr("Cover the first line of code with a test"),
                        I18n.marktr("{0} of {1} lines covered to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Coverer"),
                        I18n.marktr("Cover {0} lines of code with tests"),
                        I18n.marktr("{0} of {1} lines covered to reach the next level"), 100)
                .setLevel(3,
                        I18n.marktr("Silver Coverer"),
                        I18n.marktr("Cover {0} lines of code with tests"),
                        I18n.marktr("{0} of {1} lines covered to reach the next level"), 500)
                .setLevel(4,
                        I18n.marktr("Line Coverage Expert"),
                        I18n.marktr("Cover {0} lines of code with tests"),
                        I18n.marktr("{0} lines covered, max level reached"), 1000);

        MAX_COVERAGE
                .setLevel(0,
                        I18n.marktr("No tests with coverage"),
                        I18n.marktr("Write a test that covers at least one line of code to unlock this achievement"),
                        I18n.marktr("{0} of {1} lines covered"), 0)
                .setLevel(1,
                        I18n.marktr("Specific Tester"),
                        I18n.marktr("Write a test that covers at least one line of code"),
                        I18n.marktr("{0} of {1} lines covered to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Small Tester"),
                        I18n.marktr("Cover {0} lines of code using a single test"),
                        I18n.marktr("{0} of {1} lines covered to reach the next level"), 10)
                .setLevel(3,
                        I18n.marktr("Large-Scale Tester"),
                        I18n.marktr("Cover {0} lines of code using a single test"),
                        I18n.marktr("{0} of {1} lines covered to reach the next level"), 25)
                .setLevel(4,
                        I18n.marktr("All-Encompassing Tester"),
                        I18n.marktr("Cover {0} lines of code using a single test"),
                        I18n.marktr("You wrote a test that covered {0} lines, this is crazy, please stop :o"), 100);

        WIN_EQUIVALENCE_DUELS
                .setLevel(0,
                        I18n.marktr("No Duels won"),
                        I18n.marktr("Win your first equivalence duel to unlock this achievement"),
                        I18n.marktr("{0} of {1} equivalence duels won"), 0)
                .setLevel(1,
                        I18n.marktr("Beginner Duelist"),
                        I18n.marktr("Win your first equivalence duel"),
                        I18n.marktr("{0} of {1} equivalence duels won to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Duelist"),
                        I18n.marktr("Win {0} equivalence duels"),
                        I18n.marktr("{0} of {1} equivalence duels won to reach the next level"), 5)
                .setLevel(3,
                        I18n.marktr("Silver Duelist"),
                        I18n.marktr("Win {0} equivalence duels"),
                        I18n.marktr("{0} of {1} equivalence duels won to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("The Best Duelist"),
                        I18n.marktr("Win {0} equivalence duels"),
                        I18n.marktr("{0} equivalence duels won, max level reached"), 50);

        WIN_EQUIVALENCE_DUELS_AS_ATTACKER
                .setLevel(0,
                        I18n.marktr("No Duels won as attacker"),
                        I18n.marktr("Win your first equivalence duel as attacker to unlock this achievement"),
                        I18n.marktr("{0} of {1} equivalence duels won as attacker"), 0)
                .setLevel(1,
                        I18n.marktr("Prove them wrong!"),
                        I18n.marktr("Win your first equivalence duel as attacker"),
                        I18n.marktr("{0} of {1} equivalence duels won as attacker to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Mutant Defender"),
                        I18n.marktr("Win {0} equivalence duels as attacker"),
                        I18n.marktr("{0} of {1} equivalence duels won as attacker to reach the next level"), 5)
                .setLevel(3,
                        I18n.marktr("Silver Mutant Defender"),
                        I18n.marktr("Win {0} equivalence duels as attacker"),
                        I18n.marktr("{0} of {1} equivalence duels won as attacker to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("The Best Mutant Defender"),
                        I18n.marktr("Win {0} equivalence duels as attacker"),
                        I18n.marktr("{0} equivalence duels won as attacker, max level reached"), 30);

        WIN_EQUIVALENCE_DUELS_AS_DEFENDER
                .setLevel(0,
                        I18n.marktr("No Duels won as defender"),
                        I18n.marktr("Win your first equivalence duel as defender to unlock this achievement"),
                        I18n.marktr("{0} of {1} equivalence duels won as defender"), 0)
                .setLevel(1,
                        I18n.marktr("Beginner Equivalence Detective"),
                        I18n.marktr("Win your first equivalence duel as defender"),
                        I18n.marktr("{0} of {1} equivalence duels won as defender to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("Bronze Equivalence Detective"),
                        I18n.marktr("Win {0} equivalence duels as defender"),
                        I18n.marktr("{0} of {1} equivalence duels won as defender to reach the next level"), 5)
                .setLevel(3,
                        I18n.marktr("Silver Equivalence Detective"),
                        I18n.marktr("Win {0} equivalence duels as defender"),
                        I18n.marktr("{0} of {1} equivalence duels won as defender to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("The Best Equivalence Detective"),
                        I18n.marktr("Win {0} equivalence duels as defender"),
                        I18n.marktr("{0} equivalence duels won as defender, max level reached"), 30);

        MAX_TESTS_IN_SHORT_TIME
                .setLevel(0,
                        I18n.marktr("Not enough tests written in 5 minutes"),
                        I18n.marktr("Write two tests in 5 minutes to unlock this achievement"),
                        I18n.marktr("{0} of {1} tests written in 5 minutes"), 0)
                .setLevel(1,
                        I18n.marktr("Speedy Tester"),
                        I18n.marktr("Write {0} tests in 5 minutes"),
                        I18n.marktr("{0} of {1} tests written in 5 minutes to reach the next level"), 2)
                .setLevel(2,
                        I18n.marktr("Bronze Speedy Tester"),
                        I18n.marktr("Write {0} tests in 5 minutes"),
                        I18n.marktr("{0} of {1} tests written in 5 minutes to reach the next level"), 5)
                .setLevel(3,
                        I18n.marktr("Silver Speedy Tester"),
                        I18n.marktr("Write {0} tests in 5 minutes"),
                        I18n.marktr("{0} of {1} tests written in 5 minutes to reach the next level"), 10)
                .setLevel(4,
                        I18n.marktr("The Fastest Tester"),
                        I18n.marktr("Write {0} tests in 5 minutes"),
                        I18n.marktr("{0} tests written in 5 minutes, max level reached"), 25);

        MAX_MUTANTS_IN_SHORT_TIME
                .setLevel(0,
                        I18n.marktr("Not enough mutants created in 5 minutes"),
                        I18n.marktr("Create five mutants in 5 minutes to unlock this achievement"),
                        I18n.marktr("{0} of {1} mutants created in 5 minutes"), 0)
                .setLevel(1,
                        I18n.marktr("Speedy Mutant Creator"),
                        I18n.marktr("Create {0} mutants in 5 minutes"),
                        I18n.marktr("{0} of {1} mutants created in 5 minutes to reach the next level"), 5)
                .setLevel(2,
                        I18n.marktr("Bronze Speedy Mutator"),
                        I18n.marktr("Create {0} mutants in 5 minutes"),
                        I18n.marktr("{0} of {1} mutants created in 5 minutes to reach the next level"), 10)
                .setLevel(3,
                        I18n.marktr("Silver Speedy Mutator"),
                        I18n.marktr("Create {0} mutants in 5 minutes"),
                        I18n.marktr("{0} of {1} mutants created in 5 minutes to reach the next level"), 20)
                .setLevel(4,
                        I18n.marktr("The Fastest Mutator"),
                        I18n.marktr("Create {0} mutants in 5 minutes"),
                        I18n.marktr("{0} mutants created in 5 minutes, max level reached"), 50);

        PUZZLES_SOLVED_ON_FIRST_TRY
                .setLevel(0,
                        I18n.marktr("No puzzles solved on the first try"),
                        I18n.marktr("Solve a puzzle on the first try to unlock this achievement"),
                        I18n.marktr("{0} of {1} puzzles solved on the first try"), 0)
                .setLevel(1,
                        I18n.marktr("One and Done"),
                        I18n.marktr("Solve your first puzzle on the first try"),
                        I18n.marktr("{0} of {1} puzzles solved on the first try to reach the next level"), 1)
                .setLevel(2,
                        I18n.marktr("{0} Birds with {0} Stones"),
                        I18n.marktr("Solve {0} puzzles on the first try"),
                        I18n.marktr("{0} of {1} puzzles solved on the first try to reach the next level"), 3)
                .setLevel(3,
                        I18n.marktr("{0} Birds with {0} Stones"),
                        I18n.marktr("Solve {0} puzzles on the first try"),
                        I18n.marktr("{0} of {1} puzzles solved on the first try to reach the next level"), 5)
                .setLevel(4,
                        I18n.marktr("{0} Birds with {0} Stones"),
                        I18n.marktr("Solve {0} puzzles on the first try"),
                        I18n.marktr("{0} puzzles solved on the first try, max level reached"), 10);
    }
}

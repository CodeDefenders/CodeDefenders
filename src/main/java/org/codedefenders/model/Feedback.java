/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
import java.util.Collections;
import java.util.List;

/**
 * This class contains static constants for feedback and the {@link Type feedback type}.
 */
public class Feedback {

    public static List<Type> types = Collections.unmodifiableList(Arrays.asList(Type.values()));
    public final static int MAX_RATING = 5;
    public final static int MIN_RATING = -1;

    public enum Type {
        CUT_MUTATION_DIFFICULTY("Mutation Difficulty", "The class under test is difficult to mutate"),
        CUT_TEST_DIFFICULTY("Test Difficulty", "The class under test is difficult to test"),
        ATTACKER_COMPETENCE("Attacker Competence", "The attacking team is competent"),
        DEFENDER_COMPETENCE("Defender Competence", "The defending team is competent"),
        ATTACKER_FAIRNESS("Attacker Fairness", "The attacking team is playing fair"),
        DEFENDER_FAIRNESS("Defender Fairness", "The defending team is playing fair"),
        GAME_ENGAGING("Game Engaging", "The game is engaging");

        String displayName;
        String description;

        Type(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }
    }

    private Feedback() {
    }
}

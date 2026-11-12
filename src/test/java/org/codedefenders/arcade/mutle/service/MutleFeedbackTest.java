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
package org.codedefenders.arcade.mutle.service;

import java.util.List;
import java.util.Set;

import org.codedefenders.arcade.mutle.fixture.MutleMutant;
import org.junit.jupiter.api.Test;

import static org.codedefenders.arcade.mutle.service.MutleFeedback.Mark.KILLED;
import static org.codedefenders.arcade.mutle.service.MutleFeedback.Mark.LINE;
import static org.codedefenders.arcade.mutle.service.MutleFeedback.Mark.METHOD;
import static org.codedefenders.arcade.mutle.service.MutleFeedback.Mark.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MutleFeedbackTest {

    private static final String SOURCE = """
            public class Locker {
                private int stored;

                public boolean deposit() {
                    if (stored == 4) {
                        return false;
                    }
                    stored++;
                    return true;
                }

                public int stored() {
                    return stored;
                }
            }
            """;

    private static final MutleMutant MUTANT = new MutleMutant("full-locker", "mutants/full-locker.java",
            "deposit", List.of(5), "ROR", "fills-locker", "The full locker accepts a parcel.");

    @Test
    void classifiesCoveredChangedAndCallableLines() {
        assertEquals(LINE, MutleFeedback.classify(SOURCE, MUTANT, Set.of(5, 8), false));
        assertEquals(METHOD, MutleFeedback.classify(SOURCE, MUTANT, Set.of(4, 8), false));
        assertEquals(NONE, MutleFeedback.classify(SOURCE, MUTANT, Set.of(12, 13), false));
    }

    @Test
    void killedTakesPrecedenceOverChangedCoveredLines() {
        assertEquals(KILLED, MutleFeedback.classify(SOURCE, MUTANT, Set.of(5, 8), true));
    }

    @Test
    void parsesConstructorRangesForConstructorMutants() {
        String source = """
                public class Locker {
                    public Locker() {
                        int capacity = 4;
                    }
                }
                """;
        MutleMutant constructorMutant = new MutleMutant("constructor", "mutants/constructor.java", "<init>",
                List.of(3), "ROR", "fills-locker", "The capacity changed.");

        assertEquals(LINE, MutleFeedback.classify(source, constructorMutant, Set.of(3), false));
    }

    @Test
    void rejectsFixtureMetadataThatDoesNotNameACallable() {
        MutleMutant invalidMutant = new MutleMutant("field", "mutants/field.java", "missing", List.of(2),
                "ROR", "fills-locker", "The field changed.");

        assertThrows(IllegalStateException.class, () -> MutleFeedback.classify(SOURCE, invalidMutant, Set.of(), false));
    }

    @Test
    void selectsTheOverloadContainingTheDeclaredChangedLine() {
        String source = """
                class Locker {
                    void store(int value) {
                        int first = value;
                    }
                    void store(String value) {
                        int second = value.length();
                    }
                }
                """;
        MutleMutant overloaded = new MutleMutant("overloaded", "mutants/overloaded.java", "store", List.of(6),
                "ROR", "fills-locker", "The string store changed.");

        assertEquals(METHOD, MutleFeedback.classify(source, overloaded, Set.of(5), false));
    }
}

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
package org.codedefenders.validation.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    private final List<ValidationRule> testRules = List.of(
            new TestRule.Builder("A", "1", "").build(),
            new TestRule.Builder("A", "2", "").build(),
            new TestRule.Builder("A", "3", "").build(),
            new TestRule.Builder("B", "4", "").build(),
            new TestRule.Builder("B", "5", "").build(),
            new TestRule.Builder("C", "6", "").build(),
            new TestRule.Builder("D", "7", "").build()
    );

    private void assertSimilarTieredList(List<List<ValidationRule>> expected, List<List<ValidationRule>> actual) {
        assertEquals(expected.size(), actual.size());
        for (List<ValidationRule> e : expected) {
            boolean exists = false;
            for (List<ValidationRule> a : actual) {
                if (e.size() == a.size()) {
                    boolean isEqual = true;
                    for (ValidationRule rule : e) {
                        if (!a.contains(rule)) {
                            isEqual = false;
                            break;
                        }
                    }
                    if (isEqual) {
                        exists = true;
                        break;
                    }
                }
            }
            assertTrue(exists, "List " + e + " is not present in result " + actual);
        }
    }

    private void assertSimilarSingleList(List<ValidationRule> expected, List<ValidationRule> actual) {
        assertEquals(expected.size(), actual.size());
        for (ValidationRule rule : expected) {
            assertTrue(actual.contains(rule));
        }
    }

    @Test
    void getTieredRules() {
        List<List<ValidationRule>> expected = List.of(
                List.of(testRules.get(0), testRules.get(1), testRules.get(2)),
                List.of(testRules.get(3), testRules.get(4))
        );
        List<List<ValidationRule>> result = ValidationUtils.getTieredRules(testRules);
        assertSimilarTieredList(expected, result);
        for (int i = 0; i < 100; i++) {
            List<ValidationRule> shuffled = new ArrayList<>(testRules);
            Collections.shuffle(shuffled, new Random(i));
            result = ValidationUtils.getTieredRules(shuffled);
            assertSimilarTieredList(expected, result);
        }

    }

    @Test
    void getSingleRules() {
        List<ValidationRule> expected = List.of(testRules.get(5), testRules.get(6));
        List<ValidationRule> result = ValidationUtils.getSingleRules(testRules);
        assertSimilarSingleList(expected, result);
        for (int i = 0; i < 100; i++) {
            List<ValidationRule> shuffled = new ArrayList<>(testRules);
            Collections.shuffle(shuffled, new Random(i));
            result = ValidationUtils.getSingleRules(shuffled);
            assertSimilarSingleList(expected, result);
        }
    }
}

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
package org.codedefenders.arcade.mutle.tools;

import java.util.Set;

import org.codedefenders.arcade.mutle.fixture.MutleClass;
import org.codedefenders.arcade.mutle.fixture.MutleFixtureLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MutleShippedPackValidityTest {
    @Test
    void everyPackedEntryReferencesAValidatedFixture() {
        MutleFixtureLoader loader = new MutleFixtureLoader();
        Set<String> classKeys = loader.pack().entries().stream()
                .map(entry -> entry.classKey())
                .collect(java.util.stream.Collectors.toSet());

        for (String classKey : classKeys) {
            MutleClass fixtureClass = loader.fixtureClass(classKey);
            assertFalse(fixtureClass.tests().isEmpty());
            assertFalse(fixtureClass.mutants().isEmpty());
        }
    }
}

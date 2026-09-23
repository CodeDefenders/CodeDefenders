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

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.codedefenders.arcade.mutle.fixture.MutleClass;
import org.codedefenders.arcade.mutle.fixture.MutleMutant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MutleFixtureInstallerTest {

    private static final MutleClass FIXTURE = new MutleClass("parcel-locker", "Parcel Locker", "Stores parcels.",
            List.of("state"), "ParcelLocker.java", List.of(), List.of());

    private static final MutleMutant MUTANT = new MutleMutant("full-locker", "mutants/full-locker.java", "deposit",
            List.of(12), "ROR", "fills-locker", "The full locker accepts a parcel.");

    @Test
    void hashIsDeterministicAndChangesWithImmutableContent() {
        String first = MutleFixtureInstaller.contentHash(FIXTURE, MUTANT, "class ParcelLocker {}", "mutant",
                "proof");
        String second = MutleFixtureInstaller.contentHash(FIXTURE, MUTANT, "class ParcelLocker {}", "mutant",
                "proof");
        String changed = MutleFixtureInstaller.contentHash(FIXTURE, MUTANT, "class ParcelLocker { int x; }", "mutant",
                "proof");
        String changedProof = MutleFixtureInstaller.contentHash(FIXTURE, MUTANT, "class ParcelLocker {}", "mutant",
                "changed proof");

        assertEquals(first, second);
        assertEquals("f56d5ebfc88eab46d1395bbf975c1ac92cb0f85dbad6354f4c777be832e1d3c6", first);
        assertEquals(64, first.length());
        assertNotEquals(first, changed);
        assertNotEquals(first, changedProof);
    }

    @Test
    void reusesMatchingLeftoverFixtureFile(@TempDir Path directory) throws Exception {
        Path expected = directory.resolve("ParcelLocker.java");
        Files.writeString(expected, "class ParcelLocker {}", StandardCharsets.UTF_8);

        assertEquals(expected, MutleFixtureInstaller.storeFixtureFile(directory, expected.getFileName().toString(),
                "class ParcelLocker {}"));
    }

    @Test
    void rejectsMismatchingLeftoverFixtureFile(@TempDir Path directory) throws Exception {
        Path expected = directory.resolve("ParcelLocker.java");
        Files.writeString(expected, "class ParcelLocker { int stale; }", StandardCharsets.UTF_8);

        assertThrows(FileAlreadyExistsException.class,
                () -> MutleFixtureInstaller.storeFixtureFile(directory, expected.getFileName().toString(),
                        "class ParcelLocker {}"));
    }

    @Test
    void storesFixtureSourcesInEstablishedClassAndMutantLayout(@TempDir Path sourcesDirectory) {
        String source = "package org.codedefenders.mutle; class AppointmentBook {}";

        assertEquals(sourcesDirectory.resolve("mutle-appointment-book/classes/org/codedefenders/mutle"),
                MutleFixtureInstaller.cutDirectory(sourcesDirectory, "appointment-book", source));
        assertEquals(sourcesDirectory.resolve(
                "mutle-appointment-book/mutants/cancellation-wrong-slot-stored/org/codedefenders/mutle"),
                MutleFixtureInstaller.mutantDirectory(sourcesDirectory, "appointment-book",
                        "cancellation-wrong-slot-stored", source));
    }

}

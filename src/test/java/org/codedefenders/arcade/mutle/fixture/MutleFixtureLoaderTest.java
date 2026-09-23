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
package org.codedefenders.arcade.mutle.fixture;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutleFixtureLoaderTest {

    private static final String PACK = """
            {"entries":[{"series":1,"classKey":"parcel-locker","mutantKey":"full-locker"}]}
            """;

    private static final String CLASS = """
            {
              "id":"parcel-locker",
              "title":"Parcel Locker",
              "specification":"Stores parcels.",
              "concepts":["state"],
              "sourcePath":"ParcelLocker.java",
              "tests":[{"key":"fills-locker","title":"fills the locker","sourcePath":"tests/fills-locker.java"}],
              "mutants":[{
                "key":"full-locker",
                "sourcePath":"mutants/full-locker.java",
                "method":"deposit",
                "mutatedLines":[12],
                "operator":"RELATIONAL",
                "proofTestKey":"fills-locker",
                "revealExplanation":"A full locker must reject another parcel."
              }]
            }
            """;

    private static Map<String, String> validResources() {
        Map<String, String> resources = new HashMap<>();
        resources.put(MutleFixtureLoader.packResource(), PACK);
        resources.put(MutleFixtureLoader.classResource("parcel-locker"), CLASS);
        resources.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/ParcelLocker.java", "public class ParcelLocker {}\n");
        resources.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/tests/fills-locker.java",
                "public class TestParcelLocker {}\n");
        resources.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/mutants/full-locker.java",
                "public class ParcelLocker {}\n");
        return resources;
    }

    private static MutleFixtureLoader loader(Map<String, String> resources) {
        Function<String, InputStream> open = resource -> {
            String content = resources.get(resource);
            return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        };
        return new MutleFixtureLoader(open);
    }

    @Test
    void loadsImmutableFixtureMetadataAndSeriesReferences() {
        MutleFixtureLoader loader = loader(validResources());

        assertEquals(List.of(new MutleEntry(1, "parcel-locker", "full-locker")), loader.pack().entries());
        MutleClass fixtureClass = loader.fixtureClass("parcel-locker");
        assertEquals("Parcel Locker", fixtureClass.title());
        assertEquals("ParcelLocker.java", fixtureClass.sourcePath());
        assertEquals("fills-locker", fixtureClass.mutants().get(0).proofTestKey());
        assertThrows(UnsupportedOperationException.class, () -> fixtureClass.mutants().get(0).mutatedLines().add(13));
    }

    @Test
    void loadsTheBundledPackAndParcelLockerMetadata() {
        MutleFixtureLoader loader = new MutleFixtureLoader();

        assertEquals(32, loader.pack().entries().size());
        MutleClass fixtureClass = loader.fixtureClass("parcel-locker");
        assertEquals("Parcel Locker", fixtureClass.title());
        assertEquals(16, fixtureClass.tests().size());
        assertEquals(4, fixtureClass.mutants().size());
        MutleClass scoreBoard = loader.fixtureClass("score-board");
        assertEquals("Score Board", scoreBoard.title());
        assertEquals(16, scoreBoard.tests().size());
        assertEquals(3, scoreBoard.mutants().size());
    }

    @Test
    void rejectsDuplicateSeriesNumbersAndMissingReferences() {
        Map<String, String> resources = validResources();
        resources.put(MutleFixtureLoader.packResource(), """
                {"entries":[
                  {"series":1,"classKey":"parcel-locker","mutantKey":"full-locker"},
                  {"series":1,"classKey":"parcel-locker","mutantKey":"missing"}
                ]}
                """);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> loader(resources).pack());
        assertTrue(exception.getMessage().contains("duplicated"));

        resources.put(MutleFixtureLoader.packResource(),
                "{\"entries\":[{\"series\":1,\"classKey\":\"parcel-locker\",\"mutantKey\":\"missing\"}]}");
        exception = assertThrows(IllegalStateException.class, () -> loader(resources).pack());
        assertTrue(exception.getMessage().contains("missing mutant"));
    }

    @Test
    void rejectsMalformedFieldsUnsafePathsAndMissingResources() {
        Map<String, String> unsafePathResources = validResources();
        unsafePathResources.put(MutleFixtureLoader.classResource("parcel-locker"),
                CLASS.replace("\"sourcePath\":\"ParcelLocker.java\"", "\"sourcePath\":\"../ParcelLocker.java\""));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loader(unsafePathResources).fixtureClass("parcel-locker"));
        assertTrue(exception.getMessage().contains("unsafe"));

        Map<String, String> missingResourceResources = validResources();
        missingResourceResources.remove(MutleFixtureLoader.classDirectory("parcel-locker") + "/mutants/full-locker.java");
        exception = assertThrows(IllegalStateException.class,
                () -> loader(missingResourceResources).fixtureClass("parcel-locker"));
        assertTrue(exception.getMessage().contains("missing from the classpath"));

        Map<String, String> malformedLineResources = validResources();
        malformedLineResources.put(MutleFixtureLoader.classResource("parcel-locker"),
                CLASS.replace("\"mutatedLines\":[12]", "\"mutatedLines\":[0]"));
        exception = assertThrows(IllegalStateException.class,
                () -> loader(malformedLineResources).fixtureClass("parcel-locker"));
        assertTrue(exception.getMessage().contains("positive integer"));
    }

    @Test
    void ignoresUnknownFieldsAndRejectsDuplicateKeysAndUnknownProofTests() {
        Map<String, String> unknownFieldResources = validResources();
        unknownFieldResources.put(MutleFixtureLoader.packResource(), "{\"entries\":[],\"difficulty\":\"easy\"}");
        assertTrue(loader(unknownFieldResources).pack().entries().isEmpty());

        Map<String, String> unknownProofTestResources = validResources();
        unknownProofTestResources.put(MutleFixtureLoader.classResource("parcel-locker"),
                CLASS.replace("\"proofTestKey\":\"fills-locker\"", "\"proofTestKey\":\"unknown\""));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loader(unknownProofTestResources).fixtureClass("parcel-locker"));
        assertTrue(exception.getMessage().contains("proof test unknown is missing"));

        Map<String, String> duplicateTestResources = validResources();
        duplicateTestResources.put(MutleFixtureLoader.classResource("parcel-locker"), CLASS.replace(
                "\"tests\":[{\"key\":\"fills-locker\",\"title\":\"fills the locker\",\"sourcePath\":\"tests/fills-locker.java\"}]",
                "\"tests\":[{\"key\":\"fills-locker\",\"title\":\"one\",\"sourcePath\":\"tests/fills-locker.java\"},"
                        + "{\"key\":\"fills-locker\",\"title\":\"two\",\"sourcePath\":\"tests/fills-locker.java\"}]"));
        exception = assertThrows(IllegalStateException.class,
                () -> loader(duplicateTestResources).fixtureClass("parcel-locker"));
        assertTrue(exception.getMessage().contains("test key fills-locker is duplicated"));
    }
}

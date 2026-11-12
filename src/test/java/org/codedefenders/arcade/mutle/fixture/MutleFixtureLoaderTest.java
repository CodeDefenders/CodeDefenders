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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
                "mutatedLines":[2],
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
        resources.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/ParcelLocker.java",
                "public class ParcelLocker {\n  public void deposit() {}\n}\n");
        resources.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/tests/fills-locker.java",
                "public class TestParcelLocker {\n  @org.junit.Test public void proves() {}\n}\n");
        resources.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/mutants/full-locker.java",
                "public class ParcelLocker {\n  public void deposit() {}\n}\n");
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
                CLASS.replace("\"mutatedLines\":[2]", "\"mutatedLines\":[0]"));
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

    @Test
    void loadsArchiveWithAClassDirectoryPrefix() throws Exception {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
            String classPrefix = MutleFixtureLoader.classDirectory("parcel-locker") + "/";
            for (Map.Entry<String, String> resource : validResources().entrySet()) {
                if (!resource.getKey().startsWith(classPrefix)) {
                    continue;
                }
                String path = "parcel-locker/" + resource.getKey().substring(classPrefix.length());
                zip.putNextEntry(new ZipEntry(path));
                zip.write(resource.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }

        var bundle = MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive.toByteArray()));
        assertEquals("parcel-locker", bundle.classKey());
        assertEquals(CLASS, bundle.classJson());
        assertEquals("public class ParcelLocker {\n  public void deposit() {}\n}\n",
                bundle.resources().get("ParcelLocker.java"));
        assertEquals("public class ParcelLocker {\n  public void deposit() {}\n}\n",
                bundle.resources().get("mutants/full-locker.java"));
    }

    @Test
    void rejectsSourcesWithUnknownMethodOrOutOfMethodLine() {
        Map<String, String> unknownMethod = validResources();
        unknownMethod.put(MutleFixtureLoader.classResource("parcel-locker"),
                CLASS.replace("\"method\":\"deposit\"", "\"method\":\"missing\""));
        IOException exception = assertThrows(IOException.class,
                () -> MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive(unknownMethod))));
        assertTrue(exception.getMessage().contains("method missing is missing"));

        Map<String, String> outOfMethod = validResources();
        outOfMethod.put(MutleFixtureLoader.classResource("parcel-locker"),
                CLASS.replace("\"mutatedLines\":[2]", "\"mutatedLines\":[3]"));
        exception = assertThrows(IOException.class,
                () -> MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive(outOfMethod))));
        assertTrue(exception.getMessage().contains("must belong to exactly one"));
    }

    @Test
    void rejectsUnparsableCutAndProofSources() {
        Map<String, String> invalidCut = validResources();
        invalidCut.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/ParcelLocker.java",
                "public class ParcelLocker {\n");
        IOException exception = assertThrows(IOException.class,
                () -> MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive(invalidCut))));
        assertTrue(exception.getMessage().contains("unparsable CUT"));

        Map<String, String> uncompilableCut = validResources();
        uncompilableCut.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/ParcelLocker.java",
                "public class ParcelLocker {\n  public void deposit() { int value = \"bad\"; }\n}\n");
        exception = assertThrows(IOException.class,
                () -> MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive(uncompilableCut))));
        assertTrue(exception.getMessage().contains("CUT ParcelLocker.java does not compile"));

        Map<String, String> invalidProof = validResources();
        invalidProof.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/tests/fills-locker.java",
                "public class TestParcelLocker {\n");
        exception = assertThrows(IOException.class,
                () -> MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive(invalidProof))));
        assertTrue(exception.getMessage().contains("unparsable proof test"));
    }

    @Test
    void rejectsProofWithoutJUnitTestAndReusedSourcePath() throws IOException {
        Map<String, String> missingTest = validResources();
        missingTest.put(MutleFixtureLoader.classDirectory("parcel-locker") + "/tests/fills-locker.java",
                "public class TestParcelLocker {\n  public void helper() {}\n}\n");
        IOException exception = assertThrows(IOException.class,
                () -> MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive(missingTest))));
        assertTrue(exception.getMessage().contains("must declare a JUnit test method"));

        Map<String, String> reusedPath = validResources();
        reusedPath.put(MutleFixtureLoader.classResource("parcel-locker"),
                CLASS.replace("\"sourcePath\":\"mutants/full-locker.java\"",
                        "\"sourcePath\":\"ParcelLocker.java\""));
        exception = assertThrows(IOException.class,
                () -> MutleFixtureLoader.loadArchive(new ByteArrayInputStream(archive(reusedPath))));
        assertTrue(exception.getMessage().contains("source path ParcelLocker.java is reused"));
    }

    private static byte[] archive(Map<String, String> resources) throws IOException {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(archive, StandardCharsets.UTF_8)) {
            String classPrefix = MutleFixtureLoader.classDirectory("parcel-locker") + "/";
            for (Map.Entry<String, String> resource : resources.entrySet()) {
                if (!resource.getKey().startsWith(classPrefix)) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry("parcel-locker/" + resource.getKey().substring(classPrefix.length())));
                zip.write(resource.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive.toByteArray();
    }
}

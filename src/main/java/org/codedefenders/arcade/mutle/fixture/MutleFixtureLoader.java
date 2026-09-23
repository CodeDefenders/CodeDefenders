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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/** Loads and validates the trusted Mutle fixture resources shipped with the application. */
@ApplicationScoped
public class MutleFixtureLoader {

    public static final String BASE = "arcade/mutle";
    private static final Gson GSON = new Gson();

    private final Function<String, InputStream> resources;
    private final Map<String, MutleClass> classes = new ConcurrentHashMap<>();
    private volatile MutlePack pack;

    public MutleFixtureLoader() {
        this(MutleFixtureLoader::classpathResource);
    }

    MutleFixtureLoader(Function<String, InputStream> resources) {
        this.resources = resources;
    }

    public MutlePack pack() {
        MutlePack cached = pack;
        if (cached == null) {
            synchronized (this) {
                cached = pack;
                if (cached == null) {
                    cached = loadPack();
                    pack = cached;
                }
            }
        }
        return cached;
    }

    public MutleClass fixtureClass(String key) {
        validateKey(key, "class key", packResource());
        return classes.computeIfAbsent(key, this::loadClass);
    }

    public static String packResource() {
        return BASE + "/pack.json";
    }

    public static String classDirectory(String key) {
        return BASE + "/classes/" + key;
    }

    public static String classResource(String key) {
        return classDirectory(key) + "/class.json";
    }

    public String readClassResource(String classKey, String relativePath) {
        validateKey(classKey, "class key", packResource());
        path(relativePath, classResource(classKey), "resource path");
        return read(classDirectory(classKey) + "/" + relativePath);
    }

    private MutlePack loadPack() {
        String resource = packResource();
        MutlePack pack = json(resource, MutlePack.class);
        check(pack.entries() != null, resource, "entries is missing");

        Set<Integer> seriesNumbers = new HashSet<>();
        Set<String> fixtureKeys = new HashSet<>();
        Map<String, Integer> previousClassPositions = new java.util.HashMap<>();
        for (int index = 0; index < pack.entries().size(); index++) {
            MutleEntry entry = pack.entries().get(index);
            check(entry != null, resource, "series " + (index + 1) + " must be an object");
            int series = positive(entry.series(), resource, "series number");
            check(seriesNumbers.add(series), resource, "series number " + series + " is duplicated");
            check(series == index + 1, resource, "series numbers must be contiguous and ordered from 1");
            String classKey = key(entry.classKey(), resource, "class key");
            String mutantKey = key(entry.mutantKey(), resource, "mutant key");
            check(fixtureKeys.add(classKey + "/" + mutantKey), resource,
                    "fixture " + classKey + "/" + mutantKey + " is duplicated");
            Integer previousPosition = previousClassPositions.put(classKey, index);
            check(previousPosition == null || index - previousPosition >= 4, resource,
                    "class " + classKey + " must have three other entries before it repeats");
            MutleClass fixtureClass = fixtureClass(classKey);
            check(fixtureClass.mutants().stream().anyMatch(mutant -> mutant.key().equals(mutantKey)), resource,
                    "series " + series + " references missing mutant " + classKey + "/" + mutantKey);
        }
        return pack;
    }

    private MutleClass loadClass(String key) {
        String resource = classResource(key);
        MutleClass fixtureClass = json(resource, MutleClass.class);
        check(key(fixtureClass.id(), resource, "id").equals(key), resource, "id must match its class key " + key);
        text(fixtureClass.title(), resource, "title");
        text(fixtureClass.specification(), resource, "specification");
        check(fixtureClass.concepts() != null && !fixtureClass.concepts().isEmpty(), resource,
                "concepts must not be empty");
        fixtureClass.concepts().forEach(concept -> text(concept, resource, "concept"));
        unique(fixtureClass.concepts(), resource, "concept");
        String sourcePath = path(fixtureClass.sourcePath(), resource, "source path");
        check(sourcePath.endsWith(".java"), resource, "source must name a Java file");
        read(classDirectory(key) + "/" + sourcePath);

        check(fixtureClass.tests() != null, resource, "tests is missing");
        Set<String> testKeys = new HashSet<>();
        for (MutleTest test : fixtureClass.tests()) {
            check(test != null, resource, "test must be an object");
            String testKey = key(test.key(), resource, "test key");
            check(testKeys.add(testKey), resource, "test key " + testKey + " is duplicated");
            String testPath = path(test.sourcePath(), resource, "test source path");
            check(testPath.startsWith("tests/") && testPath.endsWith(".java"), resource,
                    "test source must name a Java file in tests");
            read(classDirectory(key) + "/" + testPath);
        }

        check(fixtureClass.mutants() != null, resource, "mutants is missing");
        Set<String> mutantKeys = new HashSet<>();
        for (MutleMutant mutant : fixtureClass.mutants()) {
            check(mutant != null, resource, "mutant must be an object");
            String mutantKey = key(mutant.key(), resource, "mutant key");
            check(mutantKeys.add(mutantKey), resource, "mutant key " + mutantKey + " is duplicated");
            String mutantPath = path(mutant.sourcePath(), resource, "mutant source path");
            check(mutantPath.endsWith(".java"), resource, "mutant source must name a Java file");
            read(classDirectory(key) + "/" + mutantPath);
            text(mutant.method(), resource, "mutant method");
            List<Integer> lines = mutant.mutatedLines();
            check(lines != null && !lines.isEmpty(), resource, "mutated lines must not be empty");
            lines.forEach(line -> positive(line, resource, "mutated lines"));
            unique(lines, resource, "mutated line");
            text(mutant.operator(), resource, "operator");
            String proofTest = key(mutant.proofTestKey(), resource, "proof test key");
            check(testKeys.contains(proofTest), resource, "proof test " + proofTest + " is missing");
            text(mutant.revealExplanation(), resource, "reveal explanation");
        }
        return fixtureClass;
    }

    private <T> T json(String resource, Class<T> type) {
        try (InputStream input = resources.apply(resource)) {
            check(input != null, resource, "is missing from the classpath");
            T value = GSON.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), type);
            check(value != null, resource, "must not be empty");
            return value;
        } catch (JsonParseException e) {
            throw new IllegalStateException("Malformed Mutle resource " + resource, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Mutle resource " + resource, e);
        }
    }

    private String read(String resource) {
        try (InputStream input = resources.apply(resource)) {
            check(input != null, resource, "is missing from the classpath");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Mutle resource " + resource, e);
        }
    }

    private static String key(String value, String resource, String field) {
        text(value, resource, field);
        validateKey(value, field, resource);
        return value;
    }

    private static void validateKey(String key, String field, String resource) {
        check(key != null && key.matches("[a-z][a-z0-9-]*"), resource, field + " is malformed");
    }

    private static String path(String value, String resource, String field) {
        text(value, resource, field);
        check(!value.startsWith("/") && !value.startsWith("\\") && !value.contains("\\"), resource,
                field + " is unsafe");
        for (String segment : value.split("/", -1)) {
            check(segment.matches("[A-Za-z0-9][A-Za-z0-9._-]*") && !segment.equals(".."), resource,
                    field + " is unsafe");
        }
        return value;
    }

    private static String text(String value, String resource, String field) {
        check(value != null && !value.isBlank(), resource, field + " must not be blank");
        return value;
    }

    private static int positive(Integer value, String resource, String field) {
        check(value != null && value > 0, resource, field + " must be a positive integer");
        return value;
    }

    private static <T> void unique(List<T> values, String resource, String field) {
        check(new HashSet<>(values).size() == values.size(), resource, field + " is duplicated");
    }

    private static void check(boolean condition, String resource, String problem) {
        if (!condition) {
            throw new IllegalStateException("Mutle resource " + resource + " " + problem);
        }
    }

    private static InputStream classpathResource(String resource) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            InputStream input = context.getResourceAsStream(resource);
            if (input != null) {
                return input;
            }
        }
        ClassLoader own = MutleFixtureLoader.class.getClassLoader();
        return own == null ? ClassLoader.getSystemResourceAsStream(resource) : own.getResourceAsStream(resource);
    }
}

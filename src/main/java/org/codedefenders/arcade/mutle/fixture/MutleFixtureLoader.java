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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.enterprise.context.ApplicationScoped;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.codedefenders.arcade.mutle.model.MutleFixtureBundle;
import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Loads and validates Mutle fixture resources.
 *
 * <p>Uploaded sources are checked for parseability and for metadata pointing at real callables and
 * changed lines. CUT and mutant sources are compiled during import; proof tests are parsed only,
 * because their dependency-aware compilation and execution belong to the test harness.
 */
@ApplicationScoped
public class MutleFixtureLoader {

    public static final String BASE = "arcade/mutle";
    private static final Gson GSON = new Gson();
    private static final int MAX_ARCHIVE_BYTES = 8 * 1024 * 1024;
    private static final int MAX_ARCHIVE_ENTRIES = 512;

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

    /** Returns the complete immutable source bundle for an installed class. */
    public MutleFixtureBundle bundle(String classKey) {
        MutleClass fixtureClass = fixtureClass(classKey);
        Map<String, String> resources = new TreeMap<>();
        resources.put(fixtureClass.sourcePath(), readClassResource(classKey, fixtureClass.sourcePath()));
        fixtureClass.tests().forEach(test -> resources.put(test.sourcePath(),
                readClassResource(classKey, test.sourcePath())));
        fixtureClass.mutants().forEach(mutant -> resources.put(mutant.sourcePath(),
                readClassResource(classKey, mutant.sourcePath())));
        String classJson = read(classResource(classKey));
        return new MutleFixtureBundle(classKey, classJson, resources, contentHash(classJson, resources),
                Clock.systemUTC().instant());
    }

    /** Reads and validates a ZIP containing one class fixture in the shipped fixture format. */
    public static MutleFixtureBundle loadArchive(InputStream input) throws IOException {
        Map<String, String> files = new TreeMap<>();
        int totalBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (files.size() >= MAX_ARCHIVE_ENTRIES) {
                    throw new IOException("Mutle fixture archive contains too many files");
                }
                String path = safeArchivePath(entry.getName());
                if (files.containsKey(path)) {
                    throw new IOException("Mutle fixture archive contains duplicate file " + path);
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    totalBytes += read;
                    if (totalBytes > MAX_ARCHIVE_BYTES) {
                        throw new IOException("Mutle fixture archive is too large");
                    }
                    bytes.write(buffer, 0, read);
                }
                files.put(path, bytes.toString(StandardCharsets.UTF_8));
            }
        }
        String classJsonPath = files.containsKey("class.json") ? "class.json" : files.keySet().stream()
                .filter(path -> path.endsWith("/class.json"))
                .findFirst()
                .orElseThrow(() -> new IOException("Mutle fixture archive must contain class.json"));
        String prefix = classJsonPath.substring(0, classJsonPath.length() - "class.json".length());
        Map<String, String> normalizedFiles = new TreeMap<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            if (!file.getKey().startsWith(prefix)) {
                throw new IOException("Mutle fixture archive contains files outside its class directory");
            }
            String relativePath = file.getKey().substring(prefix.length());
            if (relativePath.isEmpty()) {
                throw new IOException("Mutle fixture archive contains an invalid class path");
            }
            normalizedFiles.put(relativePath, file.getValue());
        }
        String classJson = normalizedFiles.get("class.json");
        if (classJson == null) {
            throw new IOException("Mutle fixture archive must contain class.json");
        }
        MutleClass metadata;
        try {
            metadata = GSON.fromJson(classJson, MutleClass.class);
        } catch (JsonParseException exception) {
            throw new IOException("Malformed Mutle class.json", exception);
        }
        if (metadata == null || metadata.id() == null) {
            throw new IOException("Mutle class.json has no class id");
        }
        Map<String, String> resources = new TreeMap<>(normalizedFiles);
        resources.remove("class.json");
        MutleFixtureLoader validator = new MutleFixtureLoader(path -> {
            String archivePath = path.equals(classResource(metadata.id())) ? "class.json" : path;
            String resourcePrefix = classDirectory(metadata.id()) + "/";
            if (archivePath.startsWith(resourcePrefix)) {
                archivePath = archivePath.substring(resourcePrefix.length());
            }
            String value = normalizedFiles.get(archivePath);
            return value == null ? null : new java.io.ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        });
        try {
            validator.fixtureClass(metadata.id());
            validator.validateSources(metadata, "archive fixture " + metadata.id());
        } catch (RuntimeException exception) {
            throw new IOException("Invalid Mutle fixture " + metadata.id() + ": " + exception.getMessage(), exception);
        }
        return new MutleFixtureBundle(metadata.id(), classJson, resources,
                contentHash(classJson, resources), Clock.systemUTC().instant());
    }

    private static String safeArchivePath(String path) throws IOException {
        if (path == null || path.isBlank() || path.startsWith("/") || path.startsWith("\\")
                || path.contains("\\")) {
            throw new IOException("Mutle fixture archive contains an unsafe path");
        }
        for (String segment : path.split("/", -1)) {
            if (!segment.matches("[A-Za-z0-9][A-Za-z0-9._-]*") || segment.equals("..")) {
                throw new IOException("Mutle fixture archive contains an unsafe path");
            }
        }
        return path;
    }

    private static String contentHash(String classJson, Map<String, String> resources) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash(digest, "class.json", classJson);
            resources.forEach((path, source) -> hash(digest, path, source));
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void hash(MessageDigest digest, String path, String value) {
        digest.update(path.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
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
        Set<String> sourcePaths = new HashSet<>();
        sourcePaths.add(sourcePath);

        check(fixtureClass.tests() != null, resource, "tests is missing");
        Set<String> testKeys = new HashSet<>();
        for (MutleTest test : fixtureClass.tests()) {
            check(test != null, resource, "test must be an object");
            String testKey = key(test.key(), resource, "test key");
            check(testKeys.add(testKey), resource, "test key " + testKey + " is duplicated");
            String testPath = path(test.sourcePath(), resource, "test source path");
            check(testPath.startsWith("tests/") && testPath.endsWith(".java"), resource,
                    "test source must name a Java file in tests");
            check(sourcePaths.add(testPath), resource, "source path " + testPath + " is reused");
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
            check(sourcePaths.add(mutantPath), resource, "source path " + mutantPath + " is reused");
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

    private void validateSources(MutleClass fixtureClass, String resource) {
        String sourcePath = fixtureClass.sourcePath();
        String cutSource = read(classDirectory(fixtureClass.id()) + "/" + sourcePath);
        CompilationUnit cut = parseJava(cutSource, resource, "CUT " + sourcePath);
        compileJava(cutSource, sourcePath, resource, "CUT " + sourcePath);

        for (MutleTest test : fixtureClass.tests()) {
            String testSource = read(classDirectory(fixtureClass.id()) + "/" + test.sourcePath());
            CompilationUnit proof = parseJava(testSource, resource, "proof test " + test.key());
            check(proof.findAll(MethodDeclaration.class).stream()
                            .anyMatch(method -> method.getAnnotations().stream()
                                    .anyMatch(annotation -> annotation.getName().getIdentifier().equals("Test"))),
                    resource, "proof test " + test.key() + " must declare a JUnit test method");
        }

        for (MutleMutant mutant : fixtureClass.mutants()) {
            String mutantSource = read(classDirectory(fixtureClass.id()) + "/" + mutant.sourcePath());
            CompilationUnit changed = parseJava(mutantSource, resource, "mutant " + mutant.key());
            // Installer compiles mutant contents after renaming them to the CUT's basename.
            compileJava(mutantSource, sourcePath, resource, "mutant " + mutant.key());
            List<CallableDeclaration<?>> cutCallables = callables(cut, mutant.method());
            List<CallableDeclaration<?>> mutantCallables = callables(changed, mutant.method());
            check(!cutCallables.isEmpty(), resource,
                    "mutant method " + mutant.method() + " is missing from the CUT");
            check(!mutantCallables.isEmpty(), resource,
                    "mutant method " + mutant.method() + " is missing from mutant " + mutant.key());
            for (Integer line : mutant.mutatedLines()) {
                check(callablesContainingLine(cutCallables, line) == 1, resource,
                        "mutated line " + line + " must belong to exactly one " + mutant.method()
                                + " callable in the CUT");
                check(callablesContainingLine(mutantCallables, line) == 1, resource,
                        "mutated line " + line + " must belong to exactly one " + mutant.method()
                                + " callable in mutant " + mutant.key());
            }
        }
    }

    private static CompilationUnit parseJava(String source, String resource, String description) {
        return JavaParserUtils.parse(source).orElseThrow(() -> new IllegalStateException(
                "Mutle resource " + resource + " contains an unparsable " + description));
    }

    private static List<CallableDeclaration<?>> callables(CompilationUnit source, String methodName) {
        if ("<init>".equals(methodName)) {
            return new java.util.ArrayList<>(source.findAll(ConstructorDeclaration.class));
        }
        return new java.util.ArrayList<>(source.findAll(MethodDeclaration.class).stream()
                .filter(method -> method.getNameAsString().equals(methodName)).toList());
    }

    private static long callablesContainingLine(List<CallableDeclaration<?>> callables, int line) {
        return callables.stream()
                .filter(callable -> callable.getRange()
                        .map(range -> range.begin.line <= line && line <= range.end.line).orElse(false))
                .count();
    }

    private static void compileJava(String source, String sourcePath, String resource, String description) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, resource, "cannot validate " + description + " without a Java compiler");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, null,
                StandardCharsets.UTF_8)) {
            JavaFileManager files = new ForwardingJavaFileManager<>(standard) {
                @Override
                public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                            JavaFileObject.Kind kind, FileObject sibling) {
                    return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension),
                            kind) {
                        @Override
                        public java.io.OutputStream openOutputStream() {
                            return new ByteArrayOutputStream();
                        }
                    };
                }
            };
            JavaFileObject input = new SimpleJavaFileObject(URI.create("string:///" + sourcePath),
                    JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            Boolean compiled = compiler.getTask(null, files, diagnostics, List.of("-proc:none"), null,
                    List.of(input)).call();
            if (!Boolean.TRUE.equals(compiled)) {
                String detail = diagnostics.getDiagnostics().stream()
                        .map(diagnostic -> diagnostic.getMessage(java.util.Locale.ROOT))
                        .findFirst().orElse("compiler rejected the source");
                throw new IllegalStateException("Mutle resource " + resource + " " + description
                        + " does not compile: " + detail);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to validate " + description, exception);
        }
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

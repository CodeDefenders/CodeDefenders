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
package org.codedefenders.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.database.DependencyDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.Compiler;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMapService;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.TestingFramework;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleType;
import org.codedefenders.model.Dependency;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.JavaFileObject;
import org.codedefenders.util.SimpleFile;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;

import static org.codedefenders.util.Constants.CUTS_DEPENDENCY_DIR;
import static org.codedefenders.util.FileUtils.nextFreeNumberPath;


/**
 * Imports puzzles and puzzle chapters from files.
 * See the puzzle management page for file format explanations.
 */
public class PuzzleImporter {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleImporter.class);

    private final BackendExecutorService backend;
    private final CoverageGenerator coverageGenerator;
    private final KillMapService killMapService;
    private final TestRepository testRepo;
    private final MutantRepository mutantRepo;
    private final PuzzleRepository puzzleRepo;
    private final GameClassRepository gameClassRepo;

    @Inject
    public PuzzleImporter(BackendExecutorService backend,
                          CoverageGenerator coverageGenerator,
                          KillMapService killMapService,
                          TestRepository testRepo,
                          MutantRepository mutantRepo,
                          PuzzleRepository puzzleRepo,
                          GameClassRepository gameClassRepo) {
        this.backend = backend;
        this.coverageGenerator = coverageGenerator;
        this.killMapService = killMapService;
        this.testRepo = testRepo;
        this.mutantRepo = mutantRepo;
        this.puzzleRepo = puzzleRepo;
        this.gameClassRepo = gameClassRepo;
    }

    /**
     * Input data for installing a single puzzle.
     */
    public record PuzzleData(
            SimpleFile properties,
            SimpleFile cut,
            List<SimpleFile> deps,
            List<SimpleFile> mutants,
            List<SimpleFile> tests) {}

    /**
     * Input data for installing a single puzzle chapter.
     */
    public record ChapterData(
            SimpleFile properties,
            List<PuzzleData> puzzles) {}

    /**
     * Imports a single puzzle into an existing chapter.
     */
    public void importPuzzle(Collection<SimpleFile> files, Integer chapterId) throws CompileException,
            CoverageGenerator.CoverageGeneratorException, IOException, BackendExecutorService.ExecutionException {
        PuzzleData data = SinglePuzzleImporter.readPuzzleData(files);
        importPuzzle(data, chapterId);
    }

    /**
     * Imports a single puzzle into an existing chapter.
     */
    public void importPuzzle(PuzzleData data, Integer chapterId) throws CoverageGenerator.CoverageGeneratorException,
            CompileException, IOException, BackendExecutorService.ExecutionException {
        new SinglePuzzleImporter(data, chapterId).install();
    }

    /**
     * Imports a single puzzle without assigning a chapter. It will put into the unassigned category.
     */
    public void importPuzzle(Collection<SimpleFile> files) throws CoverageGenerator.CoverageGeneratorException,
            CompileException, IOException, BackendExecutorService.ExecutionException {
        importPuzzle(files, null);
    }

    /**
     * Imports a single puzzle without assigning a chapter. It will be put into the unassigned category.
     */
    public void importPuzzle(PuzzleData data) throws CoverageGenerator.CoverageGeneratorException, CompileException,
            IOException, BackendExecutorService.ExecutionException {
        importPuzzle(data, null);
    }

    /**
     * Imports a single puzzle chapter.
     */
    public void importPuzzleChapter(Collection<SimpleFile> files) throws CoverageGenerator.CoverageGeneratorException,
            CompileException, IOException, BackendExecutorService.ExecutionException {
        ChapterData data = readChapterData(files);
        importPuzzleChapter(data);
    }

    /**
     * Imports a single puzzle chapter.
     */
    public void importPuzzleChapter(ChapterData data) throws IOException, CoverageGenerator.CoverageGeneratorException,
            CompileException, BackendExecutorService.ExecutionException {
        PuzzleChapter chapter = storePuzzleChapterToDB(data.properties);
        for (PuzzleData puzzleData : data.puzzles) {
            importPuzzle(puzzleData, chapter.getChapterId());
        }
    }

    public record PuzzleChapterProperties(String title, String description) { }

    public PuzzleChapterProperties readPuzzleChapterProperties(SimpleFile propertiesFile) throws IOException {
        Properties cfg = new Properties();
        cfg.load(new ByteArrayInputStream(propertiesFile.getContent()));

        String title = cfg.getProperty("title");
        if (title == null || title.isEmpty()) {
            throw new ValidationException("Missing title for puzzle chapter.");
        }

        String description = cfg.getProperty("description");
        return new PuzzleChapterProperties(title, description);
    }

    private PuzzleChapter storePuzzleChapterToDB(SimpleFile propertiesFile) throws IOException {
        var props = readPuzzleChapterProperties(propertiesFile);

        // Find next free position.
        int position = puzzleRepo.getPuzzleChapters().stream()
                .map(PuzzleChapter::getPosition)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;

        PuzzleChapter chapter = new PuzzleChapter(-1, position, props.title, props.description);
        int chapterId = puzzleRepo.storePuzzleChapter(chapter);
        chapter.setChapterId(chapterId);

        logger.info("Stored puzzle chapter with id {}", chapterId);
        return chapter;
    }

    public static ChapterData readChapterData(Collection<SimpleFile> files) throws ValidationException {
        SimpleFile propertiesFile = files.stream()
                .filter(file -> file.getFilename().equals("chapter.properties") && file.getPath().getNameCount() == 1)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Missing properties file for chapter."));

        // Find puzzle folders by searching for "puzzle.properties" files at depth 2.
        List<SimpleFile> puzzlePropertiesFiles = files.stream()
                .filter(file -> file.getPath().getNameCount() == 2 && file.getFilename().equals("puzzle.properties"))
                .toList();

        // Sort files for puzzles.
        List<PuzzleData> puzzles = puzzlePropertiesFiles.stream()
                .map(file -> file.getPath().getParent())
                .sorted(Comparator.comparing(parent -> parent.getFileName().toString()))
                .map(parent -> {
                    List<SimpleFile> puzzleFiles = files.stream()
                            .filter(otherFile -> otherFile.getPath().getName(0).equals(parent))
                            .map(otherFile -> new SimpleFile(
                                    otherFile.getPath().subpath(1, otherFile.getPath().getNameCount()),
                                    otherFile.getContent()))
                            .toList();
                    return SinglePuzzleImporter.readPuzzleData(puzzleFiles);
                })
                .toList();

        return new ChapterData(propertiesFile, puzzles);
    }

    /**
     * This class exists only to store the intermediate results from the installation as class members.
     */
    public class SinglePuzzleImporter {
        // Input Data
        private final PuzzleData puzzleData;
        private final Integer chapterId;

        // Working data
        private PuzzleProperties properties;
        private List<JavaFileObject> dependencyFiles;
        private Path cutDir;
        private GameClass cut;
        private List<Dependency> dependencies;
        private List<Mutant> mutants;
        private List<Test> tests;

        public SinglePuzzleImporter(PuzzleData puzzleData, Integer chapterId) {
            this.puzzleData = puzzleData;
            this.chapterId = chapterId;
        }

        public void install() throws CompileException, IOException, CoverageGenerator.CoverageGeneratorException,
                BackendExecutorService.ExecutionException {
            String cutName = FilenameUtils.removeExtension(puzzleData.cut.getFilename());
            cutDir = FileUtils.nextFreeClassDirectory(cutName);

            readPuzzleProperties();
            installCUT();
            installMutants();
            installTests();
            installPuzzle();
        }

        private void installCUT() throws IOException, CompileException {
            String cutName = FilenameUtils.removeExtension(puzzleData.cut.getFilename());
            String alias = gameClassRepo.nextFreeAlias(cutName);

            // Store cut file.
            Path cutPath = FileUtils.storeFile(cutDir,puzzleData.cut.getFilename(),
                    puzzleData.cut.getContentAsString());

            // Store dependencies files.
            storeDependenciesSources();

            // Compile. This also moves the class files to the dependencies folder.
            String cutClassFilePath = Compiler.compileJavaFileWithDependencies(cutPath.toString(), dependencyFiles);

            // Store CUT to DB.
            String classQualifiedName = FileUtils.getFullyQualifiedName(cutClassFilePath);
            cut = GameClass.build()
                    .name(classQualifiedName)
                    .alias(alias)
                    .javaFile(cutPath.toString())
                    .classFile(cutClassFilePath)
                    .mockingEnabled(false) // TODO: don't use a default value
                    .testingFramework(TestingFramework.JUNIT4) // TODO: don't use a default value
                    .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST) // TODO: don't use a default value
                    .puzzleClass(true)
                    .active(false)
                    .create();
            int cutId = gameClassRepo.storeClass(cut);
            cut.setId(cutId);

            // Store dependencies to DB.
            storeDependenciesToDB();

            logger.info("Stored class '{}' with id {} to '{}'", alias, cutId, cutPath);
        }

        private void storeDependenciesSources() throws IOException {
            Path storagePath = cutDir.resolve(CUTS_DEPENDENCY_DIR);
            dependencyFiles = new ArrayList<>();

            for (SimpleFile depFile : puzzleData.deps) {
                String fileContent = depFile.getContentAsString();
                Path internalPath = depFile.getPath().getParent();
                Path fullStoragePath = internalPath != null
                        ? storagePath.resolve(internalPath)
                        : storagePath;
                Path depPath = FileUtils.storeFile(fullStoragePath, depFile.getFilename(), fileContent);
                dependencyFiles.add(new JavaFileObject(depPath.toString(), fileContent));
            }
        }

        private void storeDependenciesToDB() {
            dependencies = new ArrayList<>();

            for (JavaFileObject depFile : dependencyFiles) {
                String javaFilePath = depFile.getPath();
                String classFilePath = javaFilePath.replace(".java", ".class");

                Dependency dependency = new Dependency(cut.getId(), javaFilePath, classFilePath);
                int id = DependencyDAO.storeDependency(dependency);
                dependency.setId(id);

                dependencies.add(dependency);
            }
        }

        private void installMutants() throws CompileException, IOException {
            mutants = new ArrayList<>();

            List<SimpleFile> mutantsQueue = new ArrayList<>(puzzleData.mutants);
            if (properties.type == PuzzleType.EQUIVALENCE && mutantsQueue.size() > 1) {
                logger.warn("Equivalence puzzles should only have one mutant.");
                mutantsQueue = List.of(mutantsQueue.get(0));
            }

            for (SimpleFile mutantFile : mutantsQueue) {
                // Create the mutants directory if it doesn't exist.
                Path mutantsListDir = cutDir.resolve(Constants.CUTS_MUTANTS_DIR);
                if (!Files.exists(mutantsListDir)) {
                    Files.createDirectories(mutantsListDir);
                }

                // Find a free numbered subdirectory.
                Path mutantDir = nextFreeNumberPath(mutantsListDir);

                // Store the mutant file.
                Path javaFilePath = FileUtils.storeFile(mutantDir, mutantFile.getFilename(),
                        mutantFile.getContentAsString());

                // Compile.
                String classFilePath = Compiler.compileJavaFileWithDependencies(javaFilePath.toString(), dependencyFiles, true);

                // Store to DB.
                String md5 = CodeValidator.getMD5FromText(mutantFile.getContentAsString());
                Mutant mutant = new Mutant(javaFilePath.toString(), classFilePath, md5, cut.getId());
                int mutantId = mutantRepo.storeMutant(mutant);
                mutant.setId(mutantId);
                mutants.add(mutant);

                // Map to class.
                mutantRepo.mapMutantToClass(mutantId, cut.getId());

                logger.info("Stored mutant with id {} to '{}'", mutantId, mutantDir);
            }
        }

        private void installTests() throws IOException, CompileException, BackendExecutorService.ExecutionException,
                CoverageGenerator.CoverageGeneratorException {
            tests = new ArrayList<>();

            for (SimpleFile testFile : puzzleData.tests) {
                // Create the tests directory if it doesn't exist.
                Path testsPath = cutDir.resolve(Constants.CUTS_TESTS_DIR);
                if (!Files.exists(testsPath)) {
                    Files.createDirectories(testsPath);
                }

                // Find a free numbered subdirectory.
                Path testPath = nextFreeNumberPath(testsPath);

                // Store the test file.
                Path javaFilePath = FileUtils.storeFile(testPath, testFile.getFilename(), testFile.getContentAsString());

                // Compile. All tests require the CUT as a dependency.
                List<JavaFileObject> deps = new ArrayList<>(dependencyFiles);
                deps.add(new JavaFileObject(cut.getJavaFile(), cut.getSourceCode()));
                String classFilePath = Compiler.compileJavaTestFile(javaFilePath.toString(), deps, true); // TODO: true?
                String qualifiedName = FileUtils.getFullyQualifiedName(classFilePath);

                // Run JaCoCo and produce the jacoco.exec file to the testDir
                backend.testOriginal(cut, testPath.toString(), qualifiedName);

                // Store to DB.
                LineCoverage lineCoverage = coverageGenerator.generate(cut, javaFilePath);
                Test test = new Test(javaFilePath.toString(), classFilePath, cut.getId(), lineCoverage);
                int testId = testRepo.storeTest(test);
                test.setId(testId);
                tests.add(test);

                // Map to class.
                testRepo.mapTestToClass(testId, cut.getId());

                logger.info("Stored test with id {} to '{}'", testId, testPath);
            }
        }

        public record PuzzleProperties(PuzzleType type, boolean isEquivalent, GameLevel level, String title,
                                       String description, Integer editableLinesStart, Integer editableLinesEnd) { }

        public void readPuzzleProperties() throws IOException {
            // Load config.
            Properties cfg = new Properties();
            cfg.load(new ByteArrayInputStream(puzzleData.properties.getContent()));

            // Read values from specification file
            PuzzleType type = PuzzleType.valueOf(cfg.getProperty("type"));
            boolean isEquivalent = Boolean.parseBoolean(cfg.getProperty("isEquivalent", "false"));
            GameLevel level = GameLevel.valueOf(cfg.getProperty("gameLevel", "HARD"));
            String title = cfg.getProperty("title");
            String description = cfg.getProperty("description");
            Integer editableLinesStart = Optional.ofNullable(cfg.getProperty("editableLinesStart", null))
                    .flatMap(s -> s.isEmpty() ? Optional.empty() : Optional.of(s))
                    .map(Integer::parseInt)
                    .orElse(null);
            Integer editableLinesEnd = Optional.ofNullable(cfg.getProperty("editableLinesEnd", null))
                    .flatMap(s -> s.isEmpty() ? Optional.empty() : Optional.of(s))
                    .map(Integer::parseInt)
                    .orElse(null);

            this.properties = new PuzzleProperties(type, isEquivalent, level, title, description, editableLinesStart,
                    editableLinesEnd);
        }

        private void installPuzzle() throws IOException {
            // Store the properties file.
            FileUtils.storeFile(cutDir, puzzleData.properties.getFilename(),
                    puzzleData.properties.getContentAsString());

            // Default values
            // TODO: Don't use default value for puzzles
            int maxAssertionsPerTest = CodeValidator.DEFAULT_NB_ASSERTIONS;
            CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.MODERATE;

            // Find the next position in the chapter.
            List<Puzzle> otherPuzzles = chapterId != null
                    ? puzzleRepo.getPuzzlesForChapterId(chapterId)
                    : puzzleRepo.getUnassignedPuzzles();
            Integer position = otherPuzzles.stream()
                    .map(Puzzle::getPosition)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0) + 1;

            var props = this.properties;

            // Store puzzle to DB.
            Puzzle puzzle = new Puzzle(-1, cut.getId(), props.type, props.isEquivalent, props.level,
                    maxAssertionsPerTest, mutantValidatorLevel, props.editableLinesStart, props.editableLinesEnd,
                    chapterId, position, props.title, props.description);
            int puzzleId = puzzleRepo.storePuzzle(puzzle);
            puzzle.setPuzzleId(puzzleId);

            logger.info("Created Puzzle {}", puzzleId);

            // Run killmap.
            try {
                KillMap killMap = killMapService.forCustom(tests, mutants, cut.getId(), new ArrayList<>());
                KillmapDAO.insertManyKillMapEntries(killMap.getEntries(), cut.getId());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error while calculating killmap for successfully installed puzzle.", e);
            }
        }

        public static PuzzleData readPuzzleData(Collection<SimpleFile> files) throws ValidationException {
            if (files.isEmpty()) {
                throw new IllegalArgumentException("No files for puzzle.");
            }

            BiFunction<SimpleFile, Integer, SimpleFile> subpath = (file, beginIndex) -> {
                Path oldPath = file.getPath();
                Path newPath = oldPath.subpath(beginIndex, oldPath.getNameCount());
                return new SimpleFile(newPath, file.getContent());
            };

            SimpleFile properties = null;
            List<SimpleFile> cuts = new ArrayList<>();
            List<SimpleFile> deps = new ArrayList<>();
            List<SimpleFile> mutants = new ArrayList<>();
            List<SimpleFile> tests = new ArrayList<>();

            for (SimpleFile file : files) {
                String[] parts = Streams.stream(file.getPath())
                        .map(Path::toString)
                        .toArray(String[]::new);

                switch (parts[0]) {
                    case "puzzle.properties" -> {
                        properties = file;
                        continue;
                    }
                    case "cut" -> {
                        if (parts.length == 2 && parts[1].endsWith(".java")) {
                            cuts.add(subpath.apply(file, 1));
                            continue;
                        } else if (parts.length > 2
                                && parts[1].equals("deps")
                                && parts[parts.length-1].endsWith(".java")) {
                            deps.add(subpath.apply(file, 2));
                            continue;
                        }
                    }
                    case "mutants" -> {
                        if (parts[parts.length-1].endsWith(".java")) {
                            mutants.add(subpath.apply(file, 1));
                            continue;
                        }
                    }
                    case "tests" -> {
                        if (parts[parts.length-1].endsWith(".java")) {
                            tests.add(subpath.apply(file, 1));
                            continue;
                        }
                    }
                }

                logger.info("Unexpected file for puzzle: '{}'", file.getPath());
            }

            if (properties == null) {
                throw new ValidationException("Missing properties file for puzzle.");
            }
            if (cuts.isEmpty()) {
                throw new ValidationException("Missing CUT for puzzle.");
            } else if (cuts.size() > 1) {
                String cutNames = cuts.stream()
                        .map(SimpleFile::getPath)
                        .map(path -> String.format("'%s'", path))
                        .collect(Collectors.joining(", "));
                throw new ValidationException("Multiple CUT files for puzzle: " + cutNames);
            }

            // Sort mutants and tests by their subdirectory.
            mutants.sort(Comparator.comparing(mutantFile -> mutantFile.getPath().getName(0)));
            tests.sort(Comparator.comparing(mutantFile -> mutantFile.getPath().getName(0)));

            return new PuzzleData(properties, cuts.get(0), deps, mutants, tests);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}

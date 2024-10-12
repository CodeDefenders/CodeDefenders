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
package org.codedefenders.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.BackendExecutorService;
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
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.JavaFileObject;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class allows adding {@link GameClass game classes (CUTs)}, {@link Mutant mutants},
 * {@link Test tests}, {@link PuzzleChapter puzzle chapters} and {@link Puzzle puzzles}
 * programmatically.
 *
 * @author gambi
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class Installer {
    private static final Logger logger = LoggerFactory.getLogger(Installer.class);

    private final BackendExecutorService backend;
    private final CoverageGenerator coverageGenerator;
    private final KillMapService killMapService;
    private final Configuration config;
    private final TestRepository testRepo;
    private final MutantRepository mutantRepo;
    private final PuzzleRepository puzzleRepo;
    private final GameClassRepository gameClassRepo;

    @Inject
    public Installer(BackendExecutorService backend,
                     CoverageGenerator coverageGenerator,
                     KillMapService killMapService,
                     @SuppressWarnings("CdiInjectionPointsInspection") Configuration config,
                     TestRepository testRepo,
                     MutantRepository mutantRepo,
                     PuzzleRepository puzzleRepo,
                     GameClassRepository gameClassRepo) {
        this.backend = backend;
        this.coverageGenerator = coverageGenerator;
        this.killMapService = killMapService;
        this.config = config;
        this.testRepo = testRepo;
        this.mutantRepo = mutantRepo;
        this.puzzleRepo = puzzleRepo;
        this.gameClassRepo = gameClassRepo;
    }

    /**
     * Mapping from CUT alias to the {@link GameClass} instance.
     */
    private final Map<String, GameClass> installedCuts = new HashMap<>();
    /**
     * Mapping from a CUT alias to a mapping of positions to mutants.
     */
    private final Map<String, Map<Integer, Mutant>> installedMutants = new HashMap<>();
    /**
     * Mapping from a CUT alias to a mapping of positions to test cases.
     */
    private final Map<String, Map<Integer, Test>> installedTests = new HashMap<>();
    /**
     * Set of identifiers of puzzle chapters.
     */
    private final Set<Integer> puzzleChapters = new HashSet<>();

    /**
     * Maps the original class aliases to their numbered version used for dodging naming collisions.
     */
    private final Map<String, String> aliasMapping = new HashMap<>();
    /**
     * Maps the original class aliases to their storage directory.
     */
    private final Map<String, Path> pathMapping = new HashMap<>();
    /**
     * Maps the chapter IDs from the uploaded puzzle files to their ID in the database.
     */
    private final Map<Integer, Integer> chapterIdMapping = new HashMap<>();

    /**
     * Looks for puzzle related files in a given directory.
     * In the directory, this method will look for files in the following sub-folders:
     * <ul>
     * <li>{@code cuts/}</li> holds puzzles classes.
     *   See {@link #installCUT(File) installCUT()} for file convention.
     * <li>{@code mutants/}</li> holds puzzle mutants.
     *   See {@link #installMutant(File)}  installedMutant()} for file convention.
     * <li>{@code tests/}</li> holds puzzles tests.
     *   See {@link #installTest(File) installTest()} for file convention.
     * <li>{@code puzzleChapters/}</li> holds puzzle chapters information.
     *   See {@link #installPuzzleChapter(File) installPuzzleChapter()} for file convention.
     * <li>{@code puzzles/}</li> holds puzzle information.
     *   See {@link #installPuzzle(File) installPuzzle()} for file convention.
     * </ul>
     *
     * @param directory the directory puzzle related files are looked at.
     */
    public void installPuzzles(Path directory) {
        final List<File> cuts = getFilesForDir(directory.resolve("cuts"), ".java");
        final List<File> mutants = getFilesForDir(directory.resolve("mutants"), ".java");
        final List<File> tests = getFilesForDir(directory.resolve("tests"), ".java");
        final List<File> puzzleChapterSpecs = getFilesForDir(directory.resolve("puzzleChapters"), ".properties");
        final List<File> puzzleSpecs = getFilesForDir(directory.resolve("puzzles"), ".properties");

        run(cuts, mutants, tests, puzzleChapterSpecs, puzzleSpecs);
    }

    /**
     * Finds all files for in a given directory for a given file extension.
     *
     * <p>Iterates through a maximal folder depth of 5.
     *
     * @param directory     the directory the files should be found in.
     * @param fileExtension the extension of the to be found files.
     * @return a list of found files. Empty if none or an error occurred.
     */
    private static List<File> getFilesForDir(Path directory, String fileExtension) {
        try (Stream<Path> paths = Files.find(directory, 5,
                (path, attrs) -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(fileExtension))) {
            return paths.map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to search files in directory", e);
            return new ArrayList<>();
        }
    }

    private void run(List<File> cuts, List<File> mutants, List<File> tests,
                     List<File> puzzleChapterSpecs, List<File> puzzleSpecs) {
        for (File cutFile : cuts) {
            try {
                installCUT(cutFile);
            } catch (Exception e) {
                logger.error("Failed to install CUT " + cutFile, e);
            }
        }

        for (File mutantFile : mutants) {
            try {
                installMutant(mutantFile);
            } catch (Exception e) {
                logger.error("Failed to install MUTANT " + mutantFile, e);
            }
        }

        for (File testFile : tests) {
            try {
                installTest(testFile);
            } catch (Exception e) {
                logger.error("Failed to install TEST " + testFile, e);
            }
        }

        for (File puzzleChapterSpecFile : puzzleChapterSpecs) {
            try {
                installPuzzleChapter(puzzleChapterSpecFile);
            } catch (Exception e) {
                logger.error("Failed to install puzzle chapter " + puzzleChapterSpecFile, e);
            }
        }

        for (File puzzleSpecFile : puzzleSpecs) {
            try {
                installPuzzle(puzzleSpecFile);
            } catch (Exception e) {
                logger.error("Failed to install PUZZLE " + puzzleSpecFile, e);
            }
        }
    }

    /**
     * File convention: {@code cuts/<cut_alias>/<filename>}.
     */
    private void installCUT(File cutFile) throws Exception {
        String fileName = cutFile.getName();
        String alias = cutFile.getParentFile().getName();

        String newAlias = gameClassRepo.nextFreeAlias(alias);
        aliasMapping.put(alias, newAlias);
        Path storagePath = FileUtils.nextFreeClassDirectory(alias);
        pathMapping.put(alias, storagePath);

        String fileContent = Files.readString(cutFile.toPath(), Charset.defaultCharset());
        String cutJavaFilePath = FileUtils.storeFile(storagePath, fileName, fileContent).toString();
        String cutClassFilePath = Compiler.compileJavaFileForContent(cutJavaFilePath, fileContent);
        String classQualifiedName = FileUtils.getFullyQualifiedName(cutClassFilePath);

        GameClass cut = GameClass.build()
                .name(classQualifiedName)
                .alias(newAlias)
                .javaFile(cutJavaFilePath)
                .classFile(cutClassFilePath)
                .mockingEnabled(false) // TODO: don't use a default value
                .testingFramework(TestingFramework.JUNIT4) // TODO: don't use a default value
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST) // TODO: don't use a default value
                .puzzleClass(true)
                .active(false)
                .create();

        gameClassRepo.storeClass(cut);
        logger.info("installCut(): Stored Class " + cut.getId() + " to " + cutJavaFilePath);

        installedCuts.put(alias, cut);
        installedMutants.put(alias, new HashMap<>());
        installedTests.put(alias, new HashMap<>());
    }

    /**
     * File convention: {@code mutants/<cut_alias>/<position>/<filename>}.
     */
    @SuppressWarnings("Duplicates")
    private void installMutant(File mutantFile) throws Exception {
        String mutantFileName = mutantFile.getName();
        String classAlias = mutantFile.getParentFile().getParentFile().getName();
        if (!installedMutants.containsKey(classAlias)) {
            logger.warn("CUT {} does not exist. Skipping installation of mutant.", classAlias);
            return;
        }
        GameClass cut = installedCuts.get(classAlias);

        int targetPosition;
        try {
            targetPosition = Integer.parseInt(mutantFile.getParentFile().getName());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("No valid position provided.");
        }

        String mutantFileContent = Files.readString(mutantFile.toPath(), Charset.defaultCharset());

        Path cutDir = pathMapping.get(classAlias);
        if (cutDir == null) {
            logger.warn("Couldn't get storage path for CUT {}", classAlias);
            return;
        }

        Path folderPath = cutDir.resolve(Constants.CUTS_MUTANTS_DIR).resolve(String.valueOf(targetPosition));
        String javaFilePath = FileUtils.storeFile(folderPath, mutantFileName, mutantFileContent).toString();
        String classFilePath = Compiler.compileJavaFileForContent(javaFilePath, mutantFileContent);

        String md5 = CodeValidator.getMD5FromText(mutantFileContent);
        Mutant mutant = new Mutant(javaFilePath, classFilePath, md5, cut.getId());

        int mutantId = mutantRepo.storeMutant(mutant);
        mutantRepo.mapMutantToClass(mutantId, cut.getId());

        logger.info("installMutant(): Stored mutant " + mutant.getId() + " in position " + targetPosition);

        installedMutants.get(classAlias).put(targetPosition, mutant);
    }

    /**
     * File convention: {@code tests/<cut_alias>/<position>/<filename>}.
     */
    @SuppressWarnings("Duplicates")
    private void installTest(File testFile) throws Exception {
        String testFileName = testFile.getName();
        String classAlias = testFile.getParentFile().getParentFile().getName();

        if (!installedTests.containsKey(classAlias)) {
            logger.warn("CUT {} does not exist. Skipping installation of mutant.", classAlias);
            return;
        }
        GameClass cut = installedCuts.get(classAlias);

        int targetPosition;
        try {
            targetPosition = Integer.parseInt(testFile.getParentFile().getName());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("No valid position provided.");
        }

        // All the tests require the same dependency on the CUT
        List<JavaFileObject> dependencies = Collections.singletonList(new JavaFileObject(cut.getJavaFile()));

        String testFileContent = Files.readString(testFile.toPath(), Charset.defaultCharset());

        Path cutDir = pathMapping.get(classAlias);
        if (cutDir == null) {
            logger.warn("Couldn't get storage path for CUT {}", classAlias);
            return;
        }

        Path folderPath = cutDir.resolve(Constants.CUTS_TESTS_DIR).resolve(String.valueOf(targetPosition));
        Path javaFilePath = FileUtils.storeFile(folderPath, testFileName, testFileContent);
        String classFilePath = Compiler.compileJavaTestFileForContent(javaFilePath.toString(),
                testFileContent, dependencies, true);

        String testDir = folderPath.toString();
        String qualifiedName = FileUtils.getFullyQualifiedName(classFilePath);

        // This adds a jacoco.exec file to the testDir
        backend.testOriginal(cut, testDir, qualifiedName);

        LineCoverage lineCoverage = coverageGenerator.generate(cut, javaFilePath);
        Test test = new Test(javaFilePath.toString(), classFilePath, cut.getId(), lineCoverage);

        int testId = testRepo.storeTest(test);
        testRepo.mapTestToClass(testId, cut.getId());

        logger.info("installTest() Stored test " + test.getId() + " in position " + targetPosition);

        installedTests.get(classAlias).put(targetPosition, test);
    }

    /**
     * File convention: {@code puzzleChapters/<files>}. Chapter specification files just
     * need to be in that folder.
     *
     * <p>Mandatory properties: {@code chapterId}.
     *
     * <p>Optional properties: {@code position}, {@code title}, {@code description}.
     */
    private void installPuzzleChapter(File puzzleChapterSpecFile) throws Exception {
        Properties cfg = new Properties();
        cfg.load(new FileInputStream(puzzleChapterSpecFile));

        int chapterId = Optional.ofNullable(cfg.getProperty("chapterId"))
                .map(Integer::parseInt)
                .orElseThrow(() -> new IllegalArgumentException("No valid chapterId provided."));
        Integer position = Optional.ofNullable(cfg.getProperty("position"))
                .map(Integer::parseInt)
                .orElse(null);
        String title = cfg.getProperty("title");
        String description = cfg.getProperty("description");

        PuzzleChapter chapter = new PuzzleChapter(-1, position, title, description);

        int databaseId = puzzleRepo.storePuzzleChapter(chapter);
        chapterIdMapping.put(chapterId, databaseId);

        logger.info("installPuzzleChapter() Stored puzzle chapter with id " + chapterId);

        puzzleChapters.add(chapterId);
    }

    /**
     * File convention: {@code puzzles/<cut_alias>/<puzzle_alias_ext>}.
     * {@code <puzzle_alias_ext>} is used for the puzzle alias, which is constructed as follows:
     * {@code <cut_alias>_puzzle_<puzzle_alias_ext>}
     *
     * <p>Mandatory properties: {@code activeRole} ({@link Role#DEFENDER 'DEFENDER'} or
     * {@link Role#ATTACKER 'ATTACKER'}), {@code gameLevel} ({@link GameLevel#EASY 'EASY'}
     * or {@link GameLevel#HARD 'HARD'},
     * {@code chapterId} (has to be of existing chapter).
     *
     * <p>Optional properties: {@code mutants}, {@code tests}, {@code title}, {@code description},
     * {@code editableLinesStart}, {@code editableLinesEnd},
     * {@code position}.
     */
    private void installPuzzle(File puzzleSpecFile) throws Exception {
        Properties cfg = new Properties();
        cfg.load(new FileInputStream(puzzleSpecFile));

        String cutAlias = puzzleSpecFile.getParentFile().getName();
        GameClass cut = installedCuts.get(cutAlias);
        if (cut == null) {
            logger.warn("CUT {} does not exist. Skipping installation of puzzle.", cutAlias);
            return;
        }

        String[] mutantPositions = cfg.getProperty("mutants", "").split(",");

        Map<Integer, Mutant> positionToMutantMap = installedMutants.get(cutAlias);
        List<Mutant> originalMutants = Arrays.stream(mutantPositions)
                .map(Integer::parseInt)
                .map(positionToMutantMap::get)
                .filter(Objects::nonNull)
                .toList();

        String[] testPositions = cfg.getProperty("tests", "").split(",");
        Map<Integer, Test> positionToTestMap = installedTests.get(cutAlias);
        List<Test> originalTests = Arrays.stream(testPositions)
                .map(Integer::parseInt)
                .map(positionToTestMap::get)
                .filter(Objects::nonNull)
                .toList();

        String puzzleAliasExt = puzzleSpecFile.getName().replace(".properties", "");
        String puzzleAlias = cutAlias + "_puzzle_" + puzzleAliasExt;

        // Create a puzzle cut with another alias for this puzzle
        String newAlias = gameClassRepo.nextFreeAlias(puzzleAlias);
        GameClass puzzleClass = GameClass.ofPuzzle(cut, newAlias);

        int puzzleClassId = gameClassRepo.storeClass(puzzleClass);
        logger.info("installPuzzle(); Created Puzzle Class " + puzzleClassId);

        // Read values from specification file
        Role activeRole = Role.valueOf(cfg.getProperty("activeRole"));
        boolean isEquivalent = Boolean.parseBoolean(cfg.getProperty("isEquivalent", "false"));
        boolean isEquivalencePuzzle = cfg.containsKey("isEquivalent");
        GameLevel level = GameLevel.valueOf(cfg.getProperty("gameLevel", "HARD"));
        String title = cfg.getProperty("title");
        String description = cfg.getProperty("description");
        Integer editableLinesStart = Optional.ofNullable(cfg.getProperty("editableLinesStart", null))
                .map(Integer::parseInt)
                .orElse(null);
        Integer editableLinesEnd = Optional.ofNullable(cfg.getProperty("editableLinesEnd", null))
                .map(Integer::parseInt)
                .orElse(null);
        Optional<Integer> chapterIdOpt = Optional.ofNullable(cfg.getProperty("chapterId")).map(Integer::parseInt);
        if (chapterIdOpt.isEmpty() || !puzzleChapters.contains(chapterIdOpt.get())) {
            logger.warn("Provided chapterId for puzzle was not provided or does not exist. Skipping this puzzle.");
            return;
        }
        int chapterId = chapterIdOpt.get();

        if (!chapterIdMapping.containsKey(chapterId)) {
            logger.warn("Couldn't get database chapter ID for chapter {}", chapterId);
            return;
        }
        int databaseChapterId = chapterIdMapping.get(chapterId);

        Integer position = Optional.ofNullable(cfg.getProperty("position"))
                .map(Integer::parseInt)
                .orElse(null);

        // Allow only one mutant for equivalence puzzles
        if (isEquivalencePuzzle && originalMutants.size() > 1) {
            logger.warn("Equivalence puzzles should only have one mutant.");
            originalMutants = List.of(originalMutants.get(0));
        }

        // Default values
        int maxAssertionsPerTest = CodeValidator.DEFAULT_NB_ASSERTIONS; // TODO: Don't use default value for puzzles

        CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.MODERATE;

        Puzzle puzzle = new Puzzle(-1, puzzleClassId, activeRole, isEquivalent, isEquivalencePuzzle, level,
                maxAssertionsPerTest, mutantValidatorLevel, editableLinesStart, editableLinesEnd, databaseChapterId,
                position, title, description);
        int puzzleId = puzzleRepo.storePuzzle(puzzle);

        List<Mutant> puzzleMutants = new ArrayList<>();
        for (Mutant m : originalMutants) {
            Mutant puzzleMutant = new Mutant(m.getJavaFile(), m.getClassFile(), m.getMd5(), puzzleClassId);

            int mutantId = mutantRepo.storeMutant(puzzleMutant);
            puzzleMutant.setId(mutantId);

            mutantRepo.mapMutantToClass(puzzleMutant.getId(), puzzleClassId);
            logger.info("installPuzzle(); Created Puzzle Mutant " + puzzleMutant.getId());
            puzzleMutants.add(puzzleMutant);
        }

        List<Test> puzzleTests = new ArrayList<>();
        for (Test t : originalTests) {
            Test puzzleTest = new Test(t.getJavaFile(), t.getClassFile(), puzzleClassId, t.getLineCoverage());

            int testId = testRepo.storeTest(puzzleTest);
            puzzleTest.setId(testId);

            testRepo.mapTestToClass(puzzleTest.getId(), puzzleClassId);
            logger.info("installPuzzle(); Created Puzzle Test " + puzzleTest.getId());
            puzzleTests.add(puzzleTest);
        }

        logger.info("installPuzzle() Created Puzzle " + puzzleId);

        try {
            KillMap killMap = killMapService.forCustom(puzzleTests, puzzleMutants, puzzleClassId, new ArrayList<>());
            KillmapDAO.insertManyKillMapEntries(killMap.getEntries(), puzzleClassId);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error while calculating killmap for successfully installed puzzle.", e);
        }
    }
}

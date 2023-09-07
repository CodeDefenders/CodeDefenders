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
import java.nio.file.Paths;
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

import javax.inject.Inject;

import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.database.TestDAO;
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

    @Inject
    public Installer(BackendExecutorService backend,
                     CoverageGenerator coverageGenerator,
                     KillMapService killMapService,
            @SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        this.backend = backend;
        this.coverageGenerator = coverageGenerator;
        this.killMapService = killMapService;
        this.config = config;
    }

    /**
     * Mapping from CUT alias to the {@link GameClass} instance.
     */
    private Map<String, GameClass> installedCuts = new HashMap<>();
    /**
     * Mapping from a CUT alias to a mapping of positions to mutants.
     */
    private Map<String, Map<Integer, Mutant>> installedMutants = new HashMap<>();
    /**
     * Mapping from a CUT alias to a mapping of positions to test cases.
     */
    private Map<String, Map<Integer, Test>> installedTests = new HashMap<>();
    /**
     * Set of identifiers of puzzle chapters.
     */
    private Set<Integer> puzzleChapters = new HashSet<>();


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
        String classAlias = cutFile.getParentFile().getName();
        if (GameClassDAO.classExistsForAlias(classAlias)) {
            logger.warn("Class alias {} does already exist. Skipping installation of CUT.", classAlias);
            return;
        }

        String fileContent = Files.readString(cutFile.toPath(), Charset.defaultCharset());

        Path cutDir = Paths.get(config.getSourcesDir().getAbsolutePath(), classAlias);
        String cutJavaFilePath = FileUtils.storeFile(cutDir, fileName, fileContent).toString();
        String cutClassFilePath = Compiler.compileJavaFileForContent(cutJavaFilePath, fileContent);
        String classQualifiedName = FileUtils.getFullyQualifiedName(cutClassFilePath);

        GameClass cut = GameClass.build()
                .name(classQualifiedName)
                .alias(classAlias)
                .javaFile(cutJavaFilePath)
                .classFile(cutClassFilePath)
                .mockingEnabled(false) // TODO: dont't use a default value
                .testingFramework(TestingFramework.JUNIT4) // TODO: dont't use a default value
                .assertionLibrary(AssertionLibrary.JUNIT4_HAMCREST) // TODO: dont't use a default value
                .puzzleClass(true)
                .active(false)
                .create();

        cut.insert();
        logger.info("installCut(): Stored Class " + cut.getId() + " to " + cutJavaFilePath);

        installedCuts.put(classAlias, cut);
        installedMutants.put(classAlias, new HashMap<>());
        installedTests.put(classAlias, new HashMap<>());
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

        Path cutDir = Paths.get(config.getSourcesDir().getAbsolutePath(), classAlias);
        Path folderPath = cutDir.resolve(Constants.CUTS_MUTANTS_DIR).resolve(String.valueOf(targetPosition));
        String javaFilePath = FileUtils.storeFile(folderPath, mutantFileName, mutantFileContent).toString();
        String classFilePath = Compiler.compileJavaFileForContent(javaFilePath, mutantFileContent);

        String md5 = CodeValidator.getMD5FromText(mutantFileContent);
        Mutant mutant = new Mutant(javaFilePath, classFilePath, md5, cut.getId());

        int mutantId = MutantDAO.storeMutant(mutant);
        MutantDAO.mapMutantToClass(mutantId, cut.getId());

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

        Path cutDir = Paths.get(config.getSourcesDir().getAbsolutePath(), classAlias);
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

        int testId = TestDAO.storeTest(test);
        TestDAO.mapTestToClass(testId, cut.getId());

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

        PuzzleChapter chapter = new PuzzleChapter(chapterId, position, title, description);
        PuzzleDAO.storePuzzleChapter(chapter);

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
                .collect(Collectors.toList());

        String[] testPositions = cfg.getProperty("tests", "").split(",");
        Map<Integer, Test> positionToTestMap = installedTests.get(cutAlias);
        List<Test> originalTests = Arrays.stream(testPositions)
                .map(Integer::parseInt)
                .map(positionToTestMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String puzzleAliasExt = puzzleSpecFile.getName().replace(".properties", "");
        String puzzleAlias = cutAlias + "_puzzle_" + puzzleAliasExt;

        // Create a puzzle cut with another alias for this puzzle
        GameClass puzzleClass = GameClass.ofPuzzle(cut, puzzleAlias);

        int puzzleClassId = GameClassDAO.storeClass(puzzleClass);
        logger.info("installPuzzle(); Created Puzzle Class " + puzzleClassId);

        // Read values from specification file
        Role activeRole = Role.valueOf(cfg.getProperty("activeRole"));
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
        Integer position = Optional.ofNullable(cfg.getProperty("position"))
                .map(Integer::parseInt)
                .orElse(null);

        // Default values
        int maxAssertionsPerTest = CodeValidator.DEFAULT_NB_ASSERTIONS; // TODO: Don't use default value for puzzles

        CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.MODERATE;

        Puzzle puzzle = new Puzzle(-1, puzzleClassId, activeRole, level, maxAssertionsPerTest,
                mutantValidatorLevel, editableLinesStart, editableLinesEnd, chapterId, position, title, description);
        int puzzleId = PuzzleDAO.storePuzzle(puzzle);

        List<Mutant> puzzleMutants = new ArrayList<>();
        for (Mutant m : originalMutants) {
            Mutant puzzleMutant = new Mutant(m.getJavaFile(), m.getClassFile(), m.getMd5(), puzzleClassId);
            puzzleMutant.insert();
            MutantDAO.mapMutantToClass(puzzleMutant.getId(), puzzleClassId);
            logger.info("installPuzzle(); Created Puzzle Mutant " + puzzleMutant.getId());
            puzzleMutants.add(puzzleMutant);
        }

        List<Test> puzzleTests = new ArrayList<>();
        for (Test t : originalTests) {
            Test puzzleTest = new Test(t.getJavaFile(), t.getClassFile(), puzzleClassId, t.getLineCoverage());
            puzzleTest.insert();
            TestDAO.mapTestToClass(puzzleTest.getId(), puzzleClassId);
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

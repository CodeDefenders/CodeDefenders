package org.codedefenders.installer;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import com.mysql.cj.jdbc.MysqlDataSource;

import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.Compiler;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.LineCoverageGenerator;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.JavaFileObject;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * This class allows adding {@link GameClass game classes (CUTs)}, {@link Mutant mutants},
 * {@link Test tests}, {@link PuzzleChapter puzzle chapters} and {@link Puzzle puzzles}
 * programmatically.
 * <p>
 * Using {@link #main(String[]) the main method}, this installer can be used
 * as a command line tool.
 * <p>
 * Using {@link #installPuzzles(Path) installPuzzles()}, the installer can be
 * called programmatically inside code defenders.
 *
 * @author gambi
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
public class Installer {

    private static final Logger logger = LoggerFactory.getLogger(Installer.class);

    /**
     * Used for parsing the command line.
     */
    private interface ParsingInterface {
        @Option(defaultToNull = true)
        File getConfigurations();
        
        /**
         * Directory containing the resources for creating the puzzled. Same as
         * the one used to generate the zip file
         * 
         * @return
         */
        @Option(longName = "bundle-directory")
        File getBundleDirectory();
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
     * Inserts in order: game classes (CUTs), mutants, tests, puzzle chapters and puzzles.
     */
    public static void main(String[] args) throws IOException, NamingException {
        ParsingInterface commandLine = CliFactory.parseArguments(ParsingInterface.class, args);
        Properties configurations = new Properties();
        configurations.load(new FileInputStream(commandLine.getConfigurations()));
        setupInitialContext(configurations);
        // No need to create the zip file, we can directly use the "exploded" directory
        Installer.installPuzzles(commandLine.getBundleDirectory().toPath());
        // Running this code with mvn exec:java hangs the execution so we force
        // the exit
        System.exit(0);
    }

    /**
     * Looks for puzzle related files in a given directory.
     * In the directory, this method will look for files in the following sub-folders:
     * <ul>
     * <li>{@code cuts/}</li> holds puzzles classes. See {@link #installCUT(File) installCUT()} for file convention.
     * <li>{@code mutants/}</li> holds puzzle mutants. See {@link #installMutant(File)}  installedMutant()} for file convention.
     * <li>{@code tests/}</li> holds puzzles tests. See {@link #installTest(File) installTest()} for file convention.
     * <li>{@code puzzleChapters/}</li> holds puzzle chapters information. See {@link #installPuzzleChapter(File) installPuzzleChapter()} for file convention.
     * <li>{@code puzzles/}</li> holds puzzle information. See {@link #installPuzzle(File) installPuzzle()} for file convention.
     * </ul>
     *
     * @param directory the directory puzzle related files are looked at.
     */
    public static void installPuzzles(Path directory) {
        final List<File> cuts = getFilesForDir(directory.resolve("cuts"), ".java");
        final List<File> mutants = getFilesForDir(directory.resolve("mutants"), ".java");
        final List<File> tests = getFilesForDir(directory.resolve("tests"), ".java");
        final List<File> puzzleChapterSpecs = getFilesForDir(directory.resolve("puzzleChapters"), ".properties");
        final List<File> puzzleSpecs = getFilesForDir(directory.resolve("puzzles"), ".properties");

        Installer installer = new Installer();
        installer.run(cuts, mutants, tests, puzzleChapterSpecs, puzzleSpecs);
    }

    /**
     * Finds all files for in a given directory for a given file extension.
     * <p>
     * Iterates through a maximal folder depth of 5.
     *
     * @param directory the directory the files should be found in.
     * @param fileExtension the extension of the to be found files.
     * @return a list of found files. Empty if none or an error occurred.
     */
    private static List<File> getFilesForDir(Path directory, String fileExtension) {
        List<File> files;
        try {
            files = Files.find(directory, 5, (path, basicFileAttributes) -> {
                if (path.toFile().isDirectory()) {
                    return false;
                }
                if (!path.getFileName().toString().endsWith(fileExtension)) {
                    return false;
                }
                return true;
            })
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            files = new ArrayList<>();
        }
        return files;
    }

    private void run(List<File> cuts, List<File> mutants, List<File> tests, List<File> puzzleChapterSpecs, List<File> puzzleSpecs) {
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
     * Sets up the {@link InitialContext} for a given configuration.
     * <p>
     * Also adds the database {@link DataSource} to the initial context.
     *
     * @param configurations the configuration used to set the initial context.
     * @throws NamingException when setting the initial context fails.
     */
    private static void setupInitialContext(Properties configurations) throws NamingException {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

        InitialContext ic = new InitialContext();

        ic.createSubcontext("java:");
        ic.createSubcontext("java:comp");
        ic.createSubcontext("java:comp/env");

        // Alessio: Maybe there a better way to do it...
        for (String pName : configurations.stringPropertyNames()) {
            logger.info("createDataSource() Storing property " + pName + " in the env with value "
                    + configurations.get(pName));
            ic.bind("java:comp/env/" + pName, configurations.get(pName));
        }

        ic.createSubcontext("java:comp/env/jdbc");

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(configurations.getProperty("db.url"));
        dataSource.setUser(configurations.getProperty("db.username"));
        dataSource.setPassword(configurations.getProperty("db.password"));

        ic.bind("java:comp/env/jdbc/codedefenders", dataSource);
    }

    /**
     * File convention: {@code cuts/<cut_alias>/<filename>}.
     *
     * @see ParsingInterface#getCuts()
     */
    private void installCUT(File cutFile) throws Exception {
        String fileName = cutFile.getName();
        String classAlias = cutFile.getParentFile().getName();
        if (GameClassDAO.classExistsForAlias(classAlias)) {
            logger.warn("Class alias {} does already exist. Skipping installation of CUT.", classAlias);
            return;
        }

        String fileContent = new String(Files.readAllBytes(cutFile.toPath()), Charset.defaultCharset());

        Path cutDir = Paths.get(Constants.CUTS_DIR, classAlias);
        String cutJavaFilePath = FileUtils.storeFile(cutDir, fileName, fileContent).toString();
        String cutClassFilePath = Compiler.compileJavaFileForContent(cutJavaFilePath, fileContent);
        String classQualifiedName = FileUtils.getFullyQualifiedName(cutClassFilePath);

        // Store the CUT
        boolean isMockingEnabled = false;
        GameClass cut = new GameClass(classQualifiedName, classAlias, cutJavaFilePath, cutClassFilePath, isMockingEnabled);
        cut.insert();

        logger.info("installCut(): Stored Class " + cut.getId() + " to " + cutJavaFilePath);

        installedCuts.put(classAlias, cut);
        installedMutants.put(classAlias, new HashMap<>());
        installedTests.put(classAlias, new HashMap<>());
    }

    /**
     * File convention: {@code mutants/<cut_alias>/<position>/<filename>}.
     *
     * @see ParsingInterface#getMutants()
     */
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

        String mutantFileContent = new String(Files.readAllBytes(mutantFile.toPath()), Charset.defaultCharset());

        Path cutDir = Paths.get(Constants.CUTS_DIR, classAlias);
        Path folderPath = cutDir.resolve(Constants.CUTS_MUTANTS_DIR).resolve(String.valueOf(targetPosition));
        String javaFilePath = FileUtils.storeFile(folderPath, mutantFileName, mutantFileContent).toString();
        String classFilePath = Compiler.compileJavaFileForContent(javaFilePath, mutantFileContent);

        String md5 = CodeValidator.getMD5FromText(mutantFileContent);
        Mutant mutant = new Mutant(javaFilePath, classFilePath, md5, cut.getId());
        mutant.insert();
        MutantDAO.mapMutantToClass(mutant.getId(), cut.getId());

        logger.info("installMutant(): Stored mutant " + mutant.getId() + " in position " + targetPosition);

        installedMutants.get(classAlias).put(targetPosition, mutant);
    }

    /**
     * File convention: {@code tests/<cut_alias>/<position>/<filename>}.
     *
     * @see ParsingInterface#getTests() Parser tests convention.
     */
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

        String testFileContent = new String(Files.readAllBytes(testFile.toPath()), Charset.defaultCharset());

        Path cutDir = Paths.get(Constants.CUTS_DIR, classAlias);
        Path folderPath = cutDir.resolve(Constants.CUTS_TESTS_DIR).resolve(String.valueOf(targetPosition));
        Path javaFilePath = FileUtils.storeFile(folderPath, testFileName, testFileContent);
        String classFilePath = Compiler.compileJavaTestFileForContent(javaFilePath.toString(), testFileContent, dependencies, true);

        String testDir = folderPath.toString();
        String qualifiedName = FileUtils.getFullyQualifiedName(classFilePath);

        // This adds a jacoco.exec file to the testDir
        AntRunner.testOriginal(cut, testDir, qualifiedName);

        LineCoverage lineCoverage = LineCoverageGenerator.generate(cut, javaFilePath);
        Test test = new Test(javaFilePath.toString(), classFilePath, cut.getId(), lineCoverage);
        test.insert();
        TestDAO.mapTestToClass(test.getId(), cut.getId());

        logger.info("installTest() Stored test " + test.getId() + " in position " + targetPosition);

        installedTests.get(classAlias).put(targetPosition, test);
    }

    /**
     * File convention: {@code puzzleChapters/<files>}. Chapter specification files just
     * need to be in that folder.
     * <p>
     * Mandatory properties: {@code chapterId}.
     * <p>
     * Optional properties: {@code position}, {@code title}, {@code description}.
     *
     * @see ParsingInterface#getPuzzleChapterSpecs() Parser puzzle chapter convention.
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
     * <p>
     * Mandatory properties: {@code activeRole} ({@link Role#DEFENDER 'DEFENDER'} or {@link Role#ATTACKER 'ATTACKER'}),
     * {@code gameLevel} ({@link GameLevel#EASY 'EASY'} or {@link GameLevel#HARD 'HARD'},
     * {@code chapterId} (has to be of existing chapter).
     *
     * <p>
     * Optional properties: {@code mutants}, {@code tests}, {@code title}, {@code description},
     * {@code editableLinesStart}, {@code editableLinesEnd},
     * {@code position}.
     *
     * @see ParsingInterface#getPuzzleSpecs() Parser puzzles convention.
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
        GameClass puzzleClass = GameClass.ofPuzzle(cut.getName(), puzzleAlias, cut.getJavaFile(), cut.getClassFile(), cut.isMockingEnabled());
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
        if (!chapterIdOpt.isPresent() || !puzzleChapters.contains(chapterIdOpt.get())) {
            logger.warn("Provided chapterId for puzzle was not provided or does not exist. Skipping this puzzle.");
            return;
        }
        int chapterId = chapterIdOpt.get();
        Integer position = Optional.ofNullable(cfg.getProperty("position"))
                .map(Integer::parseInt)
                .orElse(null);

        // Default values
        int maxAssertionsPerTest = CodeValidator.DEFAULT_NB_ASSERTIONS;
        CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.MODERATE;

        Puzzle puzzle = new Puzzle(-1, puzzleClassId, activeRole, level, maxAssertionsPerTest, mutantValidatorLevel,
                editableLinesStart, editableLinesEnd, chapterId, position, title, description);
        int puzzleId = PuzzleDAO.storePuzzle(puzzle);

        List<Mutant> puzzleMutants = new ArrayList<>();
        // TODO batch insert
        for (Mutant m : originalMutants) {
            Mutant puzzleMutant = new Mutant(m.getJavaFile(), m.getClassFile(), m.getMd5(), puzzleClassId);
            puzzleMutant.insert();
            MutantDAO.mapMutantToClass(puzzleMutant.getId(), puzzleClassId);
            logger.info("installPuzzle(); Created Puzzle Mutant " + puzzleMutant.getId());
            puzzleMutants.add(puzzleMutant);
        }

        List<Test> puzzleTests = new ArrayList<>();
        // TODO batch insert
        for (Test t : originalTests) {
            Test puzzleTest = new Test(t.getJavaFile(), t.getClassFile(), puzzleClassId, t.getLineCoverage());
            puzzleTest.insert();
            TestDAO.mapTestToClass(puzzleTest.getId(), puzzleClassId);
            logger.info("installPuzzle(); Created Puzzle Test " + puzzleTest.getId());
            puzzleTests.add(puzzleTest);
        }

        logger.info("installPuzzle() Created Puzzle " + puzzleId);

        try {
            KillMap killMap = KillMap.forCustom(puzzleTests, puzzleMutants, puzzleClassId, new ArrayList<>(), true, (t, m) -> true);
            for (KillMap.KillMapEntry entry : killMap.getEntries()) {
                // TODO Phil 04/12/18: Batch insert instead of insert in a for loop
                KillmapDAO.insertKillMapEntry(entry, puzzleClassId);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could error while calculating killmap for successfully installed puzzle.", e);
        }
    }
}

package org.codedefenders.installer;

import static org.codedefenders.util.Constants.F_SEP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.Compiler;
import org.codedefenders.execution.LineCoverageGenerator;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.servlets.ClassUploadManager;
import org.codedefenders.util.Constants;
import org.codedefenders.util.JavaFileObject;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import com.mysql.cj.jdbc.MysqlDataSource;

public class Installer {

	public interface ParsingInterface {

		@Option
		File getConfigurations();

		@Option
		List<File> getCuts();

		@Option(defaultToNull = true)
		List<File> getMutants();

		@Option(defaultToNull = true)
		List<File> getTests();

		@Option(longName = "puzzles", defaultToNull = true)
		List<File> getPuzzleSpecs();

	}

	private static void setupInitialContext(Properties configurations) throws NamingException {
		// Create initial context
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
		System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

		InitialContext ic = new InitialContext();

		ic.createSubcontext("java:");
		ic.createSubcontext("java:/comp");
		ic.createSubcontext("java:/comp/env");
		// Maybe there a better way to do it ...
		for (String pName : configurations.stringPropertyNames()) {
			System.out.println("Installer.createDataSource() Storing property " + pName + " in the env with value "
					+ configurations.get(pName));
			ic.bind("java:/comp/env/" + pName, configurations.get(pName));
		}

		ic.createSubcontext("java:/comp/env/jdbc");

		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setURL(configurations.getProperty("db.url"));
		dataSource.setUser(configurations.getProperty("db.username"));
		dataSource.setPassword(configurations.getProperty("db.password"));

		ic.bind("java:/comp/env/jdbc/codedefenders", dataSource);

	}

	// Maps CUT name to the cutID in the DB
	private Map<String, GameClass> installedCuts = new HashMap<>();
	// Map classAlias and list of mutants for it
	// TODO Choose a better implementation
	private Map<String, Map<Integer, Mutant>> installedMutants = new HashMap<>();
	private Map<String, Map<Integer, Test>> installedTests = new HashMap<>();

	private void installCUT(File cutFile) throws Exception {
		String fileName = cutFile.getName();
		String classAlias = fileName.replaceAll(".java", "");

		byte[] encoded = Files.readAllBytes(Paths.get(cutFile.getAbsolutePath()));
		String fileContent = new String(encoded, Charset.defaultCharset());

		String cutDir = Constants.CUTS_DIR + F_SEP + classAlias;

		boolean isMockingEnabled = false;

		// Store the CUT
		String cutJavaFilePath = null;
		String cutClassFilePath = null;
		String classQualifiedName = null;
		GameClass cut = null;
		int cutId = -1;
		cutJavaFilePath = ClassUploadManager.storeJavaFile(cutDir, fileName, fileContent);
		System.out.println("Installer: stored CUT file to " + cutJavaFilePath);
		cutClassFilePath = Compiler.compileJavaFileForContent(cutJavaFilePath, fileContent);
		System.out.println("Installer: compiled CUT file to " + cutClassFilePath);
		classQualifiedName = ClassUploadManager.getFullyQualifiedName(cutClassFilePath);
		cut = new GameClass(classQualifiedName, classAlias, cutJavaFilePath, cutClassFilePath, isMockingEnabled);
		// Pretty sure this does not update the cut object as one might
		// expect. Or at least, return a new instance of the CUT object
		cutId = GameClassDAO.storeClass(cut);
		// Update the class to include the cutID
		cut = new GameClass(cutId, classQualifiedName, classAlias, cutJavaFilePath, cutClassFilePath, isMockingEnabled);
		System.out.println("Installer.main() Stored Class " + cut.getId() + " to " + cutJavaFilePath);
		installedCuts.put(classAlias, cut);
		installedMutants.put(classAlias, new HashMap<Integer, Mutant>());
		installedTests.put(classAlias, new HashMap<Integer, Test>());
	}

	private void installMutant(File mutantFile) throws Exception {
		String mutantFileName = mutantFile.getName();
		String classAlias = mutantFileName.replaceAll(".java", "");
		// Convention over configuration
		Integer targetPosition = Integer.parseInt(mutantFile.getParentFile().getName());

		if (!installedMutants.containsKey(classAlias)) {
			throw new RuntimeException("There is not CUT installed for this mutant");
		}
		GameClass cut = installedCuts.get(classAlias);

		String cutDir = Constants.CUTS_DIR + F_SEP + classAlias;

		byte[] mutantEncoded = Files.readAllBytes(Paths.get(mutantFile.getAbsolutePath()));
		String mutantFileContent = new String(mutantEncoded, Charset.defaultCharset());

		String folderPath = String.join(F_SEP, cutDir, Constants.CUTS_MUTANTS_DIR, String.valueOf(targetPosition));
		//
		String javaFilePath = ClassUploadManager.storeJavaFile(folderPath, mutantFileName, mutantFileContent);
		String classFilePath = Compiler.compileJavaFileForContent(javaFilePath, mutantFileContent);

		byte[] cutEncoded = Files.readAllBytes(Paths.get(cut.getJavaFile()));
		String cutContent = new String(cutEncoded, Charset.defaultCharset());

		String md5 = CodeValidator.getMD5FromText(cutContent);
		Mutant mutant = new Mutant(javaFilePath, classFilePath, md5, cut.getId());

		int mutantId = MutantDAO.storeMutant(mutant);
		mutant = DatabaseAccess.getMutantById(mutantId);

		System.out.println("Installer.main() Stored mutant " + mutantId + " in position " + targetPosition);
		installedMutants.get(classAlias).put(targetPosition, mutant);
	}

	private void installedTest(File testFile) throws Exception {
		String testFileName = testFile.getName();
		String classAlias = testFile.getParentFile().getParentFile().getName();

		System.out.println("Installer.installedTest() " + classAlias);

		// Convention over configuration
		Integer targetPosition = Integer.parseInt(testFile.getParentFile().getName());

		if (!installedTests.containsKey(classAlias)) {
			throw new RuntimeException("There is not CUT installed for this test");
		}
		GameClass cut = installedCuts.get(classAlias);

		// All the tests require the same dependency on the CUT
		List<JavaFileObject> dependencies = new ArrayList<>();
		dependencies.add(new JavaFileObject(cut.getJavaFile()));

		byte[] testEncoded = Files.readAllBytes(Paths.get(testFile.getAbsolutePath()));
		String testFileContent = new String(testEncoded, Charset.defaultCharset());

		String cutDir = Constants.CUTS_DIR + F_SEP + classAlias;
		final String folderPath = String.join(F_SEP, cutDir, Constants.CUTS_TESTS_DIR, String.valueOf(targetPosition));
		//
		String javaFilePath = ClassUploadManager.storeJavaFile(folderPath, testFileName, testFileContent);
		String classFilePath = Compiler.compileJavaTestFileForContent(javaFilePath, testFileContent, dependencies,
				true);

		final String testDir = Paths.get(javaFilePath).getParent().toString();
		final String qualifiedName = ClassUploadManager.getFullyQualifiedName(classFilePath);

		// This adds a jacoco.exec file to the testDir
		AntRunner.testOriginal(cut, testDir, qualifiedName);

		// LineCoverage#generate requires at least a dummy test object
		final Test dummyTest = new Test(javaFilePath, null, -1, null);

		final LineCoverage lineCoverage = LineCoverageGenerator.generate(cut, dummyTest);
		Test test = new Test(javaFilePath, classFilePath, cut.getId(), lineCoverage);
		int testId = TestDAO.storeTest(test);

		test = DatabaseAccess.getTestForId(testId);

		System.out.println("Installer.main() Stored test " + test.getId() + " in position " + targetPosition);

		installedTests.get(classAlias).put(targetPosition, test);
	}

	private List<Mutant> getMutantsByPositions(String classAlias, String[] positions) {
		List<Mutant> mutants = new ArrayList<>();
		for (String position : positions) {
			int p = Integer.parseInt(position);
			if (installedMutants.get(classAlias).containsKey(p)) {
				mutants.add(installedMutants.get(classAlias).get(p));
			}
		}
		return mutants;
	}

	private List<Test> getTestsByPositions(String classAlias, String[] positions) {
		List<Test> tests = new ArrayList<>();
		for (String position : positions) {
			int p = Integer.parseInt(position);
			if (installedTests.get(classAlias).containsKey(p)) {
				tests.add(installedTests.get(classAlias).get(p));
			}
		}
		return tests;
	}

	public void createPuzzle(Properties cfg) throws Exception {
		// Collect the data to create the puzzle
		GameClass originalCut = getCutByAlias(cfg.getProperty("cut"));
		if (originalCut == null) {
			throw new Exception("Cannot create puzzle. Missing CUT " + cfg.getProperty("cut"));
		}

		List<Mutant> originalMutants = getMutantsByPositions(originalCut.getAlias(),
				cfg.getProperty("mutants").split(","));
		List<Test> originalTests = getTestsByPositions(originalCut.getAlias(), cfg.getProperty("tests").split(","));

		// TODO How to create an Alias ?
		// This shall be handled directly inside the DB with a query of some
		// sort...
		String alias = null;
		for (int i = 1; i < 100; i++) {
			alias = originalCut.getAlias() + "_Puzzle_" + i;
			if (!GameClassDAO.classNotExistsForAlias(alias))
				break;
		}
		if (alias == null) {
			throw new Exception("Cannot create puzzle. Cannot find a suitable alias for CUT " + cfg.getProperty("cut"));
		}

		// Create another cut for this puzzle
		GameClass puzzleClass = new GameClass(originalCut.getName(), alias,
				originalCut.getJavaFile(), originalCut.getClassFile(), originalCut.isMockingEnabled());
		// Pretty sure this does not update the cut object as one might
		// expect. Or at least, return a new instance of the CUT object
		int puzzleClassId = GameClassDAO.storeClass(puzzleClass);
		System.out.println("Installer.createPuzzle() Created Puzzle Class " + puzzleClassId );
		
		// TODO Attach all the tests and mutants to this class... Really this shall
		// be handled with TRANSACTIONS !
		for (Mutant m : originalMutants) {
			Mutant puzzleMutant = new Mutant(m.getJavaFile(), m.getClassFile(), m.getMd5(), puzzleClassId);
			int puzzleMutantId = MutantDAO.storeMutant(puzzleMutant);
			System.out.println("Installer.createPuzzle() Created Puzzle Mutant " + puzzleMutantId );
		}

		// TODO Attach all the tests and mutants to this class... Really this shall
		// be handled with TRANSACTIONS !
		for (Test t : originalTests) {
			Test puzzleTest = new Test(t.getJavaFile(), t.getClassFile(), puzzleClassId, t.getLineCoverage());
			int puzzleTestId = TestDAO.storeTest(puzzleTest);
			System.out.println("Installer.createPuzzle() Created Puzzle Test " + puzzleTestId );

		}

		// String classId =
		Role activeRole = Role.valueOf(cfg.getProperty("activeRole"));
		GameLevel level = GameLevel.valueOf(cfg.getProperty("gameLevel", "HARD"));

		// Default values follows
		int maxAssertionsPerTest = 2;
		CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.MODERATE;
		String title = cfg.getProperty("title");
		String description = cfg.getProperty("description");

		Integer editableLinesStart = null;
		Integer editableLinesEnd = null;
		Integer chapterId = null;
		Integer position = null;

		Puzzle puzzle = new Puzzle(-1, puzzleClassId, activeRole, level, maxAssertionsPerTest, mutantValidatorLevel,
				editableLinesStart, editableLinesEnd, chapterId, position, title, description);
		int puzzleId = PuzzleDAO.storePuzzle(puzzle);
		System.out.println("Installer.createPuzzle() Created Puzzle " + puzzleId );

	}

	private GameClass getCutByAlias(String alias) {
		for (GameClass cut : installedCuts.values()) {
			if (cut.getAlias().equals(alias)) {
				return cut;
			}
		}
		return null;
	}

	// No validation of input at the moment
	// We make the assumption that mutants are named after class alias !
	public static void main(String[] args) throws FileNotFoundException, IOException, NamingException {
		ParsingInterface commandLine = CliFactory.parseArguments(ParsingInterface.class, args);
		Properties configurations = new Properties();
		configurations.load(new FileInputStream(commandLine.getConfigurations()));
		setupInitialContext(configurations);

		Installer installer = new Installer();
		for (File cutFile : commandLine.getCuts()) {
			try {
				installer.installCUT(cutFile);
			} catch (Exception e) {
				System.err.println("Failed to install CUT " + cutFile + ", " + e);
			}
		}
		// Mutants must be processed in order starting from 0 !
		for (File mutantFile : commandLine.getMutants()) {
			try {
				installer.installMutant(mutantFile);
			} catch (Exception e) {
				System.err.println("Failed to install MUTANT " + mutantFile + ", " + e);
			}
		}

		for (File testFile : commandLine.getTests()) {
			try {
				installer.installedTest(testFile);
			} catch (Exception e) {
				System.err.println("Failed to install TEST " + testFile + ", " + e);
			}
		}

		for (File puzzleSpecFile : commandLine.getPuzzleSpecs()) {
			try {
				Properties cfg = new Properties();
				cfg.load(new FileInputStream(puzzleSpecFile));
				installer.createPuzzle(cfg);
			} catch (Exception e) {
				System.err.println("Failed to install PUZZLE " + puzzleSpecFile + ", " + e);
				e.printStackTrace();
			}
		}

		// Running this code with mvn exec:java hangs the execution so we force the exit
		System.exit(0);
	}

}

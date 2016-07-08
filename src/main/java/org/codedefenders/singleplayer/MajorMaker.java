package org.codedefenders.singleplayer;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.codedefenders.Constants.AI_DIR;
import static org.codedefenders.Constants.F_SEP;
import static org.codedefenders.Constants.JAVA_SOURCE_EXT;

public class MajorMaker {

	private String cutTitle;
	private int cId;
	private GameClass cut;
	private Game dGame;
	private ArrayList<Mutant> validMutants;

	public MajorMaker(int classId, Game dummyGame) {
		cId = classId;
		cut = DatabaseAccess.getClassForKey("Class_ID", cId);
		cutTitle = cut.getBaseName();
		dGame = dummyGame;
	}

	public boolean createMutants() {
		AntRunner.generateMutantsFromCUT(cutTitle);

		File cutFile = new File(cut.javaFile);
		List<String> cutLines = FileManager.readLines(cutFile.toPath());
		validMutants = new ArrayList<Mutant>();

		for (String info : getMutantList()) {
			//Each mutant in mutants log.

			//Get required mutant info.
			MutantPatch mP = createMutantPatch(info);

			//Modify original contents with mutant.
			List<String> newLines = doPatch(cutLines, mP);
			String mText = "";
			for (String l : newLines) {
				mText += l + "\n";
			}
			Mutant m = createMutant(mText);
			if (m != null) {
				//Successfully created and compiled(?) mutant.
				validMutants.add(m);
			}
		}

		return true;
	}

	public boolean createMutantIndex() {
		File dir = new File(AI_DIR + F_SEP + "mutants" + F_SEP + cutTitle);

		String xml = "<?xml version=\"1.0\"?>\n";
		xml += "<mutantindex>\n";
		xml += "\t<mutants>\n";
		for(Mutant m : validMutants) {
			xml += "\t\t<mutant ";
			xml += "id=\"" + m.getId() + "\" ";
			//xml += "killed=\"" + m.getTimesKilledAi() + "\" ";
			xml += "/>\n";
		}
		xml += "\t</mutants>\n";
		xml += "\t<quantity>" + validMutants.size() + "</quantity>\n";
		xml += "\t<dummygame>" + dGame.getId() + "</dummygame> \n";

		xml += "</mutantindex>\n";

		try {
			FileManager.createIndexXML(dir, "MutantsIndex", xml);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Modify a list of strings from a class to have a mutant.
	 * @param lines original class's lines.
	 * @param patch the mutant's patch information.
	 * @return the new list of lines.
	 */
	private List<String> doPatch(List<String> lines, MutantPatch patch) {
		//Copy contents of original lines.
		List<String> newLines = new ArrayList<String>(lines);
		String l = newLines.get(patch.getLineNum() - 1);
		String newLine = l.replaceFirst(patch.getOriginal(), patch.getReplacement().replace("QE", ""));
		newLines.set(patch.getLineNum() - 1, newLine);
		return newLines;
	}

	private MutantPatch createMutantPatch(String mutantInfo) {
		String[] splitInfo = mutantInfo.split(":");
			/*
			0 = Mutant's id
			1 = Name of mutation operator
			2 = Original operator symbol
			3 = New operator symbol
			4 = Full name of mutated method
			5 = Line number of CUT
			6 = 'from' |==> 'to'  (<NO-OP> means empty string)
			 */
		//Only really need values 5 and 6.
		//Use replace option?
		int lineNum = Integer.parseInt(splitInfo[5]);
		String[] beforeAfter = splitInfo[6].split(Pattern.quote(" |==> ")); //Before = 0, After = 1

		return new MutantPatch(lineNum, beforeAfter);
	}

	private Mutant createMutant(String mutantText) {

		try {
			File srcFile = new File(cut.javaFile);
			String srcCode = new String(Files.readAllBytes(srcFile.toPath()));

			// Runs diff match patch between the two Strings to see if there are any differences.
			DiffMatchPatch dmp = new DiffMatchPatch();
			LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(srcCode.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);
			boolean noChange = true;
			for (DiffMatchPatch.Diff d : changes) {
				if (d.operation != DiffMatchPatch.Operation.EQUAL) {
					noChange = false;
				}
			}
			// If there were no differences, return, as the mutant is the same as original.
			if (noChange)
				return null;

			// Setup folder the files will go in
			File newMutantDir = FileManager.getNextSubDir(AI_DIR + F_SEP + "mutants" + F_SEP + cutTitle);

			// Write the Mutant String into a java file
			String mutantFileName = newMutantDir + F_SEP + cutTitle + JAVA_SOURCE_EXT;
			File mutantFile = new File(mutantFileName);
			FileWriter fw = new FileWriter(mutantFile);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(mutantText);
			bw.close();

			// Compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.
			return AntRunner.compileMutant(newMutantDir, mutantFileName, dGame.getId(), cut, 1);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;


		/*
		GameManager gm = new GameManager();
		try {
			//Create mutant and insert it into the database.
			//TODO: CHECK MUTANT COMPILES
			Mutant m = gm.createMutant(dGame.getId(), cut.id, mutantText, 1);
			//TODO: More error checking.
			ArrayList<String> messages = new ArrayList<String>();
			MutationTester.runAllTestsOnMutant(dGame, m, messages);
		} catch (IOException e) {
			//Try again if an exception.
			e.printStackTrace();
			return false;
		}
		return true;
		*/
	}

	private List<String> getMutantList() {
		String loc = AI_DIR + F_SEP + "mutants" + F_SEP + cutTitle + ".log";
		File f = new File(loc);
		List<String> l = FileManager.readLines(f.toPath());
		//TODO: Handle errors.
		return l;
	}
}

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
package org.codedefenders.game.singleplayer.automated.attacker;

import static org.codedefenders.util.Constants.AI_DIR;
import static org.codedefenders.util.Constants.F_SEP;
import static org.codedefenders.util.Constants.JAVA_SOURCE_EXT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.ClassCompilerService;
import org.codedefenders.execution.MutantGeneratorService;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MajorMaker {
    private static final Logger logger = LoggerFactory.getLogger(MajorMaker.class);

    @Inject
    private MutantGeneratorService mutantGenerator;
    @Inject
    private ClassCompilerService classCompiler;
    
	private int cId;
	private GameClass cut;
	private DuelGame dGame;
	private ArrayList<Mutant> validMutants;

	public MajorMaker(int classId, DuelGame dummyGame) {
		cId = classId;
		cut = GameClassDAO.getClassForId(cId);
		dGame = dummyGame;
	}

	public ArrayList<Mutant> getValidMutants() {
		return validMutants;
	}

	public void createMutants() throws NoMutantsException {
	    mutantGenerator.generateMutantsFromCUT(cut);

		File cutFile = new File(cut.getJavaFile());
		List<String> cutLines = FileUtils.readLines(cutFile.toPath());
		validMutants = new ArrayList<Mutant>();

		for (String info : getMutantList()) {
			//Each mutant in mutants log.

			//Get required mutant info.
			MutantPatch mP = createMutantPatch(info);

			//Modify original contents with mutant.
			List<String> newLines = doPatch(cutLines, mP);
			final String mutantText = String.join("\n", newLines);

			Mutant m = createMutant(mutantText);
			if (m != null) {
				//Successfully created mutant.
				if(m.getClassFile() != null) {
					validMutants.add(m);
				}
			}
		}
		if (validMutants.isEmpty()) {
			//No valid mutants exist.
			throw new NoMutantsException();
		}
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
		String pOrig = patch.getOriginal();
		//TODO: Check the validity of the escape char fixing in the replacement below (ie replace QE ...)
		String pRepl = patch.getReplacement().replace("QE","");
		String newLine = l.replaceFirst(Pattern.quote(pOrig), pRepl);
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
			final String sourceCode = cut.getSourceCode();

			// Runs diff match patch between the two Strings to see if there are any differences.
			DiffMatchPatch dmp = new DiffMatchPatch();
			LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(sourceCode.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);
			boolean noChange = true;
			for (DiffMatchPatch.Diff d : changes) {
				if (d.operation != DiffMatchPatch.Operation.EQUAL) {
					noChange = false;
					break;
				}
			}
			// If there were no differences, return, as the mutant is the same as original.
			if (noChange) {
				return null;
			}

			// Setup folder the files will go in
			File newMutantDir = FileUtils.getNextSubDir(AI_DIR + F_SEP + "mutants" + F_SEP + cut.getAlias());

			// 1 the Mutant String into a java file
			String mutantFileName = newMutantDir + F_SEP + cut.getBaseName() + JAVA_SOURCE_EXT;
			File mutantFile = new File(mutantFileName);
			FileWriter fw = new FileWriter(mutantFile);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(mutantText);
			bw.close();
			fw.close();

			// Compile the mutant - if you can, add it to the Game State, otherwise, delete these files created.
			return classCompiler.compileMutant(newMutantDir, mutantFileName, dGame.getId(), cut, AiAttacker.ID);
		} catch (IOException e) {
			logger.error("Could not write mutant", e);
		}
		return null;

	}

	private List<String> getMutantList() {
		String loc = AI_DIR + F_SEP + "mutants" + F_SEP + cut.getAlias() + ".log";
		File f = new File(loc);
		List<String> l = FileUtils.readLines(f.toPath());
		//TODO: Handle errors.
		return l;
	}
}

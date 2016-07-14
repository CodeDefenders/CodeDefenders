package org.codedefenders.singleplayer;

import difflib.Patch;
import difflib.PatchFailedException;
import org.codedefenders.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.regex.Pattern;

import static org.codedefenders.Constants.*;

/**
 * @author Ben Clegg
 * An AI attacker, which chooses mutants generated by Major when the class is uploaded.
 */
public class AiAttacker extends AiPlayer {

	public AiAttacker(Game g) {
		super(g);
		role = Role.ATTACKER;
	}

	/**
	 * Hard difficulty attacker turn.
	 * @return true if mutant generation succeeds, or if no non-existing mutants have been found to prevent infinite loop.
	 */
	public boolean turnHard() {
		//Use only one mutant per round.
		//Perhaps modify the line with the least test coverage?

		//TODO: Determine by lowest test coverage. Using easy behaviour for now.
		return turnEasy();
	}

	/**
	 * Easy difficulty attacker turn.
	 * @return true if mutant generation succeeds, or if no non-existing mutants have been found to prevent infinite loop.
	 */
	public boolean turnEasy() {
		try {
			MutantsIndexContents ind = new MutantsIndexContents(game.getCUT());

			int mNum = selectMutant(GenerationMethod.RANDOM, ind);
			try {
				useMutantFromSuite(mNum, ind);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} catch (Exception e) {
			//Assume no more unused mutants remain, do nothing.
		}

		return true;
	}


	private int selectMutant(GenerationMethod strategy, MutantsIndexContents indexCon) throws Exception {
		ArrayList<Integer> usedMutants = DatabaseAccess.getUsedAiMutantsForGame(game);
		Exception e = new Exception("No unused mutants remain.");
		int totalMutants = indexCon.getNumMutants();

		if(usedMutants.size() == totalMutants) {
			throw e;
		}
		int m = -1;

		Game dummyGame = DatabaseAccess.getGameForKey("Game_ID", indexCon.getDummyGameId());
		ArrayList<Mutant> origMutants = dummyGame.getMutants();

		for (int i = 0; i < 10; i++) {
			//Try standard strategy to select a mutant.
			int n = 0;
			if(strategy.equals(GenerationMethod.RANDOM)) {
				n = (int) Math.floor(Math.random() * totalMutants);
				//0 -> totalMutants - 1.
			}
			//TODO: Other generation strategies

			Mutant origM = origMutants.get(n);
			m = origM.getId();

			if ((!usedMutants.contains(m)) && (m != -1)) {
				//Found an unused mutant.
				return m;
			}
		}

		//If standard strategy fails, make a choice linearly.
		for (int x = 0; x < totalMutants; x++) {

			Mutant origM = origMutants.get(x);
			m = origM.getId();

			if(!usedMutants.contains(m)) {
				//Found an unused mutant.
				return m;
			}
		}

		//Something went wrong.
		throw e;
	}

	private void useMutantFromSuite(int origMutNum, MutantsIndexContents indexCon) throws IOException {
		Game dummyGame = DatabaseAccess.getGameForKey("Game_ID", indexCon.getDummyGameId());
		ArrayList<Mutant> origMutants = dummyGame.getMutants();

		Mutant origM = null;

		for (Mutant m : origMutants) {
			if(m.getId() == origMutNum) {
				origM = m;
				break;
			}
		}

		if(origM != null) {
			String jFile = origM.getSourceFile();
			String cFile = origM.getClassFile();
			Mutant m = new Mutant(game.getId(), jFile, cFile, true, 1);
			m.insert();
			m.update();

			ArrayList<String> messages = new ArrayList<String>();
			MutationTester.runAllTestsOnMutant(game, m, messages);
			DatabaseAccess.setAiMutantAsUsed(origMutNum, game);
			game.update();
		}
	}

}


class MutantsIndexContents {

	private ArrayList<Integer> mutantIds;
	private int dummyGameId;
	private int numMutants;

	public MutantsIndexContents(GameClass cut) {

		mutantIds = new ArrayList<Integer>();
		dummyGameId = -1;
		numMutants = -1;
		//Parse the test index file of a given class.
		try {
			File f = new File(AI_DIR + F_SEP + "mutants" + F_SEP +
					cut.getAlias() + F_SEP + "MutantsIndex.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuild = dbFactory.newDocumentBuilder();
			Document d = dBuild.parse(f);

			d.getDocumentElement().normalize();

			NodeList mutNodes = d.getElementsByTagName("mutant");
			for (int i = 0; i < mutNodes.getLength(); i++) {
				Node mutNode = mutNodes.item(i);
				Node id = mutNode.getAttributes().getNamedItem("id");
				mutantIds.add(Integer.parseInt(id.getTextContent()));
			}
			NodeList q = d.getElementsByTagName("quantity");
			numMutants = Integer.parseInt(q.item(0).getTextContent());
			NodeList g = d.getElementsByTagName("dummygame");
			dummyGameId = Integer.parseInt(g.item(0).getTextContent());

		} catch (Exception e) {
			e.printStackTrace();
			//TODO: Handle errors.
		}


	}

	public ArrayList<Integer> getMutantIds() {
		return mutantIds;
	}

	public int getNumMutants() {
		return numMutants;
	}

	public int getDummyGameId() {
		return dummyGameId;
	}

}
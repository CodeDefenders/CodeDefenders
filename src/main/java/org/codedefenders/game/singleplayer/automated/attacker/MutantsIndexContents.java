/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

import org.codedefenders.game.GameClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.codedefenders.util.Constants.AI_DIR;
import static org.codedefenders.util.Constants.F_SEP;

/**
 * @author Ben Clegg
 */
public class MutantsIndexContents {

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

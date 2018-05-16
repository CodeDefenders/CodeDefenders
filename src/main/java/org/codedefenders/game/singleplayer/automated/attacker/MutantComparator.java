package org.codedefenders.game.singleplayer.automated.attacker;

import org.codedefenders.game.Mutant;

import java.util.Comparator;

/**
 * @author Ben Clegg
 */
public class MutantComparator implements Comparator<Mutant> {
	@Override
	public int compare(Mutant m1, Mutant m2) {
		return m1.getTimesKilledAi() - m2.getTimesKilledAi();
	}
}

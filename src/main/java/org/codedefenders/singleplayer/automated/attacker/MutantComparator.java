package org.codedefenders.singleplayer.automated.attacker;

import org.codedefenders.Mutant;

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

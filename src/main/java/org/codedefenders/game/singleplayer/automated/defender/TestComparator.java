package org.codedefenders.game.singleplayer.automated.defender;

import org.codedefenders.game.Test;

import java.util.Comparator;

/**
 * @author Ben Clegg
 */
class TestComparator implements Comparator<Test> {
	@Override
	public int compare(Test t1, Test t2) {
		return t1.getAiMutantsKilled() - t2.getAiMutantsKilled();
	}
}

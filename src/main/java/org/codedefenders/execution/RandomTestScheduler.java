package org.codedefenders.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codedefenders.game.Test;

import edu.emory.mathcs.backport.java.util.Collections;

public class RandomTestScheduler implements TestScheduler {

	@Override
	public List<Test> scheduleTests(Collection<Test> tests) {
		// Shuffle tests
		List<Test> randomSchedule = new ArrayList<Test>( tests );
		Collections.shuffle( randomSchedule );
		return randomSchedule;
	}

}

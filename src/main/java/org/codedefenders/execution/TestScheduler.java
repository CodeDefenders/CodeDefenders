package org.codedefenders.execution;

import java.util.Collection;
import java.util.List;

import org.codedefenders.game.Test;

public interface TestScheduler {
	
	/**
	 * Returns an ordered sequence of tests to be executed
	 * @param tests
	 * @return
	 */
	public List<Test> scheduleTests(Collection<Test> tests);

}

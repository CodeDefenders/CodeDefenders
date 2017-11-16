package org.codedefenders.itests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * This is just to showcase how to use JUnit Categories
 * 
 * @author gambi
 *
 */
@Category(IntegrationTest.class)
public class SimpleIntegrationTest {

	@Test
	public void longRunningServiceTest() throws Exception {
	}
}

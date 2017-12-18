package org.codedefenders.itests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Vector;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class ExecuteMockTest {
	
	@Test
	public void testAntRunnerWithMock(){
		Vector<String> v = new Vector<String>();
		Vector spy = spy(v);
	    doReturn("foo").when(spy).get(0);
	    assertEquals("foo", spy.get(0));
	}

}

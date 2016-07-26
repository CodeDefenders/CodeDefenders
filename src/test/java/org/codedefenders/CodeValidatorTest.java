package org.codedefenders;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.codedefenders.validation.CodeValidator.validMutant;
import static org.codedefenders.validation.CodeValidator.validTestCode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Jose Rojas
 */
public class CodeValidatorTest {

	@Test
	public void testInvalidSuiteWithTwoClasses() throws IOException {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoClasses.java");
		assertFalse("Should be invalid; file contains a two classes", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidEmptyTest() throws IOException {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource("EmptyTest.java");
		assertFalse(validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTwoTests() throws IOException {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoTests.java");
		assertFalse("Should be invalid; class contains two tests", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTestWithTooManyAssertions() throws IOException {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithTooManyAssertions.java");
		assertFalse("Should be invalid; test has too many assertions", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTestWithIf() throws IOException {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithIf.java");
		assertFalse("Should be invalid; test contains if statement", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTestWithSystemCalls() throws IOException {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall.java");
		assertFalse("Should be invalid; test contains system call", validTestCode(url.getPath()));

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall2.java");
		assertFalse("Should be invalid; test contains system call", validTestCode(url.getPath()));

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall3.java");
		assertFalse("Should be invalid; test contains system call", validTestCode(url.getPath()));

	}

	@Test
	public void testValidTest() throws IOException {
		
		URL url = Thread.currentThread().getContextClassLoader().getResource("ValidTest.java");
		assertTrue("Should be valid", validTestCode(url.getPath()));
	}

	@Test
	public void testValidMutant1() {
		String orig = "int x = x + 0;";
		String mutant = "int x = x + 1;";

		
		assertTrue(validMutant(orig, mutant));
	}

	@Test
	public void testValidMutant2() {
		String orig = "int x = x + 1;";
		String mutant = "int x = x - 1;";

		
		assertTrue(validMutant(orig, mutant));
	}

	@Test
	public void testValidMutant3() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; x++;";

		
		assertTrue(validMutant(orig, mutant));
	}

	@Test
	public void testInvalidMutant1() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; if (x>0) {return false;}";

		
		assertFalse(validMutant(orig, mutant));
	}

	@Test
	public void testInvalidMutant2() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; while (x>0) {return false;}";

		
		assertFalse(validMutant(orig, mutant));
	}

	@Test
	public void testInvalidMutant3() {
		String orig = "int x = 0;";
		String mutant = "System.getCurrentMillis(); int x = 0;";

		
		assertFalse(validMutant(orig, mutant));
	}
}

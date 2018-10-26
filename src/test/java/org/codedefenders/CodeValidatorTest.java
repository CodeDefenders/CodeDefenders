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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders;

import org.apache.commons.io.FileUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.GameClass;
import org.codedefenders.servlets.games.GameManager;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.CodeValidator;
import org.codedefenders.validation.CodeValidatorException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.codedefenders.validation.CodeValidator.CodeValidatorLevel;
import static org.codedefenders.validation.CodeValidator.getValidationMessage;
import static org.codedefenders.validation.CodeValidator.validMutant;
import static org.codedefenders.validation.CodeValidator.validTestCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jose Rojas
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseAccess.class})
public class CodeValidatorTest {

	private CodeValidator.CodeValidatorLevel codeValidatorLevel = CodeValidator.CodeValidatorLevel.STRICT;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testMakingStringLiteralsDoesNotTriggersValidation() throws IOException{
		String originalCode = "" + "\n" +
"	public final String[] getValues(String name) {" + "\n" +
"	    List<String> result = new ArrayList<String>();" + "\n" +
"	    for (Fieldable field : fields) {" + "\n" +
"	      if (field.name().equals(name) && (!field.isBinary()))" + "\n" +
"	        result.add(field.stringValue());" + "\n" +
"	    }" + "\n" +
"" + "\n" +
"	    if (result.size() == 0)" + "\n" +
"	      return NO_STRINGS;" + "\n" +
"" + "\n" +
"	    return result.toArray(new String[result.size()]);" + "\n" +
"	  }";

		String mutatedCode = "" + "\n" +
"	public final String[] getValues(String name) {" + "\n" +
"	    List<String> result = new ArrayList<String>();" + "\n" +
"	    for (Fieldable field : fields) {" + "\n" +
// The following line is changed !
"	      if (field.name().equals(\"name\") && (!field.isBinary()))" + "\n" +
//
"	        result.add(field.stringValue());" + "\n" +
"	    }" + "\n" +
"" + "\n" +
"	    if (result.size() == 0)" + "\n" +
"	      return NO_STRINGS;" + "\n" +
"" + "\n" +
"	    return result.toArray(new String[result.size()]);" + "\n" +
"	  }";

		File tmpFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(tmpFile, originalCode);

		// Mock the class provide a temmp sourceFile with original content in it
		GameClass mockedGameClass = mock(GameClass.class);

		when(mockedGameClass.getJavaFile()).thenReturn(tmpFile.getPath());

		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getClassForKey(anyString(), anyInt())).thenReturn(mockedGameClass);

		String validityMessage = GameManager.getMutantValidityMessage(1, mutatedCode, codeValidatorLevel);

		assertEquals(Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE, validityMessage);
	}

	// TODO Validation should be checked at the game interface level !
	@Test
	public void testSameStringsShouldTriggerValidation() throws IOException{
		String originalCode = "@Override\n"+ 
"  public final String toString() {\n"+
"    StringBuilder buffer = new StringBuilder();\n"+
"    buffer.append(\"Document<\");\n"+
"    for (int i = 0; i < fields.size(); i++) {\n"+
"      Fieldable field = fields.get(i);\n"+
"      buffer.append(field.toString());\n"+
"      if (i != fields.size()-1)\n"+
"        buffer.append(\" \");\n"+
"    }\n"+
"    buffer.append(\">\");\n"+
"    return buffer.toString();\n"+
"  }";
		
		String mutatedCode = "@Override\n"+ 
				"  public final String toString() {\n"+
				"    StringBuilder buffer = new StringBuilder();\n"+
				"    buffer.append(\"Document<\");\n"+
				"    for (int i = 0; i < fields.size(); i++) {\n"+
				"      Fieldable field = fields.get(i);\n"+
				"      buffer.append(field.toString());\n"+
				"      if (i != fields.size()-1)\n"+
				"        buffer.append(\" \");\n"+
				"    }\n"+
				"    buffer.append(\">\");\n"+
				"    return buffer.toString();\n"+
				"  }";

		File tmpFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(tmpFile, originalCode);
		
		// Mock the class provide a temmp sourceFile with original content in it
		GameClass mockedGameClass = mock(GameClass.class); //new GameClass(1, "name", "name", "jFile", "cFile", true);
		
		when(mockedGameClass.getJavaFile()).thenReturn( tmpFile.getPath() );
		
		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getClassForKey(anyString(), anyInt())).thenReturn( mockedGameClass );
		
		String validityMessage = GameManager.getMutantValidityMessage(1, originalCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_IDENTICAL_MESSAGE, validityMessage);
	}
	
	@Test
	public void testAddSpaceToStringsShouldNotTriggerValidation() throws IOException{
		String originalCode = "@Override\n"+ 
"  public final String toString() {\n"+
"    StringBuilder buffer = new StringBuilder();\n"+
"    buffer.append(\"Document<\");\n"+
"    for (int i = 0; i < fields.size(); i++) {\n"+
"      Fieldable field = fields.get(i);\n"+
"      buffer.append(field.toString());\n"+
"      if (i != fields.size()-1)\n"+
"        buffer.append(\" \");\n"+
"    }\n"+
"    buffer.append(\">\");\n"+
"    return buffer.toString();\n"+
"  }";
		
		String mutatedCode = "@Override\n"+ 
				"  public final String toString() {\n"+
				"    StringBuilder buffer = new StringBuilder();\n"+
				// This is the mutated line !
				"    buffer.append(\"Document< \");\n"+
				//
				"    for (int i = 0; i < fields.size(); i++) {\n"+
				"      Fieldable field = fields.get(i);\n"+
				"      buffer.append(field.toString());\n"+
				"      if (i != fields.size()-1)\n"+
				"        buffer.append(\" \");\n"+
				"    }\n"+
				"    buffer.append(\">\");\n"+
				"    return buffer.toString();\n"+
				"  }";

		File tmpFile = temporaryFolder.newFile();
		FileUtils.writeStringToFile(tmpFile, originalCode);
		
		// Mock the class provide a temmp sourceFile with original content in it
		GameClass mockedGameClass = mock(GameClass.class); //new GameClass(1, "name", "name", "jFile", "cFile", true);
		
		when(mockedGameClass.getJavaFile()).thenReturn( tmpFile.getPath() );
		
		PowerMockito.mockStatic(DatabaseAccess.class);
		when(DatabaseAccess.getClassForKey(anyString(), anyInt())).thenReturn( mockedGameClass );
		
		String validityMessage = GameManager.getMutantValidityMessage(1, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE, validityMessage);

	}
	
	@Test
	public void testValidChangeDoesNotTriggersValidation(){
		String originalCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String mutatedCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return 0; }\n"
				+ "};";
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE, validationMessage);
	}

	@Test
	public void testChangeFieldNameTriggersValidation(){
		String originalCode = "public class UnderTest{\n"
				+ "private int foo;\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String mutatedCode = "public class UnderTest{\n"
				+ "private int bar;\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE, validationMessage);
	}

	@Test
	public void testChangeFieldInitializerDoesNotTriggersValidation(){
		String originalCode = "public class UnderTest{\n"
				+ "private int foo;\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String mutatedCode = "public class UnderTest{\n"
				+ "private int foo = 1;\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE, validationMessage);
	}

	@Test
	public void testChangeInConstructorSignatureTriggersValidation(){
		String originalCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String mutatedCode = "public class UnderTest{\n"
				+ "public UnderTest(int bar){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE, validationMessage);
	}

	@Test
	public void testChangeInPrivateMethodSignatureTriggersValidation(){
		String originalCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String mutatedCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int fooXXX){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE, validationMessage);
	}

	@Test
	public void testChangeInProtectedMethodSignatureTriggersValidation(){
		String originalCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String mutatedCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected int setBar(int bar){ return 0;}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE, validationMessage);
	}

	@Test
	public void testChangeInPublicMethodSignatureTriggersValidation(){
		String originalCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBib(){ return -1; }\n"
				+ "};";
		String mutatedCode = "public class UnderTest{\n"
				+ "public UnderTest(){}\n"
				+ "private void setFoo(int foo){}\n"
				+ "protected void setBar(int bar){}\n"
				+ "public int getBibXXX(){ return -1; }\n"
				+ "};";
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode, codeValidatorLevel);
		assertEquals(Constants.MUTANT_VALIDATION_METHOD_SIGNATURE_MESSAGE, validationMessage);
	}

	@Ignore
	@Test
	public void testInfiniteParserRecursionWithSingleTokens() throws IOException, CodeValidatorException {
		File tempClassFile = temporaryFolder.newFile();

		try (PrintWriter pw = new PrintWriter(tempClassFile)) {
			pw.println("public class XmlElementTest {");
			pw.println("	");
			pw.println("	@Test(timeout = 4000)");
			pw.println("	public void test() throws Throwable {");
			// This one is problematic
			pw.println("		XmlElement x = new XmlElement('Test');");
			pw.println("		assertNotNull(x.getData());");
			pw.println("	}");
			pw.println("}");
			//
			pw.flush();
		}
		assertTrue(validTestCode(tempClassFile.getAbsolutePath()));
	}

	@Test
	public void testMissingSemiColon() throws IOException, CodeValidatorException {
		File tempClassFile = temporaryFolder.newFile();

		try (PrintWriter pw = new PrintWriter(tempClassFile)) {
			pw.println("public class XmlElementTest {");
			pw.println("	");
			pw.println("	@Test(timeout = 4000)");
			pw.println("	public void test() throws Throwable {");
			pw.println("		XmlElement x = new XmlElement(\"Test\");");
			// This line misses a ';' so it should fail
			pw.println("		assertNotNull(x.getData())");
			pw.println("	}");
			pw.println("}");
			//
			pw.flush();
		}
		assertFalse(validTestCode(tempClassFile.getAbsolutePath()));
	}

	@Test
	public void testInvalidSuiteWithTwoClasses() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoClasses.java");
		assertFalse("Should be invalid; file contains a two classes", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidEmptyTest() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("EmptyTest.java");
		assertFalse(validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTwoTests() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoTests.java");
		assertFalse("Should be invalid; class contains two tests", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTestWithTooManyAssertions() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithTooManyAssertions.java");
		assertFalse("Should be invalid; test has too many assertions", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTestWithIf() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithIf.java");
		assertFalse("Should be invalid; test contains if statement", validTestCode(url.getPath()));
	}

	@Test
	public void testInvalidTestWithSystemCalls() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall.java");
		assertFalse("Should be invalid; test contains system call", validTestCode(url.getPath()));

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall2.java");
		assertFalse("Should be invalid; test contains system call", validTestCode(url.getPath()));

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall3.java");
		assertFalse("Should be invalid; test contains system call", validTestCode(url.getPath()));

	}

	@Test //TODO change this if CodeValidator is reverted to prohibiting new lines
	public void testValidMutantAddStmt() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/Lift/Lift.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/InvalidMutantLift1.java").toPath()),
				Charset.defaultCharset());
		assertTrue(validMutant(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testValidMutantInitializeString() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/XmlElement/XmlElement.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(
						new File("src/test/resources/itests/mutants/XmlElement/MutantXmlElement1.java").toPath()),
				Charset.defaultCharset());
		assertTrue(validMutant(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test //TODO change this if CodeValidator is reverted to prohibiting new lines
	public void testValidMutant() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/Lift/Lift.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/MutantLift1.java").toPath()),
				Charset.defaultCharset());
		System.out.println(getValidationMessage(originalCode, mutatedCode, codeValidatorLevel));
		assertTrue("Line added, mutant is valid", validMutant(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testValidTest() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("ValidTest.java");
		assertTrue("Should be valid", validTestCode(url.getPath()));
	}

	@Test
	public void testValidMutant1() {
		String orig = "int x = x + 0;";
		String mutant = "int x = x + 1;";
		assertTrue(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testValidMutant2() {
		String orig = "int x = x + 1;";
		String mutant = "int x = x - 1;";
		assertTrue(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test //TODO change this if CodeValidator is reverted to prohibiting multiple statements
	public void testInValidMutant3() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; x++;";
		assertTrue("Should be valid as mutant has multiple statements per line", validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutant1() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; if (x>0) {return false;}";
		assertFalse(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutant2() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; while (x>0) {return false;}";
		assertFalse(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantContainsIf() {
		String orig = "if (x >= 0) return 1; else return -x;";
		String mutant = "if (x >= 0) if (x >= 0) { return 1; } else return -x;";
		assertFalse(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantContainsSystemCall() {
		String orig = "if (x >= 0) return 1; else return -x;";
		String mutant = "if (x >= 0) { System.currentTimeMillis(); return x; } else return -x;";
		assertFalse(validMutant(orig, mutant, codeValidatorLevel));

		orig = "highestFloor = 10;";
		mutant = "highestFloor = java.util.Random().nextInt();";
		assertFalse(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator1() {
		String orig = "x = 1;";
		String mutant = "x = x == 0 ? 1 : 0;";
		assertFalse(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator2() {
		String orig = "currentFloor--;";
		String mutant = "currentFloor = currentFloor + currentFloor % 8 == 0 ? (-1) : 0;";
		assertFalse(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testValidWithString() {
		String orig = "if (!isHierachic(path))";
		String mutant = "if (!isHierachic(\"test.value\"))"; // raises
		// com.github.javaparser.TokenMgrException
		assertTrue(validMutant(orig, mutant, codeValidatorLevel));
	}

	@Ignore
	@Test
	public void testMultipleStatements() throws Exception {
		assertFalse(validMutant("mul();", "mul(); add();", codeValidatorLevel));
		assertFalse(validMutant("for (int i = 0; i <= 10; i ++) {", "mul(); for (int i = 0; i <= 10; i ++) {", codeValidatorLevel));
	}

	@Test
	public void testBitshifts() throws Exception {
		assertFalse(validMutant("r.num = r.num;", "r.num = r.num | ((r.num & (1 << 29)) << 1);", codeValidatorLevel));
		assertFalse(validMutant("r.num = r.num;", "r.num = r.num << 1+344;", codeValidatorLevel));
	}

	@Test
	public void testSignatureChange() throws Exception {
		assertFalse(validMutant("public class Rational  {", "public final class Rational  {", codeValidatorLevel));
		assertFalse(validMutant("class Rational  {", "public class Rational  {", codeValidatorLevel));
		assertFalse(validMutant("class Rational  {", "final class Rational  {", codeValidatorLevel));
		assertFalse(validMutant("public class Rational  {", "class Rational  {", codeValidatorLevel));
		assertFalse(validMutant("public class Rational  {", "protected class Rational  {", codeValidatorLevel));
		assertFalse(validMutant("final class Rational  {", "class Rational  {", codeValidatorLevel));

	}

	@Ignore //TODO figure out if this is worth the effort if it can't be achieved with line-by-line matching
	@Test
	public void testLiterals() throws Exception {
		assertTrue(validMutant("format(\"first\", \"second\", \"third\");", "format(\"\", \"sec\", \"third\");", codeValidatorLevel));
		assertTrue(validMutant("String s = \"\";", "String s = \" \";", codeValidatorLevel));
		assertTrue(validMutant("String s = \"stringval\";", "String s = \"stringval \";", codeValidatorLevel));
		for (String p : CodeValidator.PROHIBITED_BITWISE_OPERATORS) {
			assertTrue(p + " in a String should be valid",
					validMutant("String s = \"\";", "String s = \"" + p + "\";", codeValidatorLevel));
		}
		assertTrue(validMutant("String s = \"\";", "String s = \";?{} <<\";", codeValidatorLevel));
		assertTrue(validMutant("String s = \"\";", "String s = \"public final protected\";", codeValidatorLevel));
		assertTrue(validMutant("Char c = \'c\';", "Char c = \';\';", codeValidatorLevel));
	}

	@Test
	public void testComments() throws Exception {
		assertFalse("added single line comment", validMutant("String s = \"\";", "String s = \"\";// added comment", codeValidatorLevel));
		assertFalse("added single line comment in new line",
				validMutant("if(x > 0) \n\t return x;", "if(x > 1) \n\t return x; // comment", codeValidatorLevel));
        assertFalse("modified single line comment",
                validMutant("if(x > 0) \n\t return x; //x is positive",
						"if(x > 1) \n\t return x; //x is gt 1",
						codeValidatorLevel));
		assertTrue("modified code, single line comment unchanged",
				validMutant("String s = \"old\";// comment", "String s = \"new\";// comment", codeValidatorLevel));
		assertFalse("added multiline comment", validMutant("String s = \"\";", "String s = \"\"; /*added comment*/", codeValidatorLevel));
		assertTrue("changed code in new line after unchanged comment",
				validMutant("String test = \"\"; // comment\nfoo1", "String test = \"\"; // comment\nfoo2", codeValidatorLevel));
        assertFalse("modified comment in new line after unchanged comment", validMutant(
                "String s = \"\"//comment\nfoo; // comment\nfoo1", "String s = \"\"; //comment\nfoo//new comment",
				codeValidatorLevel));
	}

	@Test
	public void testModerateLevel() throws Exception {
		checkModerateRelaxations(CodeValidator.CodeValidatorLevel.STRICT);
		checkModerateRelaxations(CodeValidator.CodeValidatorLevel.MODERATE);
	}

	//bitshifts and signature changes are valid with a moderate validator
	public void checkModerateRelaxations(CodeValidator.CodeValidatorLevel level) {
		boolean isValid = !level.equals(CodeValidator.CodeValidatorLevel.STRICT);

		assertEquals(isValid, validMutant("public class Rational  {", "public final class Rational  {", level));
		assertEquals(isValid, validMutant("class Rational  {", "public class Rational  {", level));
		assertEquals(isValid, validMutant("class Rational  {", "final class Rational  {", level));
		assertEquals(isValid, validMutant("public class Rational  {", "class Rational  {", level));
		assertEquals(isValid, validMutant("public class Rational  {", "protected class Rational  {", level));
		assertEquals(isValid, validMutant("final class Rational  {", "class Rational  {", level));

		assertEquals(isValid, validMutant("r.num = r.num;", "r.num = r.num | ((r.num & (1 << 29)) << 1);", level));
		assertEquals(isValid, validMutant("r.num = r.num;", "r.num = r.num << 1+344;", level));
	}

	@Test
	public void testRelaxedLevel() throws Exception {
		checkModerateRelaxations(CodeValidator.CodeValidatorLevel.STRICT);
		checkModerateRelaxations(CodeValidator.CodeValidatorLevel.RELAXED);
		checkRelaxedRelaxations(CodeValidator.CodeValidatorLevel.MODERATE);
		checkRelaxedRelaxations(CodeValidator.CodeValidatorLevel.RELAXED);

	}

	// additional comments, additional logical operators, ternary operators, new control structures are valid with a relaxed validator
	private void checkRelaxedRelaxations(CodeValidator.CodeValidatorLevel level){
		// all the following mutants should be invalid unless the validator is relaxed
		boolean isValid = level.equals(CodeValidator.CodeValidatorLevel.RELAXED);

		assertEquals(isValid, validMutant("String s = \"\";", "String s = \"\";// added comment", level));
		assertEquals(isValid, validMutant("if(x > 0) \n\t return x;", "if(x > 1) \n\t return x; // comment", level));
		assertEquals(isValid, validMutant("String s = \"\";", "String s = \"\"; /*added comment*/", level));

		String orig = "x = 1;";
		String mutant = "x = x == 0 ? 1 : 0;";
		assertEquals(isValid, validMutant(orig, mutant, level));
		orig = "currentFloor--;";
		mutant = "currentFloor = currentFloor + currentFloor % 8 == 0 ? (-1) : 0;";
		assertEquals(isValid, validMutant(orig, mutant, level));

		orig = "if (x >= 0) return 1; else return -x;";
		mutant = "if (x >= 0) if (x >= 0) { return 1; } else return -x;";
		assertEquals(isValid, validMutant(orig, mutant, level));

		orig = "int x = 0;";
		mutant = "int x = 0; if (x>0) {return false;}";
		assertEquals(isValid, validMutant(orig, mutant, level));

		orig = "int x = 0;";
		mutant = "int x = 0; while (x>0) {return false;}";
		assertEquals(isValid, validMutant(orig, mutant, level));
	}

	@Test
	public void testInvalidMutantWithLogicalOps() {
		assertTrue(validMutant("if (numRiders + numEntering <= capacity) {",
				"if (numRiders + numEntering <= capacity && false) {",
				CodeValidator.CodeValidatorLevel.RELAXED));
		assertFalse(validMutant("if (numRiders + numEntering <= capacity) {",
				"if (numRiders + numEntering <= capacity && false) {",
				CodeValidator.CodeValidatorLevel.MODERATE));
		assertFalse(validMutant("if (numRiders + numEntering <= capacity) {",
				"if (numRiders + numEntering <= capacity && false) {",
				CodeValidator.CodeValidatorLevel.STRICT));
	}

	@Test
	public void testInvalidMutantWithChangedAccess(){
		assertFalse(validMutant("public void test() {", "void test() {", codeValidatorLevel));
		assertFalse(validMutant("public void test() {", "protected void test() {", codeValidatorLevel));
		assertFalse(validMutant("public void test() {", "private void test() {", codeValidatorLevel));
	}


	@Test
	public void testInvalidMutantWithLogicalOps2() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/Lift/Lift.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/InvalidMutantLift2.java").toPath()),
				Charset.defaultCharset());
		assertTrue(validMutant(originalCode, mutatedCode, CodeValidatorLevel.RELAXED));
		assertFalse(validMutant(originalCode, mutatedCode, CodeValidatorLevel.MODERATE));
		assertFalse(validMutant(originalCode, mutatedCode, CodeValidatorLevel.STRICT));
	}
}

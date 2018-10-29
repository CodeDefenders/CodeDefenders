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
package org.codedefenders;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorException;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS;
import static org.codedefenders.validation.code.CodeValidator.validateMutantGetMessage;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_IDENTICAL;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_METHOD_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseAccess.class})
public class CodeValidatorTest {

	private CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.STRICT;

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

		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testSameStringsShouldTriggerValidation() throws IOException {
		String code = "@Override\n" +
				"  public final String toString() {\n" +
				"    StringBuilder buffer = new StringBuilder();\n" +
				"    buffer.append(\"Document<\");\n" +
				"    for (int i = 0; i < fields.size(); i++) {\n" +
				"      Fieldable field = fields.get(i);\n" +
				"      buffer.append(field.toString());\n" +
				"      if (i != fields.size()-1)\n" +
				"        buffer.append(\" \");\n" +
				"    }\n" +
				"    buffer.append(\">\");\n" +
				"    return buffer.toString();\n" +
				"  }";

		assertEquals(MUTANT_VALIDATION_IDENTICAL, validateMutantGetMessage(code, code, codeValidatorLevel));
	}

	@Test
	public void testSpacesNotInStringsShouldTriggerValidation() throws IOException {
		String code = "@Override\n" +
				"  public final String toString() {\n" +
				"    StringBuilder buffer = new StringBuilder();\n" +
				"    buffer.append(\"Document<\");\n" +
				"    for (int i = 0; i < fields.size(); i++) {\n" +
				"      Fieldable field = fields.get(i);\n" +
				"      buffer.append(field.toString());\n" +
				"      if (i != fields.size()-1)\n" +
				"        buffer.append(\" \");\n" +
				"    }\n" +
				"    buffer.append(\">\");\n" +
				"    return buffer.toString();\n" +
				"  }";

		String mutatedCode = "@Override\n" +
				"  public final String toString() {\n" +
				"    StringBuilder buffer = new StringBuilder();\n" +
				"    buffer.append(\"Document<\");    \n" + // Add random spaces here
				"    for (int i = 0; i < fields.size(); i++) {\n" +
				"      Fieldable field = fields.get(i);\n" +
				"      buffer.append(field.toString());\n" +
				"      if (i != fields.size()-1)\n" +
				"        buffer.append(\" \");\n" +
				"    }\n" +
				"    buffer.append(\">\");\n" +
				"    return buffer.toString();\n" +
				"  }";

		assertEquals(MUTANT_VALIDATION_IDENTICAL, validateMutantGetMessage(code, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testAddSpaceToStringsShouldNotTriggerValidation() throws IOException {
		String originalCode = "@Override\n" +
				"  public final String toString() {\n" +
				"    StringBuilder buffer = new StringBuilder();\n" +
				"    buffer.append(\"Document<\");\n" +
				"    for (int i = 0; i < fields.size(); i++) {\n" +
				"      Fieldable field = fields.get(i);\n" +
				"      buffer.append(field.toString());\n" +
				"      if (i != fields.size()-1)\n" +
				"        buffer.append(\" \");\n" +
				"    }\n" +
				"    buffer.append(\">\");\n" +
				"    return buffer.toString();\n" +
				"  }";

		String mutatedCode = "@Override\n" +
				"  public final String toString() {\n" +
				"    StringBuilder buffer = new StringBuilder();\n" +
				// This is the mutated line !
				"    buffer.append(\"Document< \");\n" +
				//
				"    for (int i = 0; i < fields.size(); i++) {\n" +
				"      Fieldable field = fields.get(i);\n" +
				"      buffer.append(field.toString());\n" +
				"      if (i != fields.size()-1)\n" +
				"        buffer.append(\" \");\n" +
				"    }\n" +
				"    buffer.append(\">\");\n" +
				"    return buffer.toString();\n" +
				"  }";

		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));

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
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
		assertEquals(MUTANT_VALIDATION_METHOD_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
		assertEquals(MUTANT_VALIDATION_METHOD_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
		assertEquals(MUTANT_VALIDATION_METHOD_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
		assertEquals(MUTANT_VALIDATION_METHOD_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
		assertEquals(MUTANT_VALIDATION_METHOD_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Ignore
	@Test
	public void testInfiniteParserRecursionWithSingleTokens() throws IOException, CodeValidatorException {
		final String code = String.join("\n",
				"public class XmlElementTest {",
				"	",
				"	@Test(timeout = 4000)",
				"	public void test() throws Throwable {",
				// This one is problematic
				"		XmlElement x = new XmlElement('Test');",
				"		assertNotNull(x.getData());",
				"	}",
				"}");
		assertTrue(CodeValidator.validateTestCode(code, CodeValidator.DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testMissingSemiColon() throws IOException, CodeValidatorException {
		String code = String.join("\n",
				"public class XmlElementTest {",
				"	",
				"	@Test(timeout = 4000)",
				"	public void test() throws Throwable {",
				"		XmlElement x = new XmlElement(\"Test\");",
				// This line misses a ';' so it should fail
				"		assertNotNull(x.getData())",
				"	}",
				"}"
		);
		assertFalse(CodeValidator.validateTestCode(code, CodeValidator.DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testInvalidSuiteWithTwoClasses() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoClasses.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; file contains a two classes", CodeValidator.validateTestCode(code, DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testInvalidEmptyTest() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("EmptyTest.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse(CodeValidator.validateTestCode(code, DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testInvalidTwoTests() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoTests.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; class contains two tests", CodeValidator.validateTestCode(code, DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testInvalidTestWithTooManyAssertions() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithTooManyAssertions.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test has too many assertions", CodeValidator.validateTestCode(code, DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testInvalidTestWithIf() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithIf.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains if statement", CodeValidator.validateTestCode(code, DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testInvalidTestWithSystemCalls() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall.java");
		final String code1 = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains system call", CodeValidator.validateTestCode(code1, DEFAULT_NB_ASSERTIONS));

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall2.java");
		final String code2 = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains system call", CodeValidator.validateTestCode(code2, DEFAULT_NB_ASSERTIONS));

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall3.java");
		final String code3 = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains system call", CodeValidator.validateTestCode(code3, DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testValidMutantAddStmt() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/Lift/Lift.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/InvalidMutantLift1.java").toPath()),
				Charset.defaultCharset());
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testValidMutant() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/Lift/Lift.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/MutantLift1.java").toPath()),
				Charset.defaultCharset());
		assertEquals("Line added, mutant is valid", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testValidTest() throws IOException, CodeValidatorException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("ValidTest.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertTrue("Should be valid", CodeValidator.validateTestCode(code, DEFAULT_NB_ASSERTIONS));
	}

	@Test
	public void testValidMutant1() {
		String orig = "int x = x + 0;";
		String mutant = "int x = x + 1;";
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testValidMutant2() {
		String orig = "int x = x + 1;";
		String mutant = "int x = x - 1;";
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInValidMutant3() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; x++;";
		assertEquals("Should be valid as mutant has multiple statements per line", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutant1() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; if (x>0) {return false;}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutant2() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; while (x>0) {return false;}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantContainsIf() {
		String orig = "if (x >= 0) return 1; else return -x;";
		String mutant = "if (x >= 0) if (x >= 0) { return 1; } else return -x;";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantContainsSystemCall() {
		String orig = "if (x >= 0) return 1; else return -x;";
		String mutant = "if (x >= 0) { System.currentTimeMillis(); return x; } else return -x;";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));

		orig = "highestFloor = 10;";
		mutant = "highestFloor = java.util.Random().nextInt();";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator1() {
		String orig = "x = 1;";
		String mutant = "x = x == 0 ? 1 : 0;";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator2() {
		String orig = "currentFloor--;";
		String mutant = "currentFloor = currentFloor + currentFloor % 8 == 0 ? (-1) : 0;";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testValidWithString() {
		String orig = "if (!isHierachic(path))";
		String mutant = "if (!isHierachic(\"test.value\"))"; // raises
		// com.github.javaparser.TokenMgrException
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Ignore
	@Test
	public void testMultipleStatements() throws Exception {
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("mul();", "mul(); add();", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("for (int i = 0; i <= 10; i ++) {", "mul(); for (int i = 0; i <= 10; i ++) {", codeValidatorLevel));
	}

	@Test
	public void testBitshifts() throws Exception {
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("r.num = r.num;", "r.num = r.num | ((r.num & (1 << 29)) << 1);", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("r.num = r.num;", "r.num = r.num << 1+344;", codeValidatorLevel));
	}

	@Test
	public void testClassSignatureChange() throws Exception {
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "public final class Rational  {", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("class Rational  {", "public class Rational  {", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("class Rational  {", "final class Rational  {", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "class Rational  {", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "protected class Rational  {", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("final class Rational  {", "class Rational  {", codeValidatorLevel));
	}

	@Test
	public void testLiterals() throws Exception {
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("format(\"first\", \"second\", \"third\");", "format(\"\", \"sec\", \"third\");", codeValidatorLevel));
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \" \";", codeValidatorLevel));
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"stringval\";", "String s = \"stringval \";", codeValidatorLevel));
		for (String p : CodeValidator.PROHIBITED_BITWISE_OPERATORS) {
			assertEquals(p + " in a String should be valid", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"" + p + "\";", codeValidatorLevel));
		}
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \";?{} <<\";", codeValidatorLevel));
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"public final protected\";", codeValidatorLevel));
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("Char c = \'c\';", "Char c = \';\';", codeValidatorLevel));
	}

	@Test
	public void testComments() throws Exception {
		assertNotEquals("added single line comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"\";// added comment", codeValidatorLevel));
		assertNotEquals("added single line comment in new line", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("if(x > 0) \n\t return x;", "if(x > 1) \n\t return x; // comment", codeValidatorLevel));
		assertNotEquals("modified single line comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("if(x > 0) \n\t return x; //x is positive", "if(x > 1) \n\t return x; //x is gt 1", codeValidatorLevel));
		assertEquals("modified code, single line comment unchanged", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"old\";// comment", "String s = \"new\";// comment", codeValidatorLevel));
		assertNotEquals("added multiline comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"\"; /*added comment*/", codeValidatorLevel));
		assertEquals("changed code in new line after unchanged comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String test = \"\"; // comment\nfoo1", "String test = \"\"; // comment\nfoo2", codeValidatorLevel));
		assertNotEquals("modified comment in new line after unchanged comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\"//comment\nfoo; // comment\nfoo1", "String s = \"\"; //comment\nfoo//new comment", codeValidatorLevel));
	}

	@Test
	public void testModerateLevel() throws Exception {
		checkModerateRelaxations(CodeValidatorLevel.STRICT);
		checkModerateRelaxations(CodeValidatorLevel.MODERATE);
	}

	//bitshifts and signature changes are valid with a moderate validator
	public void checkModerateRelaxations(CodeValidatorLevel level) {
		boolean isValid = !level.equals(CodeValidatorLevel.STRICT);
		if (isValid) {
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "public final class Rational  {", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("class Rational  {", "public class Rational  {", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("class Rational  {", "final class Rational  {", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "class Rational  {", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "protected class Rational  {", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("final class Rational  {", "class Rational  {", level));

			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("r.num = r.num;", "r.num = r.num | ((r.num & (1 << 29)) << 1);", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("r.num = r.num;", "r.num = r.num << 1+344;", level));
		} else {
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "public final class Rational  {", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("class Rational  {", "public class Rational  {", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("class Rational  {", "final class Rational  {", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "class Rational  {", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  {", "protected class Rational  {", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("final class Rational  {", "class Rational  {", level));

			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("r.num = r.num;", "r.num = r.num | ((r.num & (1 << 29)) << 1);", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("r.num = r.num;", "r.num = r.num << 1+344;", level));
		}
	}

	@Test
	public void testRelaxedLevel() throws Exception {
		checkModerateRelaxations(CodeValidatorLevel.STRICT);
		checkModerateRelaxations(CodeValidatorLevel.RELAXED);
		checkRelaxedRelaxations(CodeValidatorLevel.MODERATE);
		checkRelaxedRelaxations(CodeValidatorLevel.RELAXED);

	}

	// additional comments, additional logical operators, ternary operators, new control structures are valid with a relaxed validator
	private void checkRelaxedRelaxations(CodeValidatorLevel level){
		// all the following mutants should be invalid unless the validator is relaxed
		boolean isValid = level.equals(CodeValidatorLevel.RELAXED);
		if (isValid) {
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"\";// added comment", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("if(x > 0) \n\t return x;", "if(x > 1) \n\t return x; // comment", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"\"; /*added comment*/", level));

			String orig = "x = 1;";
			String mutant = "x = x == 0 ? 1 : 0;";
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));
			orig = "currentFloor--;";
			mutant = "currentFloor = currentFloor + currentFloor % 8 == 0 ? (-1) : 0;";
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));

			orig = "if (x >= 0) return 1; else return -x;";
			mutant = "if (x >= 0) if (x >= 0) { return 1; } else return -x;";
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));

			orig = "int x = 0;";
			mutant = "int x = 0; if (x>0) {return false;}";
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));

			orig = "int x = 0;";
			mutant = "int x = 0; while (x>0) {return false;}";
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));
		} else {
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"\";// added comment", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("if(x > 0) \n\t return x;", "if(x > 1) \n\t return x; // comment", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("String s = \"\";", "String s = \"\"; /*added comment*/", level));

            String orig = "x = 1;";
            String mutant = "x = x == 0 ? 1 : 0;";
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));
            orig = "currentFloor--;";
            mutant = "currentFloor = currentFloor + currentFloor % 8 == 0 ? (-1) : 0;";
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));

            orig = "if (x >= 0) return 1; else return -x;";
            mutant = "if (x >= 0) if (x >= 0) { return 1; } else return -x;";
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));

            orig = "int x = 0;";
            mutant = "int x = 0; if (x>0) {return false;}";
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));

            orig = "int x = 0;";
            mutant = "int x = 0; while (x>0) {return false;}";
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, level));
        }
	}

	@Test
	public void testInvalidMutantWithLogicalOps() {
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("if (numRiders + numEntering <= capacity) {", "if (numRiders + numEntering <= capacity && false) {", CodeValidatorLevel.RELAXED));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("if (numRiders + numEntering <= capacity) {", "if (numRiders + numEntering <= capacity && false) {", CodeValidatorLevel.MODERATE));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("if (numRiders + numEntering <= capacity) {", "if (numRiders + numEntering <= capacity && false) {", CodeValidatorLevel.STRICT));
	}

	@Test
	public void testInvalidMutantWithChangedAccess(){
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public void test() {", "void test() {", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public void test() {", "protected void test() {", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public void test() {", "private void test() {", codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantWithLogicalOps2() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/Lift/Lift.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/InvalidMutantLift2.java").toPath()),
				Charset.defaultCharset());
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, CodeValidatorLevel.RELAXED));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, CodeValidatorLevel.MODERATE));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, CodeValidatorLevel.STRICT));
	}
}

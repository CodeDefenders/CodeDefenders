package org.codedefenders;

import static org.codedefenders.validation.CodeValidator.validMutant;
import static org.codedefenders.validation.CodeValidator.validTestCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.codedefenders.exceptions.CodeValidatorException;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.validation.CodeValidator;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Jose Rojas
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseAccess.class})
public class CodeValidatorTest {

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

		String validityMessage = GameManager.getMutantValidityMessage(1, mutatedCode);

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
		
		String validityMessage = GameManager.getMutantValidityMessage(1, originalCode);
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
		
		String validityMessage = GameManager.getMutantValidityMessage(1, mutatedCode);
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
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode);
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
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode);
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
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode);
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
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode);
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
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode);
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
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode);
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
		String validationMessage = CodeValidator.getValidationMessage(originalCode, mutatedCode);
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
		assertTrue(validMutant(originalCode, mutatedCode));
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
		assertTrue(validMutant(originalCode, mutatedCode));
	}

	@Test //TODO change this if CodeValidator is reverted to prohibiting new lines
	public void testValidMutant() throws IOException {
		String originalCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/sources/Lift/Lift.java").toPath()),
				Charset.defaultCharset());
		String mutatedCode = new String(
				Files.readAllBytes(new File("src/test/resources/itests/mutants/Lift/MutantLift1.java").toPath()),
				Charset.defaultCharset());
		assertTrue("Line added, mutant is valid", validMutant(originalCode, mutatedCode));
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
		assertTrue(validMutant(orig, mutant));
	}

	@Test
	public void testValidMutant2() {
		String orig = "int x = x + 1;";
		String mutant = "int x = x - 1;";
		assertTrue(validMutant(orig, mutant));
	}

	@Test //TODO change this if CodeValidator is reverted to prohibiting multiple statements
	public void testInValidMutant3() {
		String orig = "int x = 0;";
		String mutant = "int x = 0; x++;";
		assertTrue("Should be valid as mutant has multiple statements per line", validMutant(orig, mutant));
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
	public void testInvalidMutantContainsIf() {
		String orig = "if (x >= 0) return 1; else return -x;";
		String mutant = "if (x >= 0) if (x >= 0) { return 1; } else return -x;";
		assertFalse(validMutant(orig, mutant));
	}

	@Test
	public void testInvalidMutantContainsSystemCall() {
		String orig = "if (x >= 0) return 1; else return -x;";
		String mutant = "if (x >= 0) { System.currentTimeMillis(); return x; } else return -x;";
		assertFalse(validMutant(orig, mutant));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator1() {
		String orig = "x = 1;";
		String mutant = "x = x == 0 ? 1 : 0;";
		assertFalse(validMutant(orig, mutant));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator2() {
		String orig = "currentFloor--;";
		String mutant = "currentFloor = currentFloor + currentFloor % 8 == 0 ? (-1) : 0;";
		assertFalse(validMutant(orig, mutant));
	}

	@Test
	public void testValidWithString() {
		String orig = "if (!isHierachic(path))";
		String mutant = "if (!isHierachic(\"test.value\"))"; // raises
		// com.github.javaparser.TokenMgrException
		assertTrue(validMutant(orig, mutant));
	}

	@Ignore
	@Test
	public void testMultipleStatements() throws Exception {
		assertFalse(validMutant("mul();", "mul(); add();"));
		assertFalse(validMutant("for (int i = 0; i <= 10; i ++) {", "mul(); for (int i = 0; i <= 10; i ++) {"));
	}

	@Test
	public void testBitshifts() throws Exception {
		assertFalse(validMutant("r.num = r.num;", "r.num = r.num | ((r.num & (1 << 29)) << 1);"));
		assertFalse(validMutant("r.num = r.num;", "r.num = r.num << 1+344;"));
	}

	@Test
	public void testSignatureChange() throws Exception {
		assertFalse(validMutant("public class Rational  {", "public final class Rational  {"));
		assertFalse(validMutant("class Rational  {", "public class Rational  {"));
		assertFalse(validMutant("class Rational  {", "final class Rational  {"));
		assertFalse(validMutant("public class Rational  {", "class Rational  {"));
		assertFalse(validMutant("public class Rational  {", "protected class Rational  {"));
		assertFalse(validMutant("final class Rational  {", "class Rational  {"));

	}

	@Ignore //TODO figure out if this is worth the effort if it can't be achieved with line-by-line matching
	@Test
	public void testLiterals() throws Exception {
		assertTrue(validMutant("format(\"first\", \"second\", \"third\");", "format(\"\", \"sec\", \"third\");"));
		assertTrue(validMutant("String s = \"\";", "String s = \" \";"));
		assertTrue(validMutant("String s = \"stringval\";", "String s = \"stringval \";"));
		for (String p : CodeValidator.PROHIBITED_OPERATORS) {
			assertTrue(p + " in a String should be valid",
					validMutant("String s = \"\";", "String s = \"" + p + "\";"));
		}
		assertTrue(validMutant("String s = \"\";", "String s = \";?{} <<\";"));
		assertTrue(validMutant("String s = \"\";", "String s = \"public final protected\";"));
		assertTrue(validMutant("Char c = \'c\';", "Char c = \';\';"));
	}

	@Test
	public void testComments() throws Exception {
		assertFalse("added single line comment", validMutant("String s = \"\";", "String s = \"\";// added comment"));
		assertFalse("added single line comment in new line",
				validMutant("if(x > 0) \n\t return x;", "if(x > 1) \n\t return x; // comment"));
		//TODO check if there is a way to prohibit this without being to restrictive
        /*assertFalse("modified single line comment",
                validMutant("if(x > 0) \n\t return x; //x is positive", "if(x > 1) \n\t return x; //x is gt 1"));*/
		assertTrue("modified code, single line comment unchanged",
				validMutant("String s = \"old\";// comment", "String s = \"new\";// comment"));
		assertFalse("added multiline comment", validMutant("String s = \"\";", "String s = \"\"; /*added comment*/"));
		assertTrue("changed code in new line after unchanged comment",
				validMutant("String test = \"\"; // comment\nfoo1", "String test = \"\"; // comment\nfoo2"));
        /*assertFalse("modified comment in new line after unchanged comment", validMutant(
                "String s = \"\"//comment\nfoo; // comment\nfoo1", "String s = \"\"; //comment\nfoo//new comment"));*/
	}
}

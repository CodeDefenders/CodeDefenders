/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS;
import static org.codedefenders.validation.code.CodeValidator.getMD5FromText;
import static org.codedefenders.validation.code.CodeValidator.validateMutantGetMessage;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_CLASS_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_COMMENT;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_IDENTICAL;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_LOGIC_INSTANCEOF;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_METHOD_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_PACKAGE_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CodeValidatorTest {

	private static final CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.STRICT;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
    public void changeInClassSignatureShouldTriggerValidation() {
        String originalCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + " public final class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.RELAXED;

        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
    }

	@Test
    public void noChangeInClassSignatureShouldNotTriggerValidation() {
        String originalCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.RELAXED;

        assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
    }

	// TODO Not a good test with so many assert. This is
	@Test
	public void testVariousChangesToClassSignatureRelaxed(){
	    CodeValidatorLevel level = CodeValidatorLevel.RELAXED;
	    assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Rational  {}", "public final class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("class Rational  {}", "public class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("class Rational  {}", "final class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Rational  {}", "class Rational  {}", level));
	    // This is wrong as protected is not allowed in that position
        // assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Rational  {}", "protected class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("final class Rational  {}", "class Rational  {}", level));
        //
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Outer { public class Rational  {}}", "public class Outer { protected class Rational  {}}", level));
	}

	@Test
    public void testVariousChangesToClassSignatureModerate(){
        CodeValidatorLevel level = CodeValidatorLevel.MODERATE;
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Rational  {}", "public final class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("class Rational  {}", "public class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("class Rational  {}", "final class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Rational  {}", "class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("final class Rational  {}", "class Rational  {}", level));
        //
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Outer { public class Rational  {}}", "public class Outer { protected class Rational  {}}", level));
    }

	@Test
    public void testVariousChangesToClassSignatureStrict(){
        CodeValidatorLevel level = CodeValidatorLevel.STRICT;
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Rational  {}", "public final class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("class Rational  {}", "public class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("class Rational  {}", "final class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Rational  {}", "class Rational  {}", level));
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("final class Rational  {}", "class Rational  {}", level));
        //
        assertEquals(MUTANT_VALIDATION_CLASS_SIGNATURE, validateMutantGetMessage("public class Outer { public class Rational  {}}", "public class Outer { protected class Rational  {}}", level));
    }


    @Test
    public void changeToPackageShouldTriggerValidation() {
        String originalCode = ""
                + "package theoriginalpackage;"+ "\n"
                + "\n"
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + "package anotherpackage;"+ "\n"
                + "\n"
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.RELAXED;

        assertEquals(MUTANT_VALIDATION_PACKAGE_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
    }

    @Test
    public void changeToEmptyPackageShouldTriggerValidation() {
        String originalCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + "package anotherpackage;"+ "\n"
                + "\n"
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.RELAXED;

        assertEquals(MUTANT_VALIDATION_PACKAGE_SIGNATURE, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
    }

    @Test
    public void samePackageDeclarationShouldNotTriggerValidation() {
        String originalCode = ""
                + "package theoriginalpackage;"+ "\n"
                + "\n"
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + "package theoriginalpackage;"+ "\n"
                + "\n"
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.RELAXED;

        assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
    }

    @Test
    public void sameEmptyPackageDeclarationShouldNotTriggerValidation() {
        String originalCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.RELAXED;

        assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
    }

	@Test
    public void mutantChangeInstanceofUsingStrictCheckingTriggerValidation(){
        String originalCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.STRICT;

        assertEquals(MUTANT_VALIDATION_LOGIC_INSTANCEOF, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
    }

	@Test
    public void mutantMultipleChangesInstanceofUsingStrictCheckingTriggerValidation(){
        String originalCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int c = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int c = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int c = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.STRICT;
        ValidationMessage actualMessage = validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel);
        assertEquals(MUTANT_VALIDATION_LOGIC_INSTANCEOF, actualMessage);
    }

	@Test
    public void mutantNoChangeInstanceofUsingStrictCheckingShouldNotTriggerValidation(){
        String originalCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int b = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        String mutatedCode = ""
                + " public class Test{"+ "\n"
                + "  public void pow() { " + "\n"
                + "   Integer a = new Integer(3);"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int c = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Object ){"+ "\n"
                + "      int c = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + "   if( a instanceof Number ){"+ "\n"
                + "      int c = a.intValue();"+ "\n"
                + "   }"+ "\n"
                + " }"+ "\n"
                + "}";

        CodeValidatorLevel codeValidatorLevel = CodeValidatorLevel.STRICT;
        ValidationMessage actualMessage = validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel);
        assertEquals(MUTANT_VALIDATION_SUCCESS, actualMessage);
    }



	@Test
	public void mutantAfterSlashShouldNotTriggerValidation(){
		String originalCode = ""
				+ "/**" + "\n"
				+ "* Some Java Doc" + "\n"
				+ "**/" + "\n"
				+ 	" public class \n\t Test{"+ "\n"
				+ "/*" + "\n"
				+ "* Some Comment" + "\n"
				+ "*/" + "\n"
				+ "public Complex pow(Complex p) { " + "\n"
		        +"double a = real, b = imag, c = p.real, d = p.imag;" + "\n"
		        +"double ns = a * a + b * b;" + "\n"
		        +"double dArg = d / 2;" + "\n"
		        +"double cArg = c * arg();" + "\n"
		        +"double dDenom = Math.pow(Math.E, d * arg());" + "\n"
		        +"double newReal = Math.pow(ns, c / 2) / dDenom * (Math.cos(dArg) * Math.cos(cArg) * Math.log(ns) - Math.sin(dArg) * Math.sin(cArg) * Math.log(ns));" + "\n"
		        +"double newImag = Math.pow(ns, c / 2) / dDenom * (Math.cos(dArg) * Math.sin(cArg) * Math.log(ns) + Math.sin(dArg) * Math.cos(cArg) * Math.log(ns));" + "\n"
		        +"return new Complex(newReal, newImag);" + "\n"
		        + "}"
		        + "}";

		String mutatedCode = ""
				+ "/**" + "\n"
				+ "* Some Java Doc" + "\n"
				+ "**/" + "\n"
				+ 	" public class Test{"+ "\n"
				+ "/*" + "\n"
				+ "* Some Comment" + "\n"
				+ "*/" + "\n"
				+ "public Complex pow(Complex p) { " + "\n"
		        +"double a = real, b = imag, c = p.real, d = p.imag;" + "\n"
		        +"double ns = a * a + b * b;" + "\n"
		        +"double dArg = d / 5;" + "\n" // Here's the mutation
		        +"double cArg = c * arg();" + "\n"
		        +"double dDenom = Math.pow(Math.E, d * arg());" + "\n"
		        +"double newReal = Math.pow(ns, c / 2) / dDenom * (Math.cos(dArg) * Math.cos(cArg) * Math.log(ns) - Math.sin(dArg) * Math.sin(cArg) * Math.log(ns));" + "\n"
		        +"double newImag = Math.pow(ns, c / 2) / dDenom * (Math.cos(dArg) * Math.sin(cArg) * Math.log(ns) + Math.sin(dArg) * Math.cos(cArg) * Math.log(ns));" + "\n"
		        +"return new Complex(newReal, newImag);" + "\n"
		        + "}"
		        + "}";

		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));

		assertNotEquals("The MD5 is the same", getMD5FromText(originalCode), getMD5FromText(mutatedCode));
	}

	@Test
	public void testMakingStringLiteralsDoesNotTriggersValidation() throws IOException {
		String originalCode = "" + "\n" +
				" public class Test{"+ "\n" +
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
				"	}" + "\n" +
				"}";

		String mutatedCode = "" + "\n" +
				" public class Test{"+ "\n" +
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
				"	}" + "\n" +
				"}";

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
	public void testAddingNewEmptyLinesOrLineBreakingShouldTriggerValidation() throws IOException {
		String originalCode = "public class Test{\n" +
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
				"  }"
				+ "}";

		String mutatedCode = "public class "
				+ "\n\t" // This is the modification: THE TAB \t is the trigger ... adding simply \n wont show the problem
				+ "Test{\n" +
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
				"  }"
				+ "}";

		assertEquals(MUTANT_VALIDATION_IDENTICAL, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
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
	public void testInfiniteParserRecursionWithSingleTokens() throws IOException {
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
		assertTrue(CodeValidator.validateTestCodeGetMessage(code, CodeValidator.DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testCompileErrorsShouldNotBeCatchedByValidator() throws IOException {
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
		assertTrue(CodeValidator.validateTestCodeGetMessage(code, CodeValidator.DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testInvalidSuiteWithTwoClasses() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoClasses.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; file contains a two classes", CodeValidator.validateTestCodeGetMessage(code, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testInvalidEmptyTest() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("EmptyTest.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse(CodeValidator.validateTestCodeGetMessage(code, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testInvalidTwoTests() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TwoTests.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; class contains two tests", CodeValidator.validateTestCodeGetMessage(code, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testInvalidTestWithTooManyAssertions() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithTooManyAssertions.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test has too many assertions", CodeValidator.validateTestCodeGetMessage(code, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testInvalidTestWithIf() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithIf.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains if statement", CodeValidator.validateTestCodeGetMessage(code, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testInvalidTestWithSystemCalls() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall.java");
		final String code1 = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains system call", CodeValidator.validateTestCodeGetMessage(code1, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall2.java");
		final String code2 = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains system call", CodeValidator.validateTestCodeGetMessage(code2, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());

		url = Thread.currentThread().getContextClassLoader().getResource("TestWithSystemCall3.java");
		final String code3 = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertFalse("Should be invalid; test contains system call", CodeValidator.validateTestCodeGetMessage(code3, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
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
	public void testValidTest() throws IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("ValidTest.java");
		final String code = new String(Files.readAllBytes(Paths.get(url.getPath())));
		assertTrue("Should be valid", CodeValidator.validateTestCodeGetMessage(code, DEFAULT_NB_ASSERTIONS, CodeValidator.DEFAULT_ASSERTION_LIBRARY).isEmpty());
	}

	@Test
	public void testValidMutant1() {
		String originalCode = "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = x + 0;" + "\n"
				+ "}"
				+ "}";
		String mutatedCode = "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = x + 1;" + "\n" // Line changed
				+ "}"
				+ "}";
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testValidMutant2() {
		String originalCode = "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = x + 1;" + "\n"
				+ "}"
				+ "}";
		String mutatedCode = "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = x - 1;" + "\n" // Line changed
				+ "}"
				+ "}";
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testInValidMutant3() {
		String originalCode = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = 0;" + "\n"
				+ "}"
				+ "}";
		String mutatedCode = "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = 0; x++;" + "\n" // Line changed
				+ "}"
				+ "}";

		assertEquals("Should be valid as mutant has multiple statements per line", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutant1() {
		String orig = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = 0;" + "\n"
				+ "}"
				+ "}";

		String mutant = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = 0; if (x>0) {return false;}"+ "\n"
				+ "}"
				+ "}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutant2() {
		String orig = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = 0;"+ "\n"
				+ "}"
				+ "}";
		String mutant = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "int x = 0; while (x>0) {return false;}"+ "\n"
				+ "}"
				+ "}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantContainsIf() {
		String orig = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "if (x >= 0) return 1; else return -x;"+ "\n"
				+ "}"
				+ "}";
		String mutant = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "if (x >= 0) if (x >= 0) { return 1; } else return -x;"+ "\n"
				+ "}"
				+ "}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantContainsSystemCall() {
		String orig = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "if (x >= 0) return 1; else return -x;"+ "\n"
				+ "}"
				+ "}";
		String mutant = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "if (x >= 0) { System.currentTimeMillis(); return x; } else return -x;"+ "\n"
				+ "}"
				+ "}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));

		orig = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "highestFloor = 10;"+ "\n"
				+ "}"
				+ "}";
		mutant = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "highestFloor = java.util.Random().nextInt();"+ "\n"
				+ "}"
				+ "}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator1() {
		String orig = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "x = 1;"+ "\n"
				+ "}"
				+ "}";
		String mutant = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "x = x == 0 ? 1 : 0;"+ "\n"
				+ "}"
				+ "}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testInvalidMutantWithTernaryOperator2() {
		String orig = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "currentFloor--;"+ "\n"
				+ "}"
				+ "}";
		String mutant = ""
				+ "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "currentFloor = currentFloor + currentFloor % 8 == 0 ? (-1) : 0;"+ "\n"
				+ "}"
				+ "}";
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(orig, mutant, codeValidatorLevel));
	}

	@Test
	public void testValidWithString() {
		String originalCode = "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "if (!isHierachic(path)) { return; }" + "\n"
				+ "}"
				+ "}";
		String mutatedCode = "public class Test{" + "\n"
				+ " public void test(){" + "\n"
				+ "if (!isHierachic(\"test.value\")) { return; }" + "\n" + "\n" // Line changed
				+ "}"
				+ "}";
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));
	}

	@Ignore
	@Test
	public void testMultipleStatements() throws Exception {
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("mul();", "mul(); add();", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("for (int i = 0; i <= 10; i ++) {", "mul(); for (int i = 0; i <= 10; i ++) {", codeValidatorLevel));
	}

	@Test
	public void testBitshifts() throws Exception {
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(
				"public class Test { public test(){ r.num = r.num;}}",
				"public class Test { public test(){ r.num = r.num | ((r.num & (1 << 29)) << 1);}} ", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(
				"public class Test { public test(){ r.num = r.num;}}",
				"public class Test { public test(){ r.num = r.num << 1+344;}}", codeValidatorLevel));
	}

	// Test smell too many assertions
	@Test
	public void testClassSignatureChange() throws Exception {
		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						"public class Rational  {}",
						"public final class Rational  {}", codeValidatorLevel));

		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						"class Rational  {}",
						"public class Rational  {}", codeValidatorLevel));

		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						"class Rational  {}",
						"final class Rational  {}", codeValidatorLevel));

		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						"public class Rational  {}",
						"class Rational  {}", codeValidatorLevel));

		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						"final class Rational  {}",
						"class Rational  {}", codeValidatorLevel));
	}

	@Test
	public void testInnerClassProtected(){
				assertNotEquals(MUTANT_VALIDATION_SUCCESS,
						validateMutantGetMessage(
								"public class Outer{ public class Rational  {}}",
								"public class Outer{ protected class Rational  {}}", CodeValidatorLevel.STRICT));
	}

	@Test
	public void testLiterals() throws Exception {
		assertEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						""
						+ "public class Test {"
						+ "public test(){"
						+ "format(\"first\", \"second\", \"third\");"
						+ "}"
						+ "}",
						""
						+ "public class Test {"
						+ "public test(){"
						+ "format(\"\", \"sec\", \"third\");"
						+ "}"
						+ "}", codeValidatorLevel));

		assertEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						""
						+ "public class Test {"
						+ "public test(){"
						+ "String s = \"\";"
						+ "}"
						+ "}",
						""
						+ "public class Test {"
						+ "public test(){"
						+ "String s = \" \";"
						+ "}"
						+ "}", codeValidatorLevel));

		assertEquals(MUTANT_VALIDATION_SUCCESS,
					validateMutantGetMessage(
							""
							+ "public class Test {"
							+ "public test(){"
							+ "String s = \"stringval\";"
							+ "}"
							+ "}",
							""
							+ "public class Test {"
							+ "public test(){"
							+ "String s = \"stringval \";"
							+ "}"
							+ "}", codeValidatorLevel));

		for (String p : CodeValidator.PROHIBITED_BITWISE_OPERATORS) {
			assertEquals(p + " in a String should be valid", MUTANT_VALIDATION_SUCCESS,
					validateMutantGetMessage(
							""
							+ "public class Test {"
							+ "public test(){"
							+ "String s = \"\";"
							+ "}"
							+ "}",
							""
							+ "public class Test {"
							+ "public test(){"
							+ "String s = \"" + p + "\";"
							+ "}"
							+ "}", codeValidatorLevel));
		}

		assertEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(""
						+ "public class Test {"
						+ "public test(){"
						+ "String s = \"\";"
						+ "}"
						+ "}",
						""
						+ "public class Test {"
						+ "public test(){"
						+ "String s = \";?{} <<\";"
						+ "}"
						+ "}", codeValidatorLevel));


		assertEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(
						""
						+ "public class Test {"
						+ "public test(){"
						+ "String s = \"\";",
						""
						+ "public class Test {"
						+ "public test(){"
						+ "String s = \"public final protected\";"
						+ "}"
						+ "}", codeValidatorLevel));

		assertEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(""
						+ "public class Test {"
						+ "public test(){"
						+ "Char c = \'c\';"
						+ "}"
						+ "}",
						""
						+ "public class Test {"
						+ "public test(){"
						+ "Char c = \';\';"
						+ "}"
						+ "}", codeValidatorLevel));
	}

	// TODO Ideally this should be split in several test cases each addressing a specific modification
	@Test
	public void testComments() throws Exception {
		String originalCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\";" + "\n"
				+ "if(x > 0) \n\t return x;" + "\n"
				+ "}" + "\n"
				+ "}";
		String mutatedCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\";// added comment" + "\n"
				+ "if(x > 0) \n\t return x;" + "\n"
				+ "}" + "\n"
				+ "}";
		assertNotEquals("added single line comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage( originalCode, mutatedCode, codeValidatorLevel));

		mutatedCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\";// added comment" + "\n"
				+ "if(x > 1) \n\t return x; // comment" + "\n"
				+ "}" + "\n"
				+ "}";
		assertNotEquals("added single line comment in new line", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));

		/////
		originalCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\";" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";
		mutatedCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\";" + "\n"
				+ "if(x > 1) \n\t return x; //x is gt 1" + "\n"
				+ "}" + "\n"
				+ "}";
		assertNotEquals("modified single line comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));

		originalCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"old\";// comment" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";
		mutatedCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"new\";// comment" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";
		assertEquals("modified code, single line comment unchanged", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));

		originalCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\";" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";
		mutatedCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\"; /*added comment*/" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";
		assertNotEquals("added multiline comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));

	}

	@Test
	public void testModifiedCommentInNewLineAfterUnchangedComment(){
		String originalCode = ""
				+ "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\"; //comment"+ "\n"
				+ "int foo; // comment"+ "\n"
				+ "int foo1;" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";
		String mutatedCode = ""
				+ "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\"; //comment\n"
				+ "int foo; //new comment" + "\n"
				+ "int foo1;" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";
		assertNotEquals("modified comment in new line after unchanged comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage( originalCode, mutatedCode, codeValidatorLevel));
	}

	@Test
	public void testValidMutantNoChangeInComment(){
		String originalCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\"; // comment" + "\n"
				+ "int foo1 = 0;" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";

		String mutatedCode = "public class Test{"+ "\n"
				+ "public void test(){"+ "\n"
				+ "String s = \"\"; // comment" + "\n"
				+ "int foo2 = 0;" + "\n"
				+ "if(x > 0) \n\t return x; //x is positive" + "\n"
				+ "}" + "\n"
				+ "}";

		assertEquals("changed code in new line after unchanged comment", MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage(originalCode, mutatedCode, codeValidatorLevel));

	}

	// THIS IS BAD PRACTICE! Cannot really tell what those tests do !
	@Test
	public void testModerateLevel() throws Exception {
		checkModerateRelaxations(CodeValidatorLevel.MODERATE);
	}
	@Test
	public void testStrictLevel() throws Exception {
		checkModerateRelaxations(CodeValidatorLevel.STRICT);
	}

	//bitshifts and signature changes are valid with a moderate validator
	public void checkModerateRelaxations(CodeValidatorLevel level) {
		boolean isValid = !level.equals(CodeValidatorLevel.STRICT);
		if (isValid) {
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  { public void test(){ r.num = r.num; }}", "public class Rational  { public void test(){  r.num = r.num | ((r.num & (1 << 29)) << 1); }}", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  { public void test(){ r.num = r.num; }}", "public class Rational  { public void test(){ r.num = r.num << 1+344; }}", level));
		} else {
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  { public void test(){ r.num = r.num; }}", "public class Rational  { public void test(){ r.num = r.num | ((r.num & (1 << 29)) << 1); }}", level));
			assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Rational  { public void test(){ r.num = r.num; }}", "public class Rational  { public void test(){ r.num = r.num << 1+344; }}", level));
		}
	}

	@Ignore // This test must be refactored into multiple tests !
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
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Test{ public void test(){  String s = \"\"; }}", "public class Test{ public void test(){  String s = \"\";// added comment\n }}", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Test{ public void test(){  if(x > 0) \n\t return x; }}", "public class Test{ public void test(){  if(x > 1) \n\t return x; // comment\n }}", level));
			assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Test{ public void test(){  String s = \"\"; }}", "public class Test{ public void test(){  String s = \"\"; /*added comment*/\n }}", level));

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
		assertEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Test{ public void test(){ if (numRiders + numEntering <= capacity) {} }}", "public class Test{ public void test(){ if (numRiders + numEntering <= capacity && false) {} }}", CodeValidatorLevel.RELAXED));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Test{ public void test(){ if (numRiders + numEntering <= capacity) {} }}", "public class Test{ public void test(){ if (numRiders + numEntering <= capacity && false) {} }}", CodeValidatorLevel.MODERATE));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS, validateMutantGetMessage("public class Test{ public void test(){ if (numRiders + numEntering <= capacity) {} }}", "public class Test{ public void test(){ if (numRiders + numEntering <= capacity && false) {} }}", CodeValidatorLevel.STRICT));
	}

	@Test
	public void testInvalidMutantWithChangedAccess(){
		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(""
						+ "public class Test {"
						+ "public void test() {}"
						+ "}",
						""
						+ "public class Test {"
						+ "void test() {}"
						+ "}", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(""
						+ "public class Test {"
						+ "public void test() {}"
						+ "}",
						""
						+ "public class Test {"
						+ "protected void test() {}"
						+ "}", codeValidatorLevel));
		assertNotEquals(MUTANT_VALIDATION_SUCCESS,
				validateMutantGetMessage(""
						+ "public class Test {"
						+ "public void test() {}"
						+ "}",
						""
						+ "public class Test {"
						+ "private void test() {}"
						+ "}", codeValidatorLevel));
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


	@Test
	public void testInvalidMutantsWithChangesAtMultilineComments(){
		String originalCode =
				"/*" + "\n"
				+ "* This is a multiLine comment " + "\n"
				+ "*/"+ "\n"
				+ "public class Test{}";
		String mutatedCode =
				"/*" + "\n"
				+ "* This is a MODIFIED multiLine comment " + "\n"
				+ "*/"+ "\n"
				+ "public class Test{}";

		assertEquals(MUTANT_VALIDATION_COMMENT, validateMutantGetMessage(originalCode, mutatedCode, CodeValidatorLevel.MODERATE));
	}

}

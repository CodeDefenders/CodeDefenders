/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.validation.code;

/**
 * This enumeration represents states and their
 * message during code validation.
 *
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class ValidationMessage {
    // Generic error message.
    public static final String MUTANT_VALIDATION_FAILED = "Invalid mutant. Your mutant does not comply with our rules.";

    public static final String MUTANT_ONLY_COMMENT_CHANGES = "Your mutant only changes comments.";
    public static final String MUTANT_PACKAGE = "Your mutant changes the package signature.";
    public static final String MUTANT_CLASS = "Your mutant adds a new class or changes a class signature.";
    public static final String MUTANT_ADDS_OR_RENAMES_FIELD = "Your mutant adds or renames a field.";
    public static final String MUTANT_ADDS_OR_RENAMES_METHOD = "Your mutant adds or renames a method.";
    public static final String MUTANT_METHOD_SIGNATURE = "Your mutant changes one or more method signatures.";
    public static final String MUTANT_IMPORT_STATEMENT = "Your mutant changes one or more import statements.";
    public static final String MUTANT_INSTANCEOF = "Your mutant modifies an instanceof condition.";
    public static final String MUTANT_IDENTICAL = "Your mutant is identical to the CUT.";
    public static final String MUTANT_COMMENT = "Your mutant adds or modifies a comment.";
    public static final String MUTANT_MODIFIER = "Your mutant changes a modifier like 'static' or 'private'.";
    public static final String MUTANT_LOGIC = "Your mutant adds a new logical operator like '&&' or '||'.";
    public static final String MUTANT_BITWISE = "Your mutant adds new bitwise operators like '&' or '>>'";
    public static final String MUTANT_CONDITIONALS = "Your mutant adds a new conditional statement like 'if', 'switch' etc.";
    public static final String MUTANT_LOOPS = "Your mutant adds a new loop.";
    public static final String MUTANT_CALL_SYSTEM = "Your mutant adds a call to System.*.";
    public static final String MUTANT_CALL_RANDOM = "Your mutant calls a random number generator.";
    public static final String MUTANT_CALL_DATE = "Your mutant calls a Date class";
    public static final String MUTANT_CALL_THREAD = "Your mutant calls a multithreading class";
    public static final String MUTANT_CALL_IO = "Your mutant calls an IO class";

    public static final String MUTANT_VALIDATION_SUCCESS = "SUCCESS"; //Never shown in the UI, only used for tests

    public static final String MUTANT_MISSING_INTENTION = "You must declare your intention.";

}

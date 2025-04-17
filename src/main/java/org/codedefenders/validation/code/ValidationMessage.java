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
 * <p>Use {@link #get()} to retrieve the message as a {@link String}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public enum ValidationMessage {
    // Generic error message.
    MUTANT_VALIDATION_FAILED("Invalid mutant. Your mutant does not comply with our rules."),

    MUTANT_VALIDATION_SUCCESS("Your mutant complies with our rules."),
    MUTANT_VALIDATION_LINES("Invalid mutant, sorry! Removing or adding lines is not allowed."),
    MUTANT_VALIDATION_MODIFIER("Invalid mutant, sorry! Changing modifiers such as 'static' or 'public' is not allowed."),
    MUTANT_VALIDATION_COMMENT("Invalid mutant, sorry! Adding or modifying comments is not allowed."),
    MUTANT_VALIDATION_LOGIC("Invalid mutant, sorry! Your mutant contains new logical operations"),

    MUTANT_VALIDATION_LOGIC_INSTANCEOF("Invalid mutant, sorry! Your mutant modifies an instanceof condition"),

    MUTANT_VALIDATION_OPERATORS("Invalid mutant, sorry! Your mutant contains prohibited operations such as bitshifts, ternary operators, added comments or multiple statments per line."),
    MUTANT_VALIDATION_CALLS("Your mutant contains calls to System.*, Random.* or new control structures.\n\nShame on you!"),
    MUTANT_VALIDATION_IDENTICAL("Invalid mutant, sorry! Your mutant is identical to the CUT"),

    MUTANT_VALIDATION_METHOD_SIGNATURE("Invalid mutant, sorry! Your mutant changes one or more method signatures"),
    MUTANT_VALIDATION_FIELD_NAME("Invalid mutant, sorry! Your mutant changes one or more field names"),
    MUTANT_VALIDATION_IMPORT_STATEMENT("Invalid mutant, sorry! Your mutant changes one or more import statements"),
    MUTANT_VALIDATION_PACKAGE_SIGNATURE("Invalid mutant, sorry! Your mutant changes the package signature"),
    MUTANT_VALIDATION_CLASS_SIGNATURE("Invalid mutant, sorry! Your mutant changes a class signature"),
    MUTANT_VALIDATION_METHOD_OR_FIELD_ADDED("Invalid mutant, sorry! Your mutant adds a new method or field, or renames an existing one"),

    MUTANT_MISSING_INTENTION("Invalid mutant, sorry! You must declare your intention."),
    MUTATION_CLASS_DECLARATION("Invalid mutation contains class declaration."),
    MUTATION_METHOD_DECLARATION("Invalid mutation contains method declaration."),

    MUTATION_SYSTEM_USE("Invalid mutation contains System uses"),
    MUTATION_SYSTEM_CALL("Invalid mutation contains a call to System.*"),
    MUTATION_SYSTEM_DECLARATION("Invalid mutation contains variable declaration using System.*"),

    MUTATION_FOR_EACH_STATEMENT("Invalid mutation contains a ForeachStmt statement"),
    MUTATION_IF_STATEMENT("Invalid mutation contains an IfStmt statement"),
    MUTATION_FOR_STATEMENT("Invalid mutation contains a ForStmt statement"),
    MUTATION_WHILE_STATEMENT("Invalid mutation contains a WhileStmt statement"),
    MUTATION_DO_STATEMENT("Invalid mutation contains a DoStmt statement"),
    MUTATION_SWITCH_STATEMENT("Invalid mutation contains a SwitchStmt statement");

    private final String message;

    ValidationMessage(String message) {
        this.message = message;
    }

    public String get() {
        return message;
    }

    @Override
    public String toString() {
        return get();
    }
}

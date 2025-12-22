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

import java.util.Arrays;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.NoCommentEqualsVisitor;

public class MutantValidationRules {
    /**
     * Categories of rules.
     */
    private static final String IDENTICAL = "Mutants must include a real change of code";
    private static final String RENAMING = "Mutants must keep the names of methods and fields unchanged";
    private static final String CONTROL = "Mutants may not change control structures";
    private static final String FORBIDDEN_EXPRESSIONS = "Some statements and expressions may not be changed by a mutant";

    public static MutantRule noCommentsEqual = new MutantRule.Builder(
            IDENTICAL,
            "No mutants that only change comments",
            ValidationMessage.MUTANT_VALIDATION_IDENTICAL)
            .withCompilation(NoCommentEqualsVisitor::equals)
            .build();

    public static MutantRule packageDeclarations = new MutantRule.Builder(
            RENAMING,
            "Changes to package signatures are not allowed",
            ValidationMessage.MUTANT_VALIDATION_PACKAGE_SIGNATURE)
            .withCompilation(CodeValidator::containsChangesToPackageDeclarations)
            .build();

    public static MutantRule classDeclarations = new MutantRule.Builder(
            RENAMING,
            "Changes to class signatures are not allowed",
            ValidationMessage.MUTANT_VALIDATION_CLASS_SIGNATURE)
            .withCompilation(CodeValidator::containsChangesToClassDeclarations)
            .build();

    public static MutantRule addOrRenameMethodsOrFields = new MutantRule.Builder(
            RENAMING,
            "No methods or fields may be added or renamed",
            ValidationMessage.MUTANT_VALIDATION_METHOD_OR_FIELD_ADDED).
            withCompilation(CodeValidator::mutantAddsOrRenamesMethodOrField)
            .build();

    public static MutantRule changesMethodSignatures = new MutantRule.Builder(
            RENAMING,
            "No changes to method signatures are allowed",
            ValidationMessage.MUTANT_VALIDATION_METHOD_SIGNATURE).
            withCompilation(CodeValidator::mutantChangesMethodSignatures)
            .build();

    /* TODO Ist jetzt eh unnötig??
    if (level == CodeValidatorLevel.STRICT && mutantChangesFieldNames(originalCU, mutatedCU)) {
            return ValidationMessage.MUTANT_VALIDATION_FIELD_NAME;
        }
     */

    public static MutantRule changesImportStatements = new MutantRule.Builder(
            FORBIDDEN_EXPRESSIONS,
            "No changes to import statements are allowed.",
            ValidationMessage.MUTANT_VALIDATION_IMPORT_STATEMENT).
            withCompilation(CodeValidator::mutantChangesImportStatements)
            .build();

    public static MutantRule instanceofChanges = new MutantRule.Builder(
            CONTROL,
            "No changes to instanceof statements are allowed",
            ValidationMessage.MUTANT_VALIDATION_LOGIC_INSTANCEOF).
            withCompilation(CodeValidator::containsInstanceOfChanges)
            .build();

    public static MutantRule astEqual = new MutantRule.Builder(
            IDENTICAL,
            "Mutants may not be identical to the Class under Test",
            ValidationMessage.MUTANT_VALIDATION_IDENTICAL).
            withCompilation(Node::equals)
            .build();

    public static MutantRule noChangesToComments = new MutantRule.Builder(
            IDENTICAL,
            "Any changes to comments are not allowed",
            ValidationMessage.MUTANT_VALIDATION_COMMENT)
            .withCompilation(CodeValidator::containsModifiedComments)
            .withInsertion(CodeValidator.COMMENT_TOKENS)
            .build();

    public static MutantRule prohibitedModifier = new MutantRule.Builder(
            FORBIDDEN_EXPRESSIONS,
            "Contains changes to modifiers that are not allowed",//TODO Was genau?
            ValidationMessage.MUTANT_VALIDATION_MODIFIER)
            .withCode(CodeValidator::containsProhibitedModifierChanges)
            .build();

    public static MutantRule logicalOperator = new MutantRule.Builder(
            CONTROL,
            "New logical operators are not allowed",
            ValidationMessage.MUTANT_VALIDATION_LOGIC)
            .withLinediff(CodeValidator::logicalOpAdded)
            .build();

    public static MutantRule prohibitedControlStructures = new MutantRule.Builder(
            CONTROL,
            "These control structures are not allowed: " + Arrays.toString(CodeValidator.PROHIBITED_CONTROL_STRUCTURES),
            ValidationMessage.MUTANT_VALIDATION_OPERATORS)
            .withInsertion(CodeValidator.PROHIBITED_CONTROL_STRUCTURES)
            .withLinediff(CodeValidator::ternaryAdded)
            .build();

    public static MutantRule prohibitedCalls = new MutantRule.Builder(
            FORBIDDEN_EXPRESSIONS,
            "No calls to these packages are allowed: " + Arrays.toString(CodeValidator.PROHIBITED_CALLS),
            ValidationMessage.MUTANT_VALIDATION_CALLS)
            .withInsertion(CodeValidator.PROHIBITED_CALLS)
            .build();

    public static MutantRule prohibitedBitwiseOperators = new MutantRule.Builder(
            CONTROL,
            "Bitwise operators are not allowed",
            ValidationMessage.MUTANT_VALIDATION_OPERATORS)
            .withInsertion(CodeValidator.PROHIBITED_BITWISE_OPERATORS)
            .build();
}

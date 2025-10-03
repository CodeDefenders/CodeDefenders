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
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.NoCommentEqualsVisitor;

public class MutantValidationRules {
    /**
     * Categories of rules.
     */
    private static final String IDENTICAL = "No mutants with only changes to comments or formatting";
    private static final String RENAMING = "No renaming of methods or fields, no additional methods or fields";
    private static final String COMMENTS = "No changes to comments";


    public static MutantComparisonRule<CompilationUnit> noCommentsEqual = new MutantComparisonRule<>(
            IDENTICAL,
            NoCommentEqualsVisitor::equals,
            ValidationMessage.MUTANT_VALIDATION_IDENTICAL
    );

    public static MutantComparisonRule<CompilationUnit> packageDeclarations = new MutantComparisonRule<>(
            RENAMING,
            "Changes to package signatures are not allowed",
            CodeValidator::containsChangesToPackageDeclarations,
            ValidationMessage.MUTANT_VALIDATION_PACKAGE_SIGNATURE
    );

    public static MutantComparisonRule<CompilationUnit> classDeclarations = new MutantComparisonRule<>(
            RENAMING,
            "Changes to class signatures are not allowed",
            CodeValidator::containsChangesToClassDeclarations,
            ValidationMessage.MUTANT_VALIDATION_CLASS_SIGNATURE
    );

    public static MutantComparisonRule<CompilationUnit> addOrRenameMethodsOrFields = new MutantComparisonRule<>(
            RENAMING,
            "No methods or fields may be added or renamed",
            CodeValidator::mutantAddsOrRenamesMethodOrField,
            ValidationMessage.MUTANT_VALIDATION_METHOD_OR_FIELD_ADDED
    );

    public static MutantComparisonRule<CompilationUnit> changesMethodSignatures = new MutantComparisonRule<>(
            "No changes to method signatures are allowed",
            CodeValidator::mutantChangesMethodSignatures,
            ValidationMessage.MUTANT_VALIDATION_METHOD_SIGNATURE
    );

    /* TODO Ist jetzt eh unnötig??
    if (level == CodeValidatorLevel.STRICT && mutantChangesFieldNames(originalCU, mutatedCU)) {
            return ValidationMessage.MUTANT_VALIDATION_FIELD_NAME;
        }
     */

    public static MutantComparisonRule<CompilationUnit> changesImportStatements = new MutantComparisonRule<>(
            "No changes to import statements are allowed.",
            CodeValidator::mutantChangesImportStatements,
            ValidationMessage.MUTANT_VALIDATION_IMPORT_STATEMENT
    );

    public static MutantComparisonRule<CompilationUnit> instanceofChanges = new MutantComparisonRule<>(
            "No changes to instanceof statements are allowed",
            CodeValidator::containsInstanceOfChanges,
            ValidationMessage.MUTANT_VALIDATION_LOGIC_INSTANCEOF
    );

    public static MutantComparisonRule<CompilationUnit> astEqual = new MutantComparisonRule<>(
            IDENTICAL,
            Node::equals,
            ValidationMessage.MUTANT_VALIDATION_IDENTICAL
    );

    public static MutantComparisonRule<CompilationUnit> noChangesToComments = new MutantComparisonRule<>(
            "Changes to comments are not allowed",
            CodeValidator::containsModifiedComments,
            ValidationMessage.MUTANT_VALIDATION_COMMENT
    );

    public static MutantComparisonRule<String> prohibitedModifier = new MutantComparisonRule<>(
            "Contains changes to modifiers that are not allowed", //TODO Was genau?
            CodeValidator::containsProhibitedModifierChanges,
            ValidationMessage.MUTANT_VALIDATION_MODIFIER
    );

    public static MutantComparisonRule<List<List<String>>> ternaryOperators = new MutantComparisonRule<>(
            "Ternary operators are not allowed",
            CodeValidator::ternaryAdded,
            ValidationMessage.MUTANT_VALIDATION_OPERATORS
    );

    public static MutantComparisonRule<List<List<String>>> logicalOperator = new MutantComparisonRule<>(
            "New logical Operations are not allowed",
            CodeValidator::logicalOpAdded,
            ValidationMessage.MUTANT_VALIDATION_LOGIC
    );

    public static MutantInsertionRule prohibitedControlStructures = new MutantInsertionRule(
            "Adding new control structures is not allowed",
            "These control structures are not allowed: " + Arrays.toString(CodeValidator.PROHIBITED_CONTROL_STRUCTURES),
            CodeValidator.PROHIBITED_CONTROL_STRUCTURES,
            ValidationMessage.MUTANT_VALIDATION_OPERATORS
    );

    public static MutantInsertionRule commentTokens = new MutantInsertionRule(
            COMMENTS,
            CodeValidator.COMMENT_TOKENS,
            ValidationMessage.MUTANT_VALIDATION_COMMENT
    );

    public static MutantInsertionRule prohibitedCalls = new MutantInsertionRule(
            "No calls to System.*, Random.*",
            "No calls to these packages are allowed: " + Arrays.toString(CodeValidator.PROHIBITED_CALLS),
            CodeValidator.PROHIBITED_CALLS,
            ValidationMessage.MUTANT_VALIDATION_CALLS
    );

    public static MutantInsertionRule prohibitedBitwiseOperators = new MutantInsertionRule(
            "Bitwise operators are not allowed",
            CodeValidator.PROHIBITED_BITWISE_OPERATORS,
            ValidationMessage.MUTANT_VALIDATION_OPERATORS
    );




}

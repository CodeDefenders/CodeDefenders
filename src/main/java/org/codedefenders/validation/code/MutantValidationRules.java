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

import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.NoCommentEqualsVisitor;

public class MutantValidationRules {
    /**
     * Categories of rules.
     */
    private static final String IDENTICAL = "Mutants must change the code";
    private static final String RENAMING = "Changes to names are restricted";
    private static final String CONTROL = "Changes to control structures are restricted";
    private static final String METHOD_CALLS = "Some classes and methods may not be called";
    private static final String FORBIDDEN_EXPRESSIONS
            = "Some statements and expressions may not be changed by a mutant";

    public static MutantRule noCommentsEqual = new MutantRule.Builder(
            IDENTICAL,
            "No mutants that only change comments",
            ValidationMessage.MUTANT_ONLY_COMMENT_CHANGES)
            .withCompilation(NoCommentEqualsVisitor::equals)
            .build();

    public static MutantRule packageDeclarations = new MutantRule.Builder(
            RENAMING,
            "No changes to package signatures",
            ValidationMessage.MUTANT_PACKAGE)
            .withCompilation(CodeValidator::containsChangesToPackageDeclarations)
            .build();

    public static MutantRule classDeclarations = new MutantRule.Builder(
            RENAMING,
            "No changes to class signatures",
            ValidationMessage.MUTANT_CLASS)
            .withCompilation(CodeValidator::containsChangesToClassDeclarations)
            .withInsertionNode(n -> n instanceof ClassOrInterfaceDeclaration || n instanceof RecordDeclaration)
            .build();

    public static MutantRule addOrRenameMethods = new MutantRule.Builder(
            RENAMING,
            "No new or renamed methods",
            ValidationMessage.MUTANT_ADDS_OR_RENAMES_METHOD)
            .withCompilation(CodeValidator::mutantAddsOrRenamesMethod)
            .withInsertionNode(n ->
                    n instanceof MethodDeclaration
                            || n instanceof ConstructorDeclaration
                            || n instanceof CompactConstructorDeclaration)
            .build();

    public static MutantRule addOrRenameFields = new MutantRule.Builder(
            RENAMING,
            "No new or renamed fields",
            ValidationMessage.MUTANT_ADDS_OR_RENAMES_FIELD)
            .withCompilation(CodeValidator::mutantAddsOrChangesFieldNames)
            .build();

    public static MutantRule changesMethodSignatures = new MutantRule.Builder(
            RENAMING,
            "No changes to method signatures",
            ValidationMessage.MUTANT_METHOD_SIGNATURE)
            .withCompilation(CodeValidator::mutantChangesMethodSignatures)
            .build();

    public static MutantRule changesImportStatements = new MutantRule.Builder(
            FORBIDDEN_EXPRESSIONS,
            "No changes to import statements",
            ValidationMessage.MUTANT_IMPORT_STATEMENT)
            .withCompilation(CodeValidator::mutantChangesImportStatements)
            .build();

    public static MutantRule instanceofChanges = new MutantRule.Builder(
            CONTROL,
            "No changes to instanceof statements",
            ValidationMessage.MUTANT_INSTANCEOF)
            .withCompilation(CodeValidator::containsInstanceOfChanges)
            .build();

    public static MutantRule astEqual = new MutantRule.Builder(
            IDENTICAL,
            "No mutants that are identical to the Class under Test",
            ValidationMessage.MUTANT_IDENTICAL)
            .withCompilation(Node::equals)
            .build();

    public static MutantRule noChangesToComments = new MutantRule.Builder(
            IDENTICAL,
            "No changes to comments",
            ValidationMessage.MUTANT_COMMENT)
            .withCompilation(CodeValidator::containsModifiedComments)
            .withInsertion(CodeValidator.COMMENT_TOKENS)
            .build();

    public static MutantRule prohibitedModifier = new MutantRule.Builder(
            FORBIDDEN_EXPRESSIONS,
            "No changes to modifiers like 'static' or 'public'",
            ValidationMessage.MUTANT_MODIFIER)
            .withCode(CodeValidator::containsProhibitedModifierChanges)
            .build();

    public static MutantRule logicalOperator = new MutantRule.Builder(
            CONTROL,
            "No new logical operators like '&&' or '||'",
            ValidationMessage.MUTANT_LOGIC)
            .withLinediff(CodeValidator::logicalOpAdded)
            .build();

    public static MutantRule prohibitedConditionals = new MutantRule.Builder(
            CONTROL,
            "No new conditional statements like if, switch etc.",
            ValidationMessage.MUTANT_CONDITIONALS)
            .withInsertion(CodeValidator.PROHIBITED_CONDITIONALS)
            .withLinediff(CodeValidator::ternaryAdded)
            .withInsertionNode(n -> n instanceof ForEachStmt
                    || n instanceof IfStmt
                    || n instanceof SwitchStmt
                    || n instanceof SwitchExpr
            )
            .build();

    public static MutantRule prohibitedLoops = new MutantRule.Builder(
            CONTROL,
            "No new loops.",
            ValidationMessage.MUTANT_LOOPS)
            .withInsertion(CodeValidator.PROHIBITED_LOOPS)
            .withInsertionNode(n -> n instanceof ForEachStmt
                    || n instanceof ForStmt
                    || n instanceof WhileStmt
                    || n instanceof DoStmt
            )
            .build();

    public static MutantRule noSystemCalls = new MutantRule.Builder(
            METHOD_CALLS,
            "No calls to System.*",
            ValidationMessage.MUTANT_CALL_SYSTEM)
            .withInsertion("System.")
            .withInsertionNode(n -> n instanceof NameExpr nameExpr
                    && nameExpr.getNameAsString().equals("System"))
            //TODO Are the next checks necessary? Why not for the other forbidden packages?
            .withInsertionNode(n -> n instanceof MethodCallExpr methodCallExpr
                    && methodCallExpr.getNameAsString().startsWith("System."))
            .withInsertionNode(n -> n instanceof VariableDeclarator variableDeclarator
                    && variableDeclarator.getInitializer().isPresent()
                    && JavaParserUtils.unparse(variableDeclarator.getInitializer().get()).startsWith("System."))
            .build();

    public static MutantRule noRandom = new MutantRule.Builder(
            METHOD_CALLS,
            "No use of random number generators",
            ValidationMessage.MUTANT_CALL_RANDOM)
            .withInsertion("Random.", "Random(", "random(", "randomUUID(") //TODO check against Math.random()
            .withInsertionNode(n -> n instanceof NameExpr nameExpr
                    && nameExpr.getNameAsString().equals("Random"))
            .build();

    public static MutantRule noDate = new MutantRule.Builder(
            METHOD_CALLS,
            "No calls to Date classes",
            ValidationMessage.MUTANT_CALL_DATE)
            .withInsertion("Date(")
            .withInsertionNode(n -> n instanceof NameExpr nameExpr
                    && nameExpr.getNameAsString().equals("Date"))
            .build();

    public static MutantRule noThreading = new MutantRule.Builder(
            METHOD_CALLS,
            "No calls to multithreading classes",
            ValidationMessage.MUTANT_CALL_THREAD)
            .withInsertion("Thread.")
            .withInsertionNode(n -> n instanceof NameExpr nameExpr
                    && nameExpr.getNameAsString().equals("Thread"))
            .build();

    public static MutantRule noIO = new MutantRule.Builder(
            METHOD_CALLS,
            "No calls new IO calls",
            ValidationMessage.MUTANT_CALL_IO)
            .withInsertion("java.io", "java.nio", "java.net", "java.sql")
            .build();


    public static MutantRule prohibitedBitwiseOperators = new MutantRule.Builder(
            CONTROL,
            "No new bitwise operators",
            ValidationMessage.MUTANT_BITWISE)
            .withInsertion(CodeValidator.PROHIBITED_BITWISE_OPERATORS)
            .build();
}

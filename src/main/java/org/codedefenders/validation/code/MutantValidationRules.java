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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.util.JavaParserUtils;

import com.github.difflib.patch.AbstractDelta;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.NoCommentEqualsVisitor;
import com.github.javaparser.ast.visitor.Visitable;

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
            .withCompilation((o, m) -> !o.getPackageDeclaration().equals(m.getPackageDeclaration()))
            .build();

    public static MutantRule classDeclarations = new MutantRule.Builder(
            RENAMING,
            "No changes to class signatures",
            ValidationMessage.MUTANT_CLASS)
            .withCompilation(MutantValidationRules::containsChangesToClassDeclarations)
            .withInsertionNode(n -> n instanceof ClassOrInterfaceDeclaration || n instanceof RecordDeclaration)
            .build();

    public static MutantRule addOrRenameMethods = new MutantRule.Builder(
            RENAMING,
            "No new or renamed methods",
            ValidationMessage.MUTANT_ADDS_OR_RENAMES_METHOD)
            .withCompilation(MutantValidationRules::mutantAddsOrRenamesMethod)
            .withInsertionNode(n ->
                    n instanceof MethodDeclaration
                            || n instanceof ConstructorDeclaration
                            || n instanceof CompactConstructorDeclaration)
            .build();

    public static MutantRule addOrRenameFields = new MutantRule.Builder(
            RENAMING,
            "No new or renamed fields",
            ValidationMessage.MUTANT_ADDS_OR_RENAMES_FIELD)
            .withCompilation(MutantValidationRules::mutantAddsOrChangesFieldNames)
            .build();

    public static MutantRule changesMethodSignatures = new MutantRule.Builder(
            RENAMING,
            "No changes to method signatures",
            ValidationMessage.MUTANT_METHOD_SIGNATURE)
            .withCompilation(MutantValidationRules::mutantChangesMethodSignatures)
            .build();

    public static MutantRule changesImportStatements = new MutantRule.Builder(
            FORBIDDEN_EXPRESSIONS,
            "No changes to import statements",
            ValidationMessage.MUTANT_IMPORT_STATEMENT)
            .withCompilation(MutantValidationRules::mutantChangesImportStatements)
            .build();

    public static MutantRule instanceofChanges = new MutantRule.Builder(
            CONTROL,
            "No changes to instanceof statements",
            ValidationMessage.MUTANT_INSTANCEOF)
            .withCompilation(MutantValidationRules::containsInstanceOfChanges)
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
            .withCompilation(MutantValidationRules::containsModifiedComments)
            .withInsertion("//", "/*")
            .build();

    public static MutantRule prohibitedModifier = new MutantRule.Builder(
            FORBIDDEN_EXPRESSIONS,
            "No changes to modifiers like 'static' or 'public'",
            ValidationMessage.MUTANT_MODIFIER)
            .withCode(MutantValidationRules::containsProhibitedModifierChanges)
            .build();

    public static MutantRule logicalOperator = new MutantRule.Builder(
            CONTROL,
            "No new logical operators like '&&' or '||'",
            ValidationMessage.MUTANT_LOGIC)
            .withInsertion("&&", "||")
            .build();

    public static MutantRule prohibitedConditionals = new MutantRule.Builder(
            CONTROL,
            "No new conditional statements like if, switch etc.",
            ValidationMessage.MUTANT_CONDITIONALS)
            .withInsertion("if", "switch")
            .withLineDiff(MutantValidationRules::ternaryAdded)
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
            .withInsertion("while", "for")
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
            .withInsertion("Random.", "Random(", "random(", "randomUUID(")
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
            "No new IO calls",
            ValidationMessage.MUTANT_CALL_IO)
            .withInsertion("java.io", "java.nio", "java.net", "java.sql")
            .build();


    public static MutantRule prohibitedBitwiseOperators = new MutantRule.Builder(
            CONTROL,
            "No new bitwise operators",
            ValidationMessage.MUTANT_BITWISE)
            .withInsertion("<<", ">>", ">>>", "|", "&")
            .build();

    /**
     * Checks if the mutation introduces a change to a class declaration.
     */
    private static boolean containsChangesToClassDeclarations(CompilationUnit originalCU, CompilationUnit mutatedCU) {
        Map<String, NodeList<Modifier>> originalTypes = new HashMap<>();
        for (TypeDeclaration<?> type : originalCU.getTypes()) {
            originalTypes.putAll(ValidationUtils.extractTypeDeclaration(type));
        }

        Map<String, NodeList<Modifier>> mutatedTypes = new HashMap<>();
        for (TypeDeclaration<?> type : mutatedCU.getTypes()) {
            mutatedTypes.putAll(ValidationUtils.extractTypeDeclaration(type));
        }

        return !originalTypes.equals(mutatedTypes);
    }

    /**
     * Checks if the mutation introduce a change to an instanceof condition.
     */
    private static boolean containsInstanceOfChanges(CompilationUnit originalCU, CompilationUnit mutatedCU) {
        final List<ReferenceType> instanceOfInsideOriginal = new ArrayList<>();
        final List<ReferenceType> instanceOfInsideMutated = new ArrayList<>();
        final AtomicBoolean analyzingMutant = new AtomicBoolean(false);


        ModifierVisitor<Void> visitor = new ModifierVisitor<>() {

            @Override
            public Visitable visit(IfStmt n, Void arg) {
                // Extract elements from the condition
                if (n.getCondition() instanceof InstanceOfExpr expr) {
                    ReferenceType type = expr.getType();

                    // Accumulate instanceOF
                    if (analyzingMutant.get()) {
                        instanceOfInsideMutated.add(type);
                    } else {
                        instanceOfInsideOriginal.add(type);
                    }

                }
                return super.visit(n, arg);
            }
        };

        visitor.visit(originalCU, null);

        if (!instanceOfInsideOriginal.isEmpty()) {
            analyzingMutant.set(true);
            visitor.visit(mutatedCU, null);
        }

        return !instanceOfInsideMutated.equals(instanceOfInsideOriginal);
    }

    private static boolean containsModifiedComments(CompilationUnit originalCU, CompilationUnit mutatedCU) {
        // We assume getAllContainedComments() preserves the order of comments
        Comment[] originalComments = originalCU.getAllContainedComments().toArray(new Comment[]{});
        Comment[] mutatedComments = mutatedCU.getAllContainedComments().toArray(new Comment[]{});
        if (originalComments.length != mutatedComments.length) {
            // added comments triggers validation
            return true;
        }
        // We cannot use equality here because inserting empty lines will change the
        // lineStart attribute of the Comment node.
        for (int i = 0; i < originalComments.length; i++) {
            // Somehow the mutated comments contain char(13) '\r' in addition to '\n'
            // TODO Where those come from? CodeMirror?
            if (!originalComments[i].getContent().replaceAll("\\r", "")
                    .equals(mutatedComments[i].getContent().replaceAll("\\r", ""))) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsProhibitedModifierChanges(String originalCode, String mutatedCode) {
        List<DiffMatchPatch.Diff> wordChanges = ValidationUtils.tokenDiff(originalCode, mutatedCode);
        return wordChanges.stream()
                .anyMatch(diff -> Stream.of("public", "final", "protected", "private", "static")
                        .anyMatch(operator -> diff.text.contains(operator))
                );
    }

    private static boolean mutantChangesMethodSignatures(final CompilationUnit orig, final CompilationUnit muta) {
        // Parse original and extract method signatures -> Set of string
        Set<String> cutMethodSignatures = new HashSet<>();
        Set<String> mutantMethodSignatures = new HashSet<>();

        for (TypeDeclaration<?> td : orig.getTypes()) {
            cutMethodSignatures.addAll(ValidationUtils.extractMethodSignaturesByType(td));
        }

        for (TypeDeclaration<?> td : muta.getTypes()) {
            mutantMethodSignatures.addAll(ValidationUtils.extractMethodSignaturesByType(td));
        }

        return !cutMethodSignatures.equals(mutantMethodSignatures);
    }

    private static boolean mutantChangesImportStatements(final CompilationUnit orig, final CompilationUnit muta) {
        // Parse original and extract method signatures -> Set of string

        Set<String> cutImportStatements = new HashSet<>(ValidationUtils.extractImportStatements(orig));
        Set<String> mutantImportStatements = new HashSet<>(ValidationUtils.extractImportStatements(muta));

        return !cutImportStatements.equals(mutantImportStatements);
    }

    private static boolean mutantAddsOrRenamesMethod(final CompilationUnit orig, final CompilationUnit muta) {
        Set<String> cutMethodSignatures = new HashSet<>();
        Set<String> mutantMethodSignatures = new HashSet<>();

        for (TypeDeclaration<?> td : orig.getTypes()) {
            cutMethodSignatures.addAll(ValidationUtils.extractMethodNamesByType(td));
        }

        for (TypeDeclaration<?> td : muta.getTypes()) {
            mutantMethodSignatures.addAll(ValidationUtils.extractMethodNamesByType(td));
        }

        return !cutMethodSignatures.containsAll(mutantMethodSignatures);
    }

    private static boolean mutantAddsOrChangesFieldNames(final CompilationUnit orig, final CompilationUnit muta) {
        // Parse original and extract method signatures -> Set of string
        Set<String> cutFieldNames = new HashSet<>();
        Set<String> mutantFieldNames = new HashSet<>();

        for (TypeDeclaration<?> td : orig.getTypes()) {
            cutFieldNames.addAll(ValidationUtils.extractFieldNamesByType(td));
        }

        for (TypeDeclaration<?> td : muta.getTypes()) {
            mutantFieldNames.addAll(ValidationUtils.extractFieldNamesByType(td));
        }

        return !cutFieldNames.equals(mutantFieldNames);
    }

    private static Optional<List<String>> ternaryAdded(List<AbstractDelta<String>> diff) {
        final Pattern pattern = Pattern.compile(".*\\?.*:.*");
        return ValidationUtils.checkLineDiff(diff, l -> pattern.matcher(l.toString()).find());
    }

}

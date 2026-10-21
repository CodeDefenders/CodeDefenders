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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.util.JavaParserUtils;
import org.xnap.commons.i18n.I18n;

import com.github.difflib.patch.DeltaType;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;

@ApplicationScoped
public class MutantValidator {

    /**
     * Checks if a mutant follows the rules of a rule set.
     *
     * @param originalCode The code of the original CuT
     * @param mutatedCode  The mutated code
     * @param ruleSet      The ruleset the mutant is validated against
     * @param ignoredRules These rules are ignored for this validation.
     * @return A {@link CodeValidationResult} containing information on all rules this mutant violated
     */
    public CodeValidationResult validateMutant(String originalCode, String mutatedCode,
                                               MutantValidationRuleSet ruleSet, I18n i18n, MutantRule... ignoredRules) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);

        MutantValidationRuleSet tmpRuleSet = new MutantValidationRuleSet("tmp", ruleSet);
        for (MutantRule r : ignoredRules) {
            tmpRuleSet.removeRule(r);
        }

        result.add(checkCompilationRules(originalCode, mutatedCode, tmpRuleSet, i18n));

        // if only string literals were changed
        if (ValidationUtils.onlyLiteralsChanged(originalCode, mutatedCode)) {
            return result;
        }

        result.add(checkCodeRules(originalCode, mutatedCode, tmpRuleSet));
        result.add(checkLineDiffRules(originalCode, mutatedCode, tmpRuleSet));
        result.add(checkInsertionRules(originalCode, mutatedCode, tmpRuleSet));

        return result;
    }

    private CodeValidationResult checkCompilationRules(String originalCode, String mutatedCode,
                                                       MutantValidationRuleSet ruleSet, I18n i18n) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);

        Optional<CompilationUnit> originalParseResult = JavaParserUtils.parse(originalCode);
        Optional<CompilationUnit> mutatedParseResult = JavaParserUtils.parse(mutatedCode);

        if (originalParseResult.isEmpty() || mutatedParseResult.isEmpty()) {
            result.setFailedParsing();
            return result;
        }

        CompilationUnit originalCU = originalParseResult.get();
        CompilationUnit mutatedCU = mutatedParseResult.get();

        for (MutantRule rule : ruleSet.getRules()) {
            result.add(rule.fails(originalCU, mutatedCU, i18n));
        }

        return result;
    }

    private CodeValidationResult checkCodeRules(String originalCode, String mutatedCode,
                                                MutantValidationRuleSet ruleSet) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);
        for (MutantRule rule : ruleSet.getRules()) {
            if (rule.fails(originalCode, mutatedCode)) {
                result.add(rule);
            }
        }

        return result;
    }

    private CodeValidationResult checkLineDiffRules(String originalCode, String mutatedCode,
                                                    MutantValidationRuleSet ruleSet) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);
        var lineDiff = ValidationUtils.getDeltas(originalCode, mutatedCode);
        // line-level diff
        for (MutantRule rule : ruleSet.getRules()) {
            result.add(rule.fails(lineDiff));
        }

        return result;
    }

    private CodeValidationResult checkInsertionRules(String originalCode, String mutatedCode,
                                                     MutantValidationRuleSet ruleSet) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);

        // Runs character-level diff match patch between the two Strings to see if there are any differences.
        DiffMatchPatch dmp = new DiffMatchPatch();
        final String text1 = originalCode.trim().replace("\n", "").replace("\r", "");
        final String text2 = mutatedCode.trim().replace("\n", "").replace("\r", "");
        LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(text1, text2, true);
        // check if there is any change
        for (DiffMatchPatch.Diff d : changes) {
            if (d.operation != DiffMatchPatch.Operation.EQUAL) {
                if (d.operation == DiffMatchPatch.Operation.INSERT) {
                    String stmtString = String.format("{ %s }", d.text);
                    Optional<BlockStmt> parseResult = JavaParserUtils.parse(
                            stmtString, JavaParserUtils.defaultParser()::parseBlock);
                    parseResult.ifPresent(blockStmt -> result.add(validateInsertionAST(ruleSet, blockStmt)));

                    // remove whitespaces
                    String diff2 = d.text.replaceAll("\\s+", "");

                    for (MutantRule rule : ruleSet.getRules()) {
                        if (rule.fails(diff2)) {
                            result.add(rule);
                        }
                    }
                }
            }
        }

        //Create a line-level diff and check if the predicates return false for the original and true for the
        // replacement

        var lineDiff = com.github.difflib.DiffUtils.diff(
                List.of(originalCode.replace("\r", "").split("\n")),
                List.of(mutatedCode.replace("\r", "").split("\n")));
        for (var delta : lineDiff.getDeltas()) {
            if (delta.getType() == DeltaType.CHANGE) {
                String originalString = String.join("\n", delta.getSource().getLines());
                String mutatedString = String.join("\n", delta.getTarget().getLines());

                //To be used for the error message
                final String unmodifiedMutatedLine = mutatedString;

                //Deltas may need some changes before being parsable

                //Close a single dangling closing bracket. Won't work for multiple dangling brackets.
                if (originalString.endsWith("{")) {
                    originalString += "}";
                }
                if (mutatedString.endsWith("{")) {
                    mutatedString += "}";
                }
                //Adding an empty block in the end will never hurt, but will fix problems for
                // one-line if-statements without curly brackets.
                //Enclosing the entire thing with {} enables it to be parsed as block.
                originalString = String.format("{%s{}}", originalString);
                mutatedString = String.format("{%s{}}", mutatedString);

                //Parse as blocks, so it doesn't need all the CompilationUnit boilerplate
                Optional<? extends Node> originalStmt = JavaParserUtils.parse(
                        originalString, JavaParserUtils.defaultParser()::parseBlock);
                Optional<? extends Node> mutatedStmt = JavaParserUtils.parse(
                        mutatedString, JavaParserUtils.defaultParser()::parseBlock);

                //Parse as an entire CompilationUnit. Only relevant if the entire class is one line?
                // Could probably be optimized to only check this when relevant
                if (originalStmt.isEmpty()) {
                    originalString = originalString.substring(1, originalString.length() - 3);
                    originalStmt = JavaParserUtils.parse(
                            originalString, JavaParserUtils.defaultParser()::parse);
                }
                if (mutatedStmt.isEmpty()) {
                    mutatedString = mutatedString.substring(1, mutatedString.length() - 3);
                    mutatedStmt = JavaParserUtils.parse(
                            mutatedString, JavaParserUtils.defaultParser()::parse);
                }

                if (originalStmt.isPresent() && mutatedStmt.isPresent()) {
                    for (MutantRule rule : ruleSet.getRules()) {

                        //Debugging convenience for breakpoints
                        //if (rule == MutantValidationRules.prohibitedBitwiseOperators) {
                        //    int x = 2;
                        //}

                        List<Node> suspiciousNodesInMutant = new ArrayList<>();
                        mutatedStmt.get().walk(n -> {
                            if (rule.fails(n)) {
                                suspiciousNodesInMutant.add(n);
                            }
                        });
                        AtomicInteger suspiciousNodesInOriginal = new AtomicInteger(0);
                        if (!suspiciousNodesInMutant.isEmpty()) {
                            originalStmt.get().walk(n -> {
                                if (rule.fails(n)) {
                                    suspiciousNodesInOriginal.incrementAndGet();
                                }
                            });
                        }
                        if (suspiciousNodesInOriginal.get() < suspiciousNodesInMutant.size()) {
                            result.add(rule, unmodifiedMutatedLine);
                        }

                        //if (!rule.fails(originalStmt.get()) && rule.fails(mutatedStmt.get())) {
                        //    result.add(rule);
                        //}
                    }
                }

            }
        }

        return result;
    }

    private CodeValidationResult validateInsertionAST(
            MutantValidationRuleSet ruleSet, BlockStmt insertionBlock) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);
        insertionBlock.walk(n -> {
            for (MutantRule rule : ruleSet.getRules()) {
                if (rule.fails(n)) {
                    result.add(rule, n);
                }
            }
        });
        return result;
    }
}

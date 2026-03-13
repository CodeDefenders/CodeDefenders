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

import java.util.LinkedList;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;

@ApplicationScoped
public class MutantValidator {

    /**
     * Checks if a mutant follows the rules of a rule set.
     *
     * @param originalCode The code of the original CuT
     * @param mutatedCode The mutated code
     * @param ruleSet The ruleset the mutant is validated against
     * @return A {@link CodeValidationResult} containing information on all rules this mutant violated
     */
    public CodeValidationResult validateMutant(String originalCode, String mutatedCode,
                                                      MutantValidationRuleSet ruleSet) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);

        result.add(checkCompilationRules(originalCode, mutatedCode, ruleSet));

        // if only string literals were changed
        if (ValidationUtils.onlyLiteralsChanged(originalCode, mutatedCode)) {
            return result;
        }

        result.add(checkCodeRules(originalCode, mutatedCode, ruleSet));
        result.add(checkLineDiffRules(originalCode, mutatedCode, ruleSet));
        result.add(checkInsertionRules(originalCode, mutatedCode, ruleSet));

        return result;
    }

    private CodeValidationResult checkCompilationRules(String originalCode, String mutatedCode,
                                                              MutantValidationRuleSet ruleSet) {
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
            result.add(rule.fails(originalCU, mutatedCU));
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

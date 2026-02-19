package org.codedefenders.validation.code;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;

public class MutantValidator {

    /**
     * Checks if a mutant follows the rules of a rule set.
     *
     * @param originalCode The code of the original CuT
     * @param mutatedCode The mutated code
     * @param ruleSet The ruleset the mutant is validated against
     * @return A {@link CodeValidationResult} containing information on all rules this mutant violated
     */
    public static CodeValidationResult validateMutant(String originalCode, String mutatedCode,
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

    private static CodeValidationResult checkCompilationRules(String originalCode, String mutatedCode,
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
            if (rule.fails(originalCU, mutatedCU)) {
                result.add(rule);
            }
        }

        return result;
    }

    private static CodeValidationResult checkCodeRules(String originalCode, String mutatedCode,
                                                       MutantValidationRuleSet ruleSet) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);
        for (MutantRule rule : ruleSet.getRules()) {
            if (rule.fails(originalCode, mutatedCode)) {
                result.add(rule);
            }
        }

        return result;
    }

    private static CodeValidationResult checkLineDiffRules(String originalCode, String mutatedCode,
                                                           MutantValidationRuleSet ruleSet) {
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);

        // line-level diff
        List<List<String>> originalLines = ValidationUtils.getOriginalLines(originalCode, mutatedCode);
        List<List<String>> changedLines = ValidationUtils.getChangedLines(originalCode, mutatedCode);
        if (originalLines.size() != changedLines.size()) {
            throw new RuntimeException("originalLines: " + originalLines + ", changedLines:" + changedLines);
        }

        for (MutantRule rule : ruleSet.getRules()) {
            result.add(rule.fails(originalLines, changedLines));
        }

        return result;
    }

    private static CodeValidationResult checkInsertionRules(String originalCode, String mutatedCode,
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

    private static CodeValidationResult validateInsertionAST(
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

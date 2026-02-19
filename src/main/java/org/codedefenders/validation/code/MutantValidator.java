package org.codedefenders.validation.code;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;

public class MutantValidator {
    public static CodeValidationResult validateMutantGetMessage(String originalCode, String mutatedCode,
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

        // line-level diff
        List<List<String>> originalLines = ValidationUtils.getOriginalLines(originalCode, mutatedCode);
        List<List<String>> changedLines = ValidationUtils.getChangedLines(originalCode, mutatedCode);
        if (originalLines.size() != changedLines.size()) {
            throw new RuntimeException("originalLines: " + originalLines + ", changedLines:" + changedLines);
        }

        for (MutantRule rule : ruleSet.getRules()) {
            if (rule.fails(originalCU, mutatedCU)) {
                result.add(rule);
            }
        }

        // if only string literals were changed
        if (ValidationUtils.onlyLiteralsChanged(originalCode, mutatedCode)) {
            return result;
        }

        for (MutantRule rule : ruleSet.getRules()) {
            if (rule.fails(originalCode, mutatedCode)) {
                result.add(rule);
            }
            result.add(rule.fails(originalLines, changedLines));
        }

        // Runs character-level diff match patch between the two Strings to see if there are any differences.
        DiffMatchPatch dmp = new DiffMatchPatch();
        final String text1 = originalCode.trim().replace("\n", "").replace("\r", "");
        final String text2 = mutatedCode.trim().replace("\n", "").replace("\r", "");
        LinkedList<DiffMatchPatch.Diff> changes = dmp.diffMain(text1, text2, true);
        // check if there is any change
        for (DiffMatchPatch.Diff d : changes) {
            if (d.operation != DiffMatchPatch.Operation.EQUAL) {
                if (d.operation == DiffMatchPatch.Operation.INSERT) {
                    result.add(validInsertion(d.text, ruleSet));
                }
            }
        }

        return result;
    }

    private static CodeValidationResult validInsertion(String diff,
                                                       MutantValidationRuleSet ruleSet) {
        String stmtString = String.format("{ %s }", diff);
        CodeValidationResult result = new CodeValidationResult(CodeValidationResult.Type.MUTANT);
        Optional<BlockStmt> parseResult = JavaParserUtils.parse(
                stmtString, JavaParserUtils.defaultParser()::parseBlock);
        if (parseResult.isPresent()) {
            // TODO Should this called always and not only for checking if there's validInsertion ?
            result.add(validateInsertionAST(ruleSet, parseResult.get()));

        } else {
            // Ignore if we can't parse the diff. It shouldn't compile, so it will fail in the next step.
        }

        // remove whitespaces
        String diff2 = diff.replaceAll("\\s+", "");

        for (MutantRule rule : ruleSet.getRules()) {
            if (rule.fails(diff2)) {
                result.add(rule);
            }
        }
        return result;
        //return ValidationMessage.MUTANT_VALIDATION_SUCCESS;
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

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
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.misc.WeldInit;
import org.codedefenders.util.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.codedefenders.game.AssertionLibrary.JUNIT4_HAMCREST;
import static org.codedefenders.util.ResourceUtils.loadResource;
import static org.codedefenders.util.Constants.DEFAULT_NB_ASSERTIONS;
import static org.codedefenders.validation.code.MutantValidator.validateMutant;
import static org.codedefenders.validation.code.TestValidator.validateTestCode;
import static org.codedefenders.validation.code.DefaultRuleSets.MODERATE;
import static org.codedefenders.validation.code.DefaultRuleSets.RELAXED;
import static org.codedefenders.validation.code.DefaultRuleSets.STRICT;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_ADDS_OR_RENAMES_FIELD;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_ADDS_OR_RENAMES_METHOD;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_BITWISE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_CALL_RANDOM;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_CALL_SYSTEM;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_CLASS;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_COMMENT;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_CONDITIONALS;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_IDENTICAL;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_IMPORT_STATEMENT;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_INSTANCEOF;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_LOGIC;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_LOOPS;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_METHOD_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_ONLY_COMMENT_CHANGES;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_PACKAGE;
import static org.codedefenders.validation.code.ValidationMessage.VALIDATION_FAILED_PARSING;
import static org.codedefenders.validation.code.ValidationMessage.VALIDATION_SUCCESS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CodeValidatorTest {

    /**
     * Resource directory path for this test.
     */
    private static final String RESOURCE_DIR = "org/codedefenders/validation/code/CodeValidator/";

    /**
     * Loads the {@code original.txt} file from a subdirectory path given by {@code name} below the {@code mutants} directory
     * in the {@link #RESOURCE_DIR}.
     */
    // TODO: Could be extracted into utility class (for tests)?!
    private static String loadMutantOriginal(String name) {
        return loadResource(RESOURCE_DIR, "mutants/" + name + "/original.txt");
    }

    /**
     * Loads the {@code mutated.txt} file from a subdirectory path given by {@code name} below the {@code mutants} directory
     * in the {@link #RESOURCE_DIR}.
     */
    // TODO: Could be extracted into utility class (for tests)?!
    private static String loadMutantMutated(String name) {
        return loadResource(RESOURCE_DIR, "mutants/" + name + "/mutated.txt");
    }

    /**
     * Loads the file given by {@code name} below the {@code tests} directory in the {@link #RESOURCE_DIR}.
     */
    // TODO: Could be extracted into utility class (for tests)?!
    private static String loadTest(String name) {
        return loadResource(RESOURCE_DIR, "tests/" + name + ".txt");
    }


    private static class ValidTestArgumentSource implements ArgumentsProvider {
        /**
         * Creates a {@link Stream} consisting of exactly one {@link Arguments}.
         *
         * <p>The {@link Arguments} contains the {@code testFile} (without extension), {@code maxNumberOfAssertions}, the {@code assertionLibrary}}.
         */
        private Stream<Arguments> testCase(String test, int maxNumberOfAssertions, AssertionLibrary assertionLibrary) {
            return Stream.of(arguments(test, maxNumberOfAssertions, assertionLibrary));
        }

        /**
         * Same as {@link #testCase(String, int, AssertionLibrary)} but uses default values for
         * {@code maxNumberOfAssertions} and {@code assertionLibrary}.
         */
        private Stream<Arguments> testCase(String test) {
            return testCase(test, DEFAULT_NB_ASSERTIONS, JUNIT4_HAMCREST);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            // Please order the testCase(s) alphabetically according to their test, so it is easier to match
            // them with their associated directory
            return Stream.of(
                    testCase("valid/compileError"),
                    testCase("valid/infiniteParserRecursionWithSingleTokens"),
                    testCase("valid/singleAssert")
            ).flatMap(Function.identity());
        }
    }

    @ParameterizedTest(name = "[{index}] Validating test {0} with max {1} assertions and assertion library {2} is successful")
    @ArgumentsSource(ValidTestArgumentSource.class)
    public void testValidateTestCodeGetMessageContainsNoValidationErrors(String test, int maxNumberOfAssertions,
                                                                         AssertionLibrary assertionLibrary) {
        String testCode = loadTest(test);

        CodeValidationResult actual = validateTestCode(testCode, maxNumberOfAssertions, assertionLibrary);

        assertThat(actual.isValid()).isTrue();
    }

    private static class InvalidTestArgumentSource implements ArgumentsProvider {
        /**
         * Creates a {@link Stream} consisting of exactly one {@link Arguments}.
         *
         * <p>The {@link Arguments} contains the {@code testFile} (without extension), {@code maxNumberOfAssertions}, {@code assertionLibrary} and a {@link List}
         * created from the {@code expectedValidationMessage} concatenated (if present) with the {@code otherExpectedValidationMessages}.
         */
        private Stream<Arguments> testCase(String testFile, int maxNumberOfAssertions,
                                           AssertionLibrary assertionLibrary,
                                           String expectedValidationMessage, String... otherExpectedValidationMessages) {
            return Stream.of(arguments(testFile, maxNumberOfAssertions, assertionLibrary, Stream.concat(Stream.of(expectedValidationMessage), Stream.of(otherExpectedValidationMessages)).collect(Collectors.toList())));
        }

        /**
         * Same as {@link #testCase(String, int, AssertionLibrary, String, String...)} but uses default values for
         * {@code maxNumberOfAssertions} and {@code assertionLibrary}.
         */
        private Stream<Arguments> testCase(String test, String expectedValidationMessage,
                                           String... otherExpectedValidationMessages) {
            return testCase(test, DEFAULT_NB_ASSERTIONS, JUNIT4_HAMCREST, expectedValidationMessage, otherExpectedValidationMessages);
        }

        /**
         * Same as {@link #testCase(String, int, AssertionLibrary, String, String...)} but uses a default value for
         * {@code assertionLibrary}.
         */
        private Stream<Arguments> testCase(String test, int maxNumberOfAssertions, String expectedValidationMessage,
                                           String... otherExpectedValidationMessages) {
            return testCase(test, maxNumberOfAssertions, JUNIT4_HAMCREST, expectedValidationMessage, otherExpectedValidationMessages);
        }

        /**
         * Same as {@link #testCase(String, int, AssertionLibrary, String, String...)} but uses a default value for
         * {@code maxNumberOfAssertions}.
         */
        private Stream<Arguments> testCase(String test, AssertionLibrary assertionLibrary,
                                           String expectedValidationMessage, String... otherExpectedValidationMessages) {
            return testCase(test, DEFAULT_NB_ASSERTIONS, assertionLibrary, expectedValidationMessage, otherExpectedValidationMessages);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            // Please order the testCase(s) alphabetically according to their test, so it is easier to match
            // them with their associated directory
            return Stream.of(
                    testCase("invalid/assertionsJunitFive", 4, "You used more than 4 assertions"),
                    testCase("invalid/assertionsJunitThree", 2, "You used more than 2 assertions"),
                    testCase("invalid/EmptyTest", "The test is empty."),
                    testCase("invalid/TestWithIf", "Conditional statements are not allowed"),
                    testCase("invalid/TestWithSystemCall", "You have called a package you may not call."),
                    testCase("invalid/TestWithSystemCall2", "You have called a package you may not call."),
                    testCase("invalid/TestWithSystemCall3", "You have called a package you may not call."),
                    testCase("invalid/TwoClasses", "You cannot create a second class."),
                    testCase("invalid/TwoTests", "You cannot create a new method.")
            ).flatMap(Function.identity());
        }
    }

    @ParameterizedTest(name = "[{index}] Validating test {0} with max {1} assertions and assertion library {2} results in one of {3}")
    @ArgumentsSource(InvalidTestArgumentSource.class)
    public void testValidateTestCodeGetMessageContainsValidationError(String test, int maxNumberOfAssertions,
                                                                      AssertionLibrary assertionLibrary, List<String> expectedValidationMessages) {
        String testCode = loadTest(test);

        CodeValidationResult actual = validateTestCode(testCode, maxNumberOfAssertions, assertionLibrary);

        assertThat(actual.isValid()).isFalse();
        for (String expected : expectedValidationMessages) {
            assertThat(actual.toString()).contains(expected);
        }

    }


    private static class MutantsArgumentSource implements ArgumentsProvider {
        /**
         * Creates a {@link Stream} consisting of exactly one {@link Arguments}.
         *
         * <p>The {@link Arguments} contains the {@code mutantDirectory}, {@code ruleSet}, and a {@link List}
         * created from the {@code validationMessage} concatenated (if present) with the {@code validationMessages}.
         */
        private Stream<Arguments> testCase(String mutantDirectory, MutantValidationRuleSet ruleSet,
                                           String validationMessage, String... validationMessages) {
            return Stream.of(arguments(mutantDirectory, ruleSet, Stream.concat(Stream.of(validationMessage), Stream.of(validationMessages)).collect(Collectors.toList())));
        }

        /**
         * Creates a {@link Stream} of {@link Arguments}.
         *
         * <p>If {@code expectedValidationMessage} is {@link ValidationMessage#VALIDATION_SUCCESS} it will generate
         * arguments with {@code validationMessage} {@link ValidationMessage#VALIDATION_SUCCESS} for all
         * {@link MutantValidationRuleSet}s that are less strict than the given {@code upToIncludingLevel}.
         * <br>It this case we also expect {@code otherExpectedValidationMessagesOnFailure} to be empty (there is only a
         * single return value that indicates success).
         * <br>See: {@link #testCasesSucceedUpTo(String, MutantValidationRuleSet)}.
         *
         * <p>If {@code expectedValidationMessage} is not {@link ValidationMessage#VALIDATION_SUCCESS} it will
         * generate arguments with the given {@code validationMessage} and {@code otherExpectedValidationMessagesOnFailure}
         * for all {@link MutantValidationRuleSet}s that are stricter than the given {@code upToIncludingLevel} and additionally
         * arguments with {@link ValidationMessage#VALIDATION_SUCCESS} for the {@link MutantValidationRuleSet}s that
         * are less strict.
         * <br>See: {@link #testCasesFailUpTo(String, MutantValidationRuleSet, String, String...)}
         */
        private Stream<Arguments> testCases(String mutantDirectory, MutantValidationRuleSet upToIncludingLevel,
                                            String expectedValidationMessage,
                                            String... otherExpectedValidationMessagesOnFailure) {
            if (expectedValidationMessage.equals(VALIDATION_SUCCESS)) {
                assume().that(otherExpectedValidationMessagesOnFailure).asList().isEmpty();

                return testCasesSucceedUpTo(mutantDirectory, upToIncludingLevel);
            } else {
                return testCasesFailUpTo(mutantDirectory, upToIncludingLevel, expectedValidationMessage, otherExpectedValidationMessagesOnFailure);
            }
        }

        /**
         * Creates a {@link Stream} of {@link Arguments}, one argument for each {@link MutantValidationRuleSet}
         * that is equal
         * or less strict than the given {@code succeedsUpToIncludingLevel}.
         * This only follows the {@link MutantValidationRuleSet#getParent()} order.
         *
         * <p>Example:
         * <br>If {@code succeedsUpToIncludingLevel} is {@link DefaultRuleSets#MODERATE} it will create arguments
         * with an expected return value of {@link ValidationMessage#VALIDATION_SUCCESS} for
         * {@link DefaultRuleSets#RELAXED} and {@link DefaultRuleSets#MODERATE}.
         */
        private Stream<Arguments> testCasesSucceedUpTo(String mutantDirectory,
                                                       MutantValidationRuleSet succeedsUpToIncludingLevel) {
            List<MutantValidationRuleSet> sets = new ArrayList<>();
            for (MutantValidationRuleSet i = succeedsUpToIncludingLevel; i != null; i = i.getParent()) {
                sets.add(i);
            }

            return sets.stream()
                    .flatMap(level -> testCase(mutantDirectory, level, VALIDATION_SUCCESS));
        }

        /**
         * Creates a {@link Stream} of {@link Arguments}, one argument for each Ruleset in the
         * {@link MutantValidationRuleSet#getParent()} ancestor and children chain.
         * <br>In the cases where the ruleset is an ancestor of the given {@code upToIncludingLevel}, it will generate an
         * argument that expects {@link ValidationMessage#VALIDATION_SUCCESS}.
         *
         * <p>This method assumes that {@link ValidationMessage#VALIDATION_SUCCESS} is neither passed as
         * {@code expectedValidationMessageOnFailure} nor contained in {@code otherExpectedValidationMessagesOnFailure}.
         *
         * <p>Example:
         * <br>If {@code expectedValidationMessage} is {@link ValidationMessage#MUTANT_CONDITIONALS} and
         * {@code upToIncludingLevel} is {@link DefaultRuleSets#MODERATE} then it will create arguments that expect
         * {@link ValidationMessage#MUTANT_CONDITIONALS} for {@link DefaultRuleSets#STRICT} and {@link DefaultRuleSets#MODERATE}
         * and another argument that expects {@link ValidationMessage#VALIDATION_SUCCESS} for {@link DefaultRuleSets#RELAXED}.
         */
        private Stream<Arguments> testCasesFailUpTo(String mutantDirectory, MutantValidationRuleSet upToIncludingLevel,
                                                    String expectedValidationMessageOnFailure,
                                                    String... otherExpectedValidationMessagesOnFailure) {
            assume().that(expectedValidationMessageOnFailure).isNotEqualTo(VALIDATION_SUCCESS);
            assume().that(otherExpectedValidationMessagesOnFailure).asList().doesNotContain(VALIDATION_SUCCESS);

            List<Arguments> result = new ArrayList<>();
            upToIncludingLevel.getDescendants().stream()
                    .flatMap(l -> testCase(mutantDirectory, l, expectedValidationMessageOnFailure, otherExpectedValidationMessagesOnFailure))
                    .forEach(result::add);
            upToIncludingLevel.getAncestors().stream()
                    .flatMap(l -> testCase(mutantDirectory, l, VALIDATION_SUCCESS))
                    .forEach(result::add);
            return result.stream();
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            // Please order the testCase(s) alphabetically according to their mutantDirectory, so it is easier to match
            // them with their associated directory
            return Stream.of(
                    // MUTANT_VALIDATION_CLASS_SIGNATURE - Is always forbidden
                    testCase("classSignature/changedClassToFinalClass", RELAXED, MUTANT_CLASS),
                    testCase("classSignature/changedClassToPublicClass", RELAXED, MUTANT_CLASS),
                    testCase("classSignature/changedFinalClassToClass", RELAXED, MUTANT_CLASS),
                    testCase("classSignature/changedInnerPublicClassToInnerProtectedClass", RELAXED, MUTANT_CLASS),
                    testCase("classSignature/changedPublicClassToClass", RELAXED, MUTANT_CLASS),
                    testCase("classSignature/changedPublicClassToPublicFinalClass", RELAXED, MUTANT_CLASS),

                    // MUTANT_VALIDATION_COMMENT
                    testCases("comments/addedAnotherCommentAtEndOfLine", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),
                    testCases("comments/addedAnotherCommentInNewLine", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),
                    testCases("comments/addedMultiLineCommentAtEndOfLine", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),
                    testCases("comments/addedSingleLineCommentAtEndOfLine", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),
                    testCases("comments/addedSingleLineCommentInNewLine", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),
                    testCases("comments/modifiedComment", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),
                    testCases("comments/modifiedCommentAndChangedCode", MODERATE, MUTANT_COMMENT),
                    testCases("comments/modifiedCommentInTheLineAfterUnmodifiedComment", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),
                    testCases("comments/modifiedMultiLineComment", RELAXED, MUTANT_IDENTICAL, MUTANT_COMMENT, MUTANT_ONLY_COMMENT_CHANGES),

                    // MUTANT_VALIDATION_FIELD_NAME - is only forbidden with STRICT validation
                    testCases("fields/changedName", RELAXED, MUTANT_ADDS_OR_RENAMES_FIELD),
                    testCases("fields/changedNameOfNested", RELAXED, MUTANT_ADDS_OR_RENAMES_FIELD),

                    testCases("identical/newLineAdded", RELAXED, MUTANT_IDENTICAL),
                    testCases("identical/same", RELAXED, MUTANT_IDENTICAL),
                    testCases("identical/spaceAdded", RELAXED, MUTANT_IDENTICAL),

                    // MUTANT_VALIDATION_IMPORT_STATEMENT
                    testCase("imports/importRemoved", STRICT, MUTANT_IMPORT_STATEMENT),

                    // Mutating instanceOf is only forbidden with STRICT validation
                    testCases("instanceOf/changedMultiple", STRICT, MUTANT_INSTANCEOF),
                    testCases("instanceOf/changedOne", STRICT, MUTANT_INSTANCEOF),

                    // MUTANT_VALIDATION_SUCCESS - Short circuit on only literal changes
                    testCase("literals/addedSpaceToNonEmptyString", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithAccessModifiers", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitShiftLeft", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitShiftRight", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitShiftRightUnsigned", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitwiseAnd", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitwiseOr", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithControlCharacters", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithSingleSpace", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedMultipleStrings", STRICT, VALIDATION_SUCCESS),
                    testCase("literals/changedSingleCharToSemicolon", STRICT, VALIDATION_SUCCESS),

                    // Logical Operator Added 01 - Only forbidden with STRICT and MODERATE validation
                    testCases("logicalOperators/addedLogicalAnd", MODERATE, MUTANT_LOGIC),
                    // Logical Operator Added on Lift Class - Only forbidden with STRICT and MODERATE validation
                    testCases("logicalOperators/addedLogicalAnd_LiftClass", MODERATE, MUTANT_LOGIC),

                    // Ternary Operator Added - Only forbidden with STRICT and MODERATE validation
                    testCases("logicalOperators/addedTernaryOperator01", MODERATE, MUTANT_CONDITIONALS),
                    testCases("logicalOperators/addedTernaryOperator02", MODERATE, MUTANT_CONDITIONALS),
                    // TODO: Same as 01, but without newlines
                    testCases("logicalOperators/addedTernaryOperator03", MODERATE, MUTANT_CONDITIONALS),
                    // TODO: Same as 02, but without newlines
                    testCases("logicalOperators/addedTernaryOperator04", MODERATE, MUTANT_CONDITIONALS),

                    // MUTANT_VALIDATION_METHOD_SIGNATURE - is only forbidden with STRICT validation
                    testCases("methodSignature/changedAccessFromPublicToPackagePrivate", STRICT, MUTANT_METHOD_SIGNATURE),
                    testCases("methodSignature/changedAccessFromPublicToPrivate", STRICT, MUTANT_METHOD_SIGNATURE),
                    testCases("methodSignature/changedAccessFromPublicToProtected", STRICT, MUTANT_METHOD_SIGNATURE),
                    testCases("methodSignature/changedConstructor", STRICT, MUTANT_METHOD_SIGNATURE),
                    testCases("methodSignature/changedConstructorOfInnerClass", STRICT, MUTANT_METHOD_SIGNATURE),
                    testCases("methodSignature/changedPrivateMethod", STRICT, MUTANT_METHOD_SIGNATURE),
                    testCases("methodSignature/changedProtectedMethod", RELAXED, MUTANT_METHOD_SIGNATURE, MUTANT_ADDS_OR_RENAMES_METHOD),
                    testCases("methodSignature/changedPublicMethod", STRICT, MUTANT_METHOD_SIGNATURE),

                    // Bit Shifts are only forbidden with STRICT validation
                    testCases("otherInvalid/addedBitShift01", STRICT, MUTANT_BITWISE),
                    testCases("otherInvalid/addedBitShift02", STRICT, MUTANT_BITWISE),

                    // Some other strange cases
                    testCases("otherInvalid/addedIfSameLine01", MODERATE, MUTANT_CONDITIONALS),
                    // TODO: Same as 01, but without newlines
                    testCases("otherInvalid/addedIfSameLine02", MODERATE, MUTANT_CONDITIONALS),
                    // TODO: Look at this one too
                    testCases("otherInvalid/addedSecondIfSameLine01", MODERATE, MUTANT_CONDITIONALS),
                    // TODO: Same as 01, but without newlines
                    testCases("otherInvalid/addedSecondIfSameLine02", MODERATE, MUTANT_CONDITIONALS),
                    // TODO: Look at this one again
                    testCases("otherInvalid/addedWhileSameLine01", MODERATE, MUTANT_LOOPS),
                    // TODO: Same as 01 but without newlines
                    testCases("otherInvalid/addedWhileSameLine02", MODERATE, MUTANT_LOOPS),

                    // MUTANT_VALIDATION_PACKAGE_SIGNATURE - Is always forbidden
                    testCase("packageSignature/addedPackage", RELAXED, MUTANT_PACKAGE),
                    testCase("packageSignature/changedPackage", RELAXED, MUTANT_PACKAGE),

                    // MUTANT_VALIDATION_OPERATORS
                    testCase("systemCalls/addCallToMath_random_toLine01", STRICT, MUTANT_CALL_RANDOM),
                    testCase("systemCalls/addedCallToJavaUtilRandom_nextInt", STRICT, MUTANT_CALL_RANDOM),
                    testCase("systemCalls/addedCallToSystem_currentTimeMillis", STRICT, MUTANT_CALL_SYSTEM),
                    testCase("systemCalls/replacedWithCallToMath_random01", STRICT, MUTANT_CALL_RANDOM),
                    testCase("systemCalls/replacedWithCallToMath_random02", STRICT, MUTANT_CALL_RANDOM),
                    testCase("systemCalls/System_exit01", STRICT, MUTANT_CALL_SYSTEM),
                    testCase("systemCalls/System_exit02", STRICT, MUTANT_CALL_SYSTEM),

                    testCase("systemCalls/withExistingMath_random01", STRICT, MUTANT_CALL_RANDOM),
                    //testCase("systemCalls/withExistingMath_random02", STRICT, MUTANT_CALL_RANDOM), TODO Fails
                    testCase("systemCalls/withExistingMath_random03", STRICT, VALIDATION_SUCCESS),

                    // MUTANT_VALIDATION_SUCCESS
                    testCase("valid/addedSecondStatementOnSingleLine", STRICT, VALIDATION_SUCCESS),
                    // Comment success
                    testCases("valid/changeAfterSlash", STRICT, VALIDATION_SUCCESS),
                    testCases("valid/changedFieldInitializer", STRICT, VALIDATION_SUCCESS),

                    testCase("valid/changeStringValue", STRICT, VALIDATION_SUCCESS),
                    testCase("valid/inlinedStringVariable01", STRICT, VALIDATION_SUCCESS),
                    testCase("valid/inlinedStringVariable02", STRICT, VALIDATION_FAILED_PARSING), //TODO What's the point of this class? Why the escape?

                    testCase("valid/instanceOfMultipleUnchanged", STRICT, VALIDATION_SUCCESS),
                    testCase("valid/instanceOfUnchanged", STRICT, VALIDATION_SUCCESS),

                    testCase("valid/liftDecrementTopFloorOnGetCurrentFloor", STRICT, VALIDATION_SUCCESS),
                    testCase("valid/liftTopFloorIsIncrementedByOneInConstructor", STRICT, VALIDATION_SUCCESS),
                    testCase("valid/simpleChange01", STRICT, VALIDATION_SUCCESS),
                    testCase("valid/simpleChange02", STRICT, VALIDATION_SUCCESS),

                    testCases("valid/withCommentInDifferentLine", STRICT, VALIDATION_SUCCESS),
                    testCases("valid/withCommentInSameLine", STRICT, VALIDATION_SUCCESS),
                    testCases("valid/withField", STRICT, VALIDATION_SUCCESS),
                    testCases("valid/withoutPackage", STRICT, VALIDATION_SUCCESS),
                    testCases("valid/withPackage", STRICT, VALIDATION_SUCCESS)
            ).flatMap(Function.identity());
        }
    }

    @ParameterizedTest(name = "[{index}] Validating mutant {0} on level {1} results in one of {2}")
    @ArgumentsSource(MutantsArgumentSource.class)
    public void testValidateMutantGetMessage(String mutant, MutantValidationRuleSet ruleSet,
                                             List<String> expectedValidationMessages) {
        try (var ignored = WeldInit.initWeld(new Class[]{}, false)) {
            String original = loadMutantOriginal(mutant);
            String mutated = loadMutantMutated(mutant);

            String actual = validateMutant(original, mutated, ruleSet).toString();

            String expectedRegex = expectedValidationMessages.stream().map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            //assertThat(actual).isIn(expectedValidationMessages);
            assertThat(actual).containsMatch(expectedRegex);
        }
    }


    @Nested
    public class GetMD5FromFileTests {

        @Test
        public void customMD5isTheSameForSameInput() {
            String originalMD5 = FileUtils.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/same/original.txt");
            String mutatedMD5 = FileUtils.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/same/mutated.txt");

            assume().that(originalMD5).isNotNull();
            assume().that(mutatedMD5).isNotNull();

            assertThat(originalMD5).isEqualTo(mutatedMD5);
        }

        @Test
        public void customMD5ignoresWhitespaceChanges() {
            String originalMD5 = FileUtils.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/spaceAdded/original.txt");
            String mutatedMD5 = FileUtils.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/spaceAdded/mutated.txt");

            assume().that(originalMD5).isNotNull();
            assume().that(mutatedMD5).isNotNull();

            assertThat(originalMD5).isEqualTo(mutatedMD5);
        }

        @Test
        public void customMD5returnsNullForMissingFile() {
            assertThat(FileUtils.getMD5FromFile("/nonexistent")).isNull();
        }
    }

    @Nested
    public class RemoveQuotedTests {

        @Test
        public void noQuotesRemoveSingleQuotes() {
            assertThat(ValidationUtils.removeQuoted("[Prefix][Postfix]", "'")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void noQuotesRemoveDoubleQuotes() {
            assertThat(ValidationUtils.removeQuoted("[Prefix][Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void singleQuotesOnlyRemoveDoubleQuotes() {
            assertThat(ValidationUtils.removeQuoted("[Prefix]'T'[Postfix]", "\"")).isEqualTo("[Prefix]'T'[Postfix]");
        }

        @Test
        public void doubleQuotesOnlyRemoveSingleQuotes() {
            assertThat(ValidationUtils.removeQuoted("[Prefix]\"Text\"[Postfix]", "'")).isEqualTo("[Prefix]\"Text\"[Postfix]");
        }

        @Test
        public void oneSingleQuotedCharacter() {
            assertThat(ValidationUtils.removeQuoted("[Prefix]'T'[Postfix]", "'")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void oneDoubleQuotedText() {
            assertThat(ValidationUtils.removeQuoted("[Prefix]\"Text\"[Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void oneSingleQuotedCharacterContainingEscapedSingleQuote() {
            assertThat(ValidationUtils.removeQuoted("[Prefix]'\\''[Postfix]", "'")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void oneDoubleQuotedTextContainingEscapedDoubleQuoteAtTheEnd() {
            assertThat(ValidationUtils.removeQuoted("[Prefix]\"Text\\\"\"[Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }

        @Disabled("public void org.codedefenders.validation.code.CodeValidatorTest$RemoveQuotedTests.oneDoubleQuotedTextContainingEscapedDoubleQuoteAtTheBeginning() is @Disabled\n"
                + "because removeQuoted can not deal properly with escaped quotes")
        @Test
        public void oneDoubleQuotedTextContainingEscapedDoubleQuoteAtTheBeginning() {
            assertThat(ValidationUtils.removeQuoted("[Prefix]\"\\\"Text\"[Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }
    }

    @Nested
    public class OnlyLiteralsChangedTests {

        @Test
        public void noQuotes() {
            assertThat(ValidationUtils.onlyLiteralsChanged("public class Test {}", "public class Test {}")).isTrue();
        }

        @Test
        public void simpleString() {
            assertThat(ValidationUtils.onlyLiteralsChanged("public class Test { String var = \"Text\"; }", "public class Test { String var = \"Changed Text\"; }")).isTrue();
        }

        @Test
        public void simpleChar() {
            assertThat(ValidationUtils.onlyLiteralsChanged("public class Test { char var = 'A'; }", "public class Test { char var = 'B'; }")).isTrue();
        }

        @Test
        public void oneNestedQuotationCharacter01() {
            String original = "public class Test { String var = \"Text\"; }";
            String mutated = "public class Test { String var = \"Changed Text\\\"\"; }";

            assertThat(ValidationUtils.onlyLiteralsChanged(original, mutated)).isTrue();
        }

        @Disabled("public void org.codedefenders.validation.code.CodeValidatorTest$OnlyLiteralsChangedTests.oneNestedQuotationCharacter02() is @Disabled\n"
                + "because onlyLiteralChanged can't properly deal with escaped quotes inside another quote")
        @Test
        public void oneNestedQuotationCharacter02() {
            String original = "public class Test { String var = \"\\\"Text\"; }";
            String mutated = "public class Test { String var = \"Changed Text\"; }";

            assertThat(ValidationUtils.onlyLiteralsChanged(original, mutated)).isTrue();
        }

        @Disabled("public void org.codedefenders.validation.code.CodeValidatorTest$OnlyLiteralsChangedTests.oneNestedQuotationCharacter03() is @Disabled\n"
                + "because onlyLiteralChanged can't properly deal with escaped quotes inside another quote")
        @Test
        public void oneNestedQuotationCharacter03() {
            String original = "public class Test { String var = \"\\\"Text\"; }";
            String mutated = "public class Test { String var = \"Changed\\\" Text\"; }";

            assertThat(ValidationUtils.onlyLiteralsChanged(original, mutated)).isTrue();
        }

        @Test
        public void oneNestedQuotationCharacter04() {
            String original = "public class Test { char var = '\"'; }";
            String mutated = "public class Test { char var = '\\''; }";

            assertThat(ValidationUtils.onlyLiteralsChanged(original, mutated)).isTrue();
        }
    }
}

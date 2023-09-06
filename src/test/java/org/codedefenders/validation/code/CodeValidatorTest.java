/*
 * Copyright (C) 2016-2019,2022 Code Defenders contributors
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codedefenders.game.AssertionLibrary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.google.common.truth.Correspondence;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.codedefenders.game.AssertionLibrary.JUNIT4_HAMCREST;
import static org.codedefenders.util.ResourceUtils.loadResource;
import static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS;
import static org.codedefenders.validation.code.CodeValidator.validateMutantGetMessage;
import static org.codedefenders.validation.code.CodeValidator.validateTestCodeGetMessage;
import static org.codedefenders.validation.code.CodeValidatorLevel.MODERATE;
import static org.codedefenders.validation.code.CodeValidatorLevel.RELAXED;
import static org.codedefenders.validation.code.CodeValidatorLevel.STRICT;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_CALLS;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_CLASS_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_COMMENT;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_FIELD_NAME;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_IDENTICAL;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_IMPORT_STATEMENT;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_LOGIC;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_LOGIC_INSTANCEOF;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_METHOD_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_OPERATORS;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_PACKAGE_SIGNATURE;
import static org.codedefenders.validation.code.ValidationMessage.MUTANT_VALIDATION_SUCCESS;
import static org.codedefenders.validation.code.ValidationMessage.MUTATION_IF_STATEMENT;
import static org.codedefenders.validation.code.ValidationMessage.MUTATION_WHILE_STATEMENT;
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

        List<String> actual = validateTestCodeGetMessage(testCode, maxNumberOfAssertions, assertionLibrary);

        assertThat(actual).isEmpty();
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
                    testCase("invalid/assertionsJunitFive", 4, "Test contains more than 4 assertions"),
                    testCase("invalid/assertionsJunitThree", 2, "Test contains more than 2 assertions"),
                    testCase("invalid/EmptyTest", "Test does not contain any valid statement."),
                    testCase("invalid/TestWithIf", "Test contains an invalid statement:"),
                    testCase("invalid/TestWithSystemCall", "Test contains a prohibited call to System."),
                    testCase("invalid/TestWithSystemCall2", "Test contains a prohibited call to java.io"),
                    testCase("invalid/TestWithSystemCall3", "Test contains a prohibited call to System."),
                    testCase("invalid/TwoClasses", "Invalid test suite contains more than one class declaration."),
                    testCase("invalid/TwoTests", "Invalid test suite contains more than one method declaration.")
            ).flatMap(Function.identity());
        }
    }

    @ParameterizedTest(name = "[{index}] Validating test {0} with max {1} assertions and assertion library {2} results in one of {3}")
    @ArgumentsSource(InvalidTestArgumentSource.class)
    public void testValidateTestCodeGetMessageContainsValidationError(String test, int maxNumberOfAssertions,
            AssertionLibrary assertionLibrary, List<String> expectedValidationMessages) {
        String testCode = loadTest(test);

        List<String> actual = validateTestCodeGetMessage(testCode, maxNumberOfAssertions, assertionLibrary);

        assertThat(actual)
                .comparingElementsUsing(Correspondence.from((String act, String exp) -> act.contains(exp), "contains"))
                .containsAtLeastElementsIn(expectedValidationMessages);
    }


    private static class MutantsArgumentSource implements ArgumentsProvider {
        /**
         * Creates a {@link Stream} consisting of exactly one {@link Arguments}.
         *
         * <p>The {@link Arguments} contains the {@code mutantDirectory}, {@code codeValidatorLevel}, and a {@link List}
         * created from the {@code validationMessage} concatenated (if present) with the {@code validationMessages}.
         */
        private Stream<Arguments> testCase(String mutantDirectory, CodeValidatorLevel codeValidatorLevel,
                ValidationMessage validationMessage, ValidationMessage... validationMessages) {
            return Stream.of(arguments(mutantDirectory, codeValidatorLevel, Stream.concat(Stream.of(validationMessage), Stream.of(validationMessages)).collect(Collectors.toList())));
        }

        /**
         * Creates a {@link Stream} of {@link Arguments}.
         *
         * <p>If {@code expectedValidationMessage} is {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS} it will generate
         * arguments with {@code validationMessage} {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS} for all
         * {@link CodeValidatorLevel}s that are less strict than the given {@code upToIncludingLevel}.
         * <br>It this case we also expect {@code otherExpectedValidationMessagesOnFailure} to be empty (there is only a
         * single return value that indicates success).
         * <br>See: {@link #testCasesSucceedUpTo(String, CodeValidatorLevel)}.
         *
         * <p>If {@code expectedValidationMessage} is not {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS} it will
         * generate arguments with the given {@code validationMessage} and {@code otherExpectedValidationMessagesOnFailure}
         * for all {@link CodeValidatorLevel}s that are stricter than the given {@code upToIncludingLevel} and additionally
         * arguments with {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS} for the {@link CodeValidatorLevel}s that
         * are less strict.
         * <br>See: {@link #testCasesFailUpTo(String, CodeValidatorLevel, ValidationMessage, ValidationMessage...)}
         */
        private Stream<Arguments> testCases(String mutantDirectory, CodeValidatorLevel upToIncludingLevel,
                ValidationMessage expectedValidationMessage,
                ValidationMessage... otherExpectedValidationMessagesOnFailure) {
            if (expectedValidationMessage.equals(MUTANT_VALIDATION_SUCCESS)) {
                assume().that(otherExpectedValidationMessagesOnFailure).asList().isEmpty();

                return testCasesSucceedUpTo(mutantDirectory, upToIncludingLevel);
            } else {
                return testCasesFailUpTo(mutantDirectory, upToIncludingLevel, expectedValidationMessage, otherExpectedValidationMessagesOnFailure);
            }
        }

        /**
         * Creates a {@link Stream} of {@link Arguments}, one argument for each {@link CodeValidatorLevel} that is equal
         * or stricter than the given {@code succeedsUpToIncludingLevel}.
         *
         * <p>Example:
         * <br>If {@code succeedsUpToIncludingLevel} is {@link CodeValidatorLevel#MODERATE} it will create arguments
         * with an expected return value of {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS} for
         * {@link CodeValidatorLevel#RELAXED} and {@link CodeValidatorLevel#MODERATE}.
         */
        private Stream<Arguments> testCasesSucceedUpTo(String mutantDirectory,
                CodeValidatorLevel succeedsUpToIncludingLevel) {
            return Arrays.stream(CodeValidatorLevel.values())
                    .filter(level -> level.compareTo(succeedsUpToIncludingLevel) <= 0)
                    .flatMap(level -> testCase(mutantDirectory, level, MUTANT_VALIDATION_SUCCESS));
        }

        /**
         * Creates a {@link Stream} of {@link Arguments}, one argument for each {@link CodeValidatorLevel}.
         * <br>In the cases where the level is less strict than the given {@code upToIncludingLevel}, it will generate an
         * argument that expects {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS}.
         *
         * <p>This method assumes that {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS} is neither passed as
         * {@code expectedValidationMessageOnFailure} nor contained in {@code otherExpectedValidationMessagesOnFailure}.
         *
         * <p>Example:
         * <br>If {@code expectedValidationMessage} is {@link ValidationMessage#MUTATION_IF_STATEMENT} and
         * {@code upToIncludingLevel} is {@link CodeValidatorLevel#MODERATE} then it will create arguments that expect
         * {@link ValidationMessage#MUTATION_IF_STATEMENT} for {@link CodeValidatorLevel#STRICT} and {@link CodeValidatorLevel#MODERATE}
         * and another argument that expects {@link ValidationMessage#MUTANT_VALIDATION_SUCCESS} for {@link CodeValidatorLevel#RELAXED}.
         */
        private Stream<Arguments> testCasesFailUpTo(String mutantDirectory, CodeValidatorLevel upToIncludingLevel,
                ValidationMessage expectedValidationMessageOnFailure,
                ValidationMessage... otherExpectedValidationMessagesOnFailure) {
            assume().that(expectedValidationMessageOnFailure).isNotEqualTo(MUTANT_VALIDATION_SUCCESS);
            assume().that(otherExpectedValidationMessagesOnFailure).asList().doesNotContain(MUTANT_VALIDATION_SUCCESS);

            return Arrays.stream(CodeValidatorLevel.values())
                    .sorted()
                    .flatMap(level -> {
                        if (level.compareTo(upToIncludingLevel) >= 0) {
                            return testCase(mutantDirectory, level, expectedValidationMessageOnFailure, otherExpectedValidationMessagesOnFailure);
                        } else {
                            return testCase(mutantDirectory, level, MUTANT_VALIDATION_SUCCESS);
                        }
                    });

        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            // Please order the testCase(s) alphabetically according to their mutantDirectory, so it is easier to match
            // them with their associated directory
            return Stream.of(
                    // MUTANT_VALIDATION_CLASS_SIGNATURE - Is always forbidden
                    testCase("classSignature/changedClassToFinalClass", RELAXED, MUTANT_VALIDATION_CLASS_SIGNATURE),
                    testCase("classSignature/changedClassToPublicClass", RELAXED, MUTANT_VALIDATION_CLASS_SIGNATURE),
                    testCase("classSignature/changedFinalClassToClass", RELAXED, MUTANT_VALIDATION_CLASS_SIGNATURE),
                    testCase("classSignature/changedInnerPublicClassToInnerProtectedClass", RELAXED, MUTANT_VALIDATION_CLASS_SIGNATURE),
                    testCase("classSignature/changedPublicClassToClass", RELAXED, MUTANT_VALIDATION_CLASS_SIGNATURE),
                    testCase("classSignature/changedPublicClassToPublicFinalClass", RELAXED, MUTANT_VALIDATION_CLASS_SIGNATURE),

                    // MUTANT_VALIDATION_COMMENT
                    testCases("comments/addedAnotherCommentAtEndOfLine", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("comments/addedAnotherCommentInNewLine", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("comments/addedMultiLineCommentAtEndOfLine", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("comments/addedSingleLineCommentAtEndOfLine", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("comments/addedSingleLineCommentInNewLine", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("comments/modifiedComment", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("comments/modifiedCommentAndChangedCode", MODERATE, MUTANT_VALIDATION_COMMENT),
                    testCases("comments/modifiedCommentInTheLineAfterUnmodifiedComment", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("comments/modifiedMultiLineComment", RELAXED, MUTANT_VALIDATION_IDENTICAL),

                    // MUTANT_VALIDATION_FIELD_NAME - is only forbidden with STRICT validation
                    testCases("fields/changedName", STRICT, MUTANT_VALIDATION_FIELD_NAME),
                    testCases("fields/changedNameOfNested", STRICT, MUTANT_VALIDATION_FIELD_NAME),

                    testCases("identical/newLineAdded", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("identical/same", RELAXED, MUTANT_VALIDATION_IDENTICAL),
                    testCases("identical/spaceAdded", RELAXED, MUTANT_VALIDATION_IDENTICAL),

                    // MUTANT_VALIDATION_IMPORT_STATEMENT
                    testCase("imports/importRemoved", STRICT, MUTANT_VALIDATION_IMPORT_STATEMENT),

                    // Mutating instanceOf is only forbidden with STRICT validation
                    testCases("instanceOf/changedMultiple", STRICT, MUTANT_VALIDATION_LOGIC_INSTANCEOF),
                    testCases("instanceOf/changedOne", STRICT, MUTANT_VALIDATION_LOGIC_INSTANCEOF),

                    // MUTANT_VALIDATION_SUCCESS - Short circuit on only literal changes
                    testCase("literals/addedSpaceToNonEmptyString", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithAccessModifiers", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitShiftLeft", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitShiftRight", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitShiftRightUnsigned", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitwiseAnd", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithBitwiseOr", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithControlCharacters", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedEmptyStringToStringWithSingleSpace", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedMultipleStrings", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("literals/changedSingleCharToSemicolon", STRICT, MUTANT_VALIDATION_SUCCESS),

                    // Logical Operator Added 01 - Only forbidden with STRICT and MODERATE validation
                    testCases("logicalOperators/addedLogicalAnd", MODERATE, MUTANT_VALIDATION_LOGIC),
                    // Logical Operator Added on Lift Class - Only forbidden with STRICT and MODERATE validation
                    testCases("logicalOperators/addedLogicalAnd_LiftClass", MODERATE, MUTANT_VALIDATION_LOGIC),

                    // Ternary Operator Added - Only forbidden with STRICT and MODERATE validation
                    testCases("logicalOperators/addedTernaryOperator01", MODERATE, MUTANT_VALIDATION_OPERATORS),
                    testCases("logicalOperators/addedTernaryOperator02", MODERATE, MUTANT_VALIDATION_OPERATORS),
                    // TODO: Same as 01, but without newlines
                    testCases("logicalOperators/addedTernaryOperator03", MODERATE, MUTANT_VALIDATION_OPERATORS),
                    // TODO: Same as 02, but without newlines
                    testCases("logicalOperators/addedTernaryOperator04", MODERATE, MUTANT_VALIDATION_OPERATORS),

                    // MUTANT_VALIDATION_METHOD_SIGNATURE - is only forbidden with STRICT validation
                    testCases("methodSignature/changedAccessFromPublicToPackagePrivate", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),
                    testCases("methodSignature/changedAccessFromPublicToPrivate", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),
                    testCases("methodSignature/changedAccessFromPublicToProtected", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),
                    testCases("methodSignature/changedConstructor", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),
                    testCases("methodSignature/changedConstructorOfInnerClass", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),
                    testCases("methodSignature/changedPrivateMethod", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),
                    testCases("methodSignature/changedProtectedMethod", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),
                    testCases("methodSignature/changedPublicMethod", STRICT, MUTANT_VALIDATION_METHOD_SIGNATURE),

                    // Bit Shifts are only forbidden with STRICT validation
                    testCases("otherInvalid/addedBitShift01", STRICT, MUTANT_VALIDATION_OPERATORS),
                    testCases("otherInvalid/addedBitShift02", STRICT, MUTANT_VALIDATION_OPERATORS),

                    // Some other strange cases
                    testCases("otherInvalid/addedIfSameLine01", MODERATE, MUTATION_IF_STATEMENT),
                    // TODO: Same as 01, but without newlines
                    testCases("otherInvalid/addedIfSameLine02", MODERATE, MUTATION_IF_STATEMENT),
                    // TODO: Look at this one too
                    testCases("otherInvalid/addedSecondIfSameLine01", MODERATE, MUTANT_VALIDATION_CALLS, MUTATION_WHILE_STATEMENT),
                    // TODO: Same as 01, but without newlines
                    testCases("otherInvalid/addedSecondIfSameLine02", MODERATE, MUTANT_VALIDATION_CALLS, MUTATION_WHILE_STATEMENT),
                    // TODO: Look at this one again
                    testCases("otherInvalid/addedWhileSameLine01", MODERATE, MUTATION_WHILE_STATEMENT, MUTANT_VALIDATION_CALLS),
                    // TODO: Same as 01 but without newlines
                    testCases("otherInvalid/addedWhileSameLine02", MODERATE, MUTATION_WHILE_STATEMENT, MUTANT_VALIDATION_CALLS),

                    // MUTANT_VALIDATION_PACKAGE_SIGNATURE - Is always forbidden
                    testCase("packageSignature/addedPackage", RELAXED, MUTANT_VALIDATION_PACKAGE_SIGNATURE),
                    testCase("packageSignature/changedPackage", RELAXED, MUTANT_VALIDATION_PACKAGE_SIGNATURE),

                    // MUTANT_VALIDATION_OPERATORS
                    testCase("systemCalls/addedCallToJavaUtilRandom_nextInt", STRICT, MUTANT_VALIDATION_OPERATORS),
                    testCase("systemCalls/addedCallToSystem_currentTimeMillis", STRICT, MUTANT_VALIDATION_OPERATORS),

                    // MUTANT_VALIDATION_SUCCESS
                    testCase("valid/addedSecondStatementOnSingleLine", STRICT, MUTANT_VALIDATION_SUCCESS),
                    // Comment success
                    testCases("valid/changeAfterSlash", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCases("valid/changedFieldInitializer", STRICT, MUTANT_VALIDATION_SUCCESS),

                    testCase("valid/changeStringValue", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("valid/inlinedStringVariable01", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("valid/inlinedStringVariable02", STRICT, MUTANT_VALIDATION_SUCCESS),

                    testCase("valid/instanceOfMultipleUnchanged", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("valid/instanceOfUnchanged", STRICT, MUTANT_VALIDATION_SUCCESS),

                    testCase("valid/liftDecrementTopFloorOnGetCurrentFloor", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("valid/liftTopFloorIsIncrementedByOneInConstructor", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("valid/simpleChange01", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCase("valid/simpleChange02", STRICT, MUTANT_VALIDATION_SUCCESS),

                    testCases("valid/withCommentInDifferentLine", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCases("valid/withCommentInSameLine", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCases("valid/withField", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCases("valid/withoutPackage", STRICT, MUTANT_VALIDATION_SUCCESS),
                    testCases("valid/withPackage", STRICT, MUTANT_VALIDATION_SUCCESS)
            ).flatMap(Function.identity());
        }
    }

    @ParameterizedTest(name = "[{index}] Validating mutant {0} on level {1} results in one of {2}")
    @ArgumentsSource(MutantsArgumentSource.class)
    public void testValidateMutantGetMessage(String mutant, CodeValidatorLevel codeValidatorLevel,
            Iterable<ValidationMessage> expectedValidationMessages) {
        String original = loadMutantOriginal(mutant);
        String mutated = loadMutantMutated(mutant);

        ValidationMessage actual = validateMutantGetMessage(original, mutated, codeValidatorLevel);

        assertThat(actual).isIn(expectedValidationMessages);
    }


    @Nested
    public class GetMD5FromFileTests {

        @Test
        public void customMD5isTheSameForSameInput() {
            String originalMD5 = CodeValidator.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/same/original.txt");
            String mutatedMD5 = CodeValidator.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/same/mutated.txt");

            assume().that(originalMD5).isNotNull();
            assume().that(mutatedMD5).isNotNull();

            assertThat(originalMD5).isEqualTo(mutatedMD5);
        }

        @Test
        public void customMD5ignoresWhitespaceChanges() {
            String originalMD5 = CodeValidator.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/spaceAdded/original.txt");
            String mutatedMD5 = CodeValidator.getMD5FromFile("src/test/resources/" + RESOURCE_DIR + "mutants/identical/spaceAdded/mutated.txt");

            assume().that(originalMD5).isNotNull();
            assume().that(mutatedMD5).isNotNull();

            assertThat(originalMD5).isEqualTo(mutatedMD5);
        }

        @Test
        public void customMD5returnsNullForMissingFile() {
            assertThat(CodeValidator.getMD5FromFile("/nonexistent")).isNull();
        }
    }

    @Nested
    public class RemoveQuotedTests {

        @Test
        public void noQuotesRemoveSingleQuotes() {
            assertThat(CodeValidator.removeQuoted("[Prefix][Postfix]", "'")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void noQuotesRemoveDoubleQuotes() {
            assertThat(CodeValidator.removeQuoted("[Prefix][Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void singleQuotesOnlyRemoveDoubleQuotes() {
            assertThat(CodeValidator.removeQuoted("[Prefix]'T'[Postfix]", "\"")).isEqualTo("[Prefix]'T'[Postfix]");
        }

        @Test
        public void doubleQuotesOnlyRemoveSingleQuotes() {
            assertThat(CodeValidator.removeQuoted("[Prefix]\"Text\"[Postfix]", "'")).isEqualTo("[Prefix]\"Text\"[Postfix]");
        }

        @Test
        public void oneSingleQuotedCharacter() {
            assertThat(CodeValidator.removeQuoted("[Prefix]'T'[Postfix]", "'")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void oneDoubleQuotedText() {
            assertThat(CodeValidator.removeQuoted("[Prefix]\"Text\"[Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void oneSingleQuotedCharacterContainingEscapedSingleQuote() {
            assertThat(CodeValidator.removeQuoted("[Prefix]'\\''[Postfix]", "'")).isEqualTo("[Prefix][Postfix]");
        }

        @Test
        public void oneDoubleQuotedTextContainingEscapedDoubleQuoteAtTheEnd() {
            assertThat(CodeValidator.removeQuoted("[Prefix]\"Text\\\"\"[Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }

        @Disabled("public void org.codedefenders.validation.code.CodeValidatorTest$RemoveQuotedTests.oneDoubleQuotedTextContainingEscapedDoubleQuoteAtTheBeginning() is @Disabled\n"
                + "because removeQuoted can not deal properly with escaped quotes")
        @Test
        public void oneDoubleQuotedTextContainingEscapedDoubleQuoteAtTheBeginning() {
            assertThat(CodeValidator.removeQuoted("[Prefix]\"\\\"Text\"[Postfix]", "\"")).isEqualTo("[Prefix][Postfix]");
        }
    }

    @Nested
    public class OnlyLiteralsChangedTests {

        @Test
        public void noQuotes() {
            assertThat(CodeValidator.onlyLiteralsChanged("public class Test {}", "public class Test {}")).isTrue();
        }

        @Test
        public void simpleString() {
            assertThat(CodeValidator.onlyLiteralsChanged("public class Test { String var = \"Text\"; }", "public class Test { String var = \"Changed Text\"; }")).isTrue();
        }

        @Test
        public void simpleChar() {
            assertThat(CodeValidator.onlyLiteralsChanged("public class Test { char var = 'A'; }", "public class Test { char var = 'B'; }")).isTrue();
        }

        @Test
        public void oneNestedQuotationCharacter01() {
            String original = "public class Test { String var = \"Text\"; }";
            String mutated = "public class Test { String var = \"Changed Text\\\"\"; }";

            assertThat(CodeValidator.onlyLiteralsChanged(original, mutated)).isTrue();
        }

        @Disabled("public void org.codedefenders.validation.code.CodeValidatorTest$OnlyLiteralsChangedTests.oneNestedQuotationCharacter02() is @Disabled\n"
                + "because onlyLiteralChanged can't properly deal with escaped quotes inside another quote")
        @Test
        public void oneNestedQuotationCharacter02() {
            String original = "public class Test { String var = \"\\\"Text\"; }";
            String mutated = "public class Test { String var = \"Changed Text\"; }";

            assertThat(CodeValidator.onlyLiteralsChanged(original, mutated)).isTrue();
        }

        @Disabled("public void org.codedefenders.validation.code.CodeValidatorTest$OnlyLiteralsChangedTests.oneNestedQuotationCharacter03() is @Disabled\n"
                + "because onlyLiteralChanged can't properly deal with escaped quotes inside another quote")
        @Test
        public void oneNestedQuotationCharacter03() {
            String original = "public class Test { String var = \"\\\"Text\"; }";
            String mutated = "public class Test { String var = \"Changed\\\" Text\"; }";

            assertThat(CodeValidator.onlyLiteralsChanged(original, mutated)).isTrue();
        }

        @Test
        public void oneNestedQuotationCharacter04() {
            String original = "public class Test { char var = '\"'; }";
            String mutated = "public class Test { char var = '\\''; }";

            assertThat(CodeValidator.onlyLiteralsChanged(original, mutated)).isTrue();
        }
    }
}

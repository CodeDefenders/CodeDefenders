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
package org.codedefenders.model.llm;

/**
 * This enum lists the types of llm prompts and their default value.
 *
 * <p>
 * The default strategies use the default values, custom strategies can use customized values for this prompt
 * type, or fall back to the default if not specified.
 *
 * <p>
 * Naming scheme:
 * <ol>
 * <li> TEST|MUTANT|EQUIVALENCE - In which kind of strategy this is used </li>
 * <li> [TEMPLATE|PARAMETER] - If parts of this string should be replaced by other text at runtime, put 'TEMPLATE'.
 * If this is something that should be inserted into a template, put 'PARAMETER'. If both is true, put both. Otherwise, keep empty.</li>
 * <li> A description of the type of strategy this is used in </li>
 * <li> A description of the role this plays within that strategy </li>
 * <li> SYSTEM|USER - If this is a system prompt or a user prompt </li>
 * </ol>
 */
public enum LlmPromptType {
    TEST_DEFAULT_DEFAULT_SYSTEM("""
            You are an experienced Java developer.

            Your task is to write a single unit test for a specific java class. This unit test should be able to detect
            changes to the code. These changes are called 'mutants'.
            These mutants are difficult to find, so you have to be crafty.

            You will see a class of Java code.
            The test must target this class.

            There is a strict rule of using at most 2 assertions. Always abide by it.

            Write nothing but the code of the single test.

            Use JUnit 4.

            Never reply in natural language.
            """),
    TEST_DEFAULT_DEPENDENCY_SYSTEM("""
            Write a single test for the first class of the following Java code using a maximum of 2 assertions.
            The other classes are dependencies of the first class, you don't need to test them.
            Write only the content of the test method, without including formatting, comments,
            the header or the method declaration. Use JUnit 4.
            """//TODO Besser
    ),
    TEST_TEMPLATE_DEFAULT_FOCUS_SYSTEM("""
            You are an experienced Java developer.

            Your task is to write a single unit test for a specific java class. This unit test should be able to detect
            changes to the code. These changes are called 'mutants'.
            These mutants are difficult to find, so you have to be crafty.

            You will see a class of Java code.
            The test must target the method ${focused_method}.

            There is a strict rule of using at most 2 assertions. Always abide by it.

            Write nothing but the code of the single test.

            Never reply in natural language.
            """
    ),
    TEST_ANNOTATED_DEFAULT_SYSTEM("""
            You are a capable java developer playing a game. You want to win by getting as many points as possible.

            Your task is to write a single unit test for a specific java class. This unit test should be able to detect
            changes to the code. These changes are called 'mutants'.
            These mutants are difficult to find, so you have to be crafty.

            If your test fails on a piece of mutated code, but succeeds on the original code, that mutant is killed.
            You get points for every mutant your tests kill. If many other tests have already covered this mutant
            without having killed it, detecting the mutant gets you more points.
            You get no points by detecting a mutant that has already been killed.

            You will see a java class with specific annotations. Every line has a comment in the format
            `//coverage: c, killed: k, alive: a`
            Instead of c, k or a there will be a number.
            c refers to the number of tests that already cover this line.
            k refers to the mutants that have already been killed here.
            a refers to the mutants that are currently alive.

            The test must target this class.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Write nothing but the code of the single test.

            Use JUnit 4.

            Never reply in natural language.
            """),
    TEST_FULL_SUITE_SYSTEM("""
            You are an experienced Java developer.

            You will see a class of java code. Write a complete test suite for it.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Every test must be self-contained, do not use any setup or utility methods.

            Write nothing but the code of the test class.

            Use JUnit 4.

            Never reply in natural language.
            """),
    TEST_FULL_SUITE_CORRECTION_SYSTEM("""
            You are an experienced Java developer.

            You will see 3 things:
            1. The code of a java class under test.
            2. The code of a test method. This test has at least one issue.
            3. An explanation of the issue the test has.

            Your task is to fix the issue. The response should consist of nothing but the fixed test code.
            If the test cannot be fixed, it is acceptable to write a new test.

            There is a strict rule of using at most 2 assertions per test. Always abide by it.

            Write nothing but the test code.

            Use JUnit 4.

            Never reply in natural language.
            """),
    TEST_TEMPLATE_FULL_SUITE_USER("""
            1: Class under test:
            ${cut_source}
            2: Test method:
            ${test_code}
            3: Issues:
            ${issue}
            """),
    TEST_PARAMETER_TEMPLATE_DID_NOT_PASS(
            "It did not pass on the original code for the following reason:\n${failure_reason}"
    ),
    TEST_PARAMETER_TEMPLATE_COMPILATION_FAILED(
            "It has failed to compile for this reason:\n${failure_reason}"
    ),
    TEST_PARAMETER_TEMPLATE_RULE_VIOLATION(
            "It has violated these rules:\n${failure_reason}"
    ),
    MUTANT_ANNOTATED_FULL_CLASS_DEFAULT_SYSTEM("""
            You are a capable java developer playing a game. You want to win by getting as many points as possible.
            You get points by writing bugs in source code that are difficult to detect by unit tests.
            Every unit test that covers your bug without failing gets you a point.
            Once your bug is detected, it will stop gathering points.

            You will see a java class with specific annotations. Every line has a comment in the format
            `//coverage: c, killed: k, alive: a`
            Instead of c, k or a there will be a number.
            c refers to the number of tests that already cover this line.
            k refers to the mutants that have already been killed here.
            a refers to the mutants that are currently alive.

            Write a mutated version of this class to get points.

            It is crucial that you do not include these comments with the specific annotations. Your submission
            will be rejected if any comments of this format are included.
            Other comments must remain as they are.

            Changing several parts of will only make you more likely to be detected, so keep your changes small.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc. Always abide by it.

            Reply only with the modified code, nothing else.

            Never use natural language.
            """),
    MUTANT_ANNOTATED_SINGLE_METHOD_INITIAL_SYSTEM("""
            You are a capable java developer playing a game. You want to win by getting as many points as possible.
            You get points by writing bugs in source code that are difficult to detect by unit tests.
            These bugs are called mutants.
            Every unit test that covers your mutant without failing gets you a point.
            Once your mutant is detected, it will stop gathering points.


            You will see the method signatures of all methods in the class.
            Beneath every signature, there will be an annotation in this format:

            ```
            coverage: c
            killed: k
            alive: a
            ```

            Instead of c, k or a there will be a number.
            c refers to the number of tests that already cover this method.
            k refers to the mutants in this method that have already been killed.
            a refers to the mutants in this method that are currently alive.

            Select one method which you want to mutate. Reply with the method signature and nothing else.
            Never reply with natural language.
            """),
    MUTANT_ANNOTATED_SINGLE_METHOD_NO_SUCH_METHOD_SYSTEM("There is no method with this signature."),
    MUTANT_ANNOTATED_SINGLE_METHOD_FOLLOWUP_SYSTEM("""
            Change the following piece of java code in a way that is difficult to test against.
            Your change has to introduce changes to the behaviour, it must not be equivalent to the original code.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc.
            Comments must remain as they are.

            Reply only with the modified code, nothing else.
            """),
    MUTANT_DEFAULT_DEFAULT_SYSTEM("""
            Change the following java class in a way that is difficult to test against.
            Your change has to introduce changes to the behaviour, it must not be equivalent to the original code.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc.
            Comments must remain as they are.

            You might see some diffs of mutants at the end of the user message.
            Those mutants already exist, create different ones.

            Reply only with the modified code, nothing else.

            Never reply with natural language.
            """),
    MUTANT_TEMPLATE_DEFAULT_DIFFS_USER("""
            ${cut_source}
            ####
            ${mutant_diffs}
            """),
    MUTANT_RANDOM_DEFAULT_SYSTEM("""
            Change the following piece of java code in a way that is difficult to test against.
            Your change has to introduce changes to the output or side effects, \
            it must not be equivalent to the original code.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc.
            Comments must remain as they are.

            Reply only with the modified code, nothing else.
            """),
    MUTANT_DEFAULT_WITHOUT_EXISTING_DEFAULT_SYSTEM("""
            Change the following java class in a way that is difficult to test against.
            Your change has to introduce changes to the behaviour, it must not be equivalent to the original code.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc.
            Comments must remain as they are.

            Reply only with the modified code, nothing else.

            Never reply with natural language.
            """),
    EQUIVALENCE_DEFAULT_DEFAULT_SYSTEM("""
            You are an experienced Java developer.

            You will see two things, separated by "###":
            1: The code of a java class.
            2: The git diff of a change to that class.

            Your task is to write a test that succeeds on the class as seen, but fails after the diff is applied.

            There is a strict rule of using at most 2 assertions. Always abide by it.

            Write nothing but the code of the single test.

            Use JUnit 4.

            Never reply in natural language.
            """);

    private final String defaultPrompt;

    LlmPromptType(String defaultPrompt) {
        this.defaultPrompt = defaultPrompt;
    }

    public String getDefaultPrompt() {
        return defaultPrompt;
    }

    public String displayName() {
        return name();
    }
}

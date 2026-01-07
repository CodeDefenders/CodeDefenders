package org.codedefenders.model.llm.strategy;

public class SingleMethodMutantStrategy extends LlmStrategy {
    public static final String initialPrompt = """
            You are a capable java developer playing a game. You want to win by getting as many points as possible.
            You get points by writing bugs in source code that are difficult to detect by unit tests.
            Every unit test that covers your bug without failing gets you a point.
            Once your bug is detected, it will stop gathering points.


            You will see the method signatures of all methods in the class.
            Beneath every signature, there will be an annotation in this format:

            ```
            coverage: c
            killed: k
            alive: a
            ```

            Instead of c, k or a there will be a number.
            c refers to the number of tests that already cover this line.
            k refers to the mutants that have already been killed here.
            a refers to the mutants that are currently alive.

            Select one method which you want to mutate. Reply with the method signature and nothing else.
            """;

    public static final String secondaryPrompt = """
            Change the following piece of code in a way that is difficult to test against. It should, however, change
            the behaviour of the program.

            Reply only with the modified code, nothing else.
            """;

    public SingleMethodMutantStrategy() {
        super("SINGLE_METHOD_MUTANT");
    }
}

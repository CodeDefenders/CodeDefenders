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
package org.codedefenders.service.llm;

import java.util.Arrays;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.JavaParserUtils;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

@RequestScoped
public class MutantStrategyAnnotatedSingleMethod extends LlmMutantService {
    private static Logger logger = LoggerFactory.getLogger(MutantStrategyAnnotatedSingleMethod.class);
    static LlmStrategy strategy = LlmStrategy.MUTANT_ANNOTATED_SINGLE_METHOD;

    public static final String initialPrompt = """
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
            """;

    public static final String initialRepairPrompt = """
                There is no method with this signature.
            """;

    public static final String secondaryPrompt = """
            Change the following piece of java code in a way that is difficult to test against.
            Your change has to introduce changes to the behaviour, it must not be equivalent to the original code.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc.
            Comments must remain as they are.

            Reply only with the modified code, nothing else.
            """;

    @Override
    protected void onSubmitSuccess() {
        finishConversation(true);
        baggage().methodDeclaration = null;
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc) {

    }

    @Override
    protected Optional<String> generate() {
        if (baggage().methodDeclaration == null) {
            setConversationType(PromptType.ATTACK_DEFAULT);
        } else {
            setConversationType(PromptType.ATTACK_DEPENDENCIES);
        }
        resetConversationAfterTooManyTries();
        if (conversation.getType() == PromptType.ATTACK_DEFAULT) {
            if (conversation.isEmpty()) {
                conversation.addSystemMessage(initialPrompt, model);
                conversation.addUserMessage(
                        LlmUtils.annotatedMethodDescriptions(game, baggage().compilationUnit), model);
            }
            String reply = promptService.getResponse(model, conversation);
            CallableDeclaration<?> declaration = baggage().getCallableDeclaration(reply);
            if (declaration == null) {
                conversation.addUserMessage(initialRepairPrompt, model);
            } else {
                baggage().methodDeclaration = declaration;
                finishConversation(true);
                setConversationType(PromptType.ATTACK_DEPENDENCIES);
            }
            return Optional.empty();
            //baggage().methodSignature = reply;

        } else if (conversation.getType() == PromptType.ATTACK_DEPENDENCIES) {
            //TODO Define other enum values, dependencies is placeholder
            String originalMethodCode = baggage().getMethodContent(game.getCUT().getSourceCode());

            if (conversation.isEmpty()) {
                conversation.addSystemMessage(secondaryPrompt, model);
                conversation.addUserMessage(originalMethodCode, model);
            }

            String reply = promptService.getResponse(model, conversation);
            reply = LlmUtils.extractMutantFromReply(reply, null);
            return Optional.of(game.getCUT().getSourceCode().replace(originalMethodCode, reply));
        } else {
            throw new RuntimeException("No support for this conversation type: " + conversation.getType());
        }
    }

    private SingleMethodBaggage baggage() {
        if (conversationBatch.getBaggage() == null) {
            conversationBatch.setBaggage(new SingleMethodBaggage(game));
        }
        return (SingleMethodBaggage)conversationBatch.getBaggage();
    }

    private static class SingleMethodBaggage {
        private CallableDeclaration<?> methodDeclaration;
        private final CompilationUnit compilationUnit;

        private SingleMethodBaggage(AbstractGame game) {
            compilationUnit = JavaParserUtils.parse(game.getCUT().getSourceCode())
                    .orElseThrow(IllegalStateException::new);
        }

        private CallableDeclaration<?> getCallableDeclaration(String signature) {
            return compilationUnit.accept(new MethodNameVisitor(), signature);
        }

        private String getMethodContent(String source) {
            Range range =  methodDeclaration.getRange().orElseThrow();
            int begin = range.begin.line - 1;
            int end = range.end.line;
            String[] lines = source.split("\n");
            String [] methodLines = Arrays.copyOfRange(lines, begin, end);
            String methodString = String.join("\n", methodLines);
            logger.info("Original method String: {}", methodString);
            return methodString;
        }
    }

    //TODO Generalize with other Single-Method-Mutants
    private static class MethodNameVisitor extends GenericVisitorAdapter<CallableDeclaration<?>, String> {
        @Override
        public MethodDeclaration visit(MethodDeclaration methodDeclaration, String searchedFor) {
            //super.visit(methodDeclaration, searchedFor);
            String stringRepresentation = methodDeclaration.getDeclarationAsString();
            if (stringRepresentation.contains(searchedFor) || searchedFor.contains(stringRepresentation)) {
                return methodDeclaration;
            } else {
                return null;
            }
        }

        @Override
        public ConstructorDeclaration visit(ConstructorDeclaration constructorDeclaration, String searchedFor) {
            //super.visit(methodDeclaration, searchedFor);
            String stringRepresentation = constructorDeclaration.getDeclarationAsString();
            if (stringRepresentation.contains(searchedFor) || searchedFor.contains(stringRepresentation)) {
                return constructorDeclaration;
            } else {
                return null;
            }
        }
    }

}

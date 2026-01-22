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
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.analysis.gameclass.MethodDescription;
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
public class MutantStrategyRandomSingleMethod extends LlmMutantService {
    private static Logger logger = LoggerFactory.getLogger(MutantStrategyRandomSingleMethod.class);
    static LlmStrategy strategy = LlmStrategy.MUTANT_RANDOM_SINGLE_METHOD;

    public static final String systemPrompt = """
            Change the following piece of java code in a way that is difficult to test against.
            Your change has to introduce changes to the output or side effects, \
            it must not be equivalent to the original code.

            There is a strict rule of not allowing any new control structures, such as if, while, ternary \
            operators, etc.
            Comments must remain as they are.

            Reply only with the modified code, nothing else.
            """;

    @Override
    protected void onSubmitSuccess() {
        finishConversation(true);
        baggage().callableDeclaration = null;
        baggage().originalMethodCode = null;
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc) {

    }

    @Override
    protected Optional<String> generate() {
        setConversationType(PromptType.ATTACK_DEFAULT);
        resetConversationAfterTooManyTries();

        if (conversation.isEmpty()) {
            List<MethodDescription> methodDescriptions = game.getCUT().getMethodDescriptions();
            MethodDescription chosenDescription = methodDescriptions.get(random.nextInt(methodDescriptions.size()));

            //TODO Not very efficient
            baggage().callableDeclaration = baggage().getMethodDeclaration(chosenDescription.getDescription());
            baggage().originalMethodCode = baggage().getMethodContent(game.getCUT().getSourceCode());


            conversation.addSystemMessage(systemPrompt, model);
            conversation.addUserMessage(baggage().originalMethodCode, model);
        }

        String reply = promptService.getResponse(model, conversation);
        reply = LlmUtils.extractMutantFromReply(reply, null);
        return Optional.of(game.getCUT().getSourceCode().replace(baggage().originalMethodCode, reply));
    }

    private SingleMethodBaggage baggage() {
        if (conversationBatch.getBaggage() == null) {
            conversationBatch.setBaggage(new SingleMethodBaggage(game));
        }
        return (SingleMethodBaggage) conversationBatch.getBaggage();
    }

    //TODO Generalize
    private static class SingleMethodBaggage {
        private CallableDeclaration<?> callableDeclaration;
        private String originalMethodCode;
        private final CompilationUnit compilationUnit;

        private SingleMethodBaggage(AbstractGame game) {
            compilationUnit = JavaParserUtils.parse(game.getCUT().getSourceCode())
                    .orElseThrow(IllegalStateException::new);
        }

        private CallableDeclaration<?> getMethodDeclaration(String signature) {
            return compilationUnit.accept(new MethodNameVisitor(), signature);
        }

        private String getMethodContent(String source) {
            Range range = callableDeclaration.getRange().orElseThrow();
            int begin = range.begin.line - 1;
            int end = range.end.line;
            String[] lines = source.split("\n");
            String[] methodLines = Arrays.copyOfRange(lines, begin, end);
            String methodString = String.join("\n", methodLines);
            logger.info("Original method String: {}", methodString);
            return methodString;
        }
    }

    private static class MethodNameVisitor
            extends GenericVisitorAdapter<CallableDeclaration<? extends CallableDeclaration<?>>, String> {
        @Override
        public MethodDeclaration visit(MethodDeclaration methodDeclaration, String searchedFor) {
            //super.visit(methodDeclaration, searchedFor);
            String stringRepresentation = methodDeclaration.getDeclarationAsString(false, false, false);
            if (stringRepresentation.contains(searchedFor) || searchedFor.contains(stringRepresentation)) {
                return methodDeclaration;
            } else {
                return null;
            }
        }

        @Override
        public ConstructorDeclaration visit(ConstructorDeclaration constructorDeclaration, String searchedFor) {
            String stringRepresentation = constructorDeclaration.getDeclarationAsString(false, false, false);
            if (stringRepresentation.contains(searchedFor) || searchedFor.contains(stringRepresentation)) {
                return constructorDeclaration;
            } else {
                return null;
            }
        }
    }

}

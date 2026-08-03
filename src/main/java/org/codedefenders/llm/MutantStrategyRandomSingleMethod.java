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
package org.codedefenders.llm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.inject.Vetoed;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.LlmPromptType;
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

public class MutantStrategyRandomSingleMethod extends AbstractMutantStrategy {
    private static final Logger logger = LoggerFactory.getLogger(MutantStrategyRandomSingleMethod.class);

    public static final String BAGGAGE_KEY = "mutant_random_single_method";

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
        setConversationType(LlmPromptType.MUTANT_RANDOM_DEFAULT_SYSTEM.displayName());
        resetConversationAfterTooManyTries();

        if (conversation.isEmpty()) {
            List<MethodDescription> methodDescriptions = context.game().getCUT().getMethodDescriptions();
            MethodDescription chosenDescription = methodDescriptions.get(
                    context.random().nextInt(methodDescriptions.size())
            );

            //TODO Not very efficient
            baggage().callableDeclaration = baggage().getMethodDeclaration(chosenDescription.getDescription());
            baggage().originalMethodCode = baggage().getMethodContent(context.game().getCUT().getSourceCode());


            conversation.addSystemMessage(context.strategy().getPrompt(LlmPromptType.MUTANT_RANDOM_DEFAULT_SYSTEM),
                    context.model());
            conversation.addUserMessage(baggage().originalMethodCode, context.model());
        }

        String reply = promptService.getResponse(context.model(), conversation);
        reply = LlmUtils.extractMutantFromReply(reply, null);
        return Optional.of(context.game().getCUT().getSourceCode().replace(baggage().originalMethodCode, reply));
    }

    private SingleMethodBaggage baggage() {
        if (!context.getBaggages().containsKey(BAGGAGE_KEY)) {
            context.getBaggages().put(BAGGAGE_KEY, new SingleMethodBaggage(context.game()));
        }
        return (SingleMethodBaggage) context.getBaggages().get(BAGGAGE_KEY);
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

    @Vetoed
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

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

import java.util.Optional;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.JavaParserUtils;
import org.codedefenders.util.LlmUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

public class MutantStrategyAnnotatedSingleMethod extends LlmMutantService {

    private static final String ASK_FOR_METHOD = "ASK_FOR_METHOD";
    private static final String FOLLOW_UP = "FOLLOW_UP";

    @Override
    protected void onSubmitSuccess() {
        finishConversation(true);
        baggage().methodDeclaration = null;
    }

    @Override
    protected void onSubmitFailure(GameManagingUtils.CreateBattlegroundMutantResult result, String testSrc) {

    }

    @Override
    protected Optional<String> generate(LlmStrategy strategy) {
        if (baggage().methodDeclaration == null) {
            setConversationType(ASK_FOR_METHOD);
        } else {
            setConversationType(FOLLOW_UP);
        }
        resetConversationAfterTooManyTries();
        if (conversation.getType().equals(ASK_FOR_METHOD)) {
            if (conversation.isEmpty()) {
                conversation.addSystemMessage(
                        strategy.getPrompt(LlmPromptType.MUTANT_ANNOTATED_SINGLE_METHOD_INITIAL_SYSTEM), model);
                conversation.addUserMessage(
                        LlmUtils.annotatedMethodDescriptions(game, baggage().compilationUnit), model);
            }
            String reply = promptService.getResponse(model, conversation);
            CallableDeclaration<?> declaration = baggage().getCallableDeclaration(reply);
            if (declaration == null) {
                conversation.addUserMessage(
                        strategy.getPrompt(LlmPromptType.MUTANT_ANNOTATED_SINGLE_METHOD_NO_SUCH_METHOD_SYSTEM), model);
            } else {
                baggage().methodDeclaration = declaration;
                finishConversation(true);
                setConversationType(FOLLOW_UP); //TODO verstehe den flow hier nicht ganz. Warum hier auf follow_up, und oben wird auch nochmal überprüft`?
            }
            return Optional.empty();
            //baggage().methodSignature = reply;

        } else if (conversation.getType().equals(FOLLOW_UP)) {
            String originalMethodCode = baggage().getMethodContent(game.getCUT().getSourceCode());

            if (conversation.isEmpty()) {
                conversation.addSystemMessage(
                        strategy.getPrompt(LlmPromptType.MUTANT_ANNOTATED_SINGLE_METHOD_FOLLOWUP_SYSTEM), model);
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
            return LlmUtils.getMethodContent(source, methodDeclaration);
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

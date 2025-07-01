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
package org.codedefenders.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.ClassCompilerService;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.notification.events.server.test.TestCompiledEvent;
import org.codedefenders.notification.events.server.test.TestSubmittedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.notification.events.server.test.TestTestedOriginalEvent;
import org.codedefenders.notification.events.server.test.TestValidatedEvent;
import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.TestSmellRepository;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import testsmell.TestFile;
import testsmell.TestSmellDetector;

import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.util.Constants.MODE_BATTLEGROUND_DIR;

@ApplicationScoped
public class LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    Configuration config;
    ChatModel model;

    @Inject
    GameClassRepository gameClassRepo;

    @Inject
    ClassCompilerService classCompiler;

    @Inject
    NotificationService notificationService;

    @Inject
    BackendExecutorService backend;

    @Inject
    TestSmellDetector testSmellDetector;

    @Inject
    TestSmellRepository testSmellRepo;

    @Inject
    UserService userService;

    @Inject
    EventDAO eventDAO;

    @Inject
    IMutationTester mutationTester;


    @Inject
    public LlmService(Configuration config) {
        this.config = config;
        this.model = OpenAiChatModel.builder()
                .apiKey(config.getOpenaiApiKey())
                .modelName(config.getOpenaiChatgptModel())
                .build();
    }


    public String getResponse(String userMessage, String... systemMessages) {
        logger.info("Send message: \n {} to LLM with system messages:\n{}", userMessage,
                String.join("\n", systemMessages));
        ChatMessage[] chatMessages = new ChatMessage[systemMessages.length + 1];
        for (int i = 0; i < systemMessages.length; i++) {
            chatMessages[i] = SystemMessage.from(systemMessages[i]);
        }
        chatMessages[chatMessages.length - 1] = UserMessage.from(userMessage);
        ChatResponse response = model.chat(chatMessages);
        String responseText = response.aiMessage().text();
        logger.info("LLM responded with {}", responseText);
        return responseText;
    }

    //TODO Find some way to deal with CDI and RequestScope
    public Test createTest(int gameId, int classId, String testText, int ownerUserId, String subDirectory)
            throws IOException {
        GameClass cut = gameClassRepo.getClassForId(classId)
                .orElseThrow();

        Path path = Paths.get(config.getTestsDir().getAbsolutePath(), subDirectory, String.valueOf(gameId), String.valueOf(ownerUserId), "original");
        File newTestDir = FileUtils.getNextSubDir(path);

        String javaFile = FileUtils.createJavaTestFile(newTestDir, cut.getBaseName(), testText);

        Test newTest = classCompiler.compileTest(newTestDir, javaFile, gameId, cut, ownerUserId);

        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
                TargetExecution.Target.COMPILE_TEST);
        boolean compileSuccess = compileTestTarget.status == TargetExecution.Status.SUCCESS;

        TestCompiledEvent tce = new TestCompiledEvent();
        tce.setGameId(gameId);
        tce.setUserId(ownerUserId);
        tce.setTestId(newTest.getId());
        tce.setSuccess(compileSuccess);
        tce.setErrorMessage(compileSuccess ? null : compileTestTarget.message);
        notificationService.post(tce);

        // If the test did not compile we short circuit here. We shall not return null
        if (!compileSuccess) {
            return newTest;
        }

        // Eventually check the test actually passes when applied to the
        // original code.
        if (compileTestTarget.status == TargetExecution.Status.SUCCESS) {
            TargetExecution testOriginalTarget = backend.testOriginal(newTestDir, newTest);
            boolean testOriginalSuccess = testOriginalTarget.status == TargetExecution.Status.SUCCESS;

            TestTestedOriginalEvent ttoe = new TestTestedOriginalEvent();
            ttoe.setGameId(gameId);
            ttoe.setUserId(ownerUserId);
            ttoe.setTestId(newTest.getId());
            ttoe.setSuccess(testOriginalSuccess);
            ttoe.setErrorMessage(testOriginalSuccess ? null : compileTestTarget.message);
            notificationService.post(ttoe);

            detectTestSmells(newTest, cut);
        }

        return newTest;
    }

    public void detectTestSmells(Test newTest, GameClass cut) {
        try {
            // Detect test smell
            TestFile testFile = new TestFile("", newTest.getJavaFile(), cut.getJavaFile());
            testSmellDetector.detectSmells(testFile);
            // TODO Post Process Smells. See #500
            testSmellRepo.storeSmell(newTest, testFile);
        } catch (Exception e) {
            logger.error("Failed to generate or store test smell.", e);
        }
    }

    public GameManagingUtils.CreateBattlegroundTestResult createBattlegroundTest(MultiplayerGame game, int userId, String code)
            throws IOException {
        TestSubmittedEvent tse = new TestSubmittedEvent();
        tse.setGameId(game.getId());
        tse.setUserId(userId);
        notificationService.post(tse);

        // Do the validation even before creating the mutant
        List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(
                code,
                game.getMaxAssertionsPerTest(),
                game.getCUT().getAssertionLibrary());
        boolean validationSuccess = validationMessage.isEmpty();

        TestValidatedEvent tve = new TestValidatedEvent();
        tve.setGameId(game.getId());
        tve.setUserId(userId);
        tve.setSuccess(validationSuccess);
        tve.setValidationMessage(validationSuccess ? null : String.join("\n", validationMessage));
        notificationService.post(tve);

        if (!validationSuccess) {
            return GameManagingUtils.CreateBattlegroundTestResult.failure(
                    null, GameManagingUtils.CreateBattlegroundTestResult.FailureReason.VALIDATION_FAILED,
                    validationMessage, null, null);
        }

        // From this point on we assume that test is valid according to the rules (but it might still not compile)
        Test newTest = createTest(game.getId(), game.getClassId(), code, userId, MODE_BATTLEGROUND_DIR);
        logger.debug("New Test {} by user {}", newTest.getId(), userId);
        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);

        if (compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            return GameManagingUtils.CreateBattlegroundTestResult.failure(
                    newTest, GameManagingUtils.CreateBattlegroundTestResult.FailureReason.COMPILATION_FAILED,
                    null, compileTestTarget.message, null
            );
        }

        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            return GameManagingUtils.CreateBattlegroundTestResult.failure(
                    newTest, GameManagingUtils.CreateBattlegroundTestResult.FailureReason.TEST_DID_NOT_PASS_ON_CUT,
                    null, null, testOriginalTarget.message
            );
        }

        var user = userService.getSimpleUserById(userId);
        final String notificationMsg = user.map(SimpleUser::getName)
                .orElse("User with the id " + userId) + " created a mutant.";
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        final Event notif = new Event(-1, game.getId(), userId, notificationMsg, EventType.DEFENDER_TEST_CREATED,
                EventStatus.GAME, timestamp);
        eventDAO.insert(notif);


        String mutationTesterMessage = mutationTester.runTestOnAllMultiplayerMutants(game, newTest);
        game.update();
        logger.info("Successfully created test {} ", newTest.getId());

        TestTestedMutantsEvent ttme = new TestTestedMutantsEvent();
        ttme.setGameId(game.getId());
        ttme.setUserId(userId);
        ttme.setTestId(newTest.getId());
        notificationService.post(ttme);

        return GameManagingUtils.CreateBattlegroundTestResult.success(newTest, mutationTesterMessage);
    }
}

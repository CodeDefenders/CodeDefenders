package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.util.ArrayList;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;

import com.github.javaparser.quality.Nullable;

import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;

@WebServlet("/llm-api/battleground/submit-test")
public class TestAPI extends APIServlet {
    @Inject
    protected GameService gameService;

    @Inject
    protected GameRepository gameRepo;

    @Inject
    protected GameClassRepository gameClassRepo;

    @Inject
    protected GameProducer gameProducer;

    @Inject
    protected GameManagingUtils gameManagingUtils;

    @Inject
    protected CodeDefendersAuth login;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");
        final var code = ServletUtils.getStringParameter(request, "code");

        if (gameId.isEmpty() || code.isEmpty()) {
            if (gameId.isEmpty()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            } else {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'code' missing."));
            }
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }

        var canSubmit = gameManagingUtils.canUserSubmitTest(game, login.getUserId());
        switch (canSubmit) {
            case YES -> {}
            default -> {
                writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        new Common.ErrorResponseDTO("Not allowed to submit test: " + canSubmit.name()));
                return;
            }
        }

        GameManagingUtils.CreateBattlegroundTestResult result;
        try {
            result = gameManagingUtils.createBattlegroundTest(game, login.getUserId(), code.get());
        } catch (IOException e) {
            writeResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new Common.ErrorResponseDTO("Server error while creating the test."));
            return;
        }

        var messages = new ArrayList<String>();

        if (result.isSuccess()) {
            Test test = result.test().orElseThrow();

            messages.add(TEST_PASSED_ON_CUT_MESSAGE);
            gameManagingUtils.getTestSmellsMessage(test).ifPresent(messages::add);
            result.mutationTesterMessage().ifPresent(messages::add);

            writeResponse(response, HttpServletResponse.SC_OK,
                    new SubmitTestResponseDTO(
                            true,
                            messages,
                            Common.TestDTO.fromTestDTO(gameService.getTest(login.getUserId(), test.getId())),
                            null
                    ));

        } else {
            var failureReason = result.failureReason().orElseThrow();
            switch (failureReason) {
                case VALIDATION_FAILED -> result.validationErrorMessages().ifPresent(messages::addAll);
                case COMPILATION_FAILED -> {
                    messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
                    result.compilationError().ifPresent(messages::add);
                }
                case TEST_DID_NOT_PASS_ON_CUT -> {
                    messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
                    result.testCutError().ifPresent(messages::add);
                }
            }

            writeResponse(response, HttpServletResponse.SC_OK,
                    new SubmitTestResponseDTO(
                            false,
                            messages,
                            null,
                            switch (failureReason) {
                                case VALIDATION_FAILED -> TestRejectReason.VALIDATION_FAILED;
                                case COMPILATION_FAILED -> TestRejectReason.COMPILATION_FAILED;
                                case TEST_DID_NOT_PASS_ON_CUT -> TestRejectReason.TEST_DID_NOT_PASS_ON_CUT;
                            }));
        }
    }

    public record SubmitTestResponseDTO(
            boolean success,
            ArrayList<String> messages,

            @Nullable
            Common.TestDTO test, // null if request failed
            @Nullable
            TestRejectReason rejectReason // null if request succeeded
    ) {
    }

    public enum TestRejectReason {
        VALIDATION_FAILED,
        COMPILATION_FAILED,
        TEST_DID_NOT_PASS_ON_CUT,
    }
}

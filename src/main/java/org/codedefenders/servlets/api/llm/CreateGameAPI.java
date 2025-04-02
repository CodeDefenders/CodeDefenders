package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.game.MultiplayerGameService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;

@WebServlet("/llm-api/battleground/create")
public class CreateGameAPI extends APIServlet {
    @Inject
    private MultiplayerGameService gameService;

    @Inject
    protected GameRepository gameRepo;

    @Inject
    protected GameClassRepository gameClassRepo;

    @Inject
    protected GameProducer gameProducer;

    @Inject
    protected CodeDefendersAuth login;

    @Override
    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        var optParams = readParameters(request, response);
        if (optParams.isEmpty()) {
            return;
        }
        var params = optParams.get();

        MultiplayerGame newGame = new MultiplayerGame.Builder(
                params.gameClass.getId(), login.getUserId(), params.maxAssertionsPerTest)
                .level(params.level)
                .chatEnabled(params.chatEnabled)
                .capturePlayersIntention(params.capturePlayersIntention)
                .mutantValidatorLevel(params.mutantValidatorLevel)
                .automaticMutantEquivalenceThreshold(params.automaticEquivalenceTrigger)
                .gameDurationMinutes(params.durationMinutes)
                .build();

        boolean success = gameService.createGame(newGame, params.withMutants, params.withTests, params.creatorRole);
        if (success) {
            writeResponse(response, HttpServletResponse.SC_OK,
                    new CreateGameResponseDTO(newGame.getId()));
        } else {
            writeResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new Common.ErrorResponseDTO("Could not create the game."));
        }
    }

    public Optional<CreateGameParameters> readParameters(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        var classId = ServletUtils.getIntParameter(request, "classId");
        var classAlias = ServletUtils.getStringParameter(request, "classAlias");

        var withTests = ServletUtils.getStringParameter(request, "withTests")
                .map(Boolean::parseBoolean);
        var withMutants = ServletUtils.getStringParameter(request, "withMutants")
                .map(Boolean::parseBoolean);

        var maxAssertionsPerTest = ServletUtils.getIntParameter(request, "maxAssertionsPerTest");
        var automaticEquivalenceTrigger = ServletUtils.getIntParameter(request, "automaticEquivalenceTrigger");
        var mutantValidatorLevel = ServletUtils.getStringParameter(request, "mutantValidatorLevel")
                .map(String::toUpperCase)
                .map(CodeValidatorLevel::valueOf);
        var creatorRole = ServletUtils.getStringParameter(request, "creatorRole")
                .map(String::toUpperCase)
                .map(Role::valueOf);
        var durationMinutes = ServletUtils.getIntParameter(request, "durationMinutes");
        var level = ServletUtils.getStringParameter(request, "level")
                .map(String::toUpperCase)
                .map(GameLevel::valueOf);

        if (classId.isPresent() && classAlias.isPresent()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("'classId' and 'classAlias' parameters cannot be used at the same time."));
            return Optional.empty();
        }

        Optional<GameClass> gameClass;
        if (classId.isPresent()) {
            gameClass = gameClassRepo.getClassForId(classId.get());
        } else if (classAlias.isPresent()) {
            gameClass = gameClassRepo.getClassForAlias(classAlias.get());
        } else {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("'classId' or 'classAlias' parameter must be present."));
            return Optional.empty();
        }

        if (gameClass.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game class not found."));
            return Optional.empty();
        }

        int defaultDuration = AdminDAO.getSystemSetting(
                AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_DEFAULT).getIntValue();
        int maxDuration = AdminDAO.getSystemSetting(
                AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_MAX).getIntValue();

        if (durationMinutes.isPresent() && durationMinutes.get() > maxDuration) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Game duration too long. Max. duration (minutes): " + maxDuration));
            return Optional.empty();
        }

        return Optional.of(new CreateGameParameters(
                gameClass.get(),
                withMutants.orElse(false),
                withTests.orElse(false),
                maxAssertionsPerTest.orElse(CodeValidator.DEFAULT_NB_ASSERTIONS),
                automaticEquivalenceTrigger.orElse(0),
                mutantValidatorLevel.orElse(CodeValidatorLevel.MODERATE),
                creatorRole.orElse(Role.OBSERVER),
                durationMinutes.orElse(defaultDuration),
                level.orElse(GameLevel.HARD),
                true, // enable chat
                false // disable intentions
        ));
    }

    public record CreateGameParameters(
            GameClass gameClass,
            boolean withMutants,
            boolean withTests,
            int maxAssertionsPerTest,
            int automaticEquivalenceTrigger,
            CodeValidatorLevel mutantValidatorLevel,
            Role creatorRole,
            int durationMinutes,
            GameLevel level,
            boolean chatEnabled,
            boolean capturePlayersIntention
    ) {}

    public record CreateGameResponseDTO(int gameId) {}
}

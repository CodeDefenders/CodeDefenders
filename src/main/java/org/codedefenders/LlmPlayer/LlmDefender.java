package org.codedefenders.LlmPlayer;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.llm.GPTException;
import org.codedefenders.llm.GPTRequestDispatcher;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.util.CDIUtil;

import java.util.List;

public class LlmDefender extends Player {
    AbstractGame game;

    GPTRequestDispatcher dispatcher = CDIUtil.getBeanFromCDI(GPTRequestDispatcher.class);
    GameService gameService = CDIUtil.getBeanFromCDI(GameService.class);
    GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

    String src;
    String systemPrompt;

    public LlmDefender(int id, int gameId, int points, boolean active) {
        super(id,null, gameId, points, Role.DEFENDER, active);
        game = gameRepo.getGame(gameId);

        src = game.getCUT().getSourceCode();
        //TODO add dependencies

        systemPrompt = "Write a short test for the following java code. Use only one assertion. "
                + "Output nothing but the test.";

    }

    public void writeTest() {
        try {
            String test = dispatcher.sendChatCompletionRequestWithContext(List.of(systemPrompt, src), List.of());
        } catch (GPTException e) {
            throw new RuntimeException(e); //TODO anders handeln
        }
    }
}

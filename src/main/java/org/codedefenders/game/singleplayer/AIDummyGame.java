package org.codedefenders.game.singleplayer;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.singleplayer.automated.attacker.AiAttacker;

/**
 * @author Ben Clegg
 */

public class AIDummyGame extends DuelGame {

	public AIDummyGame(int classId) {
		super(classId, AiAttacker.ID, 1, Role.ATTACKER, GameLevel.EASY);
		setState(GameState.ACTIVE);
	}

}

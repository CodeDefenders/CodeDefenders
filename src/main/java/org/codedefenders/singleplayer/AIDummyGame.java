package org.codedefenders.singleplayer;

import org.codedefenders.GameLevel;
import org.codedefenders.GameState;
import org.codedefenders.duel.DuelGame;
import org.codedefenders.Role;
import org.codedefenders.singleplayer.automated.attacker.AiAttacker;

/**
 * @author Ben Clegg
 */

public class AIDummyGame extends DuelGame {

	public AIDummyGame(int classId) {
		super(classId, AiAttacker.ID, 1, Role.ATTACKER, GameLevel.EASY);
		setState(GameState.ACTIVE);
	}

}

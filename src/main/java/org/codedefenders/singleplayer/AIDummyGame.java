package org.codedefenders.singleplayer;

import org.codedefenders.duel.DuelGame;
import org.codedefenders.Role;
import org.codedefenders.singleplayer.automated.attacker.AiAttacker;

/**
 * @author Ben Clegg
 */

public class AIDummyGame extends DuelGame {

	public AIDummyGame(int classId) {
		super(classId, AiAttacker.ID, 1, Role.ATTACKER, Level.EASY);
		setState(State.ACTIVE);
	}

}

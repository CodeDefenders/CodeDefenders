package org.codedefenders.singleplayer;

import org.codedefenders.Game;
import org.codedefenders.Role;
import org.codedefenders.singleplayer.automated.attacker.AiAttacker;

/**
 * @author Ben Clegg
 */

public class AIDummyGame extends Game {

	public AIDummyGame(int classId) {
		super(classId, AiAttacker.ID, 1, Role.ATTACKER, Level.EASY);
		setState(State.ACTIVE);
	}

}

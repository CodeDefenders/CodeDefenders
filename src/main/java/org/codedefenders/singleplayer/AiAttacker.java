package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * Created by midcode on 20/06/16.
 */
public class AiAttacker extends AiPlayer {
	public AiAttacker(Game g) {
		super(g);
		role = Game.Role.ATTACKER;
	}
}

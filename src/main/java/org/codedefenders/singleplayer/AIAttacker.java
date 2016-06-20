package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * Created by midcode on 20/06/16.
 */
public class AIAttacker extends AIPlayer{
	public AIAttacker(Game g) {
		super(g);
		role = Game.Role.ATTACKER;
	}
}

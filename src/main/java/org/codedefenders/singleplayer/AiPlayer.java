package org.codedefenders.singleplayer;

import org.codedefenders.duel.DuelGame;
import org.codedefenders.Role;

import java.util.ArrayList;

/**
 * @author Ben Clegg
 * Base class for AI players.
 */
public abstract class AiPlayer {

	public enum GenerationMethod {
		RANDOM, //Randomly select mutant.
		COVERAGE, //Select random mutant by least covered lines.
		KILLCOUNT //Number of mutants a test kills, or number of tests that kill mutant.
	}

	protected DuelGame game;
	protected Role role;
	protected ArrayList<String> messages;

	public AiPlayer(DuelGame g) {
		game = g;
		messages = new ArrayList<String>();
	}

	/**
	 * Make the AI's turn if it is its turn.
	 */
	public boolean makeTurn() {
		boolean success = false;
		messages.clear();
		if (game.getActiveRole().equals(role)) {
			for (int i = 0; i < 3; i++) {
				if (tryTurn()) { success = true; break; }
			}
			game.endTurn();
		}
		return success;
	}

	public boolean tryTurn() {
		switch (game.getLevel()) {
			case EASY: return turnEasy();
			case HARD: return turnHard();
			default: return turnHard();
		}
	}

	public abstract boolean turnEasy();

	public abstract boolean turnHard();

	protected abstract boolean runTurn(GenerationMethod strat);

	public abstract ArrayList<String> getMessagesLastTurn();
}

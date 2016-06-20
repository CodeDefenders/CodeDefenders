package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * Created by midcode on 15/06/16.
 */

public class SingleplayerGame extends Game {

	private Role playerRole;
	private Role aiRole;

	public SingleplayerGame (int classId, int userId, int maxRounds, Role role, Level level) {
		super(classId, userId, maxRounds, role, level);
		setMode(Mode.SINGLE); //Singleplayer
		playerRole = role; //Set player's role
		//Set ai's role
		if(playerRole.equals(Game.Role.ATTACKER)) {
			aiRole = Game.Role.DEFENDER;
			setDefenderId(1);
		} else {
			aiRole = Game.Role.ATTACKER;
			setAttackerId(1); //Potentially inefficient?
		}
		setMode(Mode.SINGLE); //Set singleplayer mode.

		//TODO: Remove next line, forces difficulty to HARD.
		setLevel(Game.Level.HARD);


	}

	/**
	 * Override end turn to make ai turns.
	 */
	public void endTurn() {
		if (getActiveRole().equals(Role.ATTACKER)) {
			setActiveRole(Role.DEFENDER);
		} else {
			setActiveRole(Role.ATTACKER);
			endRound();
		}
		if(!getState().equals(State.FINISHED)) {
			if(getActiveRole().equals(aiRole)) {
				aiMakeTurn();
			}
		}
	}

	/**
	 * Make AI turn based upon their role
	 * May have to wait for compile tim
	 */
	private void aiMakeTurn() {
		if(aiRole.equals(Role.ATTACKER)) {
			aiAttackerTurn();
		} else {
			aiDefenderTurn();
		}
	}

	/**
	 * Make AI attacker turn
	 */
	private void aiAttackerTurn() {
		//Choose a random unused mutant class of the CUT
		//Overwrite current class buffer as new mutant
	}

	/**
	 * Make AI defender turn
	 */
	private void aiDefenderTurn() {
		//Use defender strategy to select generated test(s)
		if(getLevel().equals(Level.HARD)) {
			//Use harder difficulty.
			//Run all generated tests for class.

			//Print something to show this block executes
			System.out.println("IT LIVES");
		} else {
			//Use easy difficulty - implementation not yet decided.
		}
	}
}

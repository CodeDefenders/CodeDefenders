package gammut;

import java.util.ArrayList;

public class GameState {

	public static final int ATTACKER = 0;
	public static final int DEFENDER = 1;

	private int round = 1;

	private String className;

	private int attackerScore = 0;
	private int defenderScore = 0;

	private ArrayList<Mutant> mutants = new ArrayList<Mutant>();
	private ArrayList<Test> tests = new ArrayList<Test>();

	private int turn;

	public GameState() {
		turn = ATTACKER;
	}


	public void setClassName(String s) {className = s;}
	public String getClassName() {return className;}

	// Call at end of a player's turn for housekeeping wrt current turn and round number
	public void endTurn() {
		// If end of attacker turn, switch to defender.
		if (turn == ATTACKER) {turn = DEFENDER;}
		// If end of defender turn, switch to attacker and advance game one round.
		else {
			turn = ATTACKER;
			round++;
		}
	}

	// If it is the provided player's turn, return true. Otherwise return false.
	public boolean isTurn(int player) {
		if (player == turn) {return true;}
		else {return false;}
	}

	public boolean isFinished() {
		if (round > 3) {return true;}
		else {return false;}
	}

	// Returns all mutants associated with this GameState.
	public ArrayList<Mutant> getMutants() {return mutants;}

	// Returns all alive mutants associated with this GameState.
	public ArrayList<Mutant> getAliveMutants() {
		ArrayList<Mutant> aliveMutants = new ArrayList<Mutant>();
		for (Mutant m : mutants) {
			if (m.isAlive()) {
				aliveMutants.add(m);
			}
		}
		return aliveMutants;
	}

	// Returns all tests associated with this GameState.
	public ArrayList<Test> getTests() {return tests;}

	public void addMutant(Mutant m) {mutants.add(m);}
	public void addTest(Test t) {tests.add(t);}

	public int getRound() {return round;}

	public int getScore(int player) {
		updateScores();
		if (player == ATTACKER) {return attackerScore;}
		else {return defenderScore;}
	}

	private void updateScores() {
		int tempAttackerScore = 0;

		for (Mutant m : mutants) {
			tempAttackerScore += m.getPoints();
		}

		int tempDefenderScore = 0;

		for (Test t : tests) {
			tempDefenderScore += t.getPoints();
		}
		attackerScore = tempAttackerScore;
		defenderScore = tempDefenderScore;
	}
}
package gammut;

import java.util.ArrayList;

public class Game {

	private int id;
	private int classId;
	private int attackerId;
	private int defenderId;
	private int currentRound;
	private int finalRound;
	private String activePlayer;
	private String state;

	public Game(int id, int attackerId, int defenderId, int classId, int currentRound, int finalRound, String activePlayer, String state) {
		this.id = id;
		this.attackerId = attackerId;
		this.defenderId = defenderId;
		this.classId = classId;
		this.currentRound = currentRound;
		this.finalRound = finalRound;
		this.activePlayer = activePlayer;
		this.state = state;
	}

	public int getId() {return id;}
	
	public int getClassId() {return classId;}
	public String getClassName() {return GameSelectionManager.getNameForClass(classId);}

	public int getAttackerId() {System.out.println(attackerId); return attackerId;}
	public int getDefenderId() {return defenderId;}

	public int getCurrentRound() {return currentRound;}
	public int getFinalRound() {return finalRound;}

	public String getActivePlayer() {return activePlayer;}
	// ATTACKER, DEFENDER, NEITHER

	public String getState() {return state;}
	// CREATED, IN PROGRESS, FINISHED

	public ArrayList<Mutant> getMutants() {return GameManager.getMutantsForGame(id);}
	public ArrayList<Mutant> getAliveMutants() {
		ArrayList<Mutant> aliveMutants = new ArrayList<Mutant>();
		for (Mutant m : getMutants()) {
			if (m.isAlive()) {
				aliveMutants.add(m);
			}
		}
		return aliveMutants;
	}

	public ArrayList<Test> getTests() {return GameManager.getTestsForGame(id);}

	public int getAttackerScore() {
		return 0;
	}

	public int getDefenderScore() {
		return 0;
	}

	public void endTurn() {
		if (activePlayer.equals("ATTACKER")) {activePlayer = "DEFENDER";}
		else if (activePlayer.equals("DEFENDER")) {activePlayer = "ATTACKER";}

		if (currentRound < finalRound) {currentRound++;}
		else if ((currentRound == finalRound)&&(state.equals("IN PROGRESS"))) {state = "FINISHED";}
	}
}
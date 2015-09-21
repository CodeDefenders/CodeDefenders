package gammut;

public class Game {

	private int id;
	private int classId;
	private int attackerId;
	private int defenderId;
	private int currentRound;
	private int finalRound;
	private String activePlayer;
	private String state;

	public Game(int gameId, int attackerId, int defenderId, int classId, int currentRound, int finalRound, String activePlayer, String state) {
		id = gameId;
		attackerId = attackerId;
		defenderId = defenderId;
		currentRound = currentRound;
		finalRound = finalRound;
		activePlayer = activePlayer;
		state = state;
	}

	public int getId() {return id;}
	public int getClassId() {return classId;}
	public int getAttackerId() {return attackerId;}
	public int getDefenderId() {return defenderId;}

	public int getCurrentRound() {return currentRound;}
	public int getFinalRound() {return finalRound;}

	public String getActivePlayer() {return activePlayer;}
	public String getState() {return state;}
}
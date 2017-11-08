package org.codedefenders.leaderboard;

/**
 * Created by jmr on 12/07/2017.
 */
public class Entry {
	private String username;
	private int mutantsSubmitted;
	private int attackerScore;
	private int testsSubmitted;
	private int defenderScore;
	private int mutantsKilled;
	private int totalPoints;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getMutantsSubmitted() {
		return mutantsSubmitted;
	}

	public void setMutantsSubmitted(int mutantsSubmitted) {
		this.mutantsSubmitted = mutantsSubmitted;
	}

	public int getAttackerScore() {
		return attackerScore;
	}

	public void setAttackerScore(int attackerScore) {
		this.attackerScore = attackerScore;
	}

	public int getTestsSubmitted() {
		return testsSubmitted;
	}

	public void setTestsSubmitted(int testsSubmitted) {
		this.testsSubmitted = testsSubmitted;
	}

	public int getDefenderScore() {
		return defenderScore;
	}

	public void setDefenderScore(int defenderScore) {
		this.defenderScore = defenderScore;
	}

	public int getMutantsKilled() {
		return mutantsKilled;
	}

	public void setMutantsKilled(int mutantsKilled) {
		this.mutantsKilled = mutantsKilled;
	}

	public int getTotalPoints() {
		return totalPoints;
	}

	public void setTotalPoints(int totalPoints) {
		this.totalPoints = totalPoints;
	}
}

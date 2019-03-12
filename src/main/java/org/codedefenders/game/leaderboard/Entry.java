/**
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.game.leaderboard;

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

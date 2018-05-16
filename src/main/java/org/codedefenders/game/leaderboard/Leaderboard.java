package org.codedefenders.game.leaderboard;

import org.codedefenders.database.DatabaseAccess;

import java.util.List;

/**
 * Created by jmr on 11/07/2017.
 */
public class Leaderboard {

	public static List<Entry> getAll() {
		return DatabaseAccess.getLeaderboard();
	}

}

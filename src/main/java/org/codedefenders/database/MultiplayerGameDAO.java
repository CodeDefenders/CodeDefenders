/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
package org.codedefenders.database;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.validation.code.CodeValidatorLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.codedefenders.database.DB.RSMapper;
import static org.codedefenders.database.DB.getDBV;

/**
 * This class handles the database logic for multiplayer games.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see MultiplayerGame
 */
public class MultiplayerGameDAO {

    /**
     * Constructs a {@link MultiplayerGame} from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game, or {@code null} if the game is no multiplayer game.
     * @see RSMapper
     */
    static MultiplayerGame multiplayerGameFromRS(ResultSet rs) throws SQLException {
        GameMode mode = GameMode.valueOf(rs.getString("Mode"));
        if (mode != GameMode.PARTY) {
            return null;
        }
        int id = rs.getInt("ID");
        int classId = rs.getInt("Class_ID");
        int creatorId = rs.getInt("Creator_ID");
        GameState state = GameState.valueOf(rs.getString("State"));
        GameLevel level = GameLevel.valueOf(rs.getString("Level"));
        long startTime = rs.getTimestamp("Start_Time").getTime();
        long finishTime = rs.getTimestamp("Finish_Time").getTime();
        int maxAssertionsPerTest = rs.getInt("MaxAssertionsPerTest");
        boolean chatEnabled = rs.getBoolean("ChatEnabled");
        CodeValidatorLevel mutantValidator = CodeValidatorLevel.valueOf(rs.getString("MutantValidator"));
        boolean markUncovered = rs.getBoolean("MarkUncovered");
        boolean capturePlayersIntention = rs.getBoolean("CapturePlayersIntention");
        int minDefenders = rs.getInt("Defenders_Needed");
        int minAttackers = rs.getInt("Attackers_Needed");
        boolean requiresValidation = rs.getBoolean("RequiresValidation");
        int defenderLimit = rs.getInt("Defenders_Limit");
        int attackerLimit = rs.getInt("Attackers_Limit");
        float lineCoverage = rs.getFloat("Coverage_Goal");
        float mutantCoverage = rs.getFloat("Mutant_Goal");
        int defenderValue = rs.getInt("Defender_Value");
        int attackerValue = rs.getInt("Attacker_Value");

        return new MultiplayerGame.Builder(classId, creatorId, startTime, finishTime, maxAssertionsPerTest, defenderLimit, attackerLimit, minDefenders, minAttackers)
                .id(id)
                .state(state)
                .level(level)
                .attackerValue(attackerValue)
                .defenderValue(defenderValue)
                .chatEnabled(chatEnabled)
                .markUncovered(markUncovered)
                .capturePlayersIntention(capturePlayersIntention)
                .mutantValidatorLevel(mutantValidator)
                .requiresValidation(requiresValidation)
                .lineCoverage(lineCoverage)
                .mutantCoverage(mutantCoverage)
                .build();
    }

    /**
     * Stores a given {@link MultiplayerGame} in the database.
     * <p>
     * This method does not update the given game object.
     * Use {@link MultiplayerGame#insert()} instead.
     *
     * @param game the given game as a {@link MultiplayerGame}.
     * @return the generated identifier of the game as an {@code int}.
     * @throws UncheckedSQLException If storing the game was not successful.
     */
    public static int storeMultiplayerGame(MultiplayerGame game) throws UncheckedSQLException {
        int classId = game.getClassId();
        GameLevel level = game.getLevel();
        float prize = game.getPrize();
        int defenderValue = game.getDefenderValue();
        int attackerValue = game.getAttackerValue();
        float lineCoverage = game.getLineCoverage();
        float mutantCoverage = game.getMutantCoverage();
        int creatorId = game.getCreatorId();
        int minAttackers = game.getMinAttackers();
        int minDefenders = game.getMinDefenders();
        int attackerLimit = game.getAttackerLimit();
        int defenderLimit = game.getDefenderLimit();
        long startDateTime = game.getStartDateTime();
        long finishDateTime = game.getFinishDateTime();
        GameState state = game.getState();
        int maxAssertionsPerTest = game.getMaxAssertionsPerTest();
        boolean chatEnabled = game.isChatEnabled();
        CodeValidatorLevel mutantValidatorLevel = game.getMutantValidatorLevel();
        boolean markUncovered = game.isMarkUncovered();
        boolean capturePlayersIntention = game.isCapturePlayersIntention();
        GameMode mode = game.getMode();

        String query = String.join("\n",
                "INSERT INTO games",
                "(Class_ID,",
                "Level,",
                "Prize,",
                "Defender_Value,",
                "Attacker_Value,",
                "Coverage_Goal,",
                "Mutant_Goal,",
                "Creator_ID,",
                "Attackers_Needed,",
                "Defenders_Needed,",
                "Attackers_Limit,",
                "Defenders_Limit,",
                "Start_Time,",
                "Finish_Time,",
                "State,",
                "Mode,",
                "MaxAssertionsPerTest,",
                "ChatEnabled,",
                "MutantValidator,",
                "MarkUncovered,",
                "CapturePlayersIntention)",
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(classId),
                DB.getDBV(level.name()),
                DB.getDBV(prize),
                DB.getDBV(defenderValue),
                DB.getDBV(attackerValue),
                DB.getDBV(lineCoverage),
                DB.getDBV(mutantCoverage),
                DB.getDBV(creatorId),
                DB.getDBV(minAttackers),
                DB.getDBV(minDefenders),
                DB.getDBV(attackerLimit),
                DB.getDBV(defenderLimit),
                DB.getDBV(new Timestamp(startDateTime)),
                DB.getDBV(new Timestamp(finishDateTime)),
                DB.getDBV(state.name()),
                DB.getDBV(mode.name()),
                DB.getDBV(maxAssertionsPerTest),
                DB.getDBV(chatEnabled),
                DB.getDBV(mutantValidatorLevel.name()),
                DB.getDBV(markUncovered),
                DB.getDBV(capturePlayersIntention)
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new UncheckedSQLException("Could not store multiplayer game to database.");
        }
    }

    /**
     * Updates a given {@link MultiplayerGame} in the database.
     * <p>
     * This method does not update the given game object.
     *
     * @param game the given game as a {@link MultiplayerGame}.
     * @return {@code true} if updating was successful, {@code false} otherwise.
     */
    public static boolean updateMultiplayerGame(MultiplayerGame game) {
        int classId = game.getClassId();
        GameLevel level = game.getLevel();
        float prize = game.getPrize();
        int defenderValue = game.getDefenderValue();
        int attackerValue = game.getAttackerValue();
        float lineCoverage = game.getLineCoverage();
        float mutantCoverage = game.getMutantCoverage();
        int id = game.getId();
        GameState state = game.getState();

        String query = String.join("\n",
                "UPDATE games",
                "SET Class_ID = ?,",
                "    Level = ?,",
                "    Prize = ?,",
                "    Defender_Value = ?,",
                "    Attacker_Value = ?,",
                "    Coverage_Goal = ?,",
                "    Mutant_Goal = ?,",
                "    State = ?",
                "WHERE ID = ?"
        );
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(classId),
                DB.getDBV(level.name()),
                DB.getDBV(prize),
                DB.getDBV(defenderValue),
                DB.getDBV(attackerValue),
                DB.getDBV(lineCoverage),
                DB.getDBV(mutantCoverage),
                DB.getDBV(state.name()),
                DB.getDBV(id)};

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Returns a {@link MultiplayerGame} for a given game identifier or
     * {@code null} if no game was found or the game mode differs.
     *
     * @param gameId the game identifier.
     * @return a {@link MultiplayerGame} instance or {@code null} if none matching game was found.
     */
    public static MultiplayerGame getMultiplayerGame(int gameId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM games",
                "WHERE ID=?",
                "  AND Mode = ?");

        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(gameId),
                DB.getDBV(GameMode.PARTY.name())
        };

        return DB.executeQueryReturnValue(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of all {@link MultiplayerGame MultiplayerGames} which are not finished, i.e. available.
     *
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public static List<MultiplayerGame> getAvailableMultiplayerGames() {
        String query = String.join("\n",
                "SELECT *",
                "FROM games",
                "WHERE State != ?",
                "  AND Mode = ?",
                "  AND Finish_Time > NOW();");
        DatabaseValue[] values = new DatabaseValue[]{
                getDBV(GameState.FINISHED.name()),
                getDBV(GameMode.PARTY.name())
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of all {@link MultiplayerGame MultiplayerGames} which are joinable for a given user identifier.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public static List<MultiplayerGame> getOpenMultiplayerGamesForUser(int userId) {
        // TODO Phil 27/12/18: use a view for open multiplayer games
        String query = String.join("\n",
                "SELECT *" +
                        "FROM games AS g",
                "INNER JOIN (SELECT gatt.ID, sum(CASE WHEN Role = 'ATTACKER' THEN 1 ELSE 0 END) nAttackers, sum(CASE WHEN Role = 'DEFENDER' THEN 1 ELSE 0 END) nDefenders",
                "              FROM games AS gatt LEFT JOIN players ON gatt.ID = players.Game_ID AND players.Active = TRUE GROUP BY gatt.ID) AS nplayers",
                "  ON g.ID = nplayers.ID",
                "WHERE g.Mode='PARTY' AND g.Creator_ID!=? AND (g.State='CREATED' OR g.State='ACTIVE')",
                "  AND (g.RequiresValidation=FALSE OR (? IN (SELECT User_ID FROM users WHERE Validated=TRUE)))",
                "  AND g.ID NOT IN (SELECT g.ID FROM games g INNER JOIN players p ON g.ID=p.Game_ID WHERE p.User_ID=? AND p.Active=TRUE)",
                "  AND (nplayers.nAttackers < g.Attackers_Limit OR nplayers.nDefenders < g.Defenders_Limit);");

        DatabaseValue[] values = new DatabaseValue[]{
                getDBV(userId),
                getDBV(userId),
                getDBV(userId)
        };

        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of active {@link MultiplayerGame MultiplayerGames}, which are created or
     * played by a given user.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public static List<MultiplayerGame> getMultiplayerGamesForUser(int userId) {
        String query = String.join("\n",
                "SELECT DISTINCT m.*",
                "FROM games AS m",
                "LEFT JOIN players AS p",
                "  ON p.Game_ID=m.ID",
                "    AND p.Active=TRUE",
                "WHERE m.Mode = ?",
                "  AND (p.User_ID = ? OR m.Creator_ID = ?)",
                "  AND m.State != ?;");
        DatabaseValue[] values = new DatabaseValue[]{
                getDBV(GameMode.PARTY.name()),
                getDBV(userId),
                getDBV(userId),
                getDBV(GameState.FINISHED.name())
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of active {@link MultiplayerGame MultiplayerGames}, which are
     * played by a given user.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public static List<MultiplayerGame> getJoinedMultiplayerGamesForUser(int userId) {
        String query = String.join("\n",
                "SELECT DISTINCT m.*",
                "FROM games AS m",
                "LEFT JOIN players AS p",
                "  ON p.Game_ID = m.ID \n",
                "WHERE m.Mode = ?",
                "  AND (p.User_ID = ?);");

        DatabaseValue[] values = new DatabaseValue[]{
                getDBV(GameMode.PARTY.name()),
                getDBV(userId)
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of {@link MultiplayerGame MultiplayerGames}, which were created or
     * played by a given user.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public static List<MultiplayerGame> getFinishedMultiplayerGamesForUser(int userId) {
        String query = String.join("\n",
                "SELECT DISTINCT m.* ",
                "FROM games AS m ",
                "LEFT JOIN players AS p ON p.Game_ID = m.ID ",
                "  AND p.Active = TRUE",
                "WHERE (p.User_ID = ? OR m.Creator_ID = ?)",
                "  AND m.State = ?",
                "  AND m.Mode = ?;");

        DatabaseValue[] values = new DatabaseValue[]{
                getDBV(userId),
                getDBV(userId),
                getDBV(GameState.FINISHED.name()),
                getDBV(GameMode.PARTY.name())
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of {@link MultiplayerGame MultiplayerGames}, which were created by a
     * given user and are not yet finished.
     *
     * @param creatorId the creator identifier the games are retrieved for.
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public static List<MultiplayerGame> getUnfinishedMultiplayerGamesCreatedBy(int creatorId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM games",
                "WHERE Mode = ?",
                "  AND (State = ?",
                "    OR State = ?)",
                "  AND Creator_ID = ?;");
        DatabaseValue[] values = new DatabaseValue[]{
                getDBV(GameMode.PARTY.name()),
                getDBV(GameState.ACTIVE.name()),
                getDBV(GameState.CREATED.name()),
                getDBV(creatorId)
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }
}

/**
 * Copyright (C) 2016-2018 Code Defenders contributors
 * <p>
 * This file is part of Code Defenders.
 * <p>
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * <p>
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.database;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.SinglePlayerGame;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.codedefenders.database.DB.RSMapper;

/**
 * This class handles the database logic for duel games.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see DuelGame
 */
public class DuelGameDAO {

    /**
     * Constructs a {@link DuelGame} from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed duel game.
     * @see RSMapper
     */
    static DuelGame duelGameFromRS(ResultSet rs) throws SQLException {
        DuelGame gameRecord;

        final int id = rs.getInt("ID");
        final int attackerId = rs.getInt("Attacker_ID");
        final int defenderId = rs.getInt("Defender_ID");
        final int classId = rs.getInt("Class_ID");
        final int currentRound = rs.getInt("CurrentRound");
        final int finalRound = rs.getInt("FinalRound");
        final Role activeRole = Role.valueOf(rs.getString("ActiveRole"));
        final GameState state = GameState.valueOf(rs.getString("State"));
        final GameLevel level = GameLevel.valueOf(rs.getString("Level"));
        final GameMode mode = GameMode.valueOf(rs.getString("Mode"));

        // FIXME Phil 28/12/18: I copied this from DatabaseAccess, why is this single case in here?
        switch (mode) {
            case SINGLE:
                gameRecord = new SinglePlayerGame(id, attackerId, defenderId, classId, currentRound, finalRound, activeRole, state, level, mode);
                break;
            default:
                gameRecord = new DuelGame(id, attackerId, defenderId, classId, currentRound, finalRound, activeRole, state, level, mode);
        }
        return gameRecord;
    }

    /**
     * Stores a given {@link DuelGame} in the database.
     * <p>
     * This method does not update the given game object.
     * Use {@link DuelGame#insert()} instead.
     *
     * @param game the given game as a {@link DuelGame}.
     * @return the generated identifier of the game as an {@code int}.
     * @throws UncheckedSQLException If storing the game was not successful.
     */
    public static int storeDuelGame(DuelGame game) throws UncheckedSQLException {
        String query = String.join("\n",
                "INSERT INTO games (Class_ID, Creator_ID, FinalRound, Level, Mode, State)",
                "VALUES (?, ?, ?, ?, ?, ?);");

        int classId = game.getClassId();
        int attackerId = game.getAttackerId();
        int defenderId = game.getDefenderId();
        int finalRound = game.getFinalRound();
        GameLevel level = game.getLevel();
        GameMode mode = game.getMode();
        GameState state = game.getState();

        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(classId),
                DB.getDBV((attackerId != 0) ? attackerId : defenderId),
                DB.getDBV(finalRound),
                DB.getDBV(level.name()),
                DB.getDBV(mode.name()),
                DB.getDBV(state.name())
        };
        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new UncheckedSQLException("Could not store duel game to database.");
        }
    }

    /**
     * Updates a given {@link DuelGame} in the database.
     * <p>
     * This method does not update the given game object.
     *
     * @param game the given game as a {@link DuelGame}.
     * @return {@code true} if updating was successful, {@code false} otherwise.
     */
    public static boolean updateDuelGame(DuelGame game) {
        int currentRound = game.getCurrentRound();
        int finalRound = game.getFinalRound();
        GameState state = game.getState();
        int id = game.getId();

        String query;
        DatabaseValue[] values;
        // FIXME Phil 28/12/18: I copied this from DatabaseAccess, why is this UTESTING case in here?
        switch (game.getMode()) {
            case UTESTING:
                query = String.join("\n",
                        "UPDATE games",
                        "SET CurrentRound=?,",
                        "    FinalRound=?,",
                        "    State=?",
                        "WHERE ID=?");
                values = new DatabaseValue[]{
                        DB.getDBV(currentRound),
                        DB.getDBV(finalRound),
                        DB.getDBV(state.name()),
                        DB.getDBV(id)
                };
                break;
            default:
                Role activeRole = game.getActiveRole();

                query = String.join("\n",
                        "UPDATE games",
                        "SET CurrentRound=?,",
                        "    FinalRound=?,",
                        "    ActiveRole=?,",
                        "    State=?",
                        "WHERE ID=?");
                values = new DatabaseValue[]{
                        DB.getDBV(currentRound),
                        DB.getDBV(finalRound),
                        DB.getDBV(activeRole.toString()),
                        DB.getDBV(state.name()),
                        DB.getDBV(id)
                };
        }
        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Returns a {@link DuelGame} for a given game identifier or
     * {@code null} if no game was found or the game mode differs.
     *
     * @param gameId the game identifier.
     * @return a {@link DuelGame} instance or {@code null} if none matching game was found.
     */
    public static DuelGame getDuelGameForId(int gameId) {
        String query = String.join("\n",
                "SELECT g.*,",
                "  IFNULL(att.User_ID,0) AS Attacker_ID,",
                "  IFNULL(def.User_ID,0) AS Defender_ID",
                "FROM games AS g",
                "LEFT JOIN players AS att",
                "  ON g.ID=att.Game_ID",
                "  AND att.Role='ATTACKER'",
                "  AND att.Active=TRUE",
                "LEFT JOIN players AS def",
                "  ON g.ID=def.Game_ID",
                "  AND def.Role='DEFENDER'",
                "  AND def.Active=TRUE",
                "WHERE g.ID = ?"
//                "  AND Mode = ?"
        );
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(gameId),
//                DB.getDBV(GameMode.DUEL.name())
        };
        return DB.executeQueryReturnValue(query, DuelGameDAO::duelGameFromRS, values);
    }

    /**
     * Returns a list of active {@link DuelGame DuelGames}, which were created or
     * played by a given user.
     *
     * @param userId the identifier of the user.
     * @return a list of active duel games for the given user.
     */
    public static List<DuelGame> getDuelGamesForUser(int userId) {
        String query = String.join("\n",
                "SELECT games.*,",
                "       IFNULL(att.User_ID,0) AS Attacker_ID,",
                "       IFNULL(def.User_ID,0) AS Defender_ID",
                "FROM games",
                "LEFT JOIN players AS att",
                "    ON games.ID = att.Game_ID",
                "    AND att.Role = 'ATTACKER'",
                "    AND att.Active = TRUE",
                "LEFT JOIN players AS def",
                "    ON games.ID = def.Game_ID",
                "    AND def.Role='DEFENDER'",
                "    AND def.Active = TRUE",
                "WHERE games.Mode = 'DUEL'",
                "  AND games.State != 'FINISHED'",
                "  AND (games.Creator_ID = ?",
                "    OR IFNULL(att.User_ID,0) = ?",
                "    OR IFNULL(def.User_ID,0) = ?);"
        );
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(userId),
                DB.getDBV(userId),
                DB.getDBV(userId)
        };
        return DB.executeQueryReturnList(query, DuelGameDAO::duelGameFromRS, values);
    }

    /**
     * Retrieves a list of all joinable {@link DuelGame DuelGames}.
     *
     * @return a list of joinable {@link DuelGame DuelGames}, empty if none are found.
     */
    public static List<DuelGame> getOpenDuelGames() {
        String query = String.join("\n",
                "SELECT games.*,",
                "       IFNULL(att.User_ID,0) AS Attacker_ID,",
                "       IFNULL(def.User_ID,0) AS Defender_ID",
                "FROM games",
                "LEFT JOIN players AS att",
                "    ON games.ID=att.Game_ID",
                "    AND att.Role='ATTACKER'",
                "    AND att.Active=TRUE",
                "LEFT JOIN players AS def",
                "    ON games.ID=def.Game_ID",
                "    AND def.Role='DEFENDER'",
                "    AND def.Active=TRUE",
                "WHERE games.Mode = ?",
                "  AND games.State = ?;"
        );
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(GameMode.DUEL.name()),
                DB.getDBV(GameState.CREATED.name())
        };
        return DB.executeQueryReturnList(query, DuelGameDAO::duelGameFromRS, values);
    }
}

/*
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
package org.codedefenders.database;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.UserMultiplayerGameInfo;
import org.codedefenders.validation.code.CodeValidatorLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.codedefenders.database.DB.RSMapper;

/**
 * This class handles the database logic for multiplayer games.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
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

        GameClass cut = GameClassDAO.gameClassFromRS(rs);

        int id = rs.getInt("ID");
        int classId = rs.getInt("Class_ID");
        int creatorId = rs.getInt("Creator_ID");
        GameState state = GameState.valueOf(rs.getString("State"));
        GameLevel level = GameLevel.valueOf(rs.getString("Level"));
        int maxAssertionsPerTest = rs.getInt("MaxAssertionsPerTest");
        boolean forceHamcrest = rs.getBoolean("ForceHamcrest");
        boolean chatEnabled = rs.getBoolean("ChatEnabled");
        CodeValidatorLevel mutantValidator = CodeValidatorLevel.valueOf(rs.getString("MutantValidator"));
        boolean capturePlayersIntention = rs.getBoolean("CapturePlayersIntention");
        boolean requiresValidation = rs.getBoolean("RequiresValidation");
        float lineCoverage = rs.getFloat("Coverage_Goal");
        float mutantCoverage = rs.getFloat("Mutant_Goal");
        int defenderValue = rs.getInt("Defender_Value");
        int attackerValue = rs.getInt("Attacker_Value");

        int automaticMutantEquivalenceThreshold = rs.getInt("EquivalenceThreshold");

        return new MultiplayerGame.Builder(classId, creatorId, maxAssertionsPerTest, forceHamcrest)
                .cut(cut)
                .id(id)
                .state(state)
                .level(level)
                .attackerValue(attackerValue)
                .defenderValue(defenderValue)
                .chatEnabled(chatEnabled)
                .capturePlayersIntention(capturePlayersIntention)
                .mutantValidatorLevel(mutantValidator)
                .requiresValidation(requiresValidation)
                .lineCoverage(lineCoverage)
                .mutantCoverage(mutantCoverage)
                .automaticMutantEquivalenceThreshold(automaticMutantEquivalenceThreshold)
                .build();
    }

    /**
     * Constructs an open {@link UserMultiplayerGameInfo}, i.e. a game the user can join,
     * from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game information.
     * @see RSMapper
     */
    static UserMultiplayerGameInfo openGameInfoFromRS(ResultSet rs) throws SQLException {
        final int userId = rs.getInt("userId");
        final MultiplayerGame game = multiplayerGameFromRS(rs);
        final String creatorName = rs.getString("creatorName");

        return UserMultiplayerGameInfo.forOpen(userId, game, creatorName);
    }

    /**
     * Constructs an active {@link UserMultiplayerGameInfo}, i.e. a game the user participates in,
     * from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game information.
     * @see RSMapper
     */
    static UserMultiplayerGameInfo activeGameInfoFromRS(ResultSet rs) throws SQLException {
        final int userId = rs.getInt("userId");
        final MultiplayerGame game = multiplayerGameFromRS(rs);
        final Role role = Role.valueOrNull(rs.getString("playerRole"));
        final String creatorName = rs.getString("creatorName");

        return UserMultiplayerGameInfo.forActive(userId, game, role, creatorName);
    }

    /**
     * Constructs an active {@link UserMultiplayerGameInfo}, i.e. a game the user did participate,
     * from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game information.
     * @see RSMapper
     */
    static UserMultiplayerGameInfo finishedGameInfoFromRS(ResultSet rs) throws SQLException {
        final int userId = rs.getInt("userId");
        final MultiplayerGame game = multiplayerGameFromRS(rs);
        final String creatorName = rs.getString("creatorName");

        return UserMultiplayerGameInfo.forFinished(userId, game, creatorName);
    }

    /**
     * Stores a given {@link MultiplayerGame} in the database.
     *
     * <p>This method does not update the given game object.
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
        GameState state = game.getState();
        int maxAssertionsPerTest = game.getMaxAssertionsPerTest();
        boolean forceHamcrest = game.isForceHamcrest();
        boolean chatEnabled = game.isChatEnabled();
        CodeValidatorLevel mutantValidatorLevel = game.getMutantValidatorLevel();
        boolean capturePlayersIntention = game.isCapturePlayersIntention();
        GameMode mode = game.getMode();
        int automaticMutantEquivalenceThreshold = game.getAutomaticMutantEquivalenceThreshold();

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
                "State,",
                "Mode,",
                "MaxAssertionsPerTest,",
                "ForceHamcrest,",
                "ChatEnabled,",
                "MutantValidator,",
                "CapturePlayersIntention,",
                "EquivalenceThreshold)",
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");

        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(classId),
                DatabaseValue.of(level.name()),
                DatabaseValue.of(prize),
                DatabaseValue.of(defenderValue),
                DatabaseValue.of(attackerValue),
                DatabaseValue.of(lineCoverage),
                DatabaseValue.of(mutantCoverage),
                DatabaseValue.of(creatorId),
                DatabaseValue.of(state.name()),
                DatabaseValue.of(mode.name()),
                DatabaseValue.of(maxAssertionsPerTest),
                DatabaseValue.of(forceHamcrest),
                DatabaseValue.of(chatEnabled),
                DatabaseValue.of(mutantValidatorLevel.name()),
                DatabaseValue.of(capturePlayersIntention),
                DatabaseValue.of(automaticMutantEquivalenceThreshold)
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
     *
     * <p>This method does not update the given game object.
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
                DatabaseValue.of(classId),
                DatabaseValue.of(level.name()),
                DatabaseValue.of(prize),
                DatabaseValue.of(defenderValue),
                DatabaseValue.of(attackerValue),
                DatabaseValue.of(lineCoverage),
                DatabaseValue.of(mutantCoverage),
                DatabaseValue.of(state.name()),
                DatabaseValue.of(id)};

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
                "FROM view_battleground_games",
                "WHERE ID=?;");

        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId)
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
                "FROM view_battleground_games",
                "WHERE State != ?;");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(GameState.FINISHED.name())
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of all {@link UserMultiplayerGameInfo UserMultiplayerGameInfos} for games which are joinable
     * for a given user identifier.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfos}, empty if none are found.
     */
    public static List<UserMultiplayerGameInfo> getOpenMultiplayerGamesWithInfoForUser(int userId) {
        final String query = String.join("\n",
                "SELECT DISTINCT g.*,",
                "    u.User_ID AS `userId`,",
                "    (SELECT creators.Username",
                "       FROM view_valid_users creators",
                "       WHERE g.Creator_ID = creators.User_ID) AS creatorName",
                "FROM view_battleground_games AS g,",
                "    view_valid_users u",
                "WHERE u.User_ID = ?",
                "  AND (g.State = 'CREATED' OR g.State = 'ACTIVE')",
                "  AND g.Creator_ID != u.User_ID",
                "  AND g.ID NOT IN (SELECT ig.ID",
                "    FROM games ig",
                "    INNER JOIN players p ON ig.ID = p.Game_ID",
                "    WHERE p.User_ID = u.User_ID",
                "    AND p.Active = TRUE)",
                ";");

        return DB.executeQueryReturnList(query, MultiplayerGameDAO::openGameInfoFromRS, DatabaseValue.of(userId));
    }


    /**
     * Retrieves a list of all {@link UserMultiplayerGameInfo UserMultiplayerGameInfos} for games
     * a given user has created or joined.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfos}, empty if none are found.
     */
    public static List<UserMultiplayerGameInfo> getActiveMultiplayerGamesWithInfoForUser(int userId) {
        final String query = String.join("\n",
                "SELECT g.*,",
                "       cu.User_ID                 as userId,",
                "       IFNULL(p.Role, 'OBSERVER') as playerRole,",
                "       vu.Username                as creatorName",
                "FROM view_battleground_games g",
                "         INNER JOIN view_valid_users vu",
                "                    ON g.Creator_ID = vu.User_ID",
                "         INNER JOIN view_valid_users cu",
                "                    ON cu.User_ID = ?",
                "         LEFT JOIN players p",
                "                   ON cu.User_ID = p.User_ID",
                "                       AND g.ID = p.Game_ID",
                "WHERE (g.State = 'CREATED' or g.State = 'ACTIVE')",
                "  AND (cu.User_ID = g.Creator_ID",
                "    OR (cu.User_ID = p.User_ID AND p.Active = TRUE))",
                "GROUP BY g.ID;");

        return DB.executeQueryReturnList(query, MultiplayerGameDAO::activeGameInfoFromRS, DatabaseValue.of(userId));
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
                "FROM view_battleground_games AS m",
                "LEFT JOIN players AS p",
                "  ON p.Game_ID = m.ID \n",
                "WHERE (p.User_ID = ?);");

        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(userId)
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfo objects},
     * which were created or played by a given user, but are finished.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfos}, empty if none are found.
     */
    public static List<UserMultiplayerGameInfo> getFinishedMultiplayerGamesForUser(int userId) {
        final String query = String.join("\n",
                "SELECT g.*,",
                "       cu.User_ID                 as userId,",
                "       IFNULL(p.Role, 'OBSERVER') as playerRole,",
                "       vu.Username                as creatorName",
                "FROM view_battleground_games g",
                "         INNER JOIN view_valid_users vu",
                "                    ON g.Creator_ID = vu.User_ID",
                "         INNER JOIN view_valid_users cu",
                "                    ON cu.User_ID = ?",
                "         LEFT JOIN players p",
                "                   ON cu.User_ID = p.User_ID",
                "                       AND g.ID = p.Game_ID",
                "WHERE (g.State = 'FINISHED'",
                "    AND (cu.User_ID = g.Creator_ID",
                "        OR (cu.User_ID = p.User_ID AND p.Active = TRUE)))",
                "GROUP BY g.ID;");
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::finishedGameInfoFromRS, DatabaseValue.of(userId));
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
                "FROM view_battleground_games",
                "WHERE (State = ?",
                "    OR State = ?)",
                "  AND Creator_ID = ?;");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(GameState.ACTIVE.name()),
                DatabaseValue.of(GameState.CREATED.name()),
                DatabaseValue.of(creatorId)
        };
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS, values);
    }

    /**
     * Retrieves the game in which the player plays.
     *
     * @param playerId The id of the player for which we lookup a game
     * @return
     */
    public static AbstractGame getGameWherePlayerPlays(int playerId) {
        String query = String.join("\n",
                "SELECT DISTINCT m.*",
                "FROM view_battleground_games AS m",
                "LEFT JOIN players AS p",
                "  ON p.Game_ID = m.ID \n",
                "WHERE (p.ID = ?);");

        DatabaseValue[] values = new DatabaseValue[]{DatabaseValue.of(playerId)};
        return DB.executeQueryReturnValue(query, MeleeGameDAO::meleeGameFromRS, values);
    }
}

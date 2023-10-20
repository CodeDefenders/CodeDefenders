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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.UserMeleeGameInfo;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.intellij.lang.annotations.Language;

/**
 * This class handles the database logic for melee games.
 *
 * @author gambi
 * @see MeleeGame
 */
public class MeleeGameDAO {

    /**
     * Constructs a {@link MeleeGame} from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed melee game, or {@code null} if the game is no melee game.
     * @see RSMapper
     */
    static MeleeGame meleeGameFromRS(ResultSet rs) throws SQLException {
        GameMode mode = GameMode.valueOf(rs.getString("Mode"));
        if (mode != GameMode.MELEE) {
            return null;
        }
        GameClass cut = GameClassDAO.gameClassFromRS(rs);
        int id = rs.getInt("ID");
        int classId = rs.getInt("Class_ID");
        int creatorId = rs.getInt("Creator_ID");
        GameState state = GameState.valueOf(rs.getString("State"));
        GameLevel level = GameLevel.valueOf(rs.getString("Level"));
        int maxAssertionsPerTest = rs.getInt("MaxAssertionsPerTest");
        boolean chatEnabled = rs.getBoolean("ChatEnabled");
        CodeValidatorLevel mutantValidator = CodeValidatorLevel.valueOf(rs.getString("MutantValidator"));
        boolean capturePlayersIntention = rs.getBoolean("CapturePlayersIntention");
        boolean requiresValidation = rs.getBoolean("RequiresValidation");
        float lineCoverage = rs.getFloat("Coverage_Goal");
        float mutantCoverage = rs.getFloat("Mutant_Goal");

        // These are read but never used. We always set them to 0
        int defenderValue = rs.getInt("Defender_Value");
        int attackerValue = rs.getInt("Attacker_Value");

        int gameDuration = rs.getInt("Game_Duration_Minutes");
        long startTime = rs.getLong("Timestamp_Start");

        int automaticMutantEquivalenceThreshold = rs.getInt("EquivalenceThreshold");

        Integer classroomId = rs.getInt("Classroom_ID");
        if (rs.wasNull()) {
            classroomId = null;
        }

        return new MeleeGame.Builder(classId, creatorId, maxAssertionsPerTest)
                .cut(cut)
                .id(id)
                .state(state)
                .level(level)
                .chatEnabled(chatEnabled)
                .capturePlayersIntention(capturePlayersIntention)
                .mutantValidatorLevel(mutantValidator)
                .requiresValidation(requiresValidation)
                .lineCoverage(lineCoverage)
                .mutantCoverage(mutantCoverage)
                .gameDurationMinutes(gameDuration)
                .startTimeUnixSeconds(startTime)
                .automaticMutantEquivalenceThreshold(automaticMutantEquivalenceThreshold)
                .classroomId(classroomId)
                .build();
    }

    /**
     * Constructs an open {@link UserMeleeGameInfo}, i.e. a melee game the user can
     * join, from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed melee game information.
     * @see RSMapper
     */
    static UserMeleeGameInfo openMeleeGameInfoFromRS(ResultSet rs) throws SQLException {
        final int userId = rs.getInt("userId");
        final MeleeGame game = meleeGameFromRS(rs);
        final String creatorName = rs.getString("creatorName");

        return UserMeleeGameInfo.forOpen(userId, game, creatorName);
    }

    /**
     * Constructs an active {@link UserMeleeGameInfo}, i.e. a game the user
     * participates in, from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed melee game information.
     * @see RSMapper
     */
    static UserMeleeGameInfo activeMeleeGameInfoFromRS(ResultSet rs) throws SQLException {
        final int userId = rs.getInt("userId");
        final MeleeGame game = meleeGameFromRS(rs);
        final String creatorName = rs.getString("creatorName");
        final Role role = Role.valueOrNull(rs.getString("playerRole"));
        return UserMeleeGameInfo.forActive(userId, game, role, creatorName);
    }

    /**
     * Constructs an active {@link UserMeleeGameInfo}, i.e. a game the user did
     * participate, from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed melee game information.
     * @see RSMapper
     */
    static UserMeleeGameInfo finishedGameInfoFromRS(ResultSet rs) throws SQLException {
        final int userId = rs.getInt("userId");
        final MeleeGame game = meleeGameFromRS(rs);
        final String creatorName = rs.getString("creatorName");

        return UserMeleeGameInfo.forFinished(userId, game, creatorName);
    }

    /**
     * Stores a given {@link MeleeGame} in the database.
     *
     * <p>This method does not update the given game object. Use
     * {@link MeleeGame#insert()} instead.
     *
     * @param game the given game as a {@link MeleeGame}.
     * @return the generated identifier of the game as an {@code int}.
     * @throws UncheckedSQLException If storing the game was not successful.
     */
    public static int storeMeleeGame(MeleeGame game) throws UncheckedSQLException {
        int classId = game.getClassId();
        GameLevel level = game.getLevel();
        float prize = game.getPrize();

        //int defenderValue = game.getDefenderValue();
        //int attackerValue = game.getAttackerValue();
        int defenderValue = 0;
        int attackerValue = 0;

        float lineCoverage = game.getLineCoverage();
        float mutantCoverage = game.getMutantCoverage();
        int creatorId = game.getCreatorId();
        GameState state = game.getState();
        int maxAssertionsPerTest = game.getMaxAssertionsPerTest();
        boolean chatEnabled = game.isChatEnabled();
        CodeValidatorLevel mutantValidatorLevel = game.getMutantValidatorLevel();
        boolean capturePlayersIntention = game.isCapturePlayersIntention();
        GameMode mode = game.getMode();
        int automaticMutantEquivalenceThreshold = game.getAutomaticMutantEquivalenceThreshold();
        int gameDurationMinutes = game.getGameDurationMinutes();
        Integer classroomId = game.getClassroomId().orElse(null);

        @Language("SQL") String query = """
                INSERT INTO games (
                    Class_ID,
                    Level,
                    Prize,
                    Defender_Value,
                    Attacker_Value,
                    Coverage_Goal,
                    Mutant_Goal,
                    Creator_ID,
                    State,
                    Mode,
                    MaxAssertionsPerTest,
                    ChatEnabled,
                    MutantValidator,
                    CapturePlayersIntention,
                    EquivalenceThreshold,
                    Game_Duration_Minutes,
                    Classroom_ID
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);
        """;

        DatabaseValue<?>[] values = new DatabaseValue[]{
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
                DatabaseValue.of(chatEnabled),
                DatabaseValue.of(mutantValidatorLevel.name()),
                DatabaseValue.of(capturePlayersIntention),
                DatabaseValue.of(automaticMutantEquivalenceThreshold),
                DatabaseValue.of(gameDurationMinutes),
                DatabaseValue.of(classroomId),
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new UncheckedSQLException("Could not store melee game to database.");
        }
    }

    /**
     * Updates a given {@link MeleeGame} in the database.
     *
     * <p>This method does not update the given game object.
     *
     * @param game the given game as a {@link MeleeGame}.
     * @return {@code true} if updating was successful, {@code false} otherwise.
     */
    public static boolean updateMeleeGame(MeleeGame game) {
        int classId = game.getClassId();
        GameLevel level = game.getLevel();
        float prize = game.getPrize();

        //int defenderValue = game.getDefenderValue();
        //int attackerValue = game.getAttackerValue();
        int defenderValue = 0;
        int attackerValue = 0;

        int duration = game.getGameDurationMinutes();
        float lineCoverage = game.getLineCoverage();
        float mutantCoverage = game.getMutantCoverage();
        int id = game.getId();
        GameState state = game.getState();

        @Language("SQL") String query = """
                UPDATE games
                SET Class_ID = ?,
                    Level = ?,
                    Prize = ?,
                    Defender_Value = ?,
                    Attacker_Value = ?,
                    Coverage_Goal = ?,
                    Mutant_Goal = ?,
                    State = ?,
                    Game_Duration_Minutes = ?
                WHERE ID = ?
        """;
        DatabaseValue<?>[] values = new DatabaseValue[] {
                DatabaseValue.of(classId),
                DatabaseValue.of(level.name()),
                DatabaseValue.of(prize),
                DatabaseValue.of(defenderValue),
                DatabaseValue.of(attackerValue),
                DatabaseValue.of(lineCoverage),
                DatabaseValue.of(mutantCoverage),
                DatabaseValue.of(state.name()),
                DatabaseValue.of(duration),
                DatabaseValue.of(id)
        };

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Returns a {@link MeleeGame} for a given game identifier or {@code null} if no
     * game was found or the game mode differs.
     *
     * @param gameId the game identifier.
     * @return a {@link MeleeGame} instance or {@code null} if none matching game was found.
     */
    public static MeleeGame getMeleeGame(int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_melee_games
                WHERE ID = ?;
        """;

        return DB.executeQueryReturnValue(query, MeleeGameDAO::meleeGameFromRS,
                DatabaseValue.of(gameId));
    }

    /**
     * Retrieves a list of all {@link MeleeGame} which are not finished, i.e.
     * available.
     *
     * @return a list of {@link MeleeGame}, empty if none are found.
     */
    public static List<MeleeGame> getAvailableMeleeGames() {
        @Language("SQL") String query = """
                SELECT *
                FROM view_melee_games
                WHERE State != ?;
        """;
        return DB.executeQueryReturnList(query, MeleeGameDAO::meleeGameFromRS,
                DatabaseValue.of(GameState.FINISHED.name()));
    }

    /**
     * Retrieves a list of all {@link UserMeleeGameInfo UserMeleeGameInfos} for
     * games which are joinable for a given user identifier.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link UserMeleeGameInfo UserMeleeGameInfos}, empty if none are found.
     */
    public static List<UserMeleeGameInfo> getOpenMeleeGamesWithInfoForUser(int userId) {
        @Language("SQL") final String query = """
                SELECT DISTINCT g.*,
                    u.User_ID AS `userId`,
                    (SELECT creators.Username
                       FROM view_valid_users creators
                       WHERE g.Creator_ID = creators.User_ID) AS creatorName
                FROM view_melee_games AS g,
                    view_valid_users u
                WHERE u.User_ID = ?
                  AND (g.State = 'CREATED' OR g.State = 'ACTIVE')
                  AND g.Creator_ID != u.User_ID
                  AND g.Classroom_ID IS NULL
                  AND g.ID NOT IN (SELECT ig.ID
                    FROM games ig
                    INNER JOIN players p ON ig.ID = p.Game_ID
                    WHERE p.User_ID = u.User_ID
                    AND p.Active = TRUE)
                ;
        """;

        return DB.executeQueryReturnList(query, MeleeGameDAO::openMeleeGameInfoFromRS, DatabaseValue.of(userId));
    }

    /**
     * Retrieves a list of all {@link UserMeleeGameInfo UserMeleeGameInfos} for
     * games a given user has created or joined.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link UserMeleeGameInfo UserMeleeGameInfos}, empty if none are found.
     */
    public static List<UserMeleeGameInfo> getActiveMeleeGamesWithInfoForUser(int userId) {
        @Language("SQL") final String query = """
                SELECT g.*,
                  cu.User_ID as userId,
                  IFNULL(p.Role, 'OBSERVER') as playerRole,
                  vu.Username as creatorName
                FROM view_melee_games g
                INNER JOIN view_valid_users vu
                ON g.Creator_ID = vu.User_ID
                INNER JOIN view_valid_users cu
                ON cu.User_ID = ?
                LEFT JOIN players p
                ON cu.User_ID = p.User_ID
                AND g.ID = p.Game_ID
                WHERE
                  (g.State = 'CREATED' or g.State = 'ACTIVE')
                   AND(cu.User_ID = g.Creator_ID
                       OR (cu.User_ID = p.User_ID AND p.Active = TRUE))
                GROUP BY g.ID
        """;

        return DB.executeQueryReturnList(query, MeleeGameDAO::activeMeleeGameInfoFromRS, DatabaseValue.of(userId));
    }

    /**
     * Retrieves a list of active {@link MeleeGame MeleeGames}, which are played by
     * a given user.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link MeleeGame MeleeGames}, empty if none are found.
     */
    public static List<MeleeGame> getJoinedMeleeGamesForUser(int userId) {
        @Language("SQL") String query = """
                SELECT DISTINCT m.*
                FROM view_melee_games AS m
                LEFT JOIN players AS p
                  ON p.Game_ID = m.ID
                WHERE (p.User_ID = ?);
        """;

        DatabaseValue<?>[] values = new DatabaseValue[]{DatabaseValue.of(userId)};
        return DB.executeQueryReturnList(query, MeleeGameDAO::meleeGameFromRS, values);
    }

    /**
     * Retrieves a list of {@link UserMeleeGameInfo UserMeleeGameInfo objects},
     * which were created or played by a given user, but are finished.
     *
     * @param userId the user identifier the games are retrieved for.
     * @return a list of {@link UserMeleeGameInfo UserMeleeGameInfos}, empty if none are found.
     */
    public static List<UserMeleeGameInfo> getFinishedMeleeGamesForUser(int userId) {
        @Language("SQL") final String query = """
                SELECT g.*,
                       cu.User_ID                 as userId,
                       IFNULL(p.Role, 'OBSERVER') as playerRole,
                       vu.Username                as creatorName
                FROM view_melee_games g
                         INNER JOIN view_valid_users vu
                                    ON g.Creator_ID = vu.User_ID
                         INNER JOIN view_valid_users cu
                                    ON cu.User_ID = ?
                         LEFT JOIN players p
                                   ON cu.User_ID = p.User_ID
                                       AND g.ID = p.Game_ID
                WHERE (g.State = 'FINISHED'
                    AND (cu.User_ID = g.Creator_ID
                        OR (cu.User_ID = p.User_ID AND p.Active = TRUE)))
                GROUP BY g.ID;
        """;
        return DB.executeQueryReturnList(query, MeleeGameDAO::finishedGameInfoFromRS, DatabaseValue.of(userId));
    }

    /**
     * Retrieves a list of {@link MeleeGame MeleeGames}, which were created by a
     * given user and are not yet finished.
     *
     * @param creatorId the creator identifier the games are retrieved for.
     * @return a list of {@link MeleeGame MeleeGames}, empty if none are found.
     */
    public static List<MeleeGame> getUnfinishedMeleeGamesCreatedBy(int creatorId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_melee_games
                WHERE (State = ? OR State = ?)
                  AND Creator_ID = ?;
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(GameState.ACTIVE.name()),
                DatabaseValue.of(GameState.CREATED.name()),
                DatabaseValue.of(creatorId)
        };
        return DB.executeQueryReturnList(query, MeleeGameDAO::meleeGameFromRS, values);
    }

    /**
     * Retrieves the game in which the player plays.
     *
     * @param playerId The id of the player we get the game for.
     */
    public static AbstractGame getGameWherePlayerPlays(int playerId) {
        @Language("SQL") String query = """
                SELECT DISTINCT m.*
                FROM view_melee_games AS m
                LEFT JOIN players AS p
                  ON p.Game_ID = m.ID
                WHERE (p.ID = ?);
        """;

        DatabaseValue<?>[] values = new DatabaseValue[]{DatabaseValue.of(playerId)};
        return DB.executeQueryReturnValue(query, MeleeGameDAO::meleeGameFromRS, values);
    }

    /**
     * Fetches all expired melee games.
     *
     * @return the expired melee games.
     */
    public static List<MeleeGame> getExpiredGames() {
        // do not use TIMESTAMPADD here to avoid errors with daylight saving
        @Language("SQL") String sql = """
                SELECT *
                FROM view_melee_games
                WHERE State = ?
                AND FROM_UNIXTIME(Timestamp_Start + Game_Duration_Minutes * 60) <= NOW();
        """;

        DatabaseValue<String> state = DatabaseValue.of(GameState.ACTIVE.toString());
        return DB.executeQueryReturnList(sql, MeleeGameDAO::meleeGameFromRS, state);
    }

    public static List<MeleeGame> getClassroomGames(int classroomId) {
        @Language("SQL") final String query = """
                SELECT * FROM view_melee_games as games
                WHERE games.Classroom_ID = ?;
        """;
        return DB.executeQueryReturnList(query, MeleeGameDAO::meleeGameFromRS,
                DatabaseValue.of(classroomId));
    }

    public static List<MultiplayerGame> getAvailableClassroomGames(int classroomId) {
        @Language("SQL") final String query = """
                SELECT * FROM view_melee_games as games
                WHERE games.Classroom_ID = ?
                AND games.State != ?;
        """;
        return DB.executeQueryReturnList(query, MultiplayerGameDAO::multiplayerGameFromRS,
                DatabaseValue.of(classroomId), DatabaseValue.of(GameState.FINISHED.name()));
    }
}

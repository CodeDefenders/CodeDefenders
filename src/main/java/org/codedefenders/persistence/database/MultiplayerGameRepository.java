/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.UserMultiplayerGameInfo;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * This class handles the database logic for multiplayer games.
 *
 * @see MultiplayerGame
 */
@ApplicationScoped
public class MultiplayerGameRepository {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public MultiplayerGameRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Constructs a {@link MultiplayerGame} from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game, or {@code null} if the game is no multiplayer game.
     */
    static MultiplayerGame multiplayerGameFromRS(ResultSet rs) throws SQLException {
        GameMode mode = GameMode.valueOf(rs.getString("Mode"));
        if (mode != GameMode.PARTY) {
            throw new IllegalArgumentException("Game is not a battleground game.");
        }

        GameClass cut = GameClassRepository.gameClassFromRS(rs);

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
        int defenderValue = rs.getInt("Defender_Value");
        int attackerValue = rs.getInt("Attacker_Value");
        int gameDuration = rs.getInt("Game_Duration_Minutes");
        long startTime = rs.getLong("Timestamp_Start");
        int automaticMutantEquivalenceThreshold = rs.getInt("EquivalenceThreshold");

        Integer classroomId = rs.getInt("Classroom_ID");
        if (rs.wasNull()) {
            classroomId = null;
        }

        return new MultiplayerGame.Builder(classId, creatorId, maxAssertionsPerTest)
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
                .gameDurationMinutes(gameDuration)
                .startTimeUnixSeconds(startTime)
                .automaticMutantEquivalenceThreshold(automaticMutantEquivalenceThreshold)
                .classroomId(classroomId)
                .build();
    }

    /**
     * Constructs an open {@link UserMultiplayerGameInfo}, i.e. a game the user can join,
     * from a {@link ResultSet} row.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game information.
     */
    static UserMultiplayerGameInfo openGameInfoFromRS(ResultSet rs) throws SQLException {
        final int userId = rs.getInt("userId");
        final MultiplayerGame game = multiplayerGameFromRS(rs);
        final String creatorName = rs.getString("creatorName");

        return UserMultiplayerGameInfo.forOpen(userId, game, creatorName);
    }

    /**
     * Constructs an active {@link UserMultiplayerGameInfo}, i.e. a game the user participates in,
     * from a {@link ResultSet} row.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game information.
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
     * from a {@link ResultSet} row.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed battleground game information.
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
     * @param game the given game as a {@link MultiplayerGame}.
     * @return The generated game ID.
     */
    public int storeMultiplayerGame(MultiplayerGame game) throws UncheckedSQLException {
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

        return queryRunner.insert(query,
                generatedKeyFromRS(),
                classId,
                level.name(),
                prize,
                defenderValue,
                attackerValue,
                lineCoverage,
                mutantCoverage,
                creatorId,
                state.name(),
                mode.name(),
                maxAssertionsPerTest,
                chatEnabled,
                mutantValidatorLevel.name(),
                capturePlayersIntention,
                automaticMutantEquivalenceThreshold,
                gameDurationMinutes,
                classroomId
        ).orElseThrow(() -> new UncheckedSQLException("Couldn't store game."));
    }

    /**
     * Updates a given {@link MultiplayerGame} in the database.
     *
     * <p>This method does not update the given game object.
     *
     * @param game the given game as a {@link MultiplayerGame}.
     * @return {@code true} if updating was successful, {@code false} otherwise.
     */
    public boolean updateMultiplayerGame(MultiplayerGame game) {
        int classId = game.getClassId();
        GameLevel level = game.getLevel();
        float prize = game.getPrize();
        int defenderValue = game.getDefenderValue();
        int attackerValue = game.getAttackerValue();
        float lineCoverage = game.getLineCoverage();
        float mutantCoverage = game.getMutantCoverage();
        int duration = game.getGameDurationMinutes();
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

        int updatedRows = queryRunner.update(query,
                classId,
                level.name(),
                prize,
                defenderValue,
                attackerValue,
                lineCoverage,
                mutantCoverage,
                state.name(),
                duration,
                id
        );
        return updatedRows > 0;
    }

    /**
     * Returns a {@link MultiplayerGame} for a given game identifier or
     * {@code null} if no game was found or the game mode differs.
     *
     * @param gameId The game ID.
     * @return a {@link MultiplayerGame} instance or {@code null} if none matching game was found.
     */
    public MultiplayerGame getMultiplayerGame(int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_battleground_games
                WHERE ID = ?;
        """;

        return queryRunner.query(query,
                oneFromRS(MultiplayerGameRepository::multiplayerGameFromRS),
                gameId
        ).orElse(null);
    }

    /**
     * Retrieves a list of all {@link MultiplayerGame MultiplayerGames} which are not finished, i.e. available.
     *
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public List<MultiplayerGame> getAvailableMultiplayerGames() {
        @Language("SQL") String query = """
                SELECT *
                FROM view_battleground_games
                WHERE State != ?;
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::multiplayerGameFromRS),
                GameState.FINISHED.name()
        );
    }

    /**
     * Retrieves a list of all {@link UserMultiplayerGameInfo UserMultiplayerGameInfos} which the given user can join.
     *
     * @param userId The user ID the games are retrieved for.
     * @return a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfos}, empty if none are found.
     */
    public List<UserMultiplayerGameInfo> getOpenMultiplayerGamesWithInfoForUser(int userId) {
        @Language("SQL") final String query = """
                SELECT DISTINCT g.*,
                    u.User_ID AS `userId`,
                    (SELECT creators.Username
                       FROM view_valid_users creators
                       WHERE g.Creator_ID = creators.User_ID) AS creatorName
                FROM view_battleground_games AS g,
                    view_valid_users u
                WHERE u.User_ID = ?
                  AND (g.State = 'CREATED' OR g.State = 'ACTIVE')
                  AND g.Creator_ID != u.User_ID
                  AND g.Classroom_ID IS NULL
                  AND g.ID NOT IN (SELECT ig.ID
                    FROM games ig
                    INNER JOIN players p ON ig.ID = p.Game_ID
                    WHERE p.User_ID = u.User_ID
                    AND p.Active = TRUE);
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::openGameInfoFromRS),
                userId
        );
    }

    /**
     * Retrieves a list of all {@link UserMultiplayerGameInfo UserMultiplayerGameInfos} for games
     * a given user has created or joined.
     *
     * @param userId The user ID the games are retrieved for.
     * @return a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfos}, empty if none are found.
     */
    public List<UserMultiplayerGameInfo> getActiveMultiplayerGamesWithInfoForUser(int userId) {
        @Language("SQL") final String query = """
                SELECT g.*,
                       cu.User_ID                 as userId,
                       IFNULL(p.Role, 'OBSERVER') as playerRole,
                       vu.Username                as creatorName
                FROM view_battleground_games g
                         INNER JOIN view_valid_users vu
                                    ON g.Creator_ID = vu.User_ID
                         INNER JOIN view_valid_users cu
                                    ON cu.User_ID = ?
                         LEFT JOIN players p
                                   ON cu.User_ID = p.User_ID
                                       AND g.ID = p.Game_ID
                WHERE (g.State = 'CREATED' or g.State = 'ACTIVE')
                  AND (cu.User_ID = g.Creator_ID
                    OR (cu.User_ID = p.User_ID AND p.Active = TRUE))
                GROUP BY g.ID;
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::activeGameInfoFromRS),
                userId
        );
    }

    /**
     * Retrieves a list of active {@link MultiplayerGame MultiplayerGames}, which are
     * played by a given user.
     *
     * @param userId The user ID the games are retrieved for.
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public List<MultiplayerGame> getJoinedMultiplayerGamesForUser(int userId) {
        @Language("SQL") String query = """
                SELECT DISTINCT m.*
                FROM view_battleground_games AS m
                LEFT JOIN players AS p
                  ON p.Game_ID = m.ID
                WHERE (p.User_ID = ?);
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::multiplayerGameFromRS),
                userId
        );
    }

    /**
     * Retrieves a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfo objects},
     * which were created or played by a given user, but are finished.
     *
     * @param userId The ID identifier the games are retrieved for.
     * @return a list of {@link UserMultiplayerGameInfo UserMultiplayerGameInfos}, empty if none are found.
     */
    public List<UserMultiplayerGameInfo> getFinishedMultiplayerGamesForUser(int userId) {
        @Language("SQL") final String query = """
                SELECT g.*,
                       cu.User_ID                 as userId,
                       IFNULL(p.Role, 'OBSERVER') as playerRole,
                       vu.Username                as creatorName
                FROM view_battleground_games g
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

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::finishedGameInfoFromRS),
                userId
        );
    }

    /**
     * Retrieves a list of {@link MultiplayerGame MultiplayerGames}, which were created by a
     * given user and are not yet finished.
     *
     * @param creatorId The user ID of the creator.
     * @return a list of {@link MultiplayerGame MultiplayerGames}, empty if none are found.
     */
    public List<MultiplayerGame> getUnfinishedMultiplayerGamesCreatedBy(int creatorId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_battleground_games
                WHERE (State = ?
                    OR State = ?)
                  AND Creator_ID = ?;
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::multiplayerGameFromRS),
                GameState.ACTIVE.name(),
                GameState.CREATED.name(),
                creatorId
        );
    }


    /**
     * Fetches all expired multiplayer games.
     *
     * @return the expired multiplayer games.
     */
    public List<MultiplayerGame> getExpiredGames() {
        // do not use TIMESTAMPADD here to avoid errors with daylight saving
        @Language("SQL") String query = """
                SELECT *
                FROM view_battleground_games
                WHERE State = ?
                  AND FROM_UNIXTIME(Timestamp_Start + Game_Duration_Minutes * 60) <= NOW();
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::multiplayerGameFromRS),
                GameState.ACTIVE.toString()
        );
    }

    public List<MultiplayerGame> getClassroomGames(int classroomId) {
        @Language("SQL") String query = """
                SELECT * FROM view_battleground_games as games
                WHERE games.Classroom_ID = ?;
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::multiplayerGameFromRS),
                classroomId
        );
    }

    public List<MultiplayerGame> getAvailableClassroomGames(int classroomId) {
        @Language("SQL") String query = """
                SELECT * FROM view_battleground_games as games
                WHERE games.Classroom_ID = ?
                AND games.State != ?;
        """;

        return queryRunner.query(query,
                listFromRS(MultiplayerGameRepository::multiplayerGameFromRS),
                classroomId,
                GameState.FINISHED.name()
        );
    }
}

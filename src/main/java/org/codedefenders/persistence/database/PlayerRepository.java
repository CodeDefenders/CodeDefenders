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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.Role;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.CDIUtil;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * This class handles the database logic for players.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see Player
 */
public class PlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public PlayerRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Constructs a player from a {@link ResultSet} entry.
     *
     * <p>Requires the user information to have the following column names. Naming
     * these columns should be done with SQL aliasing.
     *
     * <ul>
     * <li>{@code usersPassword}</li>
     * <li>{@code usersUsername}</li>
     * <li>{@code usersEmail}</li>
     * <li>{@code usersValidated}</li>
     * <li>{@code usersActive}</li>
     * <li>{@code usersAllowContact}</li>
     * <li>{@code usersKeyMap}</li>
     * </ul>
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed player together with an {@link UserEntity} instance.
     */
    static Player playerWithUserFromRS(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID");
        int userId = rs.getInt("User_ID");
        int gameId = rs.getInt("Game_ID");
        int points = rs.getInt("Points");
        Role role = Role.valueOf(rs.getString("Role"));
        boolean active = rs.getBoolean("Active");

        String password = rs.getString("usersPassword");
        String userName = rs.getString("usersUsername");
        String email = rs.getString("usersEmail");
        boolean validated = rs.getBoolean("usersValidated");
        boolean userActive = rs.getBoolean("usersActive");
        boolean allowContact = rs.getBoolean("usersAllowContact");
        KeyMap keyMap = KeyMap.valueOrDefault(rs.getString("usersKeyMap"));
        boolean keepPreviousTest = rs.getBoolean("usersKeepPreviousTest");

        final UserEntity user = new UserEntity(userId, userName, password, email, validated, userActive, allowContact,
                keyMap, keepPreviousTest);

        return new Player(id, user, gameId, points, role, active);
    }

    /**
     * Retrieves the player ID of a given user in a given game.
     */
    public int getPlayerIdForUserAndGame(int userId, int gameId) {
        @Language("SQL") String query = """
                SELECT players.ID
                FROM players
                WHERE User_ID = ?
                  AND Game_ID = ?
        """;

        // TODO: Return optional here
        var playerId = queryRunner.query(query,
                oneFromRS(rs -> rs.getInt("ID")),
                userId,
                gameId
        );
        return playerId.orElse(-1);
    }

    /**
     * Retrieves a player given its ID.
     */
    public Player getPlayer(int playerId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE ID = ?;
        """;

        var player = queryRunner.query(query,
                oneFromRS(PlayerRepository::playerWithUserFromRS),
                playerId
        );
        return player.orElse(null);
    }

    /**
     * Retrieves the player of a given user in a given game.
     */
    public Player getPlayerForUserAndGame(int userId, int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND User_ID = ?
                  AND Active = TRUE;
        """;

        var player = queryRunner.query(query,
                oneFromRS(PlayerRepository::playerWithUserFromRS),
                gameId,
                userId
        );
        return player.orElse(null);
    }

    public void setPlayerPoints(int points, int player) {
        @Language("SQL") String query = "UPDATE players SET Points = ? WHERE ID = ?";

        int updatedRows = queryRunner.update(query,
                points,
                player);
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update player.");
        }
    }

    public void increasePlayerPoints(int points, int player) {
        @Language("SQL") String query = "UPDATE players SET Points = Points + ? WHERE ID = ?";

        int updatedRows = queryRunner.update(query,
                points,
                player);
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update player.");
        }
    }

    public int getPlayerPoints(int playerId) {
        @Language("SQL") String query = "SELECT Points FROM players WHERE ID = ?;";

        var points = queryRunner.query(query,
                oneFromRS(rs -> rs.getInt("Points")),
                playerId
        );
        return points.orElse(0);
    }

    public Role getRole(int userId, int gameId) {
        QueryRunner queryRunner = CDIUtil.getBeanFromCDI(QueryRunner.class);

        @Language("SQL") String query = """
                SELECT *
                FROM players
                WHERE players.Active = TRUE
                  AND players.User_ID = ?
                  AND players.Game_ID = ?
        """;

        return queryRunner.query(query,
                oneFromRS(rs -> Role.valueOrNull(rs.getString("Role"))),
                userId,
                gameId
        ).orElse(Role.NONE);
    }
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.QueryUtils.makePlaceholders;
import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * This class handles the common database logic between games types.
 *
 * @see AbstractGame
 * @see MultiplayerGame
 * @see PuzzleGame
 */
@ApplicationScoped
public class GameRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameRepository.class);

    private final QueryRunner queryRunner;
    private final MeleeGameRepository meleeGameRepo;
    private final MultiplayerGameRepository multiplayerGameRepo;
    private final PuzzleRepository puzzleRepo;

    @Inject
    public GameRepository(QueryRunner queryRunner, MeleeGameRepository meleeGameRepo,
                          MultiplayerGameRepository multiplayerGameRepo, PuzzleRepository puzzleRepo) {
        this.queryRunner = queryRunner;
        this.meleeGameRepo = meleeGameRepo;
        this.multiplayerGameRepo = multiplayerGameRepo;
        this.puzzleRepo = puzzleRepo;
    }

    /**
     * Retrieves a game for which we don't know the type yet.
     *
     * @param gameId The game ID.
     * @return The {@link AbstractGame} with the given ID or null if no game found.
     */
    public AbstractGame getGame(int gameId) {
        var gameMode = getGameMode(gameId);
        return gameMode.map(mode -> switch (mode) {
            case PARTY -> multiplayerGameRepo.getMultiplayerGame(gameId);
            case MELEE -> meleeGameRepo.getMeleeGame(gameId);
            case PUZZLE -> puzzleRepo.getPuzzleGameForId(gameId);
            default -> null;
        }).orElse(null);
    }


    /**
     * Adds a player with the given user ID and {@link Role} to the game.
     * If user is already a player in the game, the {@link Role} is updated.
     *
     * @param gameId The game ID to add the player to.
     * @param userId The user ID.
     * @param role   The role.
     * @return {@code true} if the player was successfully added, {@code false} otherwise.
     */
    public boolean addPlayerToGame(int gameId, int userId, Role role) {
        @Language("SQL") String query = """
                INSERT INTO players (Game_ID, User_ID, Points, Role)
                VALUES (?, ?, 0, ?)
                ON DUPLICATE KEY UPDATE Role = ?, Active = TRUE;
        """;

        int numRows = queryRunner.update(query,
                gameId,
                userId,
                role.toString(),
                role.toString()
        );
        return numRows > 0;
    }

    /**
     * Retrieves all players identifiers for a given game ID and {@link Role}.
     *
     * @param gameId The game ID.
     * @param role The role to get players for.
     * @return The list of players.
     */
    public List<Player> getPlayersForGame(int gameId, Role role) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND Role = ?
                  AND Active = TRUE;
        """;

        return queryRunner.query(query, listFromRS(PlayerRepository::playerWithUserFromRS),
                gameId,
                role.toString());
    }

    /**
     * Retrieves all {@link Player Players}, that belong to valid users, for a given game id.
     *
     * @param gameId The id of the game the players are retrieved for.
     * @return The list of players.
     */
    public List<Player> getValidPlayersForGame(int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND Active = TRUE;
        """;

        return queryRunner.query(query, listFromRS(PlayerRepository::playerWithUserFromRS), gameId);
    }

    /**
     * Removes given user from a given game.
     *
     * @param gameId The game ID.
     * @param userId The user ID that is removed.
     * @return whether removing was successful or not.
     */
    public boolean removeUserFromGame(int gameId, int userId) {
        @Language("SQL") String query = """
                UPDATE players
                SET Active = FALSE
                WHERE Game_ID = ?
                  AND User_ID = ?;
        """;

        int updatedRows = queryRunner.update(query, gameId, userId);
        return updatedRows > 0;
    }

    public int getCurrentRound(int gameId) {
        @Language("SQL") String query = """
                SELECT CurrentRound
                FROM games
                WHERE games.ID = ?;
        """;

        var currentRound = queryRunner.query(query,
                oneFromRS(rs -> rs.getInt("CurrentRound")),
                gameId);
        return currentRound.orElseThrow();
    }

    /**
     * Checks for which of the given IDs games exist in the database.
     *
     * @param ids The game IDs to check.
     * @return The game IDs for which games exist.
     */
    public List<Integer> filterExistingGameIDs(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        @Language("SQL") String query = "SELECT ID FROM games WHERE ID IN (%s);"
                .formatted(makePlaceholders(ids.size()));

        return queryRunner.query(query,
                listFromRS(rs -> rs.getInt("ID")),
                ids.toArray());
    }


    /**
     * Returns the game mode for given game ID.
     *
     * @param gameId The game ID.
     * @return The game mode.
     */
    public Optional<GameMode> getGameMode(int gameId) {
        @Language("SQL") String query = "SELECT Mode FROM games WHERE ID = ?";

        return queryRunner.query(query,
                oneFromRS(rs -> GameMode.valueOf(rs.getString("Mode"))),
                gameId);
    }

    /**
     * Fetches a List of Multiplayer- and Melee-Games that have expired.
     *
     * @return the expired games.
     */
    public List<AbstractGame> getExpiredGames() {
        List<AbstractGame> games = new ArrayList<>();
        games.addAll(multiplayerGameRepo.getExpiredGames());
        games.addAll(meleeGameRepo.getExpiredGames());
        return games;
    }

    /**
     * Checks if a game is expired.
     *
     * @param gameId the game to check.
     * @return {@code true} if the game is active but expired.
     */
    public boolean isGameExpired(int gameId) {
        // do not use TIMESTAMPADD here to avoid errors with daylight saving
        @Language("SQL") final String query = """
                SELECT FROM_UNIXTIME(UNIX_TIMESTAMP(Start_Time) + Game_Duration_Minutes * 60) <= NOW() AS isExpired
                FROM games
                WHERE ID = ?;
        """;

        var isExpired = queryRunner.query(query,
                oneFromRS(l -> l.getBoolean("isExpired")),
                gameId
        );
        return isExpired.orElseThrow();
    }

    public boolean storeStartTime(int gameId) {
        @Language("SQL") String query = """
                UPDATE games
                SET Start_Time = NOW()
                WHERE ID = ?
        """;

        int updatedRows = queryRunner.update(query, gameId);
        return updatedRows > 0;
    }

    /**
     * Stores a new invitation link id. If the game is null, the link id is stored without a game,
     * the game will have to be added later.
     * @param gameId The id of the game to store the link for, or null if that game is not yet created.
     * @return The generated ID of the invitation link.
     */
    public int storeInvitationLink(Integer gameId) {
        @Language("SQL") String query = """
                INSERT INTO invitation_links (game_id)
                VALUES (?)
        """;

        return queryRunner.insert(query, generatedKeyFromRS(), gameId).orElseThrow();
    }

    /**
     * Retrieves the game for a given invitation link ID.
     *
     * @param inviteId The invitation link ID.
     * @return The game ID or null if no game found.
     */
    public AbstractGame getGameForInviteId(int inviteId) {
        @Language("SQL") String query = """
                SELECT game_id
                FROM invitation_links
                WHERE invitation_id = ?
        """;
        //TODO This could be improved by using a join
        int gameId = queryRunner.query(query, oneFromRS(rs -> rs.getInt("game_id")), inviteId).orElseThrow();
        return getGame(gameId);
    }

    /**
     *
     */
    public List<String> getUsernamesForGame(int gameId) {
        @Language("SQL") String query = """
                SELECT distinct Username FROM games, players, users
                WHERE games.ID = ?
                  AND players.Game_ID = games.ID
                  AND players.User_ID = users.User_ID;
        """;

        return queryRunner.query(query, listFromRS(rs -> rs.getString("UserName")), gameId);
    }
}

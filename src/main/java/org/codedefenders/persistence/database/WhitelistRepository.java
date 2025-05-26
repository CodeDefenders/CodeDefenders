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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.codedefenders.model.WhitelistElement;
import org.codedefenders.model.WhitelistType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.Constants;
import org.intellij.lang.annotations.Language;

public class WhitelistRepository {

    private final QueryRunner queryRunner;

    @Inject
    public WhitelistRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public void addToWhitelist(int gameId, int playerId) {
        @Language("SQL") String query = "INSERT INTO whitelist (game_id, user_id) VALUES (?, ?)";
        queryRunner.update(query, gameId, playerId);
    }

    public void addToWhitelist(int gameId, int playerId, WhitelistType type) {

        @Language("SQL") String query = "INSERT INTO whitelist (game_id, user_id, type) VALUES (?, ?, ?)";
        queryRunner.update(query, gameId, playerId, getDBEnum(type));
    }

    public void removeFromWhitelist(int gameId, int playerId) {
        @Language("SQL") String query = "DELETE FROM whitelist WHERE game_id = ? AND user_id = ?";
        queryRunner.update(query, gameId, playerId);
    }

    public boolean isWhitelisted(int gameId, int playerId) {
        @Language("SQL") String query = "SELECT game_id FROM whitelist WHERE game_id = ? AND user_id = ? LIMIT 1";
        return queryRunner.query(query, ResultSet::next, gameId, playerId);
    }

    public List<Integer> getWhiteListedPlayerIds(int gameId) {
        @Language("SQL") String query = "SELECT user_id FROM whitelist WHERE game_id = ?";
        return queryRunner.query(query, rs -> {
            List<Integer> playerIds = new java.util.ArrayList<>();
            while (rs.next()) {
                playerIds.add(rs.getInt("user_id"));
            }
            return playerIds;
        }, gameId);
    }

    public List<String> getWhiteListedPlayerNames(int gameId) {
        @Language("SQL") String query = "SELECT Username FROM " +
                "whitelist JOIN users ON whitelist.user_id = users.User_ID" +
                " WHERE game_id = ?";
        return queryRunner.query(query, rs -> {
            List<String> playerNames = new java.util.ArrayList<>();
            while (rs.next()) {
                playerNames.add(rs.getString("Username"));
            }
            return playerNames;
        }, gameId);
    }

    public List<String> getWhiteListedPlayerNames(int gameId, WhitelistType type) {
        @Language("SQL") String query = "SELECT Username FROM " +
                "whitelist JOIN users ON whitelist.user_id = users.User_ID" +
                " WHERE game_id = ? AND type = ?";
        return queryRunner.query(query, rs -> {
            List<String> playerNames = new java.util.ArrayList<>();
            while (rs.next()) {
                playerNames.add(rs.getString("Username"));
            }
            return playerNames;
        }, gameId, getDBEnum(type));
    }

    public WhitelistType getWhitelistType(int gameId, int playerId) {
        if (playerId == Constants.DUMMY_DEFENDER_USER_ID) {
            return WhitelistType.DEFENDER; // Dummy defender always has defender role
        } else if (playerId == Constants.DUMMY_ATTACKER_USER_ID) {
            return WhitelistType.ATTACKER; // Dummy attacker always has attacker role
        }

        @Language("SQL") String query = "SELECT type FROM whitelist WHERE game_id = ? AND user_id = ?";
        return queryRunner.query(query, rs -> {
            if (rs.next()) {
                return WhitelistType.fromString(rs.getString("type"));
            }
            return null;
        }, gameId, playerId);
    }

    public WhitelistType getWhitelistType(int gameId, String username) {
        @Language("SQL") String query = "SELECT type FROM whitelist JOIN users " +
                "ON whitelist.user_id = users.User_ID " +
                "WHERE game_id = ? AND Username = ?";
        return queryRunner.query(query, rs -> {
            if (rs.next()) {
                return WhitelistType.fromString(rs.getString("type"));
            }
            return null;
        }, gameId, username);
    }

    public Set<WhitelistElement> getWhitelist(int gameId) {
        @Language("SQL") String query = "SELECT Username, type FROM whitelist JOIN users on whitelist.user_id = users.User_ID WHERE game_id = ?";
        return queryRunner.query(query, rs -> {
            Set<WhitelistElement> whitelist = new HashSet<>();
            while (rs.next()) {
                String username = rs.getString("Username");
                String typeStr = rs.getString("type");
                WhitelistType type = WhitelistType.fromString(typeStr);
                whitelist.add(new WhitelistElement(username, type));
            }
            return whitelist;
        }, gameId);
    }


    private String getDBEnum(WhitelistType type) {
        switch (type) {
            case FLEX -> {
                return "flex";
            }
            case CHOICE -> {
                return "choice";
            }
            case ATTACKER -> {
                return "attacker";
            }
            case DEFENDER -> {
                return "defender";
            }
            default -> throw new IllegalArgumentException("Unknown enum type: " + type);
        }
    }
}

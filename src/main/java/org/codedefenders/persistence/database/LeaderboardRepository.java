/*
 * Copyright (C) 2021 Code Defenders contributors
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.entity.LeaderboardEntryEntity;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

@ApplicationScoped
public class LeaderboardRepository {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardRepository.class);

    private final QueryRunner queryRunner;

    @Language("SQL")
    private static final String baseQuery = """
            SELECT U.username AS username,
              IFNULL(NMutants,0) AS NMutants,
              IFNULL(AScore,0) AS AScore,
              IFNULL(NTests,0) AS NTests,
              IFNULL(DScore,0) AS DScore,
              IFNULL(NKilled,0) AS NKilled,
              IFNULL(AScore,0)+IFNULL(DScore,0)+IFNULL(EScore,0) AS TotalScore
            FROM view_valid_users U
            LEFT JOIN (
                SELECT PA.user_id, count(M.Mutant_ID) AS NMutants, sum(M.Points) AS AScore
                FROM players PA
                LEFT JOIN mutants M ON PA.id = M.Player_ID
                GROUP BY PA.user_id
            ) AS Attacker ON U.user_id = Attacker.user_id
            LEFT JOIN (
                SELECT PD.user_id, count(T.Test_ID) AS NTests, sum(T.Points) AS DScore, sum(T.MutantsKilled) AS NKilled
                FROM players PD
                LEFT JOIN tests T ON PD.id = T.Player_ID
                GROUP BY PD.user_id
            ) AS Defender ON U.user_id = Defender.user_id
            LEFT JOIN (
                SELECT PE.user_id, sum(PE.Points) AS EScore
                FROM players PE
                GROUP BY PE.user_id
            ) AS Player ON U.User_ID = Player.User_ID
    """;

    @Inject
    public LeaderboardRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    @Nonnull
    public List<LeaderboardEntryEntity> getLeaderboard() {
        @Language("SQL") String query = baseQuery + ";";

        try {
            return queryRunner
                    .query(query, rs -> listFromRS(rs, LeaderboardRepository::leaderboardEntryFromRS));
        } catch (SQLException e) {
            logger.error("Exception while querying leaderboard", e);
        }

        return new ArrayList<>();
    }

    @Nonnull
    public Optional<LeaderboardEntryEntity> getScore(int userId) {
        @Language("SQL") String query = baseQuery
                + " WHERE U.user_id = ?;";

        try {
            return queryRunner
                    .query(query, rs -> oneFromRS(rs, LeaderboardRepository::leaderboardEntryFromRS), userId);
        } catch (SQLException e) {
            logger.error("Exception while querying score for userId {}", userId, e);
        }
        return Optional.empty();
    }

    private static LeaderboardEntryEntity leaderboardEntryFromRS(ResultSet rs) throws SQLException {
        return new LeaderboardEntryEntity(
                rs.getString("username"),
                rs.getInt("NMutants"),
                rs.getInt("AScore"),
                rs.getInt("NTests"),
                rs.getInt("DScore"),
                rs.getInt("NKilled"),
                rs.getInt("TotalScore")
        );
    }
}

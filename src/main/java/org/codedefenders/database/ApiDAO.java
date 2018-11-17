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

import org.codedefenders.api.analytics.ClassDataDTO;
import org.codedefenders.api.analytics.UserDataDTO;
import org.codedefenders.servlets.FeedbackManager.FeedbackType;

import java.util.List;

public class ApiDAO {
    private static final  String GET_ANALYTICS_USER_DATA_QUERY = String.join("\n",
        " SELECT users.User_ID                    AS ID,",
        "       users.Username                    AS Username,",
        "       IFNULL(NrGamesPlayed,0)           AS GamesPlayed,",
        "       IFNULL(NrAttackerGamesPlayed,0)   AS AttackerGamesPlayed,",
        "       IFNULL(NrDefenderGamesPlayed,0)   AS DefenderGamesPlayed,",
        "       IFNULL(AttackerScore,0)           AS AttackerScore,",
        "       IFNULL(DefenderScore,0)           AS DefenderScore,",
        "       IFNULL(NrMutants,0)               AS MutantsSubmitted,",
        "       IFNULL(NrMutantsAlive,0)          AS MutantsAlive,",
        "       IFNULL(NrEquivalentMutants,0)     AS MutantsEquivalent,",
        "       IFNULL(NrTests,0)                 AS TestsSubmitted,",
        "       IFNULL(NrMutantsKilled,0)         AS MutantsKilled",

        "FROM users",

        /* Count number of mutants, alive mutants, equivalent mutants. Calculate total attacker score. */
        "LEFT JOIN",
        "(",
        "  SELECT players.User_ID,",
        "         COUNT(mutants.Mutant_ID) AS NrMutants,",
        "         SUM(mutants.Alive) AS NrMutantsAlive,",
        "         SUM(CASE WHEN mutants.Equivalent = 'DECLARED_YES' THEN 1 ELSE 0 END) AS NrEquivalentMutants,",
        "         SUM(mutants.Points) AS AttackerScore",
        "  FROM players, mutants",
        "  WHERE players.ID = mutants.Player_ID",
        "  GROUP BY players.User_ID",
        ")",
        "AS attackers ON users.User_ID = attackers.User_ID",

        /* Count number of tests, mutants killed. Calculate total defender score */
        "LEFT JOIN",
        "(",
        "  SELECT players.User_ID,",
        "         COUNT(tests.Test_ID) AS NrTests,",
        "         SUM(tests.Points) AS DefenderScore,",
        "         SUM(tests.MutantsKilled) AS NrMutantsKilled",
        "  FROM players, tests",
        "  WHERE players.ID = tests.Player_ID",
        "  GROUP BY players.User_ID",
        ")",
        "AS defenders ON users.User_ID = defenders.User_ID",

        /* Count number of games played (active or finished). */
        "LEFT JOIN",
        "(",
        "  SELECT players.User_ID,",
        "         COUNT(DISTINCT games.ID)                                                        AS NrGamesPlayed,",
        "         COUNT(DISTINCT CASE WHEN players.role = 'ATTACKER' THEN games.ID ELSE NULL END) AS NrAttackerGamesPlayed,",
        "         COUNT(DISTINCT CASE WHEN players.role = 'DEFENDER' THEN games.ID ELSE NULL END) AS NrDefenderGamesPlayed",
        "  FROM players, games",
        "  WHERE players.Game_ID = games.ID",
        "    AND (games.State = 'ACTIVE' OR games.State = 'FINISHED')",
        "  GROUP BY players.User_ID",
        ")",
        "AS nr_games ON users.User_ID = nr_games.User_ID",

        "WHERE users.User_ID > 2;");

    private static final String GET_ANALYTICS_CLASS_DATA_QUERY = String.join("\n",
        "SELECT classes.Class_ID                                AS ID,",
        "       IFNULL(classes.Name,0)                          AS Classname,",
        "       IFNULL(NrGames,0)                               AS NrGames,",
        "       IFNULL(NrPlayers,0)                             AS NrPlayers,",
        "       IFNULL(AttackerWins,0)                          AS AttackerWins,",
        "       IFNULL(DefenderWins,0)                          AS DefenderWins,",
        "       IFNULL(NrTestsTotal,0)                          AS TestsSubmitted,",
        "       IFNULL(NrMutantsTotal,0)                        AS MutantsSubmitted,",
        "       IFNULL(NrMutantsAliveTotal,0)                   AS MutantsAlive,",
        "       IFNULL(NrMutantsEquivalentTotal,0)              AS MutantsEquivalent,",
        "       IFNULL(ratings_CutMutationDifficulty_sum,0)     AS ratings_CutMutationDifficulty_sum,",
        "       IFNULL(ratings_CutMutationDifficulty_count,0)   AS ratings_CutMutationDifficulty_count,",
        "       IFNULL(ratings_CutTestDifficulty_sum,0)         AS ratings_CutTestDifficulty_sum,",
        "       IFNULL(ratings_CutTestDifficulty_count,0)       AS ratings_CutTestDifficulty_count,",
        "       IFNULL(ratings_GameEngaging_sum,0)              AS ratings_GameEngaging_sum,",
        "       IFNULL(ratings_GameEngaging_count,0)            AS ratings_GameEngaging_count",

        "FROM classes",

        /* Count number of games (active or finished, at least one mutant and test) and players */
        "LEFT JOIN",
        "  (",
        "    SELECT games.Class_ID,",
        "           COUNT(DISTINCT games.ID) AS NrGames,",
        "           COUNT(DISTINCT players.ID) AS NrPlayers",
        "    FROM games, players, tests, mutants",
        "    WHERE players.Game_ID = games.ID",
        "      AND (games.State = 'ACTIVE' OR games.State = 'FINISHED')",
        "      AND games.ID = tests.Game_ID",
        "      AND games.ID = mutants.Game_ID",
        "    GROUP BY games.Class_ID",
        "  ) AS nr_games ON nr_games.Class_ID = classes.Class_ID",

        /* Count number of attacker and defender wins (in games with at least one test and mutant) */
        "LEFT JOIN",
        "  (",
        "    SELECT attacker_scores.Class_ID,",
        "    SUM(CASE WHEN AttackerScore > DefenderScore THEN 1 ELSE 0 END) AS AttackerWins,",
        "    SUM(CASE WHEN DefenderScore > AttackerScore THEN 1 ELSE 0 END) AS DefenderWins",
        "    FROM",
        "      (",
        "        SELECT games.ID,",
        "               games.Class_ID,",
        "               SUM(mutants.Points) AS AttackerScore",
        "        FROM games, mutants",
        "        WHERE mutants.Game_ID = games.ID",
        "          AND (games.State = 'FINISHED')",
        "        GROUP BY games.ID",
        "      ) AS attacker_scores,",
        "      (",
        "              SELECT games.ID,",
        "                     games.Class_ID,",
        "                     SUM(tests.Points) AS DefenderScore",
        "      FROM games, tests",
        "      WHERE tests.Game_ID = games.ID",
        "        AND (games.State = 'FINISHED')",
        "      GROUP BY games.ID",
        "      ) AS defender_scores",
        "    WHERE attacker_scores.ID = defender_scores.ID",
        "    GROUP BY attacker_scores.Class_ID",
        "  ) AS nr_wins ON nr_wins.Class_ID = classes.Class_ID",

        /* Count number of tests */
        "LEFT JOIN",
        "  (",
        "    SELECT nr_tests_innter.Class_ID,",
        "    SUM(NrTests) AS NrTestsTotal",
        "    FROM (",
        "      SELECT games.Class_ID,",
        "             COUNT(Test_ID) AS NrTests",
        "      FROM games, tests",
        "      WHERE games.ID = tests.Game_ID",
        "      GROUP BY games.ID",
        "    ) AS nr_tests_innter",
        "    GROUP BY nr_tests_innter.Class_ID",
        "  ) AS nr_tests ON nr_tests.Class_ID = classes.Class_ID",

        /* Count number of mutants, alive mutants, equivalent mutants */
        "LEFT JOIN",
        "  (",
        "    SELECT nr_mutants_inner.Class_ID,",
        "           SUM(NrMutants)              AS NrMutantsTotal,",
        "           SUM(NrMutantsAlive)         AS NrMutantsAliveTotal,",
        "           SUM(NrMutantsEquivalent)    AS NrMutantsEquivalentTotal",
        "    FROM (",
        "      SELECT games.Class_ID,",
        "             COUNT(Mutant_ID)      AS NrMutants,",
        "             SUM(mutants.Alive)    AS NrMutantsAlive,",
        "             SUM(CASE WHEN mutants.Equivalent = 'DECLARED_YES' THEN 1 ELSE 0 END) AS NrMutantsEquivalent",
        "      FROM games, mutants",
        "      WHERE games.ID = mutants.Game_ID",
        "      GROUP BY games.ID",
        "    ) AS nr_mutants_inner",
        "    GROUP BY nr_mutants_inner.Class_ID",
        "  ) AS nr_mutants ON nr_mutants.Class_ID = classes.Class_ID",


        /* Count and sum feedback ratings */
        "LEFT JOIN",
        "  (",
        "    SELECT feedback_inner.Class_ID,",
        "           MAX(CASE WHEN feedback_inner.type = '"+FeedbackType.CUT_MUTATION_DIFFICULTY.name()+"' THEN feedback_inner.rating_sum ELSE 0 END)     AS ratings_CutMutationDifficulty_sum,",
        "           MAX(CASE WHEN feedback_inner.type = '"+FeedbackType.CUT_MUTATION_DIFFICULTY.name()+"' THEN feedback_inner.rating_cnt ELSE 0 END)     AS ratings_CutMutationDifficulty_count,",
        "           MAX(CASE WHEN feedback_inner.type = '"+FeedbackType.CUT_TEST_DIFFICULTY.name()+"' THEN feedback_inner.rating_sum ELSE 0 END)         AS ratings_CutTestDifficulty_sum,",
        "           MAX(CASE WHEN feedback_inner.type = '"+FeedbackType.CUT_TEST_DIFFICULTY.name()+"' THEN feedback_inner.rating_cnt ELSE 0 END)         AS ratings_CutTestDifficulty_count,",
        "           MAX(CASE WHEN feedback_inner.type = '"+FeedbackType.GAME_ENGAGING.name()+"' THEN feedback_inner.rating_sum ELSE 0 END)               AS ratings_GameEngaging_sum,",
        "           MAX(CASE WHEN feedback_inner.type = '"+FeedbackType.GAME_ENGAGING.name()+"' THEN feedback_inner.rating_cnt ELSE 0 END)               AS ratings_GameEngaging_count",
        "    FROM",
        "      (",
        "        SELECT games.Class_ID,",
        "               ratings.type,",
        "               SUM(ratings.value) as rating_sum,",
        "               COUNT(ratings.value) as rating_cnt",
        "        FROM ratings, games",
        "        WHERE ratings.Game_ID = games.ID",
        "        GROUP BY games.Class_ID, ratings.type",
        "      ) AS feedback_inner",
        "    GROUP BY feedback_inner.Class_ID",
        "  ) AS feedback ON feedback.Class_ID = classes.Class_ID;");

    public static List<UserDataDTO> getAnalyticsUserData() throws UncheckedSQLException, SQLMappingException {
        return DB.executeQueryReturnList(GET_ANALYTICS_USER_DATA_QUERY, rs -> {
            UserDataDTO u = new UserDataDTO();
            u.setId(rs.getLong("ID"));
            u.setUsername(rs.getString("Username"));
            u.setAttackerScore(rs.getInt("AttackerScore"));
            u.setDefenderScore(rs.getInt("DefenderScore"));
            u.setGamesPlayed(rs.getInt("GamesPlayed"));
            u.setAttackerGamesPlayed(rs.getInt("AttackerGamesPlayed"));
            u.setDefenderGamesPlayed(rs.getInt("DefenderGamesPlayed"));
            u.setMutantsSubmitted(rs.getInt("MutantsSubmitted"));
            u.setMutantsAlive(rs.getInt("MutantsAlive"));
            u.setMutantsEquivalent(rs.getInt("MutantsEquivalent"));
            u.setTestsSubmitted(rs.getInt("TestsSubmitted"));
            u.setMutantsKilled(rs.getInt("MutantsKilled"));
            return u;
        });
    }

    public static List<ClassDataDTO> getAnalyticsClassData() throws UncheckedSQLException, SQLMappingException {
        return DB.executeQueryReturnList(GET_ANALYTICS_CLASS_DATA_QUERY, rs -> {
            ClassDataDTO c = new ClassDataDTO();
            c.setId(rs.getLong("ID"));
            c.setClassname(rs.getString("Classname"));
            c.setNrGames(rs.getInt("NrGames"));
            c.setAttackerWins(rs.getInt("AttackerWins"));
            c.setDefenderWins(rs.getInt("DefenderWins"));
            c.setNrPlayers(rs.getInt("NrPlayers"));
            c.setTestsSubmitted(rs.getInt("TestsSubmitted"));
            c.setMutantsSubmitted(rs.getInt("MutantsSubmitted"));
            c.setMutantsAlive(rs.getInt("MutantsAlive"));
            c.setMutantsEquivalent(rs.getInt("MutantsEquivalent"));

            ClassDataDTO.ClassRatings ratings = new ClassDataDTO.ClassRatings();
            ClassDataDTO.ClassRating rating;

            rating = new ClassDataDTO.ClassRating();
            rating.setCount(rs.getInt("ratings_CutMutationDifficulty_count"));
            rating.setSum(rs.getInt("ratings_CutMutationDifficulty_sum"));
            ratings.setCutMutationDifficulty(rating);

            rating = new ClassDataDTO.ClassRating();
            rating.setCount(rs.getInt("ratings_CutTestDifficulty_count"));
            rating.setSum(rs.getInt("ratings_CutTestDifficulty_sum"));
            ratings.setCutTestDifficulty(rating);

            rating = new ClassDataDTO.ClassRating();
            rating.setCount(rs.getInt("ratings_GameEngaging_count"));
            rating.setSum(rs.getInt("ratings_GameEngaging_sum"));
            ratings.setGameEngaging(rating);

            c.setRatings(ratings);
            return c;
        });
    }

}

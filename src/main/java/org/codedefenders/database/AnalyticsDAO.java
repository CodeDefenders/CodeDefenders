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

import java.util.List;

import org.codedefenders.api.analytics.ClassDataDTO;
import org.codedefenders.api.analytics.KillmapDataDTO;
import org.codedefenders.api.analytics.UserDataDTO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Feedback;
import org.intellij.lang.annotations.Language;

public class AnalyticsDAO {
    @Language("SQL")
    private static final String ANALYTICS_USER_DATA_QUERY = """
             SELECT users.User_ID                    AS ID,
                   users.Username                    AS Username,
                   IFNULL(NrGamesPlayed,0)           AS GamesPlayed,
                   IFNULL(NrAttackerGamesPlayed,0)   AS AttackerGamesPlayed,
                   IFNULL(NrDefenderGamesPlayed,0)   AS DefenderGamesPlayed,
                   IFNULL(AttackerScore,0)           AS AttackerScore,
                   IFNULL(DefenderScore,0)           AS DefenderScore,
                   IFNULL(NrMutants,0)               AS MutantsSubmitted,
                   IFNULL(NrMutantsAlive,0)          AS MutantsAlive,
                   IFNULL(NrEquivalentMutants,0)     AS MutantsEquivalent,
                   IFNULL(NrTests,0)                 AS TestsSubmitted,
                   IFNULL(NrMutantsKilled,0)         AS MutantsKilled

            FROM view_valid_users AS users

            -- Count number of mutants, alive mutants, equivalent mutants. Calculate total attacker score.
            LEFT JOIN
            (
              SELECT players.User_ID,
                     COUNT(mutants.Mutant_ID)                                             AS NrMutants,
                     SUM(mutants.Alive)                                                   AS NrMutantsAlive,
                     SUM(CASE WHEN mutants.Equivalent = 'DECLARED_YES' THEN 1 ELSE 0 END) AS NrEquivalentMutants,
                     SUM(mutants.Points)                                                  AS AttackerScore
              FROM view_players AS players,
                   view_valid_user_mutants AS mutants
              WHERE players.ID = mutants.Player_ID
              GROUP BY players.User_ID
            )
            AS attackers ON users.User_ID = attackers.User_ID

            -- Count number of tests, mutants killed. Calculate total defender score.
            LEFT JOIN
            (
              SELECT players.User_ID,
                     COUNT(tests.Test_ID)     AS NrTests,
                     SUM(tests.Points)        AS DefenderScore,
                     SUM(tests.MutantsKilled) AS NrMutantsKilled
              FROM view_players AS players,
                   view_valid_user_tests AS tests
              WHERE players.ID = tests.Player_ID
              GROUP BY players.User_ID
            )
            AS defenders ON users.User_ID = defenders.User_ID

            -- Count number of games played (active or finished).
            LEFT JOIN
            (
              SELECT players.User_ID,
                     COUNT(DISTINCT games.ID)                                                        AS NrGamesPlayed,
                     COUNT(DISTINCT CASE WHEN players.role = 'ATTACKER' THEN games.ID ELSE NULL END) AS NrAttackerGamesPlayed,
                     COUNT(DISTINCT CASE WHEN players.role = 'DEFENDER' THEN games.ID ELSE NULL END) AS NrDefenderGamesPlayed
              FROM view_players AS players,
                   games
              WHERE players.Game_ID = games.ID
                AND (games.State = 'ACTIVE' OR games.State = 'FINISHED')
              GROUP BY players.User_ID
            )
            AS nr_games ON users.User_ID = nr_games.User_ID
    """;

    @Language("SQL")
    private static final String ANALYTICS_CLASS_DATA_QUERY = """
            SELECT classes.Class_ID                                AS ID,
                   IFNULL(classes.Name,0)                          AS Classname,
                   IFNULL(classes.Alias,0)                         AS Classalias,
                   IFNULL(NrGames,0)                               AS NrGames,
                   IFNULL(NrPlayers,0)                             AS NrPlayers,
                   IFNULL(AttackerWins,0)                          AS AttackerWins,
                   IFNULL(DefenderWins,0)                          AS DefenderWins,
                   IFNULL(NrTestsTotal,0)                          AS TestsSubmitted,
                   IFNULL(NrMutantsTotal,0)                        AS MutantsSubmitted,
                   IFNULL(NrMutantsAliveTotal,0)                   AS MutantsAlive,
                   IFNULL(NrMutantsEquivalentTotal,0)              AS MutantsEquivalent,
                   IFNULL(ratings_CutMutationDifficulty_sum,0)     AS ratings_CutMutationDifficulty_sum,
                   IFNULL(ratings_CutMutationDifficulty_count,0)   AS ratings_CutMutationDifficulty_count,
                   IFNULL(ratings_CutTestDifficulty_sum,0)         AS ratings_CutTestDifficulty_sum,
                   IFNULL(ratings_CutTestDifficulty_count,0)       AS ratings_CutTestDifficulty_count,
                   IFNULL(ratings_GameEngaging_sum,0)              AS ratings_GameEngaging_sum,
                   IFNULL(ratings_GameEngaging_count,0)            AS ratings_GameEngaging_count

            FROM view_playable_classes AS classes

            -- Count number of games (active or finished, at least one mutant and test) and players
            LEFT JOIN
            (
              SELECT games.Class_ID,
                     COUNT(DISTINCT games.ID)   AS NrGames,
                     COUNT(DISTINCT players.ID) AS NrPlayers
              FROM games,
                   view_players AS players,
                   view_valid_user_tests AS tests,
                   view_valid_user_mutants AS mutants
              WHERE players.Game_ID = games.ID
                AND (games.State = 'ACTIVE' OR games.State = 'FINISHED')
                AND games.ID = tests.Game_ID
                AND games.ID = mutants.Game_ID
              GROUP BY games.Class_ID
            ) AS nr_games ON nr_games.Class_ID = classes.Class_ID

            -- Count number of attacker and defender wins (in games with at least one test and mutant)
            LEFT JOIN
            (
              SELECT attacker_scores.Class_ID,
              SUM(CASE WHEN AttackerScore > DefenderScore THEN 1 ELSE 0 END) AS AttackerWins,
              SUM(CASE WHEN DefenderScore > AttackerScore THEN 1 ELSE 0 END) AS DefenderWins
              FROM
                (
                  SELECT games.ID,
                         games.Class_ID,
                         SUM(mutants.Points) AS AttackerScore
                  FROM games,
                       view_valid_game_mutants AS mutants
                  WHERE mutants.Game_ID = games.ID
                    AND (games.State = 'FINISHED')
                  GROUP BY games.ID
                ) AS attacker_scores,
                (
                  SELECT games.ID,
                         games.Class_ID,
                         SUM(tests.Points) AS DefenderScore
                  FROM games,
                       view_valid_game_tests AS tests
                  WHERE tests.Game_ID = games.ID
                    AND (games.State = 'FINISHED')
                  GROUP BY games.ID
                ) AS defender_scores
              WHERE attacker_scores.ID = defender_scores.ID
              GROUP BY attacker_scores.Class_ID
            ) AS nr_wins ON nr_wins.Class_ID = classes.Class_ID

            -- Count number of tests
            LEFT JOIN
            (
              SELECT nr_tests_inner.Class_ID,
              SUM(NrTests) AS NrTestsTotal
              FROM
                (
                  SELECT games.Class_ID,
                         COUNT(Test_ID) AS NrTests
                  FROM games,
                       view_valid_user_tests AS tests
                  WHERE games.ID = tests.Game_ID
                  GROUP BY games.ID
                ) AS nr_tests_inner
              GROUP BY nr_tests_inner.Class_ID
            ) AS nr_tests ON nr_tests.Class_ID = classes.Class_ID

            -- Count number of mutants, alive mutants, equivalent mutants
            LEFT JOIN
            (
              SELECT nr_mutants_inner.Class_ID,
                     SUM(NrMutants)              AS NrMutantsTotal,
                     SUM(NrMutantsAlive)         AS NrMutantsAliveTotal,
                     SUM(NrMutantsEquivalent)    AS NrMutantsEquivalentTotal
              FROM
                (
                  SELECT games.Class_ID,
                         COUNT(Mutant_ID)      AS NrMutants,
                         SUM(mutants.Alive)    AS NrMutantsAlive,
                         SUM(CASE WHEN mutants.Equivalent = 'DECLARED_YES' THEN 1 ELSE 0 END) AS NrMutantsEquivalent
                  FROM games,
                       view_valid_user_mutants AS mutants
                  WHERE games.ID = mutants.Game_ID
                  GROUP BY games.ID
                ) AS nr_mutants_inner
              GROUP BY nr_mutants_inner.Class_ID
            ) AS nr_mutants ON nr_mutants.Class_ID = classes.Class_ID


            -- Count and sum feedback ratings
            LEFT JOIN
            (
              SELECT feedback_inner.Class_ID,
                     MAX(CASE WHEN feedback_inner.type = '%s' THEN feedback_inner.rating_sum ELSE 0 END) AS ratings_CutMutationDifficulty_sum,
                     MAX(CASE WHEN feedback_inner.type = '%s' THEN feedback_inner.rating_cnt ELSE 0 END) AS ratings_CutMutationDifficulty_count,
                     MAX(CASE WHEN feedback_inner.type = '%s' THEN feedback_inner.rating_sum ELSE 0 END) AS ratings_CutTestDifficulty_sum,
                     MAX(CASE WHEN feedback_inner.type = '%s' THEN feedback_inner.rating_cnt ELSE 0 END) AS ratings_CutTestDifficulty_count,
                     MAX(CASE WHEN feedback_inner.type = '%s' THEN feedback_inner.rating_sum ELSE 0 END) AS ratings_GameEngaging_sum,
                     MAX(CASE WHEN feedback_inner.type = '%s' THEN feedback_inner.rating_cnt ELSE 0 END) AS ratings_GameEngaging_count
              FROM
                (
                  SELECT games.Class_ID,
                         ratings.type,
                         SUM(ratings.value)   AS rating_sum,
                         COUNT(ratings.value) AS rating_cnt
                  FROM ratings,
                       games
                  WHERE ratings.Game_ID = games.ID
                  GROUP BY games.Class_ID, ratings.type
                ) AS feedback_inner
              GROUP BY feedback_inner.Class_ID
            ) AS feedback ON feedback.Class_ID = classes.Class_ID;
    """.formatted(
            Feedback.Type.CUT_MUTATION_DIFFICULTY.name(),
            Feedback.Type.CUT_MUTATION_DIFFICULTY.name(),
            Feedback.Type.CUT_TEST_DIFFICULTY.name(),
            Feedback.Type.CUT_TEST_DIFFICULTY.name(),
            Feedback.Type.GAME_ENGAGING.name(),
            Feedback.Type.GAME_ENGAGING.name()
    );

    @Language("SQL") private static final String ANALYTICS_KILLMAP_USEFUL_ACTIONS_QUERY = """
            SELECT killmap_participations.*,
                   IFNULL(useful_tests.Useful_Tests,0)     AS Useful_Tests,
                   IFNULL(useful_mutants.Useful_Mutants,0) AS Useful_Mutants

            -- Get all (classId, userId, role) pairs for which data exists in the killmap.
            FROM
            (
              SELECT DISTINCT killmap.Class_ID AS Class_ID,
                              classes.Name     AS Class_Name,
                              users.User_ID    AS User_ID,
                              users.Username   AS User_Name,
                              players.Role     AS Role
              FROM killmap,
                   view_playable_classes AS classes,
                   view_valid_user_mutants AS mutants,
                   view_players AS players,
                   view_valid_users AS users
              WHERE killmap.Class_ID = classes.Class_ID
                AND killmap.Mutant_ID = mutants.Mutant_ID
                AND mutants.Player_ID = players.ID
                AND players.User_ID = users.User_ID

              UNION

              SELECT DISTINCT killmap.Class_ID AS Class_ID,
                              classes.Name     AS Class_Name,
                              users.User_ID    AS User_ID,
                              users.Username   AS User_Name,
                              players.Role     AS Role
              FROM killmap,
                   view_playable_classes AS classes,
                   view_valid_user_tests AS tests,
                   view_players AS players,
                   view_valid_users AS users
              WHERE killmap.Class_ID = classes.Class_ID
                AND killmap.Test_ID = tests.Test_ID
                AND tests.Player_ID = players.ID
                AND players.User_ID = users.User_ID
            ) AS killmap_participations

            -- Count number of useful tests.
            LEFT JOIN
            (
              SELECT COUNT(DISTINCT killmap.Test_ID) AS Useful_Tests,
                     killmap.Class_ID                AS Class_ID,
                     players.User_ID                 AS User_ID,
                     players.Role                    AS Role
              FROM killmap,
                   view_valid_user_tests AS tests,
                   view_players AS players
              WHERE killmap.Status = 'KILL'
                AND killmap.Test_ID = tests.Test_ID
                AND tests.Player_ID = players.ID
              GROUP BY killmap.Class_ID, players.User_ID, players.Role
            ) AS useful_tests
              ON killmap_participations.Class_ID = useful_tests.Class_ID
              AND killmap_participations.User_ID = useful_tests.User_ID
              AND killmap_participations.Role = useful_tests.Role

            -- Count number of useful mutants.
            LEFT JOIN
            (
              SELECT COUNT(DISTINCT killmap.Mutant_ID) AS Useful_Mutants,
                     killmap.Class_ID                  AS Class_ID,
                     players.User_ID                   AS User_ID,
                     players.Role                      AS Role
              FROM killmap,
                   view_valid_user_mutants AS mutants,
                   view_players AS players
              WHERE EXISTS(SELECT k.Mutant_ID FROM killmap k WHERE k.Mutant_ID = killmap.Mutant_ID AND k.Status = 'KILL')
                AND EXISTS(SELECT k.Mutant_ID FROM killmap k WHERE k.Mutant_ID = killmap.Mutant_ID AND k.Status = 'NO_KILL')
                AND killmap.Mutant_ID = mutants.Mutant_ID
                AND players.ID = mutants.Player_ID
              GROUP BY killmap.Class_ID, players.User_ID, players.Role
            ) AS useful_mutants
              ON killmap_participations.Class_ID = useful_mutants.Class_ID
              AND killmap_participations.User_ID = useful_mutants.User_ID
              AND killmap_participations.Role = useful_mutants.Role

            ORDER BY Class_ID, User_ID;
    """;

    public static List<UserDataDTO> getAnalyticsUserData() throws UncheckedSQLException, SQLMappingException {
        return DB.executeQueryReturnList(ANALYTICS_USER_DATA_QUERY, rs -> {
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
        return DB.executeQueryReturnList(ANALYTICS_CLASS_DATA_QUERY, rs -> {
            ClassDataDTO c = new ClassDataDTO();
            c.setId(rs.getLong("ID"));
            c.setClassname(rs.getString("Classname"));
            c.setClassalias(rs.getString("Classalias"));
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

    public static List<KillmapDataDTO> getAnalyticsKillMapData() throws UncheckedSQLException, SQLMappingException {
        return DB.executeQueryReturnList(ANALYTICS_KILLMAP_USEFUL_ACTIONS_QUERY, rs -> {
            KillmapDataDTO k = new KillmapDataDTO();
            k.setClassId(rs.getInt("Class_ID"));
            k.setClassName(rs.getString("Class_Name"));
            k.setUserId(rs.getInt("User_ID"));
            k.setUserName(rs.getString("User_Name"));
            k.setRole(Role.valueOf(rs.getString("Role")));
            k.setUsefulMutants(rs.getInt("Useful_Mutants"));
            k.setUsefulTests(rs.getInt("Useful_Tests"));
            return k;
        });
    }
}

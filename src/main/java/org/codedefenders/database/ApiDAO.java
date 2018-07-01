package org.codedefenders.database;

import org.codedefenders.api.analytics.ClassDataDTO;
import org.codedefenders.api.analytics.UserDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ApiDAO {
    private static final Logger logger = LoggerFactory.getLogger(ApiDAO.class);

    private static final  String GET_ANALYTICS_USER_DATA_QUERY = String.join("\n",
        " SELECT users.User_ID                AS ID,",
        "       users.Username                AS Username,",
        "       IFNULL(NrGamesPlayed,0)       AS GamesPlayed,",
        "       IFNULL(AttackerScore,0)       AS AttackerScore,",
        "       IFNULL(DefenderScore,0)       AS DefenderScore,",
        "       IFNULL(NrMutants,0)           AS MutantsSubmitted,",
        "       IFNULL(NrMutantsAlive,0)      AS MutantsAlive,",
        "       IFNULL(NrEquivalentMutants,0) AS MutantsEquivalent,",
        "       IFNULL(NrTests,0)             AS TestsSubmitted,",
        "       IFNULL(NrMutantsKilled,0)     AS MutantsKilled",

        "FROM users",

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

        "LEFT JOIN",
        "(",
        "  SELECT players.User_ID,",
        "         COUNT(*) AS NrGamesPlayed",
        "  FROM players",
        "  GROUP BY players.User_ID",
        ")",
        "AS nr_games ON users.User_ID = nr_games.User_ID",

        "WHERE users.User_ID > 2;");

    private static final  String GET_ANALYTICS_CLASS_DATA_QUERY = String.join("\n",
        "SELECT classes.Class_ID AS ID,",
        "       classes.Name AS Classname,",
        "       NrGames AS Games,",
        "       NrTestsTotal AS TestsSubmitted,",
        "       NrMutantsTotal AS MutantsSubmitted,",
        "       NrMutantsAliveTotal AS MutantsAlive,",
        "       NrMutantsEquivalentTotal AS MutantsEquivalent",

        "FROM classes",

        "    LEFT JOIN",
        "    (",
        "        SELECT classes.Class_ID, COUNT(games.ID) AS NrGames",
        "        FROM classes, games",
        "        WHERE classes.Class_ID = games.Class_ID",
        "        GROUP BY classes.Class_ID",
        "    ) AS nr_games ON nr_games.Class_ID = classes.Class_ID",

        "    LEFT JOIN",
        "    (",
        "        SELECT nr_tests_pre.Class_ID,",
        "               SUM(NrTests) AS NrTestsTotal",
        "        FROM (",
        "            SELECT games.Class_ID,",
        "                   COUNT(Test_ID) AS NrTests",
        "            FROM games, tests",
        "            WHERE games.ID = tests.Game_ID",
        "            GROUP BY games.ID",
        "        ) AS nr_tests_pre",
        "        GROUP BY nr_tests_pre.Class_ID",
        "    ) AS nr_tests ON nr_tests.Class_ID = classes.Class_ID",

        "    LEFT JOIN",
        "    (",
        "        SELECT nr_mutants_pre.Class_ID,",
        "               SUM(NrMutants) AS NrMutantsTotal,",
        "               SUM(NrMutantsAlive) AS NrMutantsAliveTotal,",
        "               SUM(NrMutantsEquivalent) AS NrMutantsEquivalentTotal",
        "        FROM (",
        "            SELECT games.Class_ID,",
        "                   COUNT(Mutant_ID) AS NrMutants,",
        "                   IFNULL(SUM(mutants.Alive),0) AS NrMutantsAlive,",
        "                   SUM(CASE WHEN mutants.Equivalent = 'DECLARED_YES' THEN 1 ELSE 0 END) AS NrMutantsEquivalent",
        "            FROM games, mutants",
        "            WHERE games.ID = mutants.Game_ID",
        "            GROUP BY games.ID",
        "        ) AS nr_mutants_pre",
        "        GROUP BY nr_mutants_pre.Class_ID",
        "    ) AS nr_mutants ON nr_mutants.Class_ID = classes.Class_ID;");


    public static List<UserDataDTO> getAnalyticsUserData() {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = null;

        List<UserDataDTO> userList = new ArrayList<>();
        try {
            stmt = DB.createPreparedStatement(conn, GET_ANALYTICS_USER_DATA_QUERY);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserDataDTO u = new UserDataDTO();
                u.setId(rs.getLong("ID"));
                u.setUsername(rs.getString("Username"));
                u.setAttackerScore(rs.getInt("AttackerScore"));
                u.setDefenderScore(rs.getInt("DefenderScore"));
                u.setGamesPlayed(rs.getInt("GamesPlayed"));
                u.setMutantsSubmitted(rs.getInt("MutantsSubmitted"));
                u.setMutantsAlive(rs.getInt("MutantsAlive"));
                u.setMutantsEquivalent(rs.getInt("MutantsEquivalent"));
                u.setTestsSubmitted(rs.getInt("TestsSubmitted"));
                u.setMutantsKilled(rs.getInt("MutantsKilled"));
                userList.add(u);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return userList;
    }

    public static List<ClassDataDTO> getAnalyticsClassData() {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = null;

        List<ClassDataDTO> classList = new ArrayList<>();
        try {
            stmt = DB.createPreparedStatement(conn, GET_ANALYTICS_CLASS_DATA_QUERY);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ClassDataDTO c = new ClassDataDTO();
                c.setId(rs.getLong("ID"));
                c.setClassname(rs.getString("Classname"));
                c.setGames(rs.getInt("Games"));
                c.setTestsSubmitted(rs.getInt("TestsSubmitted"));
                c.setMutantsSubmitted(rs.getInt("MutantsSubmitted"));
                c.setMutantsAlive(rs.getInt("MutantsAlive"));
                c.setMutantsEquivalent(rs.getInt("MutantsEquivalent"));
                classList.add(c);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return classList;
    }

}

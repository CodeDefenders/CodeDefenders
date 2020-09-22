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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.Role;
import org.codedefenders.game.leaderboard.Entry;
import org.codedefenders.model.User;
import org.codedefenders.model.UserInfo;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME;
import org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_TYPE;

public class AdminDAO {

    public static final String TIMESTAMP_NEVER = "never";

    public static Entry getScore(int userId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT",
                "  U.username                            AS username,",
                "  IFNULL(NMutants, 0)                   AS NMutants,",
                "  IFNULL(AScore, 0)                     AS AScore,",
                "  IFNULL(NTests, 0)                     AS NTests,",
                "  IFNULL(DScore, 0)                     AS DScore,",
                "  IFNULL(NKilled, 0)                    AS NKilled,",
                "  IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore",
                "FROM users U LEFT JOIN",
                "  (SELECT",
                "     PA.user_id,",
                "     count(M.Mutant_ID) AS NMutants,",
                "     sum(M.Points)      AS AScore",
                "   FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID",
                "   GROUP BY PA.user_id)",
                "    AS Attacker ON U.user_id = Attacker.user_id",
                "  LEFT JOIN",
                "  (SELECT",
                "     PD.user_id,",
                "     count(T.Test_ID)     AS NTests,",
                "     sum(T.Points)        AS DScore,",
                "     sum(T.MutantsKilled) AS NKilled",
                "   FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID",
                "   GROUP BY PD.user_id)",
                "    AS Defender ON U.user_id = Defender.user_id",
                "WHERE U.user_id = ?;");
        return DB.executeQueryReturnValue(query, DatabaseAccess::entryFromRS, DatabaseValue.of(userId));
    }

    public static boolean deletePlayerTest(int pid) {
        String query = "DELETE FROM tests WHERE Player_ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deletePlayerMutants(int pid) {
        String query = "DELETE FROM mutants WHERE Player_ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deleteDefenderEquivalences(int pid) {
        String query = "DELETE FROM equivalences WHERE Defender_ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deleteAttackerEquivalences(int pid) {
        String query = String.join("\n",
                "DELETE FROM equivalences",
                "WHERE Mutant_ID IN (SELECT Mutant_ID",
                "                    FROM mutants",
                "                    WHERE Player_ID = ?);");
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deletePlayer(int pid) {
        String query = "DELETE FROM players WHERE ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deleteTestTargetExecutions(int tid) {
        String query = "DELETE FROM targetexecutions WHERE Test_ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(tid));
    }

    public static boolean deleteMutantTargetExecutions(int mid) {
        String query = "DELETE FROM targetexecutions WHERE Mutant_ID = ?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(mid));
    }

    private static UserInfo userInfoFromRS(ResultSet rs) throws SQLException {
        final User user = UserDAO.userFromRS(rs);

        Timestamp ts = rs.getTimestamp("lastLogin");
        final Role lastRole = Role.valueOrNull("lastRole");
        final int totalScore = rs.getInt("TotalScore");

        return new UserInfo(user, ts, lastRole, totalScore);
    }

    private static List<String> uglyListUserInfoFromRS(ResultSet rs) throws SQLException {
        List<String> userInfo = new ArrayList<>();
        userInfo.add(String.valueOf(rs.getInt("User_ID")));
        userInfo.add(rs.getString("Username"));
        userInfo.add(rs.getString("Email"));
        Timestamp ts = rs.getTimestamp("lastLogin");
        userInfo.add(ts == null ? "-- never --" : ts.toString().substring(0, ts.toString().length() - 5));
        userInfo.add(rs.getString("lastRole"));
        userInfo.add(String.valueOf(rs.getInt("TotalScore")));
        return userInfo;
    }

    public static List<List<String>> getUnassignedUsersInfo() throws UncheckedSQLException, SQLMappingException {
        final String query = String.join("\n",
                "SELECT DISTINCT",
                "  u.User_ID,",
                "  u.Username,",
                "  u.Email,",
                "  lastLogin.ts AS lastLogin,",
                "  Role         AS lastRole,",
                "  totalScore",
                "FROM",
                "  view_valid_users u",
                "  LEFT JOIN (SELECT",
                "          MAX(Timestamp) AS ts,",
                "          user_id",
                "        FROM sessions",
                "        GROUP BY User_ID) AS lastLogin ON lastLogin.User_ID = u.User_ID",
                "  LEFT JOIN",
                "  (SELECT",
                "     players.User_ID,",
                "     Role",
                "   FROM users",
                "     INNER JOIN players ON users.User_ID = players.User_ID",
                "     INNER JOIN games ON players.Game_ID = games.ID",
                "     INNER JOIN",
                "     (SELECT",
                "        players.User_ID,",
                "        max(players.Game_ID) AS latestGame",
                "      FROM players",
                "      GROUP BY players.User_ID) AS lg ON lg.User_ID = players.User_ID AND lg.latestGame = games.ID) AS lastRole",
                "    ON lastRole.User_ID = u.User_ID",
                "  JOIN",
                "  (SELECT",
                "     U.User_ID,",
                "     IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore",
                "   FROM users U LEFT JOIN",
                "     (SELECT",
                "        PA.user_id,",
                "        sum(M.Points) AS AScore",
                "      FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID",
                "      GROUP BY PA.user_id)",
                "       AS Attacker ON U.user_id = Attacker.user_id",
                "     LEFT JOIN",
                "     (SELECT",
                "        PD.user_id,",
                "        sum(T.Points) AS DScore",
                "      FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID",
                "      GROUP BY PD.user_id)",
                "       AS Defender ON U.user_id = Defender.user_id) AS totalScore ON totalScore.User_ID = u.User_ID",
                "WHERE u.User_ID NOT IN (",
                "  SELECT DISTINCT players.User_ID",
                "  FROM (players",
                "    INNER JOIN games ON players.Game_ID = games.ID)",
                "  WHERE",
                "    (State = 'ACTIVE' OR State = 'CREATED') AND Role IN ('ATTACKER', 'DEFENDER') AND Active = 1",
                ")",
                "ORDER BY lastLogin DESC, User_ID;");
        return DB.executeQueryReturnList(query, AdminDAO::uglyListUserInfoFromRS);
    }

    public static List<UserInfo> getAllUsersInfo() throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT DISTINCT",
                "  users.*,",
                "  lastLogin.ts AS lastLogin,",
                "  Role         AS lastRole,",
                "  totalScore",
                "FROM",
                "  view_valid_users AS users",
                "  LEFT JOIN (SELECT",
                "          MAX(Timestamp) AS ts,",
                "          user_id",
                "        FROM sessions",
                "        GROUP BY User_ID) AS lastLogin ON lastLogin.User_ID = users.User_ID",
                "  LEFT JOIN",
                "  (SELECT",
                "     players.User_ID,",
                "     Role",
                "   FROM users",
                "     INNER JOIN players ON users.User_ID = players.User_ID",
                "     INNER JOIN games ON players.Game_ID = games.ID",
                "     INNER JOIN",
                "     (SELECT",
                "        players.User_ID,",
                "        max(players.Game_ID) AS latestGame",
                "      FROM players",
                "      GROUP BY players.User_ID) AS lg ON lg.User_ID = players.User_ID AND lg.latestGame = games.ID) AS lastRole",
                "    ON lastRole.User_ID = users.User_ID",
                "  JOIN",
                "  (SELECT",
                "     U.User_ID,",
                "     IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore",
                "   FROM users U LEFT JOIN",
                "     (SELECT",
                "        PA.user_id,",
                "        sum(M.Points) AS AScore",
                "      FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID",
                "      GROUP BY PA.user_id)",
                "       AS Attacker ON U.user_id = Attacker.user_id",
                "     LEFT JOIN",
                "     (SELECT",
                "        PD.user_id,",
                "        sum(T.Points) AS DScore",
                "      FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID",
                "      GROUP BY PD.user_id)",
                "       AS Defender ON U.user_id = Defender.user_id) AS totalScore ON totalScore.User_ID = users.User_ID",
                "ORDER BY lastLogin DESC, User_ID;");
        return DB.executeQueryReturnList(query, AdminDAO::userInfoFromRS);
    }

    public static UserInfo getUsersInfo(int userId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT DISTINCT",
                "  users.*,",
                "  lastLogin.ts AS lastLogin,",
                "  Role         AS lastRole,",
                "  totalScore",
                "FROM",
                "  view_valid_users AS users",
                "  LEFT JOIN (SELECT",
                "          MAX(Timestamp) AS ts,",
                "          user_id",
                "        FROM sessions",
                "        GROUP BY User_ID) AS lastLogin ON lastLogin.User_ID = users.User_ID",
                "  LEFT JOIN",
                "  (SELECT",
                "     players.User_ID,",
                "     Role",
                "   FROM users",
                "     INNER JOIN players ON users.User_ID = players.User_ID",
                "     INNER JOIN games ON players.Game_ID = games.ID",
                "     INNER JOIN",
                "     (SELECT",
                "        players.User_ID,",
                "        max(players.Game_ID) AS latestGame",
                "      FROM players",
                "      GROUP BY players.User_ID) AS lg ON lg.User_ID = players.User_ID AND lg.latestGame = games.ID) AS lastRole",
                "    ON lastRole.User_ID = users.User_ID",
                "  JOIN",
                "  (SELECT",
                "     U.User_ID,",
                "     IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore",
                "   FROM users U LEFT JOIN",
                "     (SELECT",
                "        PA.user_id,",
                "        sum(M.Points) AS AScore",
                "      FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID",
                "      GROUP BY PA.user_id)",
                "       AS Attacker ON U.user_id = Attacker.user_id",
                "     LEFT JOIN",
                "     (SELECT",
                "        PD.user_id,",
                "        sum(T.Points) AS DScore",
                "      FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID",
                "      GROUP BY PD.user_id)",
                "       AS Defender ON U.user_id = Defender.user_id) AS totalScore ON totalScore.User_ID = users.User_ID",
                "WHERE users.User_ID = ?",
                "ORDER BY lastLogin DESC, User_ID;");
        return DB.executeQueryReturnValue(query, AdminDAO::userInfoFromRS, DatabaseValue.of(userId));
    }

    public static List<List<String>> getPlayersInfo(int gameId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT",
                "  ID,",
                "  Username,",
                "  Role,",
                "  lastSubmission,",
                "  TotalScore,",
                "  IFNULL(nbSubmissions, 0) as nbSubmissions",
                "FROM (SELECT",
                "        User_ID,",
                "        Role,",
                "        ID",
                "      FROM players",
                "      WHERE Game_ID = ? AND Active = TRUE) AS activePlayers",
                "  JOIN users ON activePlayers.User_ID = users.User_ID",
                "  JOIN",
                "  (SELECT",
                "     U.User_ID,",
                "     IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore",
                "   FROM users U LEFT JOIN",
                "     (SELECT",
                "        PA.user_id,",
                "        sum(M.Points) AS AScore",
                "      FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID",
                "      GROUP BY PA.user_id)",
                "       AS Attacker ON U.user_id = Attacker.user_id",
                "     LEFT JOIN",
                "     (SELECT",
                "        PD.user_id,",
                "        sum(T.Points) AS DScore",
                "      FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID",
                "      GROUP BY PD.user_id)",
                "       AS Defender ON U.user_id = Defender.user_id) AS totalScore ON totalScore.User_ID = users.User_ID",
                "  LEFT JOIN (SELECT",
                "               MAX(ts) AS lastSubmission,",
                "               Player_ID",
                "             FROM (SELECT",
                "                     MAX(mutants.Timestamp) AS ts,",
                "                     Player_ID",
                "                   FROM mutants",
                "                   GROUP BY Player_ID",
                "                   UNION",
                "                   SELECT",
                "                     MAX(tests.Timestamp) AS ts,",
                "                     Player_ID",
                "                   FROM tests",
                "                   GROUP BY Player_ID",
                "                  ) AS t",
                "             GROUP BY Player_ID) AS lastAction",
                "    ON Player_ID = activePlayers.ID",
                "  LEFT JOIN (",
                "        SELECT",
                "          COUNT(*) AS nbSubmissions,",
                "          Player_ID",
                "        FROM (SELECT",
                "                Player_ID,",
                "                tests.Test_ID",
                "              FROM (tests",
                "                JOIN targetexecutions t2 ON tests.Test_ID = t2.Test_ID)",
                "              WHERE t2.Target = 'COMPILE_TEST' AND t2.Status = 'SUCCESS'",
                "              UNION SELECT",
                "                      Player_ID,",
                "                      mutants.Mutant_ID",
                "                    FROM (mutants",
                "                      JOIN targetexecutions t2 ON mutants.Mutant_ID = t2.Mutant_ID)",
                "                    WHERE t2.Target = 'COMPILE_MUTANT' AND t2.Status = 'SUCCESS') AS TestsAndMutants",
                "             GROUP BY Player_ID) AS submissions ON submissions.Player_ID = ID",
                "ORDER BY Role, nbSubmissions;");

        final DB.RSMapper<List<String>> mapper = rs -> {
            List<String> playerInfo = new ArrayList<>();
            playerInfo.add(String.valueOf(rs.getInt("ID")));
            playerInfo.add(rs.getString("Username"));
            playerInfo.add(rs.getString("Role"));
            Timestamp ts = rs.getTimestamp("lastSubmission");
            playerInfo.add(ts == null ? TIMESTAMP_NEVER : "" + ts.getTime());
            playerInfo.add(String.valueOf(rs.getInt("TotalScore")));
            playerInfo.add(String.valueOf(rs.getInt("nbSubmissions")));
            return playerInfo;
        };
        return DB.executeQueryReturnList(query, mapper, DatabaseValue.of(gameId));
    }

    public static boolean setUserPassword(int uid, String password) {
        String query = "UPDATE users SET Password = ? WHERE User_ID = ?;";
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(password),
                DatabaseValue.of(uid)
        };
        return DB.executeUpdateQuery(query, values);
    }

    public static boolean deleteUser(int userId) {
        String query = "DELETE FROM users WHERE User_ID = ?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(userId));
        // this does not work as foreign keys are not deleted (recommended: update w/ ON DELETE CASCADE)
    }

    public static boolean updateSystemSetting(AdminSystemSettings.SettingsDTO setting) {
        String valueToSet = setting.getType().name();

        DatabaseValue value = null;
        switch (setting.getType()) {
            case STRING_VALUE:
                value = DatabaseValue.of(setting.getStringValue());
                break;
            case INT_VALUE:
                value = DatabaseValue.of(setting.getIntValue());
                break;
            case BOOL_VALUE:
                value = DatabaseValue.of(setting.getBoolValue());
                break;
            default:
                // ignored
        }

        String query = String.join("\n",
                "UPDATE settings",
                "SET " + valueToSet + " = ?",
                "WHERE name = ?;");

        return DB.executeUpdateQuery(query, value, DatabaseValue.of(setting.getName().name()));
    }

    private static AdminSystemSettings.SettingsDTO settingFromRS(ResultSet rs) throws SQLException {
        SETTING_NAME name = SETTING_NAME.valueOf(rs.getString("name"));
        SETTING_TYPE settingType = SETTING_TYPE.valueOf(rs.getString("type"));
        switch (settingType) {
            case STRING_VALUE:
                return new AdminSystemSettings.SettingsDTO(name, rs.getString(settingType.name()));
            case INT_VALUE:
                return new AdminSystemSettings.SettingsDTO(name,rs.getInt(settingType.name()));
            case BOOL_VALUE:
                return new AdminSystemSettings.SettingsDTO(name,rs.getBoolean(settingType.name()));
            default:
                return null;
        }
    }

    public static List<AdminSystemSettings.SettingsDTO> getSystemSettings()
            throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM settings;";
        return DB.executeQueryReturnList(query, AdminDAO::settingFromRS);
    }

    public static AdminSystemSettings.SettingsDTO getSystemSetting(SETTING_NAME name)
            throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM settings WHERE settings.name = ?;";
        return DB.executeQueryReturnValue(query, AdminDAO::settingFromRS, DatabaseValue.of(name.name()));
    }

    // TODO this is just used to initialize the connection pool, we should probably make more specific methods for that
    /**
     * This does not close the given {@link Connection}.
     */
    static AdminSystemSettings.SettingsDTO getSystemSettingInt(SETTING_NAME name, Connection conn) throws SQLException {
        String query = "SELECT * FROM settings WHERE settings.name = ?;";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, name.name());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            SETTING_TYPE settingType = SETTING_TYPE.valueOf(rs.getString("type"));
            return new AdminSystemSettings.SettingsDTO(name, rs.getInt(settingType.name()));
        }
        return null;
    }
}

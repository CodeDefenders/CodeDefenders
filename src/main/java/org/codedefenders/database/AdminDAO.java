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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.Role;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.UserInfo;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME;
import org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_TYPE;
import org.intellij.lang.annotations.Language;

import static org.codedefenders.persistence.database.UserRepository.userFromRS;

public class AdminDAO {

    public static boolean deletePlayerTest(int pid) {
        @Language("SQL") String query = "DELETE FROM tests WHERE Player_ID = ?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deletePlayerMutants(int pid) {
        @Language("SQL") String query = "DELETE FROM mutants WHERE Player_ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deleteDefenderEquivalences(int pid) {
        @Language("SQL") String query = "DELETE FROM equivalences WHERE Defender_ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deleteAttackerEquivalences(int pid) {
        @Language("SQL") String query = """
                DELETE FROM equivalences
                WHERE Mutant_ID IN (SELECT Mutant_ID
                                    FROM mutants
                                    WHERE Player_ID = ?)
        """;
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deletePlayer(int pid) {
        @Language("SQL") String query = "DELETE FROM players WHERE ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(pid));
    }

    public static boolean deleteTestTargetExecutions(int tid) {
        @Language("SQL") String query = "DELETE FROM targetexecutions WHERE Test_ID =?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(tid));
    }

    public static boolean deleteMutantTargetExecutions(int mid) {
        @Language("SQL") String query = "DELETE FROM targetexecutions WHERE Mutant_ID = ?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(mid));
    }

    private static UserInfo userInfoFromRS(ResultSet rs) throws SQLException {
        final UserEntity user = userFromRS(rs);

        final Timestamp ts = rs.getTimestamp("lastLogin");
        final Instant lastLogin = ts != null ? ts.toInstant() : null;

        String lastRoleStr = rs.getString("lastRole");
        final Role lastRole = lastRoleStr == null ? null : Role.valueOf(lastRoleStr);
        final int totalScore = rs.getInt("TotalScore");

        return new UserInfo(user, lastLogin, lastRole, totalScore);
    }

    public static List<UserInfo> getAllUsersInfo() throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT DISTINCT users.*,
                       lastLogin.timeStamp AS lastLogin,
                       lastRole.Role       AS lastRole,
                       IFNULL(attackerScore.score, 0) + IFNULL(defenderScore.score, 0) AS TotalScore
                FROM view_valid_users users

                -- Last login.
                LEFT JOIN (
                    SELECT MAX(Timestamp) AS timeStamp,
                           User_ID
                    FROM sessions
                    GROUP BY User_ID
                ) AS lastLogin ON lastLogin.User_ID = users.User_ID

                -- Last role.
                LEFT JOIN (
                    SELECT players.User_ID,
                           players.Role
                    FROM players,
                    (
                        SELECT players.User_ID,
                               MAX(players.Game_ID) AS latestGameID
                        FROM players, games
                        WHERE games.ID = players.Game_ID
                          AND games.Mode <> 'PUZZLE'
                          AND (players.Role = 'ATTACKER' OR players.Role = 'DEFENDER')
                        GROUP BY players.User_ID
                    ) AS latestGame
                    WHERE latestGame.User_ID = players.User_ID
                      AND latestGame.latestGameID = players.Game_ID
                ) AS lastRole ON lastRole.User_ID = users.User_ID

                -- Attacker score.
                LEFT JOIN (
                    SELECT players.User_ID,
                           SUM(mutants.Points) AS score
                    FROM players,
                         mutants
                    WHERE players.id = mutants.Player_ID
                    GROUP BY players.User_ID
                ) AS attackerScore ON users.User_ID = attackerScore.User_ID

                -- Defender score.
                LEFT JOIN (
                    SELECT players.User_ID,
                           sum(tests.Points) AS score
                    FROM players,
                         tests
                    WHERE players.id = tests.Player_ID
                    GROUP BY players.User_ID
                ) AS defenderScore ON users.User_ID = defenderScore.User_ID;
        """;
        return DB.executeQueryReturnList(query, AdminDAO::userInfoFromRS);
    }

    public static List<UserInfo> getClassroomUsersInfo(int classroomId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT DISTINCT users.*,
                                lastLogin.timeStamp AS lastLogin,
                                lastRole.Role       AS lastRole,
                                IFNULL(attackerScore.score, 0) + IFNULL(defenderScore.score, 0) AS TotalScore
                FROM view_valid_users users
                LEFT JOIN classroom_members ON classroom_members.User_ID = users.User_ID

                -- Last login.
                LEFT JOIN (
                    SELECT MAX(Timestamp) AS timeStamp,
                           User_ID
                    FROM sessions
                    GROUP BY User_ID
                ) AS lastLogin ON lastLogin.User_ID = users.User_ID

                -- Last role.
                LEFT JOIN (
                    SELECT players.User_ID,
                           players.Role
                    FROM players,
                         (
                             SELECT players.User_ID,
                                    MAX(players.Game_ID) AS latestGameID
                             FROM players, games
                             WHERE games.ID = players.Game_ID
                               AND games.Mode <> 'PUZZLE'
                               AND (players.Role = 'ATTACKER' OR players.Role = 'DEFENDER')
                               AND games.Classroom_ID = ?
                             GROUP BY players.User_ID
                         ) AS latestGame
                    WHERE latestGame.User_ID = players.User_ID
                      AND latestGame.latestGameID = players.Game_ID
                ) AS lastRole ON lastRole.User_ID = users.User_ID

                -- Attacker score.
                LEFT JOIN (
                    SELECT players.User_ID,
                           SUM(mutants.Points) AS score
                    FROM players,
                         mutants,
                         games
                    WHERE players.id = mutants.Player_ID
                      AND players.Game_ID = games.ID
                      AND games.Classroom_ID = ?
                    GROUP BY players.User_ID
                ) AS attackerScore ON users.User_ID = attackerScore.User_ID

                -- Defender score.
                LEFT JOIN (
                    SELECT players.User_ID,
                           sum(tests.Points) AS score
                    FROM players,
                         tests,
                         games
                    WHERE players.id = tests.Player_ID
                      AND players.Game_ID = games.ID
                      AND games.Classroom_ID = ?
                    GROUP BY players.User_ID
                ) AS defenderScore ON users.User_ID = defenderScore.User_ID

                WHERE classroom_members.Classroom_ID = ?;
        """;
        return DB.executeQueryReturnList(query, AdminDAO::userInfoFromRS,
                DatabaseValue.of(classroomId), DatabaseValue.of(classroomId),
                DatabaseValue.of(classroomId), DatabaseValue.of(classroomId));
    }

    public static List<List<String>> getPlayersInfo(int gameId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT ID,
                       Username,
                       Role,
                       lastSubmission,
                       TotalScore,
                       IFNULL(nbSubmissions, 0) as nbSubmissions
                FROM (
                    SELECT User_ID, Role, ID
                    FROM players
                    WHERE Game_ID = ? AND Active = TRUE
                ) AS activePlayers

                JOIN users ON activePlayers.User_ID = users.User_ID

                JOIN (
                    SELECT U.User_ID,
                           IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore
                    FROM users U
                    LEFT JOIN (
                        SELECT PA.user_id,
                               sum(M.Points) AS AScore
                        FROM players PA
                        LEFT JOIN mutants M ON PA.id = M.Player_ID
                        GROUP BY PA.user_id
                    ) AS Attacker ON U.user_id = Attacker.user_id
                    LEFT JOIN (
                        SELECT PD.user_id,
                               sum(T.Points) AS DScore
                        FROM players PD
                        LEFT JOIN tests T ON PD.id = T.Player_ID
                        GROUP BY PD.user_id
                    ) AS Defender ON U.user_id = Defender.user_id
                ) AS totalScore ON totalScore.User_ID = users.User_ID

                LEFT JOIN (
                    SELECT MAX(ts) AS lastSubmission,
                           Player_ID
                    FROM (
                        SELECT MAX(mutants.Timestamp) AS ts,
                               Player_ID
                        FROM mutants
                        GROUP BY Player_ID

                        UNION

                        SELECT MAX(tests.Timestamp) AS ts,
                               Player_ID
                        FROM tests
                        GROUP BY Player_ID
                    ) AS t
                    GROUP BY Player_ID
                ) AS lastAction ON Player_ID = activePlayers.ID

                LEFT JOIN (
                    SELECT COUNT(*) AS nbSubmissions,
                           Player_ID
                    FROM (
                        SELECT Player_ID,
                               tests.Test_ID
                        FROM (
                            tests
                            JOIN targetexecutions t2 ON tests.Test_ID = t2.Test_ID
                        )
                        WHERE t2.Target = 'COMPILE_TEST' AND t2.Status = 'SUCCESS'

                        UNION

                        SELECT Player_ID,
                               mutants.Mutant_ID
                        FROM (
                            mutants
                            JOIN targetexecutions t2 ON mutants.Mutant_ID = t2.Mutant_ID
                        )
                        WHERE t2.Target = 'COMPILE_MUTANT' AND t2.Status = 'SUCCESS'
                    ) AS TestsAndMutants
                    GROUP BY Player_ID
                ) AS submissions ON submissions.Player_ID = ID

                ORDER BY Role, nbSubmissions;
        """;

        final DB.RSMapper<List<String>> mapper = rs -> {
            List<String> playerInfo = new ArrayList<>();
            playerInfo.add(String.valueOf(rs.getInt("ID")));
            playerInfo.add(rs.getString("Username"));
            playerInfo.add(rs.getString("Role"));
            Timestamp ts = rs.getTimestamp("lastSubmission");
            playerInfo.add(ts == null ? "never" : "" + ts.getTime());
            playerInfo.add(String.valueOf(rs.getInt("TotalScore")));
            playerInfo.add(String.valueOf(rs.getInt("nbSubmissions")));
            return playerInfo;
        };
        return DB.executeQueryReturnList(query, mapper, DatabaseValue.of(gameId));
    }

    public static boolean setUserPassword(int uid, String password) {
        @Language("SQL") String query = "UPDATE users SET Password = ? WHERE User_ID = ?;";
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(password),
                DatabaseValue.of(uid)
        };
        return DB.executeUpdateQuery(query, values);
    }

    public static boolean deleteUser(int userId) {
        @Language("SQL") String query = "DELETE FROM users WHERE User_ID = ?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(userId));
        // this does not work as foreign keys are not deleted (recommended: update w/ ON DELETE CASCADE)
    }

    public static boolean updateSystemSetting(AdminSystemSettings.SettingsDTO setting) {
        String valueToSet = setting.getType().name();

        DatabaseValue<?> value = null;
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

        @Language("SQL") String query = """
                UPDATE settings
                SET %s = ?
                WHERE name = ?;
        """.formatted(valueToSet);

        return DB.executeUpdateQuery(query, value, DatabaseValue.of(setting.getName().name()));
    }

    private static AdminSystemSettings.SettingsDTO settingFromRS(ResultSet rs) throws SQLException {
        SETTING_NAME name = SETTING_NAME.valueOf(rs.getString("name"));
        SETTING_TYPE settingType = SETTING_TYPE.valueOf(rs.getString("type"));
        switch (settingType) {
            case STRING_VALUE:
                return new AdminSystemSettings.SettingsDTO(name, rs.getString(settingType.name()));
            case INT_VALUE:
                return new AdminSystemSettings.SettingsDTO(name, rs.getInt(settingType.name()));
            case BOOL_VALUE:
                return new AdminSystemSettings.SettingsDTO(name, rs.getBoolean(settingType.name()));
            default:
                return null;
        }
    }

    public static List<AdminSystemSettings.SettingsDTO> getSystemSettings()
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = "SELECT * FROM settings;";
        return DB.executeQueryReturnList(query, AdminDAO::settingFromRS);
    }

    public static AdminSystemSettings.SettingsDTO getSystemSetting(SETTING_NAME name)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = "SELECT * FROM settings WHERE settings.name = ?;";
        return DB.executeQueryReturnValue(query, AdminDAO::settingFromRS, DatabaseValue.of(name.name()));
    }
}

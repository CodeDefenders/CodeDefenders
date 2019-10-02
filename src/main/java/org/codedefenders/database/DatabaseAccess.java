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

import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.leaderboard.Entry;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This class handles database logic for functionality which has not
 * yet been extracted to specific data access objects (DAO).
 * <p>
 * This means that more or less most methods are legacy and/or should
 * be moved to DAOs.
 */
public class DatabaseAccess {

    /**
     * Sanitises user input. If a whole SQL query is entered, syntax
     * errors may occur.
     *
     * @param s user input String
     * @return sanitised String s
     */
    public static String sanitise(String s) {
        s = s.replaceAll("\\<", "&lt;");
        s = s.replaceAll("\\>", "&gt;");
        s = s.replaceAll("\\\"", "&quot;");
        s = s.replaceAll("\\'", "&apos;");
        return s;
    }

    public static List<Event> getEventsForGame(int gameId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM events ",
                "LEFT JOIN event_messages AS em",
                "  ON events.Event_Type = em.Event_Type",
                "LEFT JOIN event_chat AS ec",
                "  ON events.Event_Id = ec.Event_Id ",
                "WHERE Game_ID=? ",
                "  AND Event_Status=?");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(EventStatus.GAME.toString())
        };
        return DB.executeQueryReturnList(query, DatabaseAccess::getEvents, values);
    }

    public static void removePlayerEventsForGame(int gameId, int playerId) {
        String query = "UPDATE events SET Event_Status=? WHERE Game_ID=? AND Player_ID=?";
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(EventStatus.DELETED.toString()),
                DatabaseValue.of(gameId),
                DatabaseValue.of(playerId)};
        DB.executeUpdateQuery(query, values);
    }

    public static List<Event> getNewEventsForGame(int gameId, long time, Role role) {
        String query = String.join("\n",
                "SELECT *",
                        "FROM events",
                        "LEFT JOIN event_messages AS em",
                        "  ON events.Event_Type = em.Event_Type ",
                        "LEFT JOIN event_chat AS ec",
                        "  ON events.Event_Id = ec.Event_Id",
                        "WHERE Game_ID=?",
                        "  AND Event_Status=? ",
                        "  AND Timestamp >= FROM_UNIXTIME(?)");
        if (role.equals(Role.ATTACKER)) {
            query += " AND events.Event_Type!='DEFENDER_MESSAGE'";
        } else if (role.equals(Role.DEFENDER)) {
            query += " AND events.Event_Type!='ATTACKER_MESSAGE'";
        }

        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(EventStatus.GAME.toString()),
                DatabaseValue.of(time)};

        return DB.executeQueryReturnList(query, DatabaseAccess::getEvents, values);
    }

    /**
     * Retrieve the latest (in the past 5 minutes and not yet seen)
     * events that belong to a game and relate to equivalence duels
     */
    // FIXME userId not useful
    public static List<Event> getNewEquivalenceDuelEventsForGame(int gameId, int lastMessageId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM events",
                "LEFT JOIN event_messages AS em",
                "  ON events.Event_Type = em.Event_Type ",
                "LEFT JOIN event_chat AS ec",
                "  ON events.Event_Id = ec.Event_Id ", // FIXME this is here otherwise the getEvents call fails, get rid of this...
                "WHERE Game_ID=?",
                "  AND Event_Status=?",
                "  AND (events.Event_Type=? OR events.Event_Type=? OR events.Event_Type=?) ",
                "  AND Timestamp >= FROM_UNIXTIME(UNIX_TIMESTAMP()-300) ",
                "  AND events.Event_ID > ?");
        // DEFENDER_MUTANT_CLAIMED_EQUIVALENT
        // EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT, EventStatus.GAME,
        // ATTACKER_MUTANT_KILLED_EQUIVALENT
        DatabaseValue[] values = new DatabaseValue[]{
//                DatabaseValue.of(userId),
                DatabaseValue.of(gameId),
                DatabaseValue.of(EventStatus.GAME.toString()),
                DatabaseValue.of(EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT.toString()),
                DatabaseValue.of(EventType.DEFENDER_MUTANT_EQUIVALENT.toString()),
                DatabaseValue.of(EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT.toString()),
                DatabaseValue.of(lastMessageId)};
        return DB.executeQueryReturnList(query, DatabaseAccess::getEventsWithMessage, values);
    }

    public static List<Event> getEventsForUser(int userId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM events ",
                "LEFT JOIN event_messages AS em",
                "  ON events.Event_Type = em.Event_Type ",
                "LEFT JOIN event_chat AS ec",
                "  ON events.Event_Id = ec.Event_Id",
                "WHERE Event_Status!='DELETED' ",
                "  AND Player_ID=?;");
        return DB.executeQueryReturnList(query, DatabaseAccess::getEvents, DatabaseValue.of(userId));
    }

    public static List<Event> getNewEventsForUser(int userId, long time) {
        String query = String.join("\n",
                "SELECT *",
                "FROM events ",
                "LEFT JOIN event_messages AS em",
                "  ON events.Event_Type = em.Event_Type ",
                "LEFT JOIN event_chat AS ec",
                "  ON events.Event_Id = ec.Event_Id ",
                "WHERE Player_ID=?",
                "  AND Event_Status<>?",
                "  AND Event_Status<>? ",
                "  AND Timestamp >= FROM_UNIXTIME(?)");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(userId),
                DatabaseValue.of(EventStatus.DELETED.toString()),
                DatabaseValue.of(EventStatus.GAME.toString()),
                DatabaseValue.of(time)};
        return DB.executeQueryReturnList(query, DatabaseAccess::getEvents, values);
    }

    private static Event getEvents(ResultSet rs) throws SQLException {
        Event event = new Event(
                rs.getInt("events.Event_ID"),
                rs.getInt("Game_ID"),
                rs.getInt("Player_ID"),
                rs.getString("em.Message"),
                rs.getString("events.Event_Type"),
                rs.getString("Event_Status"),
                rs.getTimestamp("Timestamp"));
        String chatMessage = rs.getString("ec.Message");
        event.setChatMessage(chatMessage);
        return event;
    }

    private static Event getEventsWithMessage(ResultSet rs) throws SQLException {
        Event event = new Event(
                rs.getInt("events.Event_ID"),
                rs.getInt("Game_ID"),
                rs.getInt("Player_ID"),
                rs.getString("events.Event_Message"),
                rs.getString("events.Event_Type"),
                rs.getString("Event_Status"),
                rs.getTimestamp("Timestamp"));
        String chatMessage = rs.getString("ec.Message");
        event.setChatMessage(chatMessage);
        return event;
    }

    public static Role getRole(int userId, int gameId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM games AS m ",
                "LEFT JOIN players AS p",
                "  ON p.Game_ID = m.ID",
                "  AND p.Active=TRUE ",
                "WHERE m.ID = ?",
                "  AND (p.User_ID=?",
                "      AND p.Game_ID=?)");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId),
                DatabaseValue.of(gameId)};

        DB.RSMapper<Role> mapper = rs -> Role.valueOrNull(rs.getString("Role"));

        final Role role = DB.executeQueryReturnValue(query, mapper, values);

        MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);
        
        if (game.getCreatorId() == userId && role == null) {
            return Role.OBSERVER;
        }
        return Optional.ofNullable(role).orElse(Role.NONE);
    }

    public static void increasePlayerPoints(int points, int player) {
        String query = "UPDATE players SET Points=Points+? WHERE ID=?";
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(points),
                DatabaseValue.of(player)
        };
        DB.executeUpdateQuery(query, values);
    }

    public static int getEquivalentDefenderId(Mutant m) {
        String query = "SELECT * FROM equivalences WHERE Mutant_ID=?;";
        final Integer id = DB.executeQueryReturnValue(query, rs -> rs.getInt("Defender_ID"), DatabaseValue.of(m.getId()));
        return Optional.ofNullable(id).orElse(-1);
    }

    public static int getPlayerPoints(int playerId) {
        String query = "SELECT Points FROM players WHERE ID=?;";
        final Integer points = DB.executeQueryReturnValue(query, rs -> rs.getInt("Points"), DatabaseValue.of(playerId));
        return Optional.ofNullable(points).orElse(0);
    }

    public static boolean insertEquivalence(Mutant mutant, int defender) {
        String query = String.join("\n",
                "INSERT INTO equivalences (Mutant_ID, Defender_ID, Mutant_Points)",
                "VALUES (?, ?, ?)"
        );
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(mutant.getId()),
                DatabaseValue.of(defender),
                DatabaseValue.of(mutant.getScore())
        };
        return DB.executeUpdateQuery(query, values);
    }

    static Entry entryFromRS(ResultSet rs) throws SQLException {
        Entry p = new Entry();
        p.setUsername(rs.getString("username"));
        p.setMutantsSubmitted(rs.getInt("NMutants"));
        p.setAttackerScore(rs.getInt("AScore"));
        p.setTestsSubmitted(rs.getInt("NTests"));
        p.setDefenderScore(rs.getInt("DScore"));
        p.setMutantsKilled(rs.getInt("NKilled"));
        p.setTotalPoints(rs.getInt("TotalScore"));
        return p;
    }

    public static List<Entry> getLeaderboard() {
        String query = String.join("\n",
                "SELECT U.username AS username, IFNULL(NMutants,0) AS NMutants, IFNULL(AScore,0) AS AScore, IFNULL(NTests,0) AS NTests, IFNULL(DScore,0) AS DScore, IFNULL(NKilled,0) AS NKilled, IFNULL(AScore,0)+IFNULL(DScore,0) AS TotalScore",
                "FROM view_valid_users U",
                "LEFT JOIN (SELECT PA.user_id, count(M.Mutant_ID) AS NMutants, sum(M.Points) AS AScore FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID GROUP BY PA.user_id) AS Attacker ON U.user_id = Attacker.user_id",
                "LEFT JOIN (SELECT PD.user_id, count(T.Test_ID) AS NTests, sum(T.Points) AS DScore, sum(T.MutantsKilled) AS NKilled FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID GROUP BY PD.user_id) AS Defender ON U.user_id = Defender.user_id");
        return DB.executeQueryReturnList(query, DatabaseAccess::entryFromRS);
    }

    public static int getKillingTestIdForMutant(int mutantId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM targetexecutions",
                "WHERE Target = ?",
                "  AND Status != ?",
                "  AND Mutant_ID = ?;"
        );
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(TargetExecution.Target.TEST_MUTANT.name()),
                DatabaseValue.of(TargetExecution.Status.SUCCESS.name()),
                DatabaseValue.of(mutantId)
        };
        TargetExecution targ = DB.executeQueryReturnValue(query, TargetExecutionDAO::targetExecutionFromRS, values);
        // TODO: We shouldn't give away that we don't know which test killed the mutant?
        return Optional.ofNullable(targ).map(t -> t.testId).orElse(-1);
    }

    public static Test getKillingTestForMutantId(int mutantId) {
        int testId = getKillingTestIdForMutant(mutantId);
        if (testId == -1) {
            return null;
        } else {
            return TestDAO.getTestById(testId);
        }
    }
    public static Set<Mutant> getKilledMutantsForTestId(int testId) {
        String query = String.join("\n",
                "SELECT DISTINCT m.*",
                "FROM targetexecutions te, mutants m",
                "WHERE te.Target = ?",
                "  AND te.Status != ?",
                "  AND te.Test_ID = ?",
                "  AND te.Mutant_ID = m.Mutant_ID",
                "ORDER BY m.Mutant_ID ASC");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(TargetExecution.Target.TEST_MUTANT.name()),
                DatabaseValue.of(TargetExecution.Status.SUCCESS.name()),
                DatabaseValue.of(testId)
        };
        final List<Mutant> mutants = DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, values);
        return new HashSet<>(mutants);
    }

    /**
     * This also automatically update the Timestamp field using CURRENT_TIMESTAMP()
     */
    public static void logSession(int uid, String ipAddress) {
        String query = "INSERT INTO sessions (User_ID, IP_Address) VALUES (?, ?);";
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(uid),
                DatabaseValue.of(ipAddress)
        };
        DB.executeUpdateQuery(query, values);
    }

    public static int getLastCompletedSubmissionForUserInGame(int userId, int gameId, boolean isDefender) {
        String query = isDefender ? "SELECT MAX(test_id) FROM tests" : "SELECT MAX(mutant_id) FROM mutants";
        query += " WHERE game_id=? AND player_id = (SELECT id FROM players WHERE game_id=? AND user_id=?);";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId)
        };

        final Integer result = DB.executeQueryReturnValue(query, rs -> rs.getInt(1), valueList);
        return Optional.ofNullable(result).orElse(-1);
    }

    public static TargetExecution.Target getStatusOfRequestForUserInGame(int userId, int gameId, int lastSubmissionId, boolean isDefender) {
        // Current test is the one right after lastTestId in the user/game context
        String query = isDefender ?
                "SELECT * FROM targetexecutions WHERE Test_ID > ? AND Test_ID in (SELECT Test_ID FROM tests" :
                "SELECT * FROM targetexecutions WHERE Mutant_ID > ? AND Mutant_ID in (SELECT Mutant_ID FROM mutants";
        query += " WHERE game_id=? AND player_id = (SELECT id from players where game_id=? and user_id=?))"
                + "AND TargetExecution_ID >= (SELECT MAX(TargetExecution_ID) from targetexecutions);";

        DatabaseValue[] valueList = new DatabaseValue[]{
                DatabaseValue.of(lastSubmissionId),
                DatabaseValue.of(gameId),
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId)
        };
        TargetExecution t = DB.executeQueryReturnValue(query, TargetExecutionDAO::targetExecutionFromRS, valueList);
        return Optional.ofNullable(t).map(te -> te.target).orElse(null);
    }

    public static boolean setPasswordResetSecret(int userId, String pwResetSecret) {
        String query = String.join("\n",
                "UPDATE users",
                "SET pw_reset_secret = ?,",
                "    pw_reset_timestamp = CURRENT_TIMESTAMP",
                "WHERE User_ID = ?;");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(pwResetSecret),
                DatabaseValue.of(userId)
        };
        return DB.executeUpdateQuery(query, values);
    }

    public static int getUserIDForPWResetSecret(String pwResetSecret) {
        String query = "SELECT User_ID\n" +
                "FROM users\n" +
                "WHERE TIMESTAMPDIFF(HOUR, pw_reset_timestamp, CURRENT_TIMESTAMP) < (SELECT INT_VALUE\n" +
                "                                                                    FROM settings\n" +
                "                                                                    WHERE name =\n" +
                "                                                                    'PASSWORD_RESET_SECRET_LIFESPAN')\n" +
                "      AND\n" +
                "      pw_reset_secret = ?;";

        final Integer userId = DB.executeQueryReturnValue(query, rs -> rs.getInt("User_ID"), DatabaseValue.of(pwResetSecret));
        return Optional.ofNullable(userId).orElse(-1);
    }
}

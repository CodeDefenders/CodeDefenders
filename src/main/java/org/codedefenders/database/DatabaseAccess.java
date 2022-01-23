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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.EventStatus;

/**
 * This class handles database logic for functionality which has not
 * yet been extracted to specific data access objects (DAO).
 *
 * <p>This means that more or less most methods are legacy and/or should
 * be moved to DAOs.
 */
public class DatabaseAccess {

    /**
     * @param gameId The gameId for which to remove the events.
     * @param userId The userId of the events to remove.
     *
     * @implNote Even if the database table column is called {@code Player_ID} it stores User_IDs!
     */
    public static void removePlayerEventsForGame(int gameId, int userId) {
        String query = "UPDATE events SET Event_Status=? WHERE Game_ID=? AND Player_ID=? "
                + "AND Event_Type NOT IN ('GAME_CREATED', 'GAME_STARTED', 'GAME_FINISHED', 'GAME_GRACE_ONE', 'GAME_GRACE_TWO');";
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(EventStatus.DELETED.toString()),
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId)};
        DB.executeUpdateQuery(query, values);
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

        if (role == null) {
            AbstractGame game = GameDAO.getGame(gameId);
            if (game != null && game.getCreatorId() == userId) {
                return Role.OBSERVER;
            }
        }

        return Optional.ofNullable(role).orElse(Role.NONE);
    }

    public static int getEquivalentDefenderId(Mutant m) {
        String query = "SELECT * FROM equivalences WHERE Mutant_ID=?;";
        final Integer id = DB.executeQueryReturnValue(query,
                rs -> rs.getInt("Defender_ID"), DatabaseValue.of(m.getId()));
        return Optional.ofNullable(id).orElse(-1);
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
}

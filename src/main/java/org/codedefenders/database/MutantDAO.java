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

import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * This class handles the database logic for mutants.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see Mutant
 */
public class MutantDAO {
    private static final Logger logger = LoggerFactory.getLogger(MutantDAO.class);

    /**
     * Stores a given {@link Mutant} in the database.
     *
     * @param mutant the given mutant as a {@link Mutant}.
     * @throws Exception If storing the mutant was not successful.
     * @return the generated identifier of the mutant as an {@code int}.
     */
    public static int storeMutant(Mutant mutant) throws Exception {
        String javaFile = DatabaseAccess.addSlashes(mutant.getJavaFile());
        String classFile = DatabaseAccess.addSlashes(mutant.getClassFile());
        int gameId = mutant.getGameId();
        int roundCreated = mutant.getRoundCreated();
        int sqlAlive = mutant.sqlAlive();
        int playerId = mutant.getPlayerId();
        int score = mutant.getScore();
        String md5 = mutant.getMd5();
        Integer classId = mutant.getClassId();

        String query = "INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated, Alive, Player_ID, Points, MD5, Class_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(javaFile),
                DB.getDBV(classFile),
                DB.getDBV(gameId),
                DB.getDBV(roundCreated),
                DB.getDBV(sqlAlive),
                DB.getDBV(playerId),
                DB.getDBV(score),
                DB.getDBV(md5),
                (classId == null) ? null : DB.getDBV(classId)
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final int result = DB.executeUpdateGetKeys(stmt, conn);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store mutant to database.");
        }
    }

    /**
     * Stores a mapping between a {@link Mutant} and a {@link GameClass} in the database.
     *
     * @param mutantId the identifier of the mutant.
     * @param classId the identifier of the class.
     * @return {@code true} whether storing the mapping was successful, {@code false} otherwise.
     */
    public static boolean mapMutantToClass(Integer mutantId, Integer classId) {
        String query = "UPDATE mutants SET Class_ID = ? WHERE Mutant_ID = ?";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(classId),
                DB.getDBV(mutantId)
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }

    /**
     * Removes a mutant for a given identifier.
     *
     * @param id the identifier of the mutant to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeMutantForId(Integer id) {
        String query = "DELETE FROM mutants WHERE Mutant_ID = ?;";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(id),
        };

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }

    /**
     * Removes multiple mutants for a given list of identifiers.
     *
     * @param mutants the identifiers of the mutants to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeMutantsForIds(List<Integer> mutants) {
        if (mutants.isEmpty()) {
            return false;
        }

        final StringBuilder bob = new StringBuilder("(");
        for (int i = 0; i < mutants.size() - 1; i++) {
            bob.append("?,");
        }
        bob.append("?);");

        final String range = bob.toString();
        String query = "DELETE FROM mutants WHERE Mutant_ID in " + range;

        DatabaseValue[] valueList = mutants.stream().map(DB::getDBV).toArray(DatabaseValue[]::new);

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }
}

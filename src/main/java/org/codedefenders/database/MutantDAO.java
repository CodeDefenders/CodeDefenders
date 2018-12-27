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

import org.apache.commons.lang.StringUtils;
import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Mutant.Equivalence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class handles the database logic for mutants.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see Mutant
 */
public class MutantDAO {
    /**
     * Constructs a mutant from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed mutant.
     * @see RSMapper
     */
    public static Mutant mutantFromRS(ResultSet rs) throws SQLException {
        int mutantId = rs.getInt("Mutant_ID");
        int gameId = rs.getInt("Game_ID");
        int classId = rs.getInt("Class_ID");
        // before 1.3.2, mutants didn't have a mandatory classId attribute
        if (rs.wasNull()) {
            classId = -1;
        }
        String javaFile = rs.getString("JavaFile");
        String classFile = rs.getString("ClassFile");
        boolean alive = rs.getBoolean("Alive");
        Equivalence equiv = Equivalence.valueOf(rs.getString("Equivalent"));
        int roundCreated = rs.getInt("RoundCreated");
        int roundKilled = rs.getInt("RoundKilled");
        int playerId = rs.getInt("Player_ID");
        int points = rs.getInt("Points");
        String md5 = rs.getString("MD5");

        Mutant mutant = new Mutant(mutantId, classId, gameId, javaFile, classFile, alive, equiv, roundCreated,
                roundKilled, playerId, md5);
        mutant.setScore(points);
        // since mutated lines can be null
        final String mutatedLines = rs.getString("MutatedLines");
        if (mutatedLines != null && !mutatedLines.isEmpty()) {
            List<Integer> mutatedLinesList = Stream.of(mutatedLines.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            // force write
            mutant.setLines(mutatedLinesList);
        }
        try {
            String username = rs.getString("Username");
            int userId = rs.getInt("User_ID");
            mutant.setCreatorName(username);
            mutant.setCreatorId(userId);
        } catch (SQLException e) { /* Username/User_ID cannot be retrieved from query (no join). */ }

        return mutant;
    }

    /**
     * Returns the {@link Mutant} for the given mutant id.
     */
    public static Mutant getMutantById(int mutantId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT * FROM mutants ",
                "LEFT JOIN players ON players.ID = mutants.Player_ID ",
                "LEFT JOIN users ON players.User_ID = users.User_ID ",
                "WHERE mutants.Mutant_ID = ?;");
        return DB.executeQueryReturnValue(query, MutantDAO::mutantFromRS, DB.getDBV(mutantId));
    }

    /**
     * Returns the {@link Mutant} with the given md5 sum from the given game.
     */
    public static Mutant getMutantByGameAndMd5(int gameId, String md5) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT * FROM mutants ",
                "LEFT JOIN players ON players.ID=mutants.Player_ID ",
                "LEFT JOIN users ON players.User_ID = users.User_ID ",
                "WHERE mutants.Game_ID = ? AND mutants.MD5 = ?;");
        return DB.executeQueryReturnValue(query, MutantDAO::mutantFromRS, DB.getDBV(gameId), DB.getDBV(md5));
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the given game.
     */
    public static List<Mutant> getValidMutantsForGame(int gameId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT * FROM mutants ",
                "LEFT JOIN players ON players.ID = mutants.Player_ID ",
                "LEFT JOIN users ON players.User_ID = users.User_ID ",
                "WHERE mutants.Game_ID = ?",
                "  AND mutants.ClassFile IS NOT NULL;");
        return DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, DB.getDBV(gameId));
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the games played on the given class.
     */
    public static List<Mutant> getValidMutantsForClass(int classId) throws UncheckedSQLException, SQLMappingException {
        List<Mutant> result = new ArrayList<>();

        String query = String.join("\n",
                "SELECT mutants.*",
                "FROM mutants, games",
                "WHERE mutants.Game_ID = games.ID",
                "  AND games.Class_ID = ?",
                "  AND mutants.ClassFile IS NOT NULL;");

        result.addAll(DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, DB.getDBV(classId)));

        // Include also those mutants created during the upload. Player = -1
        String systemAttackerQuery = String.join("\n",
                "SELECT mutants.*",
                "FROM mutants, mutant_uploaded_with_class up",
                "WHERE mutants.Mutant_ID = up.Mutant_ID",
                "  AND mutants.Class_ID = ?",
                "  AND mutants.ClassFile IS NOT NULL;");

        result.addAll(DB.executeQueryReturnList(systemAttackerQuery, MutantDAO::mutantFromRS, DB.getDBV(classId)));

        return result;
    }

    /**
     * Returns the compilable {@link Mutant Mutants} submitted by the given player.
     */
    public static List<Mutant> getValidMutantsForPlayer(int playerId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT * FROM mutants ",
                "LEFT JOIN players ON players.ID=mutants.Player_ID ",
                "LEFT JOIN users ON players.User_ID = users.User_ID ",
                "WHERE mutants.Player_ID = ?",
                "  AND mutants.ClassFile IS NOT NULL;");
        return DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, DB.getDBV(playerId));
    }

    /**
     * Stores a given {@link Mutant} in the database.
     * <p>
     * This method does not update the given mutant object.
     * Use {@link Mutant#insert()} instead.
     *
     * @param mutant the given mutant as a {@link Mutant}.
     * @return the generated identifier of the mutant as an {@code int}.
     * @throws Exception If storing the mutant was not successful.
     */
    public static int storeMutant(Mutant mutant) throws Exception {
        String javaFile = DatabaseAccess.addSlashes(mutant.getJavaFile());
        String classFile = DatabaseAccess.addSlashes(mutant.getClassFile());
        int gameId = mutant.getGameId();
        int classId = mutant.getClassId();
        int roundCreated = mutant.getRoundCreated();
        Equivalence equivalent = mutant.getEquivalent() == null ? Equivalence.ASSUMED_NO : mutant.getEquivalent();
        int sqlAlive = mutant.sqlAlive();
        int playerId = mutant.getPlayerId();
        int score = mutant.getScore();
        String md5 = mutant.getMd5();
        String mutatedLinesString = StringUtils.join(mutant.getLines(), ",");

        String query = String.join("\n",
                "INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated, Equivalent, Alive, Player_ID, Points, MD5, Class_ID, MutatedLines)",
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
        );
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(javaFile),
                DB.getDBV(classFile),
                DB.getDBV(gameId),
                DB.getDBV(roundCreated),
                DB.getDBV(equivalent.name()),
                DB.getDBV(sqlAlive),
                DB.getDBV(playerId),
                DB.getDBV(score),
                DB.getDBV(md5),
                DB.getDBV(classId),
                DB.getDBV(mutatedLinesString)
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
     * @param classId  the identifier of the class.
     * @return {@code true} whether storing the mapping was successful, {@code false} otherwise.
     */
    public static boolean mapMutantToClass(int mutantId, int classId) {
        String query = String.join("\n",
                "INSERT INTO mutant_uploaded_with_class (Mutant_ID, Class_ID)",
                "VALUES (?, ?);"
        );
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(mutantId),
                DB.getDBV(classId)
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
        String query = String.join("\n",
                "DELETE FROM mutants WHERE Mutant_ID = ?;",
                "DELETE FROM mutant_uploaded_with_class WHERE Mutant_ID = ?"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));

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
        String query = String.join("\n",
                "DELETE FROM mutants WHERE Mutant_ID in ",
                range,
                "DELETE FROM mutant_uploaded_with_class WHERE Mutant_ID in ",
                range
        );

        // Hack to make sure all values are listed in both 'ranges'.
        mutants.addAll(new LinkedList<>(mutants));
        DatabaseValue[] valueList = mutants.stream().map(DB::getDBV).toArray(DatabaseValue[]::new);

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }

    /**
     * Returns the class ID for the given mutant.
     *
     * @param mutantId The mutant ID of the mutant.
     * @return The class ID for the given mutant.
     */
    public static Integer getClassIdForMutant(int mutantId) {
        String query = String.join("\n",
                "SELECT games.Class_ID",
                "FROM mutants, games",
                "WHERE mutants.Mutant_ID = ?",
                "  AND mutants.Game_ID = games.ID;"
        );

        return DB.executeQueryReturnValue(query, res -> res.getInt("Class_ID"), DB.getDBV(mutantId));
    }
}

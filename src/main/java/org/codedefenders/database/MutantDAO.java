package org.codedefenders.database;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedList;
import java.util.List;

/**
 * This class handles the database logic for mutants.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
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

        String query = "INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated, Alive, Player_ID, Points, MD5) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(javaFile),
                DB.getDBV(classFile),
                DB.getDBV(gameId),
                DB.getDBV(roundCreated),
                DB.getDBV(sqlAlive),
                DB.getDBV(playerId),
                DB.getDBV(score),
                DB.getDBV(md5)};
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
        String query = "INSERT INTO mutant_belongs_to_class (Mutant_ID, Class_ID) VALUES (?, ?)";
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
        String query = "DELETE FROM mutants WHERE Mutant_ID = ?;" +
                "DELETE FROM mutant_belongs_to_class WHERE Mutant_ID = ?";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(id),
                DB.getDBV(id)
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
        String query = "DELETE FROM mutants WHERE Mutant_ID in " + range +
                "DELETE FROM mutant_belongs_to_class WHERE Mutant_ID in " + range;

        // Hack to make sure all values are listed in both 'ranges'.
        mutants.addAll(new LinkedList<>(mutants));
        DatabaseValue[] valueList = mutants.stream().map(DB::getDBV).toArray(DatabaseValue[]::new);

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }
}

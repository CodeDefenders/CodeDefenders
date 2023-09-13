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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Mutant.Equivalence;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.FileUtils;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This class handles the database logic for mutants.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see Mutant
 */
public class MutantDAO {
    private static final Logger logger = LoggerFactory.getLogger(MutantDAO.class);

    /**
     * Constructs a mutant from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed mutant.
     * @see RSMapper
     */
    static Mutant mutantFromRS(ResultSet rs) throws SQLException {
        int mutantId = rs.getInt("Mutant_ID");
        int gameId = rs.getInt("Game_ID");
        int classId = rs.getInt("Class_ID");
        // before 1.3.2, mutants didn't have a mandatory classId attribute
        if (rs.wasNull()) {
            classId = -1;
        }
        String javaFile = rs.getString("JavaFile");
        String classFile = rs.getString("ClassFile");
        String absoluteJavaFile = FileUtils.getAbsoluteDataPath(javaFile).toString();
        String absoluteClassFile = classFile == null ? null : FileUtils.getAbsoluteDataPath(classFile).toString();
        boolean alive = rs.getBoolean("Alive");
        Equivalence equiv = Equivalence.valueOf(rs.getString("Equivalent"));
        int roundCreated = rs.getInt("RoundCreated");
        int roundKilled = rs.getInt("RoundKilled");
        int playerId = rs.getInt("Player_ID");
        int points = rs.getInt("Points");
        String md5 = rs.getString("MD5");

        String killMessage = rs.getString("KillMessage");

        Mutant mutant = new Mutant(mutantId, classId, gameId, absoluteJavaFile, absoluteClassFile, alive, equiv,
                roundCreated, roundKilled, playerId, md5, killMessage);
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
        @Language("SQL") String query = """
                SELECT * FROM view_mutants_with_user m
                WHERE m.Mutant_ID = ?;
        """;
        return DB.executeQueryReturnValue(query, MutantDAO::mutantFromRS, DatabaseValue.of(mutantId));
    }

    /**
     * Returns the {@link Mutant} with the given md5 sum from the given game.
     */
    public static Mutant getMutantByGameAndMd5(int gameId, String md5)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_mutants_with_user m
                WHERE m.Game_ID = ? AND m.MD5 = ?;
        """;
        return DB.executeQueryReturnValue(query, MutantDAO::mutantFromRS,
                DatabaseValue.of(gameId), DatabaseValue.of(md5));
    }

    /**
     * Returns the {@link Mutant Mutants} from the given game for the given player.
     */
    public static List<Mutant> getMutantsByGameAndPlayer(int gameId, int playerId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_valid_game_mutants m
                WHERE m.Game_ID = ?
                  AND m.Player_ID = ?;
        """;
        return DB.executeQueryReturnList(query, MutantDAO::mutantFromRS,
                DatabaseValue.of(gameId), DatabaseValue.of(playerId));
    }

    /**
     * Returns the {@link Mutant Mutants} from the given game for the given user.
     */
    public static List<Mutant> getMutantsByGameAndUser(int gameId, int userId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_valid_game_mutants m
                WHERE m.Game_ID = ?
                  AND m.User_ID = ?;
        """;
        return DB.executeQueryReturnList(query, MutantDAO::mutantFromRS,
                DatabaseValue.of(gameId), DatabaseValue.of(userId));
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the given game.
     *
     * <p>This includes valid user-submitted mutants as well as instances of predefined mutants in the game.
     */
    public static List<Mutant> getValidMutantsForGame(int gameId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT *
                FROM view_valid_game_mutants m
                WHERE m.Game_ID = ?;
        """;
        return DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, DatabaseValue.of(gameId));
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the games played on the given class.
     *
     * <p>This includes valid user-submitted mutants as well as templates of predefined mutants
     * (not the instances that are copied into games).
     */
    public static List<Mutant> getValidMutantsForClass(int classId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                WITH mutants_for_class AS
                   (SELECT * FROM view_valid_user_mutants UNION ALL SELECT * FROM view_system_mutant_templates)

                SELECT mutants.*
                FROM mutants_for_class mutants
                WHERE mutants.Class_ID = ?
        """;

        return DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, DatabaseValue.of(classId));
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the games played on the given class.
     *
     * <p>This includes valid user-submitted mutants from classroom games as well as templates of predefined mutants
     * for classes used in the classroom.
     */
    public static Multimap<Integer, Mutant> getValidMutantsForClassroom(int classroomId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                WITH relevant_classes AS (
                    SELECT DISTINCT games.Class_ID
                    FROM games
                    WHERE games.Classroom_ID = ?
                ),
                classroom_system_mutants AS (
                    SELECT mutants.*
                    FROM view_system_mutant_templates mutants
                    WHERE mutants.Class_ID IN (SELECT * FROM relevant_classes)
                ),
                classroom_user_mutants AS (
                    SELECT mutants.*
                    FROM view_valid_user_mutants mutants, games
                    WHERE mutants.Game_ID = games.ID
                      AND games.Classroom_ID = ?
                )

                SELECT * FROM classroom_system_mutants
                UNION ALL
                SELECT * FROM classroom_user_mutants;
        """;

        List<Mutant> mutants = DB.executeQueryReturnList(query, MutantDAO::mutantFromRS,
                DatabaseValue.of(classroomId), DatabaseValue.of(classroomId));

        Multimap<Integer, Mutant> mutantsMap = ArrayListMultimap.create();
        for (Mutant mutant : mutants) {
            mutantsMap.put(mutant.getClassId(), mutant);
        }
        return mutantsMap;
    }

    /**
     * Returns the compilable {@link Mutant Mutants} submitted by the given player.
     */
    public static List<Mutant> getValidMutantsForPlayer(int playerId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT *
                FROM view_valid_mutants m
                WHERE Player_ID = ?
        """;
        return DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, DatabaseValue.of(playerId));
    }

    /**
     * Stores a given {@link Mutant} in the database.
     *
     * <p>This method does not update the given mutant object.
     * Use {@link Mutant#insert()} instead.
     *
     * @param mutant the given mutant as a {@link Mutant}.
     * @return the generated identifier of the mutant as an {@code int}.
     * @throws Exception If storing the mutant was not successful.
     */
    public static int storeMutant(Mutant mutant) throws Exception {
        String relativeJavaFile = FileUtils.getRelativeDataPath(mutant.getJavaFile()).toString();
        String relativeClassFile = mutant.getClassFile()
                == null ? null : FileUtils.getRelativeDataPath(mutant.getClassFile()).toString();
        int gameId = mutant.getGameId();
        int classId = mutant.getClassId();
        int roundCreated = mutant.getRoundCreated();
        Equivalence equivalent = mutant.getEquivalent() == null ? Equivalence.ASSUMED_NO : mutant.getEquivalent();
        boolean alive = mutant.isAlive();
        int playerId = mutant.getPlayerId();
        int score = mutant.getScore();
        String md5 = mutant.getMd5();
        String mutatedLinesString = StringUtils.join(mutant.getLines(), ",");

        @Language("SQL") String query = """
                INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated, Equivalent,
                        Alive, Player_ID, Points, MD5, Class_ID, MutatedLines)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(relativeJavaFile),
                DatabaseValue.of(relativeClassFile),
                DatabaseValue.of(gameId),
                DatabaseValue.of(roundCreated),
                DatabaseValue.of(equivalent.name()),
                DatabaseValue.of(alive),
                DatabaseValue.of(playerId),
                DatabaseValue.of(score),
                DatabaseValue.of(md5),
                DatabaseValue.of(classId),
                DatabaseValue.of(mutatedLinesString)
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store mutant to database.");
        }
    }

    /**
     * Updates a given {@link Mutant} in the database and returns whether
     * updating was successful or not.
     *
     * @param mutant the given mutant as a {@link Mutant}.
     * @return whether updating was successful or not
     * @throws UncheckedSQLException If storing the mutant was not successful.
     */
    public static boolean updateMutant(Mutant mutant) throws UncheckedSQLException {
        int mutantId = mutant.getId();

        Equivalence equivalent = mutant.getEquivalent() == null ? Equivalence.ASSUMED_NO : mutant.getEquivalent();
        boolean alive = mutant.isAlive();
        int roundKilled = mutant.getRoundKilled();
        int score = mutant.getScore();

        @Language("SQL") String query = """
                UPDATE mutants
                SET Equivalent = ?,
                    Alive = ?,
                    RoundKilled = ?,
                    Points = ?
                WHERE Mutant_ID = ? AND Alive = 1;
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
            DatabaseValue.of(equivalent.name()),
            DatabaseValue.of(alive),
            DatabaseValue.of(roundKilled),
            DatabaseValue.of(score),
            DatabaseValue.of(mutantId)
        };

        return DB.executeUpdateQuery(query, values);
    }

    public static boolean updateMutantScore(Mutant mutant) throws UncheckedSQLException {
        int mutantId = mutant.getId();

        int score = mutant.getScore();

        @Language("SQL") String query = """
                UPDATE mutants
                SET Points = ?
                WHERE Mutant_ID = ?;
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(score),
                DatabaseValue.of(mutantId)
        };

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Updates a given {@link Mutant} in the database and returns whether
     * updating was successful or not.
     *
     * @param mutant the given mutant as a {@link Mutant}.
     * @return whether updating was successful or not
     * @throws UncheckedSQLException If storing the mutant was not successful.
     */
    public static boolean updateMutantKillMessageForMutant(Mutant mutant) throws UncheckedSQLException {
        int mutantId = mutant.getId();

        String killMessage = mutant.getKillMessage();

        @Language("SQL") String query = """
                UPDATE mutants
                SET KillMessage = ?
                WHERE Mutant_ID = ?;
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
            DatabaseValue.of(killMessage),
            DatabaseValue.of(mutantId)
        };

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Stores a mapping between a {@link Mutant} and a {@link GameClass} in the database.
     *
     * @param mutantId the identifier of the mutant.
     * @param classId  the identifier of the class.
     * @return {@code true} whether storing the mapping was successful, {@code false} otherwise.
     */
    public static boolean mapMutantToClass(int mutantId, int classId) {
        @Language("SQL") String query = """
                INSERT INTO mutant_uploaded_with_class (Mutant_ID, Class_ID)
                VALUES (?, ?);
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(mutantId),
                DatabaseValue.of(classId)
        };
        return DB.executeUpdateQuery(query, values);
    }


    /**
     * Removes a mutant for a given identifier.
     *
     * @param id the identifier of the mutant to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeMutantForId(Integer id) {
        @Language("SQL") String query = """
                DELETE FROM mutants WHERE Mutant_ID = ?;
                DELETE FROM mutant_uploaded_with_class WHERE Mutant_ID = ?
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(id),
                DatabaseValue.of(id)
        };
        return DB.executeUpdateQuery(query, values);
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

        String range = Stream.generate(() -> "?")
                .limit(mutants.size())
                .collect(Collectors.joining(","));

        @Language("SQL") String query = """
                DELETE FROM mutants
                WHERE Mutant_ID in (%s);
                DELETE FROM mutant_uploaded_with_class
                WHERE Mutant_ID in (%s);
        """.formatted(
                range,
                range
        );

        // Hack to make sure all values are listed in both 'ranges'.
        mutants.addAll(new LinkedList<>(mutants));
        DatabaseValue<?>[] values = mutants.stream().map(DatabaseValue::of).toArray(DatabaseValue[]::new);

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Returns the class ID for the given mutant.
     *
     * @param mutantId The mutant ID of the mutant.
     * @return The class ID for the given mutant.
     */
    public static Integer getClassIdForMutant(int mutantId) {
        @Language("SQL") String query = """
                SELECT games.Class_ID
                FROM mutants, games
                WHERE mutants.Mutant_ID = ?
                  AND mutants.Game_ID = games.ID;
        """;

        return DB.executeQueryReturnValue(query, res -> res.getInt("Class_ID"), DatabaseValue.of(mutantId));
    }

    /**
     * Returns the number of killed AI tests for a given mutant.
     *
     * @param mutantId the identifier of the mutant.
     * @return number of killed AI tests, or {@code 0} if none found.
     */
    public static int getNumTestsKillMutant(int mutantId) {
        @Language("SQL") String query = "SELECT * FROM mutants WHERE Mutant_ID=?;";
        final Integer kills = DB.executeQueryReturnValue(query, rs -> rs.getInt("NumberAiKillingTests"),
                DatabaseValue.of(mutantId));
        return Optional.ofNullable(kills).orElse(0);
    }

    public static int getEquivalentDefenderId(Mutant m) {
        @Language("SQL") String query = "SELECT * FROM equivalences WHERE Mutant_ID=?;";
        final Integer id = DB.executeQueryReturnValue(query,
                rs -> rs.getInt("Defender_ID"), DatabaseValue.of(m.getId()));
        return Optional.ofNullable(id).orElse(-1);
    }

    public static boolean insertEquivalence(Mutant mutant, int defender) {
        @Language("SQL") String query = """
                INSERT INTO equivalences (Mutant_ID, Defender_ID, Mutant_Points)
                VALUES (?, ?, ?)
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(mutant.getId()),
                DatabaseValue.of(defender),
                DatabaseValue.of(mutant.getScore())
        };
        return DB.executeUpdateQuery(query, values);
    }

    public static void incrementMutantScore(Mutant mutant, int score) {
        if (score == 0) {
            logger.debug("Do not update mutant {} score by 0", mutant.getId());
            return;
        }

        @Language("SQL") String query = """
                UPDATE mutants
                SET Points = Points + ?
                WHERE Mutant_ID = ? AND Alive = 1;
        """;

        try {
            CDIUtil.getBeanFromCDI(QueryRunner.class).update(query,
                    score, mutant.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean killMutant(Mutant mutant, Equivalence equivalence) {
        mutant.setAlive(false);
        int roundKilled = GameDAO.getCurrentRound(mutant.getGameId());
        mutant.setRoundKilled(roundKilled);
        mutant.setEquivalent(equivalence);

        @Language("SQL") String query;
        if (equivalence.equals(Equivalence.DECLARED_YES) || equivalence.equals(Equivalence.ASSUMED_YES)) {
            // if mutant is equivalent, we need to set score to 0
            query = """
                    UPDATE mutants
                    SET Equivalent = ?,
                        Alive = ?,
                        RoundKilled = ?,
                        Points = 0
                    WHERE Mutant_ID = ?
                      AND Alive = 1;
            """;
        } else {
            // We cannot update killed mutants
            query = """
                    UPDATE mutants
                    SET Equivalent = ?,
                        Alive = ?,
                        RoundKilled = ?
                    WHERE Mutant_ID = ?
                      AND Alive = 1;
            """;
        }

        try {
            return CDIUtil.getBeanFromCDI(QueryRunner.class).update(query,
                    equivalence.name(), false, roundKilled, mutant.getId()) > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.codedefenders.database.SQLMappingException;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Mutant.Equivalence;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.FileUtils;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static org.codedefenders.persistence.database.util.QueryUtils.batchParamsFromList;
import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

@ApplicationScoped
public class MutantRepository {
    private static final Logger logger = LoggerFactory.getLogger(MutantRepository.class);

    private final QueryRunner queryRunner;
    private final GameRepository gameRepo;

    @Inject
    public MutantRepository(QueryRunner queryRunner, GameRepository gameRepo) {
        this.queryRunner = queryRunner;
        this.gameRepo = gameRepo;
    }

    /**
     * Constructs a mutant from a {@link ResultSet} entry.
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
    public Mutant getMutantById(int mutantId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_mutants_with_user m
                WHERE m.Mutant_ID = ?;
        """;

        return queryRunner.query(query, oneFromRS(MutantRepository::mutantFromRS), mutantId).orElse(null);
    }

    /**
     * Returns the {@link Mutant} with the given md5 sum from the given game.
     */
    public Mutant getMutantByGameAndMd5(int gameId, String md5)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_mutants_with_user m
                WHERE m.Game_ID = ? AND m.MD5 = ?;
        """;

        return queryRunner.query(query, oneFromRS(MutantRepository::mutantFromRS), gameId, md5).orElse(null);
    }

    /**
     * Returns the {@link Mutant Mutants} from the given game for the given player.
     */
    public List<Mutant> getMutantsByGameAndPlayer(int gameId, int playerId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_valid_game_mutants m
                WHERE m.Game_ID = ?
                  AND m.Player_ID = ?;
        """;

        return queryRunner.query(query, listFromRS(MutantRepository::mutantFromRS), gameId, playerId);
    }

    public boolean gameHasPredefinedMutants(int gameId) {
        @Language("SQL") String query = """
                SELECT * FROM view_system_mutant_instances mutants
                WHERE mutants.Game_ID = ?
                LIMIT 1;
        """;

        return queryRunner.query(query, nextFromRS(rs -> true), gameId).orElse(false);
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the given game.
     *
     * <p>This includes valid user-submitted mutants as well as instances of predefined mutants in the game.
     */
    public List<Mutant> getValidMutantsForGame(int gameId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT *
                FROM view_valid_game_mutants m
                WHERE m.Game_ID = ?;
        """;

        return queryRunner.query(query, listFromRS(MutantRepository::mutantFromRS), gameId);
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the games played on the given class.
     *
     * <p>This includes valid user-submitted mutants as well as templates of predefined mutants
     * (not the instances that are copied into games).
     */
    public List<Mutant> getValidMutantsForClass(int classId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                WITH mutants_for_class AS
                   (SELECT * FROM view_valid_user_mutants UNION ALL SELECT * FROM view_system_mutant_templates)

                SELECT mutants.*
                FROM mutants_for_class mutants
                WHERE mutants.Class_ID = ?
        """;

        return queryRunner.query(query, listFromRS(MutantRepository::mutantFromRS), classId);
    }

    /**
     * Returns the compilable {@link Mutant Mutants} from the games played in the given classroom.
     *
     * <p>This includes valid user-submitted mutants from classroom games as well as templates of predefined mutants
     * for classes used in the classroom.
     *
     * @return The mutants, partitioned by Class ID.
     */
    public Multimap<Integer, Mutant> getValidMutantsForClassroom(int classroomId)
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

        var mutants = queryRunner.query(query,
                listFromRS(MutantRepository::mutantFromRS),
                classroomId,
                classroomId);

        Multimap<Integer, Mutant> mutantsMap = ArrayListMultimap.create();
        for (Mutant mutant : mutants) {
            mutantsMap.put(mutant.getClassId(), mutant);
        }
        return mutantsMap;
    }

    /**
     * Returns the valid {@link Mutant Mutants} submitted by the given player.
     */
    public List<Mutant> getValidMutantsForPlayer(int playerId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT *
                FROM view_valid_game_mutants m
                WHERE Player_ID = ?;
        """;

        return queryRunner.query(query, listFromRS(MutantRepository::mutantFromRS), playerId);
    }

    /**
     * Stores a given {@link Mutant} in the database.
     *
     * @param mutant The mutant to store.
     * @return The generated mutant ID.
     */
    public int storeMutant(Mutant mutant) {
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

        return queryRunner.insert(query,
                generatedKeyFromRS(),
                relativeJavaFile,
                relativeClassFile,
                gameId,
                roundCreated,
                equivalent.name(),
                alive,
                playerId,
                score,
                md5,
                classId,
                mutatedLinesString
        ).orElseThrow(() -> new UncheckedSQLException("Couldn't store mutant."));
    }

    /**
     * Updates a given {@link Mutant} in the database.
     *
     * @param mutant The mutant to update.
     */
    public void updateMutant(Mutant mutant) throws UncheckedSQLException {
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

        int updatedRows = queryRunner.update(query,
                equivalent.name(),
                alive,
                roundKilled,
                score,
                mutantId
        );
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update test.");
        }
    }

    /**
     * Update the score for a given {@link Mutant} in the database.
     *
     * @param mutant The mutant to update.
     */
    public void updateMutantScore(Mutant mutant) throws UncheckedSQLException {
        int mutantId = mutant.getId();
        int score = mutant.getScore();

        @Language("SQL") String query = """
                UPDATE mutants
                SET Points = ?
                WHERE Mutant_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                score,
                mutantId
        );
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update mutant.");
        }
    }

    /**
     * Update the kill message for a given {@link Mutant} in the database.
     *
     * @param mutant The mutant to update.
     */
    public void updateMutantKillMessageForMutant(Mutant mutant) throws UncheckedSQLException {
        int mutantId = mutant.getId();
        String killMessage = mutant.getKillMessage();

        @Language("SQL") String query = """
                UPDATE mutants
                SET KillMessage = ?
                WHERE Mutant_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                killMessage,
                mutantId
        );
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update mutant.");
        }
    }

    /**
     * Stores a mapping between a {@link Mutant} and a {@link GameClass} in the database.
     *
     * @param mutantId the identifier of the mutant.
     * @param classId  the identifier of the class.
     */
    public void mapMutantToClass(int mutantId, int classId) {
        @Language("SQL") String query = """
                INSERT INTO mutant_uploaded_with_class (Mutant_ID, Class_ID)
                VALUES (?, ?);
        """;

        queryRunner.insert(query, rs -> null,
                mutantId,
                classId
        );
    }


    /**
     * Removes a mutant for a given ID.
     */
    public void removeMutantForId(int mutantId) {
        @Language("SQL") String query1 = "DELETE FROM mutants WHERE Mutant_ID = ?;";
        @Language("SQL") String query2 = "DELETE FROM mutant_uploaded_with_class WHERE Mutant_ID = ?;";

        queryRunner.update(query1, mutantId);
        queryRunner.update(query2, mutantId);
    }

    /**
     * Removes multiple mutants for a given list of identifiers.
     *
     * @param mutants the identifiers of the mutants to be removed.
     */
    public void removeMutantsForIds(List<Integer> mutants) {
        if (mutants.isEmpty()) {
            return;
        }

        @Language("SQL") String query1 = "DELETE FROM mutant_uploaded_with_class WHERE Mutant_ID = ?;";
        @Language("SQL") String query2 = "DELETE FROM mutants WHERE Mutant_ID = ?;";

        var params = batchParamsFromList(mutants);

        queryRunner.batch(query1, params);
        queryRunner.batch(query2, params);
    }

    public int getEquivalentDefenderId(Mutant m) {
        @Language("SQL") String query = "SELECT * FROM equivalences WHERE Mutant_ID = ?;";

        var defenderId = queryRunner.query(query,
                        oneFromRS(rs -> rs.getInt("Defender_ID")),
                        m.getId());
        return defenderId.orElse(-1);
    }

    public void insertEquivalence(Mutant mutant, int defender) {
        @Language("SQL") String query = """
                INSERT INTO equivalences (Mutant_ID, Defender_ID, Mutant_Points)
                VALUES (?, ?, ?)
        """;

        queryRunner.insert(query, rs -> null,
                mutant.getId(),
                defender,
                mutant.getScore()
        );
    }

    public void incrementMutantScore(Mutant mutant, int score) {
        if (score == 0) {
            logger.debug("Do not update mutant {} score by 0", mutant.getId());
            return;
        }

        // Do we really need to check Alive = 1 here?
        @Language("SQL") String query = """
                UPDATE mutants
                SET Points = Points + ?
                WHERE Mutant_ID = ? AND Alive = 1;
        """;

        queryRunner.update(query,
                score, mutant.getId());
    }

    public boolean killMutant(Mutant mutant, Equivalence equivalence) {
        mutant.setAlive(false);
        int roundKilled = gameRepo.getCurrentRound(mutant.getGameId());
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

        return queryRunner.update(query,
                equivalence.name(), false, roundKilled, mutant.getId()) > 0;
    }
}

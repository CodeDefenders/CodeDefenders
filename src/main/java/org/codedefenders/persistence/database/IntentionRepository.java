/*
 * Copyright (C) 2016-2019,2022 Code Defenders contributors
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

import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.AttackerIntention;
import org.codedefenders.model.DefenderIntention;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;

import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;

/**
 * This class handles the database logic for player intentions.
 *
 * @see AttackerIntention
 * @see DefenderIntention
 */
@ApplicationScoped
public class IntentionRepository {

    private final QueryRunner queryRunner;

    @Inject
    public IntentionRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Stores a given {@link DefenderIntention} in the database.
     *
     * <p>For context information, the associated {@link Test} of the intention
     * is provided as well.
     *
     * @param test      the given test as a {@link Test}.
     * @param intention the given intention as an {@link DefenderIntention}.
     * @return the generated identifier of the intention as an {@code int}.
     * @throws UncheckedSQLException If an SQLException occurs while trying to store the intention.
     */
    public Optional<Integer> storeIntentionForTest(Test test, DefenderIntention intention) {
        int testId = test.getId();
        int gameId = test.getGameId();

        final String targetLines = intention.getLines().stream().map(String::valueOf).collect(Collectors.joining(","));

        @Language("SQL") final String query = """
            INSERT INTO intention (Test_ID, Game_ID, Target_Lines)
            VALUES (?, ?, ?);
        """;

        try {
            return queryRunner.insert(query,
                    resultSet -> nextFromRS(resultSet, rs -> rs.getInt(1)),
                    testId, gameId, targetLines);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    /**
     * Stores a given {@link AttackerIntention} in the database.
     *
     * <p>For context information, the associated {@link Mutant} of the intention
     * is provided as well.
     *
     * @param mutant    the given mutant as a {@link Mutant}.
     * @param intention the given intention as an {@link AttackerIntention}.
     * @return the generated identifier of the intention as an {@code int}.
     * @throws UncheckedSQLException If an SQLException occurs while trying to store the intention.
     */
    public Optional<Integer> storeIntentionForMutant(Mutant mutant, AttackerIntention intention) {
        @Language("SQL") final String query = """
            INSERT INTO intention (Mutant_ID, Game_ID, Target_Mutant_Type)
            VALUES (?, ?, ?);
        """;

        try {
            return queryRunner.insert(query,
                    resultSet -> nextFromRS(resultSet, rs -> rs.getInt(1)),
                    mutant.getId(), mutant.getGameId(), intention.toString());
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }
}

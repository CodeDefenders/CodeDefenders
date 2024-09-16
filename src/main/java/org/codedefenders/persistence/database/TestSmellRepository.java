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

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.Test;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import testsmell.AbstractSmell;
import testsmell.TestFile;

import static org.codedefenders.persistence.database.util.QueryUtils.extractBatchParams;
import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;

@ApplicationScoped
public class TestSmellRepository {
    private static final Logger logger = LoggerFactory.getLogger(TestSmellRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public TestSmellRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Stores all test smells of a test to the database.
     *
     * @param test the given test as a {@link Test}.
     * @param testFile a container for all test smells.
     * @throws UncheckedSQLException If storing test smells was not successful.
     */
    // TODO: make this independent of TestFile, probably best in a service
    public void storeSmell(final Test test, final TestFile testFile) throws UncheckedSQLException {
        @Language("SQL") String query = """
                INSERT INTO test_smell (Test_ID, smell_name)
                VALUES (?, ?);
        """;

        final List<AbstractSmell> testSmells = testFile.getTestSmells()
                .stream()
                .filter(AbstractSmell::hasSmell)
                .collect(Collectors.toList());

        var params = extractBatchParams(testSmells,
                smell -> test.getId(),
                AbstractSmell::getSmellName);

        queryRunner.insertBatch(query, generatedKeyFromRS(), params);
    }

    /**
     * Retrieves and returns a {@link List} of test smells for a given test.
     *
     * @param testId the id of the test the smells are retrieved for.
     * @return A list of all tests smells for a given test.
     */
    public List<String> getDetectedTestSmellsForTest(int testId) {
        @Language("SQL") String query = """
            SELECT smell_name
            FROM test_smell
            WHERE Test_ID = ?;
        """;

        return queryRunner.query(query, listFromRS(rs -> rs.getString("smell_name")), testId);
    }
}

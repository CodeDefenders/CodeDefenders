/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.execution;

import java.sql.Timestamp;

import org.codedefenders.database.TargetExecutionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetExecution {
    private static final Logger logger = LoggerFactory.getLogger(TargetExecution.class);

    public int id;
    public int testId;
    public int mutantId;
    public Target target;
    public Status status;
    public String message;
    public Timestamp timestamp;

    public enum Target {
        COMPILE_MUTANT,
        COMPILE_TEST,
        TEST_ORIGINAL,
        TEST_MUTANT,
        TEST_EQUIVALENCE
    }

    public enum Status {
        SUCCESS,
        FAIL,
        ERROR
    }

    // Constructors for initial creation of TargetExecution
    public TargetExecution(int testId, int mutantId, Target target, Status status, String message) {
        this.testId = testId;
        this.mutantId = mutantId;
        this.target = target;
        this.status = status;
        this.message = message;
    }

    public TargetExecution(int id, int testId, int mutantId, Target target, Status status,
                           String message, Timestamp timestamp) {
        this(testId, mutantId, target, status, message);
        this.id = id;
        this.timestamp = timestamp;
    }

    public boolean insert() {
        try {
            this.id = TargetExecutionDAO.storeTargetExecution(this);
            return true;
        } catch (Exception e) {
            logger.error("Failed to store target execution to database.", e);
            return false;
        }
    }

    public boolean hasTest() {
        return this.testId != 0;
    }

    public boolean hasMutant() {
        return this.mutantId != 0;
    }

    @Override
    public String toString() {
        return "TargetExecution{"
                + "id=" + id
                + ", testId=" + testId
                + ", mutantId=" + mutantId
                + ", target='" + target + '\''
                + ", status='" + status + '\''
                + ", message='" + message + '\''
                + ", timestamp=" + timestamp
                + '}';
    }

}

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
package org.codedefenders.analysis.coverage.line;

import org.jacoco.core.analysis.ICounter;

public enum LineCoverageStatus {
    // order matters
    EMPTY,
    NOT_COVERED,
    PARTLY_COVERED,
    FULLY_COVERED;

    public static LineCoverageStatus fromJacocoStatus(int status) {
        switch (status) {
            case ICounter.EMPTY:
                return EMPTY;
            case ICounter.NOT_COVERED:
                return NOT_COVERED;
            case ICounter.PARTLY_COVERED:
                return PARTLY_COVERED;
            case ICounter.FULLY_COVERED:
                return FULLY_COVERED;
            default:
                throw new IllegalArgumentException("Not a valid JaCoCo coverage status: " + status);
        }
    }

    public LineCoverageStatus upgrade(LineCoverageStatus other) {
        return this.ordinal() > other.ordinal() ? this : other;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean isNotCovered() {
        return this == NOT_COVERED;
    }

    public boolean isCovered() {
        return this == FULLY_COVERED || this == PARTLY_COVERED;
    }

    public boolean isPartlyCovered() {
        return this == PARTLY_COVERED;
    }

    public boolean isFullyCovered() {
        return this == FULLY_COVERED;
    }
}

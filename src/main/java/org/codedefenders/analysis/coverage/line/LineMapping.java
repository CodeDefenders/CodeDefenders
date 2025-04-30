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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a mapping from line numbers to any values, with non-assigned lines being mapped to an empty value
 * given by {@link LineMapping#getEmpty()}.
 */
public abstract class LineMapping<T> {
    /**
     * A list containing the values, indexed by line number (1-indexed).
     */
    private final List<T> lines;

    /**
     * The first line of the mapping (1-indexed, inclusive).
     */
    private int firstLine;

    /**
     * The last line of the mapping (1-indexed, inclusive).
     */
    private int lastLine;

    public LineMapping() {
        lines = new ArrayList<>();
        this.firstLine = Integer.MAX_VALUE;
        this.lastLine = Integer.MIN_VALUE;
    }

    public int getFirstLine() {
        return firstLine;
    }

    public int getLastLine() {
        return lastLine;
    }

    /**
     * Gets the associated value of the line number, or an empty value if the line does not fall into the range of
     * (firstLine, lastLine).
     */
    protected T get(int line) {
        if (line >= firstLine && line <= lastLine) {
            return lines.get(line);
        } else {
            return getEmpty();
        }
    }

    /**
     * Sets the associated value for a line number.
     */
    protected void set(int line, T elem) {
        updateBounds(line);
        lines.set(line, elem);
    }

    /**
     * Updates firstLine and lastLine to fit the given line number into the range,
     * and fills any new slots with the empty value.
     */
    protected void updateBounds(int line) {
        if (line < firstLine) {
            firstLine = line;
        }
        if (line > lastLine) {
            lastLine = line;
            while (lines.size() < line + 1) {
                lines.add(getEmpty());
            }
        }
    }

    /**
     * Returns the empty element of the mapping.
     */
    protected abstract T getEmpty();
}

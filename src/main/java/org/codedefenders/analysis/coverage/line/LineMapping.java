/*
 * Copyright (C) 2016-2023 Code Defenders contributors
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

public class LineMapping<T> {
    private final List<T> lines;
    private int firstLine; // inclusive
    private int lastLine; // inclusive

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

    protected T get(int line) {
        if (line >= firstLine && line <= lastLine) {
            return lines.get(line);
        } else {
            return getEmpty();
        }
    }

    protected void set(int line, T elem) {
        updateBounds(line);
        lines.set(line, elem);
    }

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

    protected T getEmpty() {
        return null;
    }
}

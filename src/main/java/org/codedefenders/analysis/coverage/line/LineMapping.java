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

    public T getEmpty() {
        return null;
    }
}

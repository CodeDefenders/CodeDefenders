package org.codedefenders.analysis.coverage.line;

import java.util.ArrayList;
import java.util.List;

public abstract class LineMapping<T> {
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

    public T get(int line) {
        if (line >= firstLine || line <= lastLine) {
            return lines.get(line - 1);
        } else {
            return getEmpty();
        }
    }

    public void set(int line, T elem) {
        if (line < firstLine) {
            firstLine = line;
        }
        if (line > lastLine) {
            lastLine = line;
            while (lines.size() < line) {
                lines.add(getEmpty());
            }
        }
        lines.set(line - 1, elem);
    }

    public abstract T getEmpty();
}

package org.codedefenders.analysis.gameclass;

public class MethodDescription {
    private String description;
    private int startLine;
    private int endLine;

    public MethodDescription(String description, int startLine, int endLine) {
        this.description = description;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getDescription() {
        return description;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }
}

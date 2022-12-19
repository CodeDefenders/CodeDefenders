package org.codedefenders.analysis.coverage.line;

import org.codedefenders.analysis.coverage.line.LineTokens.Token;

public class LineTokenAnalyser {
    public NewLineCoverage analyse(LineTokens lineTokens) {
        SimpleLineCoverage coverage = new SimpleLineCoverage();
        for (int line = lineTokens.getFirstLine(); line <= lineTokens.getLastLine(); line++) {
            Token root = lineTokens.getRoot(line);
            coverage.set(line, analyse(root));
        }
        return coverage;
    }

    public LineCoverageStatus analyse(Token token) {
        return analyse(token, LineCoverageStatus.EMPTY);
    }

    private LineCoverageStatus analyse(Token token, LineCoverageStatus currentStatus) {
        switch (token.type) {
            case ROOT:
            case EMPTY:
                break;
            case OVERRIDE:
                currentStatus = token.status;
                break;
            case COVERABLE:
                if (token.status != LineCoverageStatus.EMPTY) {
                    currentStatus = token.status;
                }
                break;
            case RESET:
                currentStatus = LineCoverageStatus.EMPTY;
                break;
        }

        token.analyserStatus = currentStatus;

        if (token.type == LineTokens.Type.OVERRIDE) {
            return currentStatus;
        }

        if (token.children.isEmpty()) {
            return currentStatus;
        }

        LineCoverageStatus childrenStatus = LineCoverageStatus.EMPTY;
        for (Token child : token.children) {
            childrenStatus = mergeForChildren(childrenStatus, analyse(child, currentStatus));
        }
        return childrenStatus;
    }

    private LineCoverageStatus mergeForChildren(LineCoverageStatus acc, LineCoverageStatus next) {
        switch (next) {
            case EMPTY:
                return acc;
            case NOT_COVERED:
                if (acc == LineCoverageStatus.EMPTY) {
                    return LineCoverageStatus.NOT_COVERED;
                } else {
                    // TODO: PARTLY_COVERED?
                    return acc;
                }
            case PARTLY_COVERED:
                if (acc == LineCoverageStatus.FULLY_COVERED) {
                    return LineCoverageStatus.FULLY_COVERED;
                } else {
                    return LineCoverageStatus.PARTLY_COVERED;
                }
            case FULLY_COVERED:
                return LineCoverageStatus.FULLY_COVERED;
            default:
                throw new IllegalArgumentException("Unknown line coverage status: " + next);
        }
    }

    // TODO: are the merge functions correct?
}

package org.codedefenders.analysis.coverage.line;


import java.util.Map;

import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;
import org.codedefenders.analysis.coverage.line.LineTokens.Token;

public class LineTokenAnalyser {
    public NewLineCoverage analyse(LineTokens lineTokens) {
        SimpleLineCoverage coverage = new SimpleLineCoverage();
        for (Map.Entry<Integer, Token> entry : lineTokens.getResults().entrySet()) {
            coverage.set(entry.getKey(), analyse(entry.getValue()));
        }
        return coverage;
    }

    public LineCoverageStatus analyse(Token token) {
        return analyse(token, LineCoverageStatus.EMPTY);
    }

    private LineCoverageStatus analyse(Token token, LineCoverageStatus currentStatus) {
        switch (token.type) {
            case ROOT:
                break;
            case OVERRIDE:
                return token.status;
            case EMPTY:
                break;
            case COVERABLE:
                currentStatus = currentStatus.merge(token.status);
                break;
            case RESET:
                currentStatus = LineCoverageStatus.EMPTY;
                break;
            case BLOCK:
                if (currentStatus == LineCoverageStatus.EMPTY) {
                    currentStatus = token.status;
                }
                break;
        }

        if (token.children.isEmpty()) {
            return currentStatus;
        }

        LineCoverageStatus childrenStatus = LineCoverageStatus.EMPTY;
        for (Token child : token.children) {
            childrenStatus = childrenStatus.merge(analyse(child, currentStatus));
        }
        return childrenStatus;
    }

    // TODO: are the merge functions correct?
    // TODO: always take the child value?
}

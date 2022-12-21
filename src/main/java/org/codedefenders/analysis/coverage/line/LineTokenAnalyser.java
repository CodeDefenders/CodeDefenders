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
        State state = analyse(token, State.defaultState());
        return state.status;
    }

    private State analyse(Token token, State state) {
        state = state.updateForNextToken(token);

        if (token.type == LineTokens.Type.OVERRIDE) {
            return state;
        }

        if (token.children.isEmpty()) {
            return state;
        }

        State acc = state;
        for (Token child : token.children) {
            State next = analyse(child, state);
            if (next.iteration > state.iteration) {
                if (acc.iteration > state.iteration) {
                    acc = acc.updateForSibling(next);
                } else {
                    acc = next;
                }
            }
        }
        return acc;
    }

    private static class State {
        private final LineCoverageStatus status;
        private final int priority;
        private final int iteration;

        public State(LineCoverageStatus status, int priority, int iteration) {
            this.status = status;
            this.priority = priority;
            this.iteration = iteration;
        }

        public State updateForNextToken(Token token) {
            switch (token.type) {
                case ROOT:
                case EMPTY:
                    break;
                case OVERRIDE:
                case COVERABLE:
                case BLOCK:
                    if (priority <= token.priority && token.status != LineCoverageStatus.EMPTY) {
                        return new State(token.status, token.priority, iteration + 1);
                    }
                    break;
                case STRONG_COVERABLE:
                    if (priority < token.priority && token.status != LineCoverageStatus.EMPTY) {
                        return new State(token.status, token.priority, iteration + 1);
                    }
                    if (priority == token.priority && token.status != LineCoverageStatus.EMPTY) {
                        return new State(status.upgrade(token.status), token.priority, iteration + 1);
                    }
                    break;
                case RESET:
                    if (priority <= token.priority) {
                        return new State(token.status, LineTokens.Priority.EMPTY, iteration + 1);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown token type: " + token.type);
            }
            return this;
        }

        public State updateForSibling(State next) {
            LineCoverageStatus status;
            int priority;

            if (next.priority > this.priority) {
                status = next.status;
                priority = next.priority;
            } else if (next.priority == this.priority) {
                status = this.status.upgrade(next.status);
                priority = this.priority;
            } else {
                status = this.status;
                priority = this.priority;
            }

            return new State(status, priority, Math.max(this.iteration, next.iteration));
        }

        public static State defaultState() {
            return new State(LineCoverageStatus.EMPTY, 0, 0);
        }
    }
}

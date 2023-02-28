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

import org.codedefenders.analysis.coverage.line.CoverageTokens.Token;

public class CoverageTokenAnalyser {
    public SimpleLineCoverage analyse(CoverageTokens coverageTokens) {
        SimpleLineCoverage coverage = new SimpleLineCoverage();
        for (int line = coverageTokens.getFirstLine(); line <= coverageTokens.getLastLine(); line++) {
            Token root = coverageTokens.getRoot(line);
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

        if (token.type == CoverageTokens.Type.OVERRIDE) {
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
                        return new State(token.status, CoverageTokens.Priority.EMPTY, iteration + 1);
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

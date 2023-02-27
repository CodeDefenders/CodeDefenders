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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus.Status;

import com.github.javaparser.ast.Node;

import static org.codedefenders.util.JavaParserUtils.beginOf;
import static org.codedefenders.util.JavaParserUtils.endOf;


public class LineTokens extends LineMapping<Deque<LineTokens.Token>> {
    @Override
    public Deque<Token> getEmpty() {
        Deque<Token> stack = new ArrayDeque<>();
        stack.push(Token.root());
        return stack;
    }

    @Override
    protected void set(int line, Deque<Token> stack) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Deque<Token> get(int line) {
        super.updateBounds(line);
        return super.get(line);
    }

    public Token getRoot(int line) {
        return get(line).peekLast();
    }

    public static LineTokens fromJaCoCo(DetailedLineCoverage coverage) {
        LineTokens lineTokens = new LineTokens();
        for (int line = coverage.getFirstLine(); line <= coverage.getLastLine(); line++) {
            LineCoverageStatus combinedStatus = coverage.get(line).combinedStatus();
            if (combinedStatus != LineCoverageStatus.EMPTY) {
                lineTokens.pushToken(line, Token.override(combinedStatus));
            } else {
                lineTokens.pushToken(line, Token.empty(null));
            }
            /*
            if (status.branchStatus() != LineCoverageStatus.EMPTY) {
                lineTokens.pushToken(line, Token.override(status.combinedStatus()));
            } else if (status.instructionStatus() == LineCoverageStatus.PARTLY_COVERED) {
                lineTokens.pushToken(line, Token.override(status.instructionStatus()));
            }
            */
            // lines where JaCoCo produces misleading coverage
            // - first line of record declaration: contains instructions for getter methods, and can therefore
            //          unexpectedly be PARTLY_COVERED or NOT_COVERED even if the record was initialized
            // - last line of try-catch-block (sometimes): I don't know what the line represents
            // - first line of empty finally-block (sometimes): I don't know what the line represents
            // - last lines of try-block with resources (branch coverage): I don't know what the line represents
        }
        return lineTokens;
    }

    private void pushToken(int line, Token newToken) {
        Deque<Token> stack = get(line);
        Token top = stack.peek();
        if (top != null) {
            top.children.add(newToken);
        }
        stack.push(newToken);
    }

    private void popToken(int line) {
        get(line).pop();
    }

    public TokenInserter forNode(Node originNode, Runnable visitCallback) {
        return new TokenInserter(originNode, visitCallback);
    }

    public class TokenInserter implements AutoCloseable {
        private final Node originNode;
        private final Runnable visitCallback;

        private final List<Integer> linesToPop;
        private final Set<Integer> emptyLines;

        private TokenInserter(Node originNode, Runnable visitCallback) {
            this.originNode = originNode;
            this.visitCallback = visitCallback;
            this.linesToPop = new ArrayList<>();
            this.emptyLines = new TreeSet<>();

            IntStream.rangeClosed(beginOf(originNode), endOf(originNode))
                    .forEach(emptyLines::add);
        }

        public TokenInserterForPosition lines(int beginLine, int endLine) {
            return new TokenInserterForPosition(beginLine, endLine);
        }

        public TokenInserterForPosition line(int line) {
            return lines(line, line);
        }

        public TokenInserterForPosition node(Node node) {
            return lines(beginOf(node), endOf(node));
        }

        @Override
        public void close() {
            // insert EMPTY token for every line that no token has been pushed onto
            for (int line : emptyLines) {
                pushToken(line, Token.empty(originNode));
                linesToPop.add(line);
            }
            emptyLines.clear();

            // visit child nodes
            visitCallback.run();

            // pop all pushed tokens from the stack
            for (int line : linesToPop) {
                popToken(line);
            }
            linesToPop.clear();
        }

        public class TokenInserterForPosition {
            private final int beginLine;
            private final int endLine;

            public TokenInserterForPosition(int beginLine, int endLine) {
                this.beginLine = beginLine;
                this.endLine = endLine;
            }

            private void insert(int start, int end, Supplier<Token> tokenSup) {
                for (int line = start; line <= end; line++) {
                    pushToken(line, tokenSup.get());
                    linesToPop.add(line);
                    emptyLines.remove(line);
                }
            }

            private void insert(Supplier<Token> tokenSup) {
                insert(beginLine, endLine, tokenSup);
            }

            public void cover(LineCoverageStatus status) {
                insert(() -> Token.cover(originNode, status));
            }

            public void cover(Status status) {
                cover(status.toLineCoverageStatus());
            }

            public void coverStrong(LineCoverageStatus status) {
                insert(beginLine, beginLine, () -> Token.coverStrong(originNode, status));
                insert(beginLine + 1, endLine, () -> Token.cover(originNode, status));
            }

            public void coverStrong(Status status) {
                coverStrong(status.toLineCoverageStatus());
            }

            public void block(LineCoverageStatus status) {
                insert(() -> Token.block(originNode, status));
            }

            public void block(Status status) {
                block(status.toLineCoverageStatus());
            }

            public void reset() {
                insert(() -> Token.reset(originNode));
            }

            public void empty() {
                insert(() -> Token.empty(originNode));
            }
        }
    }

    public static class Token {
        public final Node originNode;
        public final Type type;
        public final int priority;
        public final LineCoverageStatus status;

        public final List<Token> children;

        public static Token empty(Node originNode) {
            return new Token(originNode, Type.EMPTY, LineCoverageStatus.EMPTY, Priority.EMPTY);
        }

        public static Token block(Node originNode, LineCoverageStatus status) {
            return new Token(originNode, Type.BLOCK, status, Priority.BLOCK);
        }

        public static Token cover(Node originNode, LineCoverageStatus status) {
            return new Token(originNode, Type.COVERABLE, status, Priority.STMT);
        }

        public static Token coverStrong(Node originNode, LineCoverageStatus status) {
            return new Token(originNode, Type.STRONG_COVERABLE, status, Priority.STMT_STRONG);
        }

        public static Token reset(Node originNode) {
            return new Token(originNode, Type.RESET, LineCoverageStatus.EMPTY, Priority.STMT);
        }

        public static Token root() {
            return new Token(null, Type.ROOT, null, Priority.EMPTY);
        }

        public static Token override(LineCoverageStatus status) {
            return new Token(null, Type.OVERRIDE, status, Priority.OVERRIDE);
        }

        private Token(Node originNode, Type type, LineCoverageStatus status, int priority) {
            this.originNode = originNode;
            this.type = type;
            this.status = status;
            this.priority = priority;

            this.children = new ArrayList<>();
        }
    }

    public enum Type {
        /**
         * A dummy node to represent the tree root.
         */
        ROOT,
        /**
         * Denotes and a value from the JaCoCo coverage that should override our
         * computed coverage.
         *
         * <p>Can only occur as the root node. When found, the
         * following tokens are only used to compute block coverage for surrounding
         * lines.
         */
        OVERRIDE,
        EMPTY,
        /**
         * Denotes an AST node that is coverable.
         *
         * <p>Statements and some expressions fall under this category. Most often,
         * this token will determine the coverage of a line.
         */
        COVERABLE,
        STRONG_COVERABLE,
        BLOCK,

        /**
         * Denotes an AST node that "nullifies" the coverage of its parent nodes.
         *
         * <p>The coverage inside a class, method or lambda is (mostly)
         * independent of the surrounding statements.
         *
         * <p>E.g. consider:
         * <pre>{@code
         *     1:   whatever.addEventListener(new EventListener() {
         *     2:       @Override
         *     3:       public void handleEvent() {
         *     4:           // whatever
         *     5:       }
         *    6:   });
         * }</pre>
         * The coverage of lines 2-5 is not influenced by the coverage of the
         * surrounding call to {@code addEventListener}. Lines 1 and 6 are part of
         * the local class declaration but are influenced by the surrounding
         * coverage. In this case another Coverable token is inserted after the Reset.
         */
        RESET
    }

    public static class Priority {
        public final static int EMPTY = 0;
        public final static int BLOCK = 1;
        public final static int STMT = 2;
        public final static int STMT_STRONG = 3;
        public final static int OVERRIDE = 4;
    }
}


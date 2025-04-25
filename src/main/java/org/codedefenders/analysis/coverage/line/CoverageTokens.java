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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.codedefenders.analysis.coverage.ast.AstCoverageStatus.Status;

import com.github.javaparser.ast.Node;

import static org.codedefenders.util.JavaParserUtils.beginOf;
import static org.codedefenders.util.JavaParserUtils.endOf;

/**
 * Maps line numbers to trees of tokens used to compute line coverage.
 */
public class CoverageTokens extends LineMapping<Deque<CoverageTokens.Token>> {
    @Override
    protected Deque<Token> getEmpty() {
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

    /**
     * Gets the tree root for the given line.
     */
    public Token getRoot(int line) {
        return get(line).peekLast();
    }

    /**
     * Initializes a CoverageTokens instance by inserting and OVERRIDE token on every line that the given coverage
     * covers as non-EMPTY.
     */
    public static CoverageTokens fromExistingCoverage(DetailedLineCoverage coverage) {
        CoverageTokens coverageTokens = new CoverageTokens();
        for (int line = coverage.getFirstLine(); line <= coverage.getLastLine(); line++) {
            DetailedLine status = coverage.get(line);
            if (!status.combinedStatus().isEmpty()) {
                coverageTokens.pushToken(line, Token.override(status.combinedStatus()));
            } else {
                coverageTokens.pushToken(line, Token.empty(null));
            }

            // lines where JaCoCo produces misleading coverage
            // - first line of record declaration: contains instructions for getter methods, and can therefore
            //          unexpectedly be PARTLY_COVERED or NOT_COVERED even if the record was initialized
            // - last line of try-catch-block (sometimes): see TryCatchBlocks coverage test
            // - first line of empty finally-block (sometimes): see TryCatchBlocks coverage test
            // - last lines of try-block with resources (branch coverage): see TryCatchBlocks coverage test
        }
        return coverageTokens;
    }

    /**
     * Pushes a new token onto the stack on the given line,
     * and adds the token as a child to the token currently on top of the stack.
     */
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

    /**
     * A helper class to easily insert tokens on a range of lines.
     *
     * <p>Use in a try-block like e.g.:
     * <pre>{@code
     *     try (TokenInserter i = tokens.forNode(node, () -> super.visit(node, arg))) {
     *          // ...
     *          i.node(node.getExpr())
     *                  .cover(LineCoverageStatus.FULLY_COVERED).
     *          // ...
     *          i.lines(endOf(node.getExpr()) + 1, endOf(node))
     *                  .block(LineCoverageStatus.NOT_COVERED).
     *          // ...
     *     }
     * }</pre>
     *
     * <p>On closing, the TokenInserter will
     * <ol>
     *     <li>push EMPTY tokens onto lines that didn't have a token added</li>
     *     <li>call the given callback to visit the next node</li>
     *     <li>upon returning from the visit, pop the tokens that were previously added</li>
     * </ol>
     */
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

        /**
         * Starts an insert for the given lines.
         * If beginLine > endLine, no tokens will be added to any lines.
         *
         * @param beginLine The line to start the insertion at (1-indexed, inclusive).
         * @param endLine The line to end the insertion at (1-indexed, inclusive).
         */
        public TokenInserterForPosition lines(int beginLine, int endLine) {
            return new TokenInserterForPosition(beginLine, endLine);
        }

        /**
         * Starts an insert for all lines encompassing the given node.
         */
        public TokenInserterForPosition node(Node node) {
            return lines(beginOf(node), endOf(node));
        }

        @Override
        public void close() {
            // insert EMPTY token for every line that no token has have been pushed onto
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

            /**
             * Inserts a {@link CoverageTokens.Type#COVERABLE} token with the given status on each line.
             */
            public void cover(LineCoverageStatus status) {
                insert(() -> Token.cover(originNode, status));
            }

            /**
             * @see TokenInserterForPosition#cover(LineCoverageStatus)
             */
            public void cover(Status status) {
                cover(status.toLineCoverageStatus());
            }

            /**
             * Inserts a {@link CoverageTokens.Type#STRONG_COVERABLE} token with the given status on the first line,
             * and a {@link CoverageTokens.Type#COVERABLE} token on each other line.
             */
            public void coverStrong(LineCoverageStatus status) {
                insert(beginLine, beginLine, () -> Token.coverStrong(originNode, status));
                insert(beginLine + 1, endLine, () -> Token.cover(originNode, status));
            }

            /**
             * @see TokenInserterForPosition#coverStrong(LineCoverageStatus)
             */
            public void coverStrong(Status status) {
                coverStrong(status.toLineCoverageStatus());
            }

            /**
             * Inserts a {@link CoverageTokens.Type#BLOCK} token with the given status on each line.
             */
            public void block(LineCoverageStatus status) {
                insert(() -> Token.block(originNode, status));
            }

            /**
             * @see TokenInserterForPosition#block(LineCoverageStatus)
             */
            public void block(Status status) {
                block(status.toLineCoverageStatus());
            }

            /**
             * Inserts a {@link CoverageTokens.Type#RESET} token on each line.
             */
            public void reset() {
                insert(() -> Token.reset(originNode));
            }

            /**
             * Inserts a {@link CoverageTokens.Type#EMPTY} token on each line.
             */
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
            return new Token(originNode, Type.COVERABLE, status, Priority.BLOCK);
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
         * Represents a value from the JaCoCo coverage that should override the coverage computed from other tokens.
         */
        OVERRIDE,

        /**
         * Represents either an empty part of a coverable node, or a node that is not coverable.
         */
        EMPTY,

        /**
         * Represents a coverable AST node.
         *
         * <p>Statements and some expressions fall under this category. Most often,
         * this token will determine the coverage of a line.
         */
        COVERABLE,

        /**
         * Represents a coverable AST node that should be prioritized over others.
         *
         * <p>E.g. the first line of a COVERED method call should always be covered, even if a call parameter on the
         * same line is NOT_COVERED.
         */
        STRONG_COVERABLE,

        /**
         * Represents a code block. BLOCK has lower priority than COVERABLE or STRONG_COVERABLE.
         */
        BLOCK,

        /**
         * Represents an AST node that "nullifies" the coverage of its parent nodes.
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
         *     6:   });
         * }</pre>
         * The coverage of lines 2-5 is not influenced by the coverage of the
         * surrounding call to {@code addEventListener}. Lines 1 and 6 are part of
         * the local class declaration but are influenced by the surrounding
         * coverage. In this case another COVERABLE token is inserted after the RESET.
         */
        RESET
    }

    public static class Priority {
        public static final int EMPTY = 0;
        public static final int BLOCK = 1;
        public static final int STMT = 2;
        public static final int STMT_STRONG = 3;
        public static final int OVERRIDE = 4;
    }
}


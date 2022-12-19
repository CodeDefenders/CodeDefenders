package org.codedefenders.analysis.coverage.line;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;

import com.github.javaparser.ast.Node;

public class LineTokens extends LineMapping<Deque<LineTokens.Token>> {
    @Override
    public Deque<Token> getEmpty() {
        Deque<Token> stack = new ArrayDeque<>();
        stack.push(new Token(null, Type.ROOT, null));
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

    public static LineTokens fromJaCoCo(NewLineCoverage coverage) {
        LineTokens lineTokens = new LineTokens();
        for (int line = coverage.getFirstLine(); line <= coverage.getLastLine(); line++) {
            LineCoverageStatus status = coverage.getStatus(line);
            // TODO: push empty too and check in analyse method
            if (status == LineCoverageStatus.PARTLY_COVERED) {
                lineTokens.pushToken(line, new Token(null, Type.OVERRIDE, status));
            }
        }
        return lineTokens;
    }

    private void pushToken(int line, Token newToken) {
        Deque<Token> stack = get(line);
        Token top = stack.peek();
        top.children.add(newToken);
        stack.push(newToken);
    }

    private void popToken(int line) {
        get(line).pop();
    }

    public TokenInserter forNode(Node originNode) {
        return new TokenInserter(originNode);
    }

    public class TokenInserter implements AutoCloseable {
        private final Node originNode;
        private final Deque<Integer> linesToPop;

        public TokenInserter(Node originNode) {
            this.originNode = originNode;
            this.linesToPop = new ArrayDeque<>();
        }

        public TokenInserterForPosition lines(int beginLine, int endLine) {
            return new TokenInserterForPosition(beginLine, endLine);
        }

        public TokenInserterForPosition line(int line) {
            return lines(line, line);
        }

        public TokenInserterForPosition node(Node node) {
            return lines(node.getBegin().get().line, node.getEnd().get().line);
        }

        // TODO: last line needs to be spared too in some cases e.g. return switch() { ...
        public TokenInserterForPosition nodeExceptFirstLine(Node node) {
            return lines(node.getBegin().get().line + 1, node.getEnd().get().line);
        }

        @Override
        public void close() {
            linesToPop.forEach(LineTokens.this::popToken);
            linesToPop.clear();
        }

        public class TokenInserterForPosition {
            private final List<Integer> lines;

            public TokenInserterForPosition(int beginLine, int endLine) {
                lines = new ArrayList<>();
                for (int line = beginLine; line <= endLine; line++) {
                    lines.add(line);
                }
            }

            private void insert(Supplier<Token> tokenSup) {
                lines.forEach(line -> pushToken(line, tokenSup.get()));
                linesToPop.addAll(lines);
            }

            public void cover(LineCoverageStatus status) {
                insert(() -> new Token(originNode, Type.COVERABLE, status));
            }

            public void cover(AstCoverageStatus status) {
                cover(status.status());
            }

            public void reset() {
                insert(() -> new Token(originNode, Type.RESET, null));
            }

            public void empty() {
                insert(() -> new Token(originNode, Type.EMPTY, null));
            }
        }
    }

    public static class Token {
        public final Node originNode;
        public final Type type;
        public final LineCoverageStatus status;

        public final List<Token> children;

        // status the LineTokenAnalyser determined after reaching this token
        // for debugging purposes
        // TODO: find a better solution than saving it here
        public LineCoverageStatus analyserStatus;

        private Token(Node originNode, Type type, LineCoverageStatus status) {
            this.originNode = originNode;
            this.type = type;
            this.status = status;
            this.children = new ArrayList<>();
            this.analyserStatus = null;
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
        /**
         * Denotes an AST node that does not count as coverable, and does not
         * influence coverage in any other ways.
         *
         * <p>Most expressions aren't coverable code for JaCoCo by themselves,
         * so we don't count them either. The coverage of those lines will be
         * determined by the surrounding statement(s) that are coverable.
         */
        EMPTY,
        /**
         * Denotes an AST node that is coverable.
         *
         * <p>Statements and some expressions fall under this category. Most often,
         * this token will determine the coverage of a line.
         */
        COVERABLE,

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
}


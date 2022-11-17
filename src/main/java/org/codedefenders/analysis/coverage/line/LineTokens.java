package org.codedefenders.analysis.coverage.line;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;

import com.github.javaparser.ast.Node;

public class LineTokens {
    Map<Integer, Deque<Token>> stacks;

    public LineTokens() {
        stacks = new HashMap<>();
    }

    public static LineTokens fromJaCoCo(LineCoverageMapping coverage) {
        LineTokens lineTokens = new LineTokens();
        for (Map.Entry<Integer, LineCoverageStatus> entry : coverage.getMap().entrySet()) {
            if (entry.getValue() != LineCoverageStatus.EMPTY) {
                lineTokens.pushToken(lineTokens.getStack(entry.getKey()),
                        new Token(null, Type.OVERRIDE, entry.getValue()));
            }
        }
        return lineTokens;
    }

    public Map<Integer, Token> getResults() {
        return stacks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().peekLast()));
    }

    private Deque<Token> getStack(int line) {
        return stacks.computeIfAbsent(line, l -> {
            Deque<Token> stack = new ArrayDeque<>();
            stack.push(new Token(null, Type.ROOT, null));
            return stack;
        });
    }

    private void pushToken(Deque<Token> stack, Token newToken) {
        Token top = stack.peek();
        assert top != null;
        top.children.add(newToken);
        stack.push(newToken);
    }

    private void popToken(Deque<Token> stack) {
        stack.pop();
    }

    public TokenInserter forNode(Node originNode) {
        return new TokenInserter(originNode);
    }

    public class TokenInserter implements AutoCloseable {
        private final Node originNode;
        private final Deque<Deque<Token>> stacksToPop;

        public TokenInserter(Node originNode) {
            this.originNode = originNode;
            this.stacksToPop = new ArrayDeque<>();
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
            for (Deque<Token> stack : stacksToPop) {
                popToken(stack);
            }
        }

        public class TokenInserterForPosition {
            private final List<Deque<Token>> stacksForPos;

            public TokenInserterForPosition(int beginLine, int endLine) {
                stacksForPos = new ArrayList<>();
                for (int line = beginLine; line <= endLine; line++) {
                    stacksForPos.add(getStack(line));
                }
            }

            private void insert(Supplier<Token> tokenSup) {
                stacksForPos.forEach(stack -> pushToken(stack, tokenSup.get()));
                stacksToPop.addAll(stacksForPos);
            }

            public void cover(LineCoverageStatus status) {
                insert(() -> new Token(originNode, Type.COVERABLE, status));
            }

            public void cover(AstCoverageStatus status) {
                cover(status.toLineCoverage());
            }

            public void block(LineCoverageStatus status) {
                insert(() -> new Token(originNode, Type.BLOCK, status));
            }

            public void block(AstCoverageStatus status) {
                block(status.toLineCoverage());
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
        Node originNode;
        Type type;
        LineCoverageStatus status;

        List<Token> children;

        private Token(Node originNode, Type type, LineCoverageStatus status) {
            this.originNode = originNode;
            this.type = type;
            this.status = status;
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
}


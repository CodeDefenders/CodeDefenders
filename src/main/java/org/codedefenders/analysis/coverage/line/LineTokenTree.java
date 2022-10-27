package org.codedefenders.analysis.coverage.line;

import java.util.List;

import com.github.javaparser.ast.Node;

public class LineTokenTree {

    public abstract static class Token {
        List<Node> children;
    }

    /**
     * Denotes and a value from the JaCoCo coverage that should override our
     * computed coverage.
     *
     * <p>Can only occur as the root node. When found, the
     * following tokens are only used to compute block coverage for surrounding
     * lines.
     */
    public static class Override extends Token {
        LineCoverageStatus status;
    }

    /**
     * Denotes an AST node that does not count as coverable, and does not
     * influence coverage in any other ways.
     *
     * <p>Most expressions aren't coverable code for JaCoCo by themselves,
     * so we don't count them either. The coverage of those lines will be
     * determined by the surrounding statement(s) that are coverable.
     */
    public static class Empty extends Token {

    }

    /**
     * Denotes an AST node that is coverable.
     *
     * <p>Statements and some expressions fall under this category. Most often,
     * this token will determine the coverage of a line.
     */
    public static class Coverable extends Token {
        LineCoverageStatus status;
    }

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
     * coverage. In this case {@code ignore} is set.
     *
     * <p>On some lines, the reset nodes should be covered themselves. On those
     * lines status is set to non-empty.
     */
    public static class Reset extends Token {
        boolean ignore;
        LineCoverageStatus status;
    }

    /**
     * Denotes a code block.
     *
     * <p>On lines that don't contain any coverable tokens, code blocks may
     * extend coverage to this line.
     *
     * <p>E.g. consider:
     * <pre>{@code
     *      1:  if (whatever) {
     *      2:
     *      3:      someCall();
     *      4:
     *      5:      return;
     *      6:
     *      7:  }
     * }</pre>
     * Line 2 inherits the coverage from line 1, Line 4 from 3. Without the
     * return, line 4,5,6,7 would // TODO
     */
    public static class Block extends Token {
    }
    public static class Return extends Token {

    }
}


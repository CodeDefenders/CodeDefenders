import java.util.Arrays;
import java.util.function.Supplier;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import utils.MethodChain;
import utils.ThrowingAutoCloseable;

import static utils.Utils.doGet;
import static utils.Utils.doCall;
import static utils.Utils.doThrow;
import static utils.Utils.consume;
import static utils.Utils.callLambda;
import utils.Call;

public class Playground {
    private final static Integer INTEGER = null;

    @Call
    public void test6() {
        final
        int
                a
                =
                1,
                b = 2;

        int
                c
                =
                1,
                d = 2;

        new TrickyVariableDeclarators().get();
        consume(a + b + c + d);

        new Tricky2();
    }

    static class TrickyVariableDeclarators {
        final
        int
            a = 1,
            b = 2;


        int
                c = 1,
                d = 2;

        private
        Runnable s = () -> {};

        int i; // should be COVERED but is NOT_COVERED

        public int get() {
            return a + b + c + d + i;
        }
    }

    static  class Tricky2 {
        int i = 1;
        int j = 1,
            k = INTEGER;
        int x;
    }

    @Call
    public void test8() {
        Runnable s = () -> {};

        int i;

        ;
    }

    @Call
    public void test9() {
        Runnable s
                =
                () -> {};

        int i;

        ;
    }

    @Call
    public void test7() {
        Runnable s = () -> {};

        int i;

        ;
    }

    @Call
    public void notCoveredAtAllForSomeReason() {
        doCall();
        Object a = null;
        synchronized (a) {

        }
    }

    @Call(params = "1")
    public void test3(int i) {
        boolean x = i == 1
                ? doGet(this)
                instanceof
                Object
                : this
                instanceof // coverage on this line??
                Object;
    }

    class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // @Call(params = "(1; 2); (3; 44a); (5; 6)")
    // @Call(params = "(1; 2); (3; 44); (5; 6)")
    // @Call(params = "(1; 2); (3; 4); (5; 6)")
    @Call(params = "(1; ); (3; 4); (5; 6)")
    public List<Point> parsePoints(String in) {
        boolean foundParen = false;
        char[] chars = in.toCharArray();

        String currentMatch = null;
        List<String> currentPoint = new ArrayList<>();

        List<Point> result = new ArrayList<>();

        for (char c : chars) {
            if (!foundParen) {
                if (c == '(') {
                    foundParen = true;
                }
            } else {
                if (c == ')') {
                    currentPoint.add(currentMatch);
                    currentMatch = null;

                    result.add(new Point(
                            Integer.parseInt(currentPoint.get(0)),
                            Integer.parseInt(currentPoint.get(1))
                    ));
                    currentPoint.clear();
                    foundParen = false;
                } else if (c == ';') {
                    currentPoint.add(currentMatch);
                    currentMatch = null;
                } else if (c >= '0' && c <= '9') {
                    currentMatch = currentMatch == null
                            ? "" + c
                            : currentMatch + c;
                }
            }
        }

        return result;
    }



    // tests todo
        // binary expr
        // objectcreationexpr
        // arrays with nested initializers
        // method calls / constructor calls / super calls with exception in args
}

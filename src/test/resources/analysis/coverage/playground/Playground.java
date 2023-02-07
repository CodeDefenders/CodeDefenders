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
    public void test5() {
        boolean t = true;
        boolean f = false;

        boolean[] b = new boolean[] {
                t
                ||
                doGet(true),
                f
                &&
                doGet(false)
        };
    }

    public void param() {

    }

    @Call
    public void test() {
        callLambda(
                this
                ::
                param
        );
    }

    @Call
    public void test2() {
        boolean x
                = doGet(this)
                instanceof
                Object;
    }

    @Call
    public void test2a() {
        Object o = this;

        boolean x
                = o
                instanceof
                Integer i;

        boolean y
                = o
                instanceof
                Playground p;
    }

    @Call(params = "1")
    public void test3(int i) {
        boolean x = i == 1
                ? doGet(this)
                instanceof
                Object
                : this
                instanceof
                Object;
    }

    @Call
    public void test4() {
        int[] j
                =
                new
                        int[3];



    }

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
    public void test10() {
        int i = 1;

        i
        =
        2;

        i
        +=
        2;

        i
        =
        doGet(2);

        i
        +=
        doGet(2);
    }

    @Call
    public void test11() {
        int i = 1;

        consume(
        i
        =
        2
        );

        consume(
        i
        +=
        2
        );

        consume(
        i
        =
        doGet(2)
        );

        consume(
        i
        +=
        doGet(2)
        );
    }
    @Call
    public void test12() {
        int i = 1;

        i
        =
        doThrow();
    }

    @Call
    public void test12b() {
        int i = 1;

        i
        =
        doGet(2);
    }

    @Call
    public void test13() {
        int i = 1;

        consume(
        i
        =
        doThrow()
        );
    }


    @Call
    public void notCoveredAtAllForSomeReason() {
        doCall();
        Object a = null;
        synchronized (a) {

        }
    }

    // tests todo
        // binary expr
        // instanceof and pattern
        // assignments (and nested assignments)

    // TODO: does final modifier impact variabledeclarators?
}

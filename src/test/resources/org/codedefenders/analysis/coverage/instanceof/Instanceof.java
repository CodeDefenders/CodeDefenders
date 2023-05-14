import utils.Call;

import static utils.Utils.consume;
import static utils.Utils.doCall;
import static utils.Utils.doGet;
import static utils.Utils.doThrowObject;

/**
 * <p>Instanceof expressions
 * <p>JaCoCo coverage: Only covers the lhs expression if it's coverable, and the rhs type if it's a pattern expression.
 * <p>Extended coverage: Covers the entire expression.
 */
public class Instanceof {

    @Call
    public void regularInstanceof() {
        Object o = Integer.valueOf(1);

        boolean a = o instanceof Integer;
        boolean b = o instanceof Long;

        boolean c =
                o instanceof Integer;
        boolean d =
                o instanceof Long;

        consume(o instanceof Integer);
        consume(o instanceof Long);

        consume(
                o instanceof Integer);
        consume(
                o instanceof Long);
    }

    @Call
    public void coveredExpr() {
        Object o = Integer.valueOf(1);

        boolean a
                = doGet(o)
                instanceof
                Integer;

        boolean b
                = doGet(o)
                instanceof
                Long;

        consume(
                doGet(o)
                instanceof
                Integer);

        consume(
                doGet(o)
                instanceof
                Long);
    }

    @Call
    public void instanceofInIfs() {
        Object o = Integer.valueOf(1);

        if (o instanceof Integer) {
            o = ((Integer) o) + 1;
        }

        if (o instanceof Long) {
            o = ((Long) o) + 1;
        }

        if (
                o instanceof Integer
        ) {
            o = ((Integer) o) + 1;
        }

        if (
                o instanceof Long
        ) {
            o = ((Long) o) + 1;
        }

        if (
                o
                instanceof
                Integer
        ) {
            o = ((Integer) o) + 1;
        }

        if (
                o
                instanceof
                Long
        ) {
            o = ((Long) o) + 1;
        }
    }

    @Call
    public void coveredInstanceofInIfs() {
        Object o = Integer.valueOf(1);

        if (doGet(o) instanceof Integer) {
            o = ((Integer) o) + 1;
        }

        if (doGet(o) instanceof Long) {
            o = ((Long) o) + 1;
        }

        if (
                doGet(o) instanceof Integer
        ) {
            o = ((Integer) o) + 1;
        }

        if (
                doGet(o) instanceof Long
        ) {
            o = ((Long) o) + 1;
        }

        if (
                doGet(o)
                instanceof
                Integer
        ) {
            o = ((Integer) o) + 1;
        }

        if (
                doGet(o)
                instanceof
                Long
        ) {
            o = ((Long) o) + 1;
        }
    }

    @Call
    public void uselessPatternExpr() {
        Object o = Integer.valueOf(1);

        boolean a = o instanceof Integer i;
        boolean b = o instanceof Long l;

        boolean c =
                o instanceof Integer i;
        boolean d =
                o instanceof Long l;

        consume(o instanceof Integer i);
        consume(o instanceof Long l);

        consume(
                o instanceof Integer i);
        consume(
                o instanceof Long l);
    }

    @Call
    public void patternExprInIfs() {
        Object o = Integer.valueOf(1);

        if (o instanceof Integer i) {
            o = i + 1;
        }

        if (o instanceof Long l) {
            o = l + 1;
        }

        if (
                o instanceof Integer i
        ) {
            o = i + 1;
        }

        if (
                o instanceof Long l
        ) {
            o = l + 1;
        }

        if (
                o
                instanceof
                Integer
                i
        ) {
            o = i + 1;
        }

        if (
                o
                instanceof
                Long
                l
        ) {
            o = l + 1;
        }
    }

    @Call
    public void coveredPatternExprInIfs() {
        Object o = Integer.valueOf(1);

        if (doGet(o) instanceof Integer i) {
            o = i + 1;
        }

        if (doGet(o) instanceof Long l) {
            o = l + 1;
        }

        if (
                doGet(o) instanceof Integer i
        ) {
            o = i + 1;
        }

        if (
                doGet(o) instanceof Long l
        ) {
            o = l + 1;
        }

        if (
                doGet(o)
                instanceof
                Integer
                i
        ) {
            o = i + 1;
        }

        if (
                doGet(o)
                instanceof
                Long
                l
        ) {
            o = l + 1;
        }
    }

    @Call
    public void exception1() {
        boolean b =
                doThrowObject() instanceof Integer;
    }

    @Call
    public void exception2() {
        consume(
                doThrowObject() instanceof Integer);
    }

    @Call
    public void exception3() {
        if (doThrowObject() instanceof Integer) {
            doCall();
        }
    }

    @Call
    public void exception4() {
        if (
                doThrowObject()
                instanceof
                Integer
        ) {
            doCall();
        }
    }

    @Call
    public void exception5() {
        boolean b =
                doThrowObject() instanceof Integer i;
    }

    @Call
    public void exception6() {
        consume(
                doThrowObject() instanceof Integer i);
    }

    @Call
    public void exception7() {
        if (doThrowObject() instanceof Integer i) {
            doCall();
        }
    }

    @Call
    public void exception8() {
        if (
                doThrowObject()
                instanceof
                Integer
                i
        ) {
            doCall();
        }
    }
}

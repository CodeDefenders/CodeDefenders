import utils.Call;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;
import static utils.Utils.doThrowBoolean;

/**
 * <p>Binary Expressions
 * <p>JaCoCo coverage: Usually doesn't cover binary expressions themselves, only coverable sub-expressions.
 *                     However, if the binary expressions short-circuits and the rhs expression is coverable,
 *                     one line of the rhs expression is PARTLY_COVERED instead of NOT_COVERED.
 * <p>Extended coverage: Covers uncoverable expressions if possible, and covers the space between the lhs and rhs.
 */
public class BinaryExpressions {

    @Call
    public void regular() {
        int i =
                1
                +
                1;
    }

    @Call
    public void shortCircuit1() {
        boolean b =
                false
                &&
                true;

        // block: ignore_end_status
    }

    @Call
    public void shortCircuit1b() {
        consume(
                false
                &&
                true
        );

        // block: ignore_end_status
    }

    @Call
    public void shortCircuit2() {
        // not great
        boolean b =
                doGet(false)
                &&
                doGet(true);

        // block: ignore_end_status
    }

    @Call(params = "false, true")
    public void shortCircuit3(boolean False, boolean True) {
        // not great
        boolean b =
                False
                &&
                True;

        // block: ignore_end_status
    }

    @Call(params = "false, true")
    public void shortCircuit4(boolean False, boolean True) {
        // not great
        boolean b =
                False
                &&
                doGet(True);

        // block: ignore_end_status
    }

    @Call
    public void shortCircuit2OneLine() {
        boolean b = doGet(false) && doGet(true);

        // block: ignore_end_status
    }

    @Call
    public void shortCircuit2b() {
        // not great
        consume(
                doGet(false)
                &&
                doGet(true)
        );

        // block: ignore_end_status
    }

    @Call
    public void shortCircuit2bOneLine() {
        consume(doGet(false) && doGet(true));

        // block: ignore_end_status
    }

    @Call
    public void shortCircut3() {
        boolean d =
                doGet(false)
                &&
                MethodChain.create()
                        .get(true);

        // block: ignore_end_status
    }

    @Call
    public void shortCircut3b() {
        consume(

                doGet(false)

                &&

                MethodChain.create()

                        .get(true)
        );

        // block: ignore_end_status
    }

    @Call
    public void shortCircut4() {
        // worse
        boolean e =
                false
                &&
                MethodChain.create()
                        .get(true);

        // block: ignore_end_status
    }

    @Call
    public void shortCircut4b() {
        // worse
        consume(
                false
                &&
                MethodChain.create()
                        .get(true)
        );

        // block: ignore_end_status
    }

    @Call
    public void nestedShortCircuit() {
        boolean c =
                doGet(false)
                &&
                doGet(false)
                &&
                doGet(true);

        boolean d =
                doGet(false)
                &&
                MethodChain.create()
                        .get(false)
                &&
                MethodChain.create()
                        .get(true);
    }

    @Call
    public void exceptionInLeft1() {
        int i =
                doThrow()
                +
                1;
    }

    @Call
    public void exceptionInLeft2() {
        int i =
                doThrow()
                +
                doGet(1);
    }

    @Call
    public void exceptionInLeft3() {
        int i =
                MethodChain.create()
                        .doThrow()
                        .get(1)
                +
                1;
    }

    @Call
    public void exceptionInLeft4() {
        int i =
                MethodChain.create()
                        .doThrow()
                        .get(1)
                +
                doGet(1);
    }

    @Call
    public void exceptionInRight1() {
        int i =
                1
                +
                doThrow();
    }

    @Call
    public void exceptionInRight2() {
        int i =
                doGet(1)
                +
                doThrow();
    }

    @Call
    public void exceptionInRight3() {
        int i =
                1
                +
                MethodChain.create()
                        .doThrow()
                        .get(1);
    }

    @Call
    public void exceptionInRight4() {
        int i =
                doGet(1)
                +
                MethodChain.create()
                        .doThrow()
                        .get(1);
    }

    @Call
    public void exceptionInRight5() {
        int i =
                1
                +
                doThrow();
    }

    @Call
    public void nestedException1() {
        int i = doGet(1)
                +
                doGet(1)
                +
                doThrow();
    }

    @Call
    public void nestedException2() {
        int i = doGet(1)
                +
                doThrow()
                +
                doGet(1);
    }

    @Call
    public void nestedException3() {
        int i =
                1
                +
                1
                +
                doThrow();
    }
}

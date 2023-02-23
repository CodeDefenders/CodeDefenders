import utils.Call;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

/**
 * <p>Ternary operators
 * <p>JaCoCo coverage: Covers the condition (branch coverage) and the then- and else-expression if they are not already
 *                     covered.
 *                     <p> Also covers a line of the else-expression even if it should not be covered.
 *                     This seems to only be the case if the expr is part of an assignment.
 * <p>Extended coverage: Covers space before and after the condition and then- and else-expression as well.
 */
public class TernaryOperators {
    @Call
    public void literalsInAssignment() {
        int i =
                doGet(1) > 0

                        ? 1

                        : 0;

        int j = doGet(1) > 0 ? 1 : 0;
    }

    @Call
    public void literalsInAssignment2() {
        int i =
                doGet(1) < 0

                        ? 1

                        : 0;

        // block: ignore_end_status
    }

    @Call
    public void expressionsInAssignment() {
        int i =
                doGet(1) > 0
                        ? 1
                          + 1
                        : 0
                          + 1;
    }

    @Call
    public void methodCallsInAssignment() {
        int i =
                doGet(1) > 0
                        ? doGet(1)
                        : doGet(0);

        int m = doGet(1) > 0 ? doGet(1) : doGet(0);
    }

    @Call
    public void methodChainsInAssignment() {
        int i =
                doGet(1) > 0
                        ? MethodChain.create()
                                .call()
                                .get(1)
                        : MethodChain.create()
                                .call()
                                .get(0);
    }

    @Call
    public void literalsInMethodArg() {
        consume(doGet(1) > 0
                ? 1
                : 0);

        consume(doGet(1) > 0 ? 1 : 0);
    }

    @Call
    public void expressionsInMethodArg() {
        consume(doGet(1) > 0
                ? 1
                  + 1
                : 0
                  + 1);
    }

    @Call
    public void methodCallsInMethodArg() {
        consume(doGet(1) > 0
                        ? doGet(1)
                        : doGet(0));

        consume(doGet(1) > 0 ? doGet(1) : doGet(0));
    }

    @Call
    public void methodChainsInMethodArg() {
        consume(doGet(1) > 0
                ? MethodChain.create()
                        .call()
                        .get(1)
                : MethodChain.create()
                        .call()
                        .get(0));
    }

    @Call
    public void exceptionAtExpressions1() {
        int i =
                doGet(1) > 0
                        ? doThrow()
                        : doThrow();

        // block: ignore_end_status
    }

    @Call
    public void exceptionAtExpressions2() {
        int j = doGet(1) > 0 ? doThrow() : doThrow();
    }

    @Call
    public void exceptionAtCondition1() {
        int i =
                doThrow() > 0
                        ? doGet(1)
                        : doGet(0);
    }

    @Call
    public void exceptionAtCondition2() {
        int j = doThrow() > 0 ? doGet(1) : doGet(0);
    }

    @Call
    public void exceptionAtCondition3() {
        int i =
                doThrow() > 0
                        ? 1
                        : 0;
    }

    @Call
    public void exceptionAtCondition4() {
        int i =

                MethodChain.create()
                        .doThrow()
                        .get(0) > 0

                        ? 1

                        : 0;

        // block: ignore_end_status
    }

    @Call
    public void literalsWithEmptyCondition() {
        int i = true
                ? 1
                : 0;

        int j = false
                ? 1
                : 0;
    }

    @Call
    public void methodCallsWithEmptyCondition() {
        int i = true
                ? doGet(1)
                : doGet(0);

        int j = false
                ? doGet(1)
                : doGet(0);

        // block: ignore_end_status
    }

    @Call(params = "1")
    public void strangeCoverage1(int i) {
        boolean x = i == 1
                ? doGet(this)
                instanceof
                Object
                : this
                instanceof // partly_covered on this line?
                Object;
    }

}

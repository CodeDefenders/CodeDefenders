import utils.Call;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

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
    public void literalsInParameter() {
        consume(doGet(1) > 0
                ? 1
                : 0);

        consume(doGet(1) > 0 ? 1 : 0);
    }

    @Call
    public void expressionsInParameter() {
        consume(doGet(1) > 0
                ? 1
                  + 1
                : 0
                  + 1);
    }

    @Call
    public void methodCallsInParameter() {
        consume(doGet(1) > 0
                        ? doGet(1)
                        : doGet(0));

        consume(doGet(1) > 0 ? doGet(1) : doGet(0));
    }

    @Call
    public void methodChainsInParameter() {
        consume(doGet(1) > 0
                ? MethodChain.create()
                        .call()
                        .get(1)
                : MethodChain.create()
                        .call()
                        .get(0));
    }

    @Call
    public void exceptionAtExpressionsInAssignment1() {
        int i =
                doGet(1) > 0
                        ? doThrow()
                        : doThrow();
    }

    @Call
    public void exceptionAtExpressionsInAssignment2() {
        int j = doGet(1) > 0 ? doThrow() : doThrow();
    }

    @Call
    public void exceptionAtConditionInAssignment1() {
        int i =
                doThrow() > 0
                        ? doGet(1)
                        : doGet(0);
    }

    @Call
    public void exceptionAtConditionInAssignment2() {
        int j = doThrow() > 0 ? doGet(1) : doGet(0);
    }

    @Call
    public void exceptionAtConditionInAssignment3() {
        int i =
                doThrow() > 0
                        ? 1
                        : 0;
    }

    @Call
    public void exceptionAtConditionInAssignment4() {
        int i =
                MethodChain.create()
                        .doThrow()
                        .get(0) > 0
                        ? 1
                        : 0;
    }

    @Call
    public void exceptionAtExpressionsInParameter1() {
        consume(doGet(1) > 0
                ? doThrow()
                : doThrow());
    }

    @Call
    public void exceptionAtExpressionsInParameter2() {
        consume(doGet(1) > 0 ? doThrow() : doThrow());
    }

    @Call
    public void exceptionAtConditionInParameter1() {
        consume(doThrow() > 0
                ? doGet(1)
                : doGet(0));
    }

    @Call
    public void exceptionAtConditionInParameter2() {
        consume(doThrow() > 0 ? doGet(1) : doGet(0));
    }

    @Call
    public void exceptionAtConditionInParameter3() {
        consume(doThrow() > 0
                ? 1
                : 0);
    }

    @Call
    public void exceptionAtConditionInParameter4() {
        consume(MethodChain.create()
                .doThrow()
                .get(0) > 0
                ? 1
                : 0);
    }

    @Call
    public void literalsWithEmptyConditionInAssignment() {
        int i = true
                ? 1
                : 0;

        int j = false
                ? 1
                : 0;
    }

    @Call
    public void methodCallsWithEmptyConditionInAssignment() {
        int i = true
                ? doGet(1)
                : doGet(0);

        int j = false
                ? doGet(1)
                : doGet(0);
    }

    @Call
    public void literalsWithEmptyConditionInParameter() {
        consume(true
                ? 1
                : 0);

        consume(false
                ? 1
                : 0);
    }

    @Call
    public void methodCallsWithEmptyConditionInParameter() {
        consume(true
                ? doGet(1)
                : doGet(0));

        consume(false
                ? doGet(1)
                : doGet(0));
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

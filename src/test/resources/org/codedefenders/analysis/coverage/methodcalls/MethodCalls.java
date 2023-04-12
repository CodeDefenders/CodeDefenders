import utils.Call;
import utils.MethodChain;
import utils.TestClass;

import static utils.Utils.callLambda;
import static utils.Utils.consume;
import static utils.Utils.dontCallLambda;
import static utils.Utils.doCall;
import static utils.Utils.doCatch;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

/**
 * <p>Methods
 * <p>JaCoCo coverage: Covers the opening parenthesis of method calls.
 * <p>Extended coverage: Covers the entire call, including the scope.
 */
public class MethodCalls {
    @Call
    public void methodChain() {
        MethodChain.create()
                .call()
                .call();
    }

    @Call
    public void methodChainWithFieldAccess() {
        consume(
                MethodChain.create()
                        .call()
                        .call()
                        .field
        );
    }

    @Call
    public void methodChainWithException() {
        MethodChain.create()
                .call()
                .doThrow()
                .call();

        // block: ignore_end_status
    }

    @Call
    public void methodChainWithExceptionAndFieldAccess() {
        consume(
                MethodChain.create()
                        .call()
                        .doThrow()
                        .call()
                        .field
        );

        // block: ignore_end_status
    }

    @Call
    public void methodChainWithExceptionAndArrayAccess() {
        int[] i = new int[1];

        consume(
                MethodChain.create()
                        .call()
                        .doThrow()
                        .get(i)
                        [0]
        );

        // block: ignore_end_status
    }

    @Call
    public void methodChainWithExceptionAndMethodReference() {
        callLambda(
                MethodChain.create()
                        .call()
                        .doThrow()
                        .call()
                        ::call
        );

        // block: ignore_end_status
    }

    @Call
    public void lambdaArgs() {
        callLambda(() -> {
            consume(1);

        });

        dontCallLambda(() -> {
            consume(2);

        });

        dontCallLambda(
                () -> {
            consume(3);

        });
    }

    @Call
    public void exceptionInArg1() {
        consume(
                doThrow()
        );
    }

    @Call
    public void exceptionInArg2() {
        consume(
                doGet(1),
                doThrow()
        );

        // block: ignore_end_status
    }

    @Call
    public void exceptionInArg3() {
        consume(
                doThrow(),
                doGet(1)
        );
    }

    @Call
    public void exceptionInLambdaArg() {
        callLambda(() -> {
                doCall();
                doThrow();
        });

        // block: ignore_end_status
    }

    @Call
    public void exceptionInMethodCallArg() {
        MethodChain.create()
                .call()
                .consume
                        (

                        MethodChain.create()
                                .call()
                                .doThrow()
                                .get(4)

                );

        // block: ignore_end_status
    }

    @Call
    public void exceptionWithCoveredArg() {
        doThrow(
                doGet(1)
        );

        // block: ignore_end_status
    }

    @Call
    public void exceptionWithCoveredArg2() {
        doThrow(doGet(1));
    }

    @Call
    public void superCalls() {
        doCatch(() -> new SuperCalls());
        doCatch(() -> new SuperCalls(0));
        doCatch(() -> new SuperCalls(0, 0));
        doCatch(() -> new SuperCalls(0, 0, 0));
        doCatch(() -> new SuperCalls(0, 0, 0, 0));
    }
    public static class SuperCalls extends TestClass {
        // super call with throwing arg
        public SuperCalls() {
            super(doThrow());
        }

        // super call with throwing arg
        public SuperCalls(int i) {
            super(
                    doThrow());

            // block: ignore_end_status
        }

        // super call with covered throwing arg
        public SuperCalls(int i, int j) {
            super(MethodChain.create()
                            .doThrow()
                            .get(1)
            );

            // block: ignore_end_status
        }

        // super call with covered throwing arg
        public SuperCalls(int i, int j, int k) {
            super(
                    MethodChain.create()
                    .doThrow()
                    .get(1)
            );

            // block: ignore_end_status
        }

        // super call with covered and throwing arg
        public SuperCalls(int i, int j, int k, int l) {
            super(
                    doGet(1),
                    doThrow());

            // block: ignore_end_status
        }
    }

}

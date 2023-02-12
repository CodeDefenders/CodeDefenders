import org.checkerframework.checker.builder.qual.CalledMethods;

import utils.Call;
import utils.MethodChain;

import static utils.Utils.callLambda;
import static utils.Utils.consume;
import static utils.Utils.dontCallLambda;
import static utils.Utils.doCall;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

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

        ; // empty stmt to check if the coverage after the above stmt is correct
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

        ;
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

        ; // empty stmt to check if the coverage after the above stmt is correct
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

        ; // empty stmt to check if the coverage after the above stmt is correct
    }

    @Call
    public void lambdaParameters() {
        callLambda(() -> {

        });

        dontCallLambda(() -> {

        });

        dontCallLambda(
                () -> {

        });
    }

    @Call
    public void exceptionInParameter1() {
        consume(
                doThrow()
        );
    }

    @Call
    public void exceptionInParameter2() {
        consume(
                doGet(1),
                doThrow()
        );

        ;
    }

    @Call
    public void exceptionInParameter3() {
        consume(
                doThrow(),
                doGet(1)
        );
    }

    @Call
    public void exceptionInLambdaParameter() {
        callLambda(() -> {
                doCall();
                doThrow();
        });

        ;
    }

    @Call
    public void exceptionInMethodCallParameter() {
        MethodChain.create()
                .call()
                .consume(

                        MethodChain.create()
                                .call()
                                .doThrow()
                                .get(4)

                );

        ;
    }

    @Call
    public void exceptionWithCoveredParameter() {
        doThrow(
                doGet(1)
        );
    }

    @Call
    public void exceptionWithCoveredParameter2() {
        doThrow(doGet(1));
    }
}

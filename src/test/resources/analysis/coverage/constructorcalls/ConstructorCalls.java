import utils.Call;
import utils.MethodChain;
import utils.TestClass;
import utils.ThrowingClass;

import static utils.Utils.doThrow;
import static utils.Utils.doCall;
import static utils.Utils.doGet;

/**
 * <p>Constructor calls
 * <p>JaCoCo coverage: Covers the 'new' keyword.
 * <p>Extended coverage: Covers the call like a method call. If the call has an anonymous inner class declaration,
 *                       it is reset, such that it can be covered like a class.
 */
public class ConstructorCalls {

    @Call
    public void regular() {
        new TestClass();

        new
                TestClass
                (
                );

        new TestClass
                (
                        1,
                        doGet(2)
                );
    }

    @Call
    public void exceptionInArg1() {
        new TestClass(
                doThrow()
        );
    }

    @Call
    public void exceptionInArg2() {
        new TestClass(
                doGet(1),
                doThrow()
        );

        // block: ignore_end_status
    }

    @Call
    public void exceptionInArg3() {
        new TestClass(
                doThrow(),
                doGet(1)
        );

        ;
    }

    @Call
    public void exceptionInCoveredArg() {
        new TestClass(
                MethodChain.create()
                        .doThrow()
                        .get(1)
        );

        // block: ignore_end_status
    }

    @Call
    public void exceptionInLambdaArg() {
        new TestClass(() -> {
            doCall();
            doThrow();
        });

        // block: ignore_end_status
    }

    @Call
    public void exceptionInConstructor1() {
        new ThrowingClass();
    }

    @Call
    public void exceptionInConstructor2() {
        new ThrowingClass(doGet(1));
    }

    @Call
    public void exceptionInConstructor3() {
        new ThrowingClass(
                doGet(1));
    }

    @Call
    public void anonymousInnerClass1() {
        new Runnable() {
           int i = 1;

           public void run() {
               i = 2;
           }
        };
    }

    @Call
    public void anonymousInnerClass2() {
        new Runnable()
        {

            int i = 1;

            public void run() {
                i = 2;
            }};
    }
}

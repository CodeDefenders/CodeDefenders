import utils.Call;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doCall;
import static utils.Utils.doCatch;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

/**
 * <p>Methods
 * <p>JaCoCo coverage: Covers the closing brace if the closing brace can be reached (i.e. there is not an explicit
 *                     jump in every path).
 * <p>Extended coverage: Covers the method signature and sets the coverage of the body if empty.
 */
public class Methods {
    @Call
    public void test() {
        new Methods.InterfaceDefaultMethods(){}.empty();
        new Methods.InterfaceDefaultMethods(){}.explicitReturn();
        doCatch(new Methods.InterfaceDefaultMethods(){}::throwsException);
        doCatch(new Methods.InterfaceDefaultMethods(){}::throwsExceptionFromCoveredExpr);
        doCatch(new Methods.InterfaceDefaultMethods(){}::doesCallAndThrowsException);
    }

    @Call
    public void empty() {

    }

    @Call
    public void explicitReturn() {
        return;
    }

    @Call
    public void throwsException() {
        doThrow();

    }

    @Call
    public void throwsExceptionFromCoveredExpr1() {
        MethodChain.create()
                .doThrow();

    }

    @Call
    public void throwsExceptionFromCoveredExpr2() {
        consume(
                doGet(1),
                doThrow());

    }

    @Call
    public void doesCallAndThrowsException() {
        doCall();
        doThrow();

    }

    // same with interface default methods
    public interface InterfaceDefaultMethods {
        default void empty() {

        }

        default void explicitReturn() {
            return;
        }

        default void throwsException() {
            doThrow();
        }

        default void throwsExceptionFromCoveredExpr() {
            MethodChain.create()
                    .doThrow();
        }

        default void doesCallAndThrowsException() {
            doCall();
            doThrow();
        }
    }
}

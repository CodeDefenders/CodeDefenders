import static utils.Utils.doThrow;

public class Methods {
    /**
     * <p>empty method
     * <p><b>JaCoCo coverage</b>: covers the closing brace (closing brace of methods with implicit return is always
     *                            covered)
     * <p><b>extended coverage</b>: covers the entire signature and sets the body to covered (coverage of the body
     *                              is handled by the code block)
     */
    static void empty() {

    }

    /**
     * <p>method with return
     * <p><b>JaCoCo coverage</b>: doesn't cover the method itself at all
     * <p><b>extended coverage</b>: covers the entire signature (coverage of the body is handled by the code block)
     */
    static void explicitReturn() {
        return;
    }

    /**
     * <p>method with only exception-throwing stmt
     * <p><b>JaCoCo coverage</b>: covers neither the method nor the stmt
     * <p><b>extended coverage</b>: also covers nothing (we can't reliably detect if the method has been called, but
     *                              neither could a human)
     */
    static void throwsException() {
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
    }
}

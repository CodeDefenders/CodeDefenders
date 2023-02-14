import utils.TestRuntimeException;
import utils.ThrowingClass;

import static utils.Utils.doCall;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

public class Constructors {
    /**
     * <p>empty constructor
     * <p><b>JaCoCo coverage</b>: covers the opening and closing brace (closing brace of methods with implicit
     *                            return is always covered)
     * <p><b>extended coverage</b>: covers the entire signature and sets the body to covered (coverage of the body
     *                              is handled by the code block)
     */
    Constructors() {

    }

    /**
     * <p>constructor with return
     * <p><b>JaCoCo coverage</b>: covers the opening brace
     * <p><b>extended coverage</b>: covers the entire signature (coverage of the body is handled by the code block)
     */
    Constructors(int i) {
        return;
    }

    /**
     * <p>constructor with exception
     * <p><b>JaCoCo coverage</b>: covers the opening brace
     * <p><b>extended coverage</b>: covers the entire signature (coverage of the body is handled by the code block)
     */
    Constructors(int i, int j) {
        doThrow();
    }

    record CompactConstructorsEmpty() {
        /**
         * <p>compact constructor of empty record
         * <p><b>JaCoCo coverage</b>: covers the opening brace and closing brace
         * <p><b>extended coverage</b>: covers the entire signature (coverage of the body is handled by the code block)
         */
        CompactConstructorsEmpty {

        }
    }
    // test with throw statement here, because compact constructors may not have a return
    record CompactConstructorsEmptyException1() {
        CompactConstructorsEmptyException1 {
            throw new TestRuntimeException();
        }
    }
    record CompactConstructorsEmptyException2() {
        CompactConstructorsEmptyException2 {
            doThrow();
        }
    }

    record CompactConstructors(int i) {
        /**
         * <p>compact constructor of non-empty record
         * <p><b>JaCoCo coverage</b>: covers the opening brace, closing brace and constructor name (probably for the
         *                            implicit field initialization)
         * <p><b>extended coverage</b>: covers the entire signature (coverage of the body is handled by the code block)
         */
        CompactConstructors {

        }
    }
    record CompactConstructorsException1(int i) {
        CompactConstructorsException1 {
            throw new TestRuntimeException();
        }
    }
    record CompactConstructorsException2(int i) {
        CompactConstructorsException2 {
            doThrow();
        }
    }

    static class ThrowingBaseClass extends ThrowingClass {
        ThrowingBaseClass() {
            doCall();
        }
    }
    static class ThrowingBaseClassWithSuper extends ThrowingClass {
        ThrowingBaseClassWithSuper() {
            super();
            doCall();
        }
    }
    static class ThrowingBaseClassWithSuperAndCoveredArg1 extends ThrowingClass {
        ThrowingBaseClassWithSuperAndCoveredArg1() {
            super(doGet(1));
            doCall();
        }
    }
    static class ThrowingBaseClassWithSuperAndCoveredArg2 extends ThrowingClass {
        ThrowingBaseClassWithSuperAndCoveredArg2() {
            super(
                    doGet(1));
            doCall();
        }
    }
}

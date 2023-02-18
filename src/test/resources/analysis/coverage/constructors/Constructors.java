import utils.Call;
import utils.TestRuntimeException;
import utils.ThrowingClass;

import static utils.Utils.doCall;
import static utils.Utils.doCatch;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

public class Constructors {
    @Call
    public static void test() {
        new Constructors();
        new Constructors(0);
        doCatch(() -> new Constructors(0, 0));
        new Constructors.CompactConstructorsEmpty();
        doCatch(Constructors.CompactConstructorsEmptyException1::new);
        doCatch(Constructors.CompactConstructorsEmptyException2::new);
        new Constructors.CompactConstructors(0);
        doCatch(() -> new Constructors.CompactConstructorsException1(0));
        doCatch(() -> new Constructors.CompactConstructorsException2(0));
        doCatch(Constructors.ThrowingBaseClass::new);
        doCatch(Constructors.ThrowingBaseClassWithSuper::new);
        doCatch(Constructors.ThrowingBaseClassWithSuperAndCoveredArg1::new);
        doCatch(Constructors.ThrowingBaseClassWithSuperAndCoveredArg2::new);
    }


    /**
     * <p>Empty constructor
     * <p>JaCoCo coverage: Covers the opening and closing brace (closing brace of methods with implicit
     *                     return is always covered).
     * <p>Extended coverage: Covers the entire signature and sets the body to covered (coverage of the body
     *                       is handled by the code block).
     */
    Constructors() {

    }

    /**
     * <p>Constructor with return
     * <p>JaCoCo coverage: Covers the opening brace (and the return instead of the closing brace).
     * <p>Extended coverage: Covers the entire signature (coverage of the body is handled by the code block).
     */
    Constructors(int i) {
        return;
    }

    /**
     * <p>Constructor with exception
     * <p>JaCoCo coverage: Covers the opening brace and closing brace.
     * <p>Extended coverage: Covers the entire signature (coverage of the body is handled by the code block).
     */
    Constructors(int i, int j) {
        doThrow();
    }

    record CompactConstructorsEmpty() {
        /**
         * <p>Compact constructor of empty record
         * <p>JaCoCo coverage: Covers the opening brace and closing brace.
         * <p>Extended coverage: Covers the entire signature (coverage of the body is handled by the code block).
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
         * <p>Compact constructor of non-empty record
         * <p>JaCoCo coverage: Covers the opening brace, closing brace and constructor name (probably for the
         *                     implicit field initialization).
         * <p>Extended coverage: Covers the entire signature (coverage of the body is handled by the code block).
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

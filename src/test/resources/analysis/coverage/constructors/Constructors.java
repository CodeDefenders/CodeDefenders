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
}

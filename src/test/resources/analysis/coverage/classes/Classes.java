public class Classes {
    /**
     * <p>class without constructor
     * <p><b>JaCoCo coverage</b>: covers class keyword
     * <p><b>extended coverage</b>: covers entire class signature
     */
    static class ClassWithoutConstructor {

    }

    /**
     * <p>class with constructor
     * <p><b>JaCoCo coverage</b>: covers opening brace of covered constructor
     * <p><b>extended coverage</b>: covers class signature (coverage of constructor is independent of the class)
     */
    static class ClassWithConstructor {
        public ClassWithConstructor() {

        }
    }

    static abstract class AbstractClassWithoutConstructor {

    }

    static abstract class AbstractClassWithConstructor {
        public AbstractClassWithConstructor() {

        }
    }

    /**
     * <p>interface
     * <p><b>JaCoCo coverage</b>: never covered
     * <p><b>extended coverage</b>: never covered
     */
    interface Interface {
        void method();
    }

    /**
     * <p>interface with default method
     * <p><b>JaCoCo coverage</b>: never covered (except for the method)
     * <p><b>extended coverage</b>: never covered (except for the method)
     */
    interface InterfaceWithDefaultMethod {
        default void method() {

        }
    }

    /**
     * <p>empty record (no fields) without constructor
     * <p><b>JaCoCo coverage</b>: covers record keyword
     * <p><b>extended coverage</b>: covers entire record signature
     */
    record EmptyRecordWithoutConstructor() {

    }

    /**
     * <p>empty record (no fields) with constructor
     * <p><b>JaCoCo coverage</b>: covers opening brace of constructor
     * <p><b>extended coverage</b>: covers record signature
     */
    record EmptyRecordWithConstructor() {
        public EmptyRecordWithConstructor {

        }
    }

    /**
     * <p>record (with fields) without constructor
     * <p><b>JaCoCo coverage</b>: combines coverage of the record (initialized or not) and coverage of implicit getters
     *                            on the line of the record keyword
     * <p><b>extended coverage</b>: covers record signature if initialized
     */
    record RecordWithoutConstructor(int i) {

    }

    /**
     * <p>record (with fields) with constructor
     * <p><b>JaCoCo coverage</b>: combines coverage of implicit getters on the line of the record keyword
     * <p><b>extended coverage</b>: covers record signature if initialized
     */
    record RecordWithConstructor(int i) {
        public RecordWithConstructor {

        }
    }

    enum EnumWithoutConstants {

    }

    enum EnumWithoutConstructor {
        CONSTANT;
    }

    enum EnumWithConstructor {
        CONSTANT(true);

        EnumWithConstructor(boolean dummy) {

        }
    }
}

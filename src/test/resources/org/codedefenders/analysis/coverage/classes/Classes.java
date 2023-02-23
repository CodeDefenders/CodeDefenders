import utils.Call;

import static utils.Utils.consume;

public class Classes {
    @Call
    public void test() {
        // Classes
        new Classes.ClassWithoutConstructor();
        new Classes.ClassWithConstructor();

        // Abstract classes
        new Classes.AbstractClassWithoutConstructor(){};
        new Classes.AbstractClassWithConstructor(){};

        // Interfaces
        // implement the interfaces and invoke their methods to make sure to cover them if they were coverable
        new Classes.InterfaceWithDefaultMethod(){}.method();
        new Classes.Interface(){
            @Override
            public void method() {

            }
        }.method();

        // Records
        new Classes.EmptyRecordWithoutConstructor();
        new Classes.EmptyRecordWithConstructor();
        new Classes.RecordWithoutConstructor(0);
        new Classes.RecordWithoutConstructor2(0);
        new Classes.RecordWithConstructor(0);
        new Classes.RecordWithConstructor2(0);

        // Enums
        consume(Classes.EnumWithoutConstructor.CONSTANT);
        consume(Classes.EnumWithConstructor.CONSTANT);
    }

    /**
     * <p>Class without constructor
     * <p>JaCoCo coverage: Covers class keyword.
     * <p>Extended coverage: Covers entire class signature.
     */
    static
    class
    ClassWithoutConstructor
    <T extends Object>

    {

    }

    /**
     * <p>Class with constructor
     * <p>JaCoCo coverage: Covers opening brace of covered constructor.
     * <p>Extended coverage: Covers class signature (constructor is covered independently of the class).
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
     * <p>Interface
     * <p>JaCoCo coverage: Never covered.
     * <p>Extended coverage: Never covered.
     */
    interface Interface {
        void method();
    }

    /**
     * <p>Interface with default method
     * <p>JaCoCo coverage: Never covered (except for the method).
     * <p>Extended coverage: Never covered (except for the method).
     */
    interface InterfaceWithDefaultMethod {
        default void method() {

        }
    }

    /**
     * <p>Empty record (no fields) without constructor
     * <p>JaCoCo coverage: Covers record keyword.
     * <p>Extended coverage: Covers entire record signature.
     */
    record EmptyRecordWithoutConstructor() {

    }

    /**
     * <p>Empty record (no fields) with constructor
     * <p>JaCoCo coverage: Covers opening brace of constructor.
     * <p>Extended coverage: Covers record signature.
     */
    record EmptyRecordWithConstructor() {
        public EmptyRecordWithConstructor {

        }
    }

    /**
     * <p>Eecord (with fields) without constructor
     * <p>JaCoCo coverage: Combines coverage of the record (initialized or not) and coverage of implicit getters
     *                     on the line of the record keyword.
     * <p>Extended coverage: Covers record signature if initialized.
     */
    record RecordWithoutConstructor(int i) {

    }
    record
    RecordWithoutConstructor2
    (int i)

    {

    }

    /**
     * <p>Record (with fields) with constructor
     * <p>JaCoCo coverage: Combines coverage of implicit getters on the line of the record keyword.
     * <p>Extended coverage: Covers record signature if initialized.
     */
    record RecordWithConstructor(int i) {
        public RecordWithConstructor {

        }
    }
    record
    RecordWithConstructor2
    (int i)

    {
        public RecordWithConstructor2 {

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

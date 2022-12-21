import static utils.Utils.doThrow;

/**
 * <p>fields
 * <p><b>JaCoCo coverage</b>: covers the first line of fields. doesn't cover fields without initializer, since they
 *                            don't correspond to a bytecode instruction
 * <p><b>extended coverage</b>: covers all lines of fields. non-static fields without initializer are covered if the
 *                              class is covered and no field before them is not-covered. static fields without
 *                              initializer are covered if anything in the class is covered (the class does not have
 *                              to be initialized)
 */
public class Fields {
    static class RegularFields {
        // field with initializer
        int i = 0;

        // field without initializer
        int j;

        // not-covered field with exception in initializer
        int k = doThrow();

        // not-covered field with initializer
        int l = 1;

        // not-covered field without initializer
        int m;

        // static field with initializer
        static int n = 1;
    }

    static class ExceptionWithMultipleDelcarators {
        int i = 0;

        int j = 0,
            k = doThrow();

        int l;
    }

    static class ExceptionWithMultipleDelcaratorsAndEmpty {
        int i = 0;

        int j = 0,
            k = doThrow(),
            l;

        int m;
    }

    static class ExceptionWithMultipleDelcaratorsOnSameLine {
        int i = 0;

        int j = 0, k = doThrow();

        int l;
    }

    // to check if static fields without initializer are covered when the class isn't initialized
    static class StaticFieldWithoutInitializer {
        // static field without initializer
        static int i;

        // method, so we can cover something in the class without initializing it
        static void method() {

        }
    }

    static class TrickyVariableDeclarators {
        // field with partly covered initializer
        Runnable r = () -> {};

        // field with not-covered variable declarator
        private
        Runnable s = () -> {};
    }

    // fields in a local class
    // wrap in a not-covered class to check if the ObjectCreationExpression or the surrounding class is checked as the
    // declaring type
    static class FieldsInLocalClass {
        static void method() {
            class Local {
                int i;
                int j = doThrow();
                int k;
                static int l;
            }
            new Local();
        }
    }

    // fields in an anonymous class
    // wrap in a not-covered class to check if the local class or the surrounding class is checked as the declaring type
    static class FieldsInAnonymousClass {
        static void method() {
            new Runnable() {
                int i;
                int j = doThrow();
                int k;
                static int l;

                @Override
                public void run() {

                }
            };
        }
    }
}

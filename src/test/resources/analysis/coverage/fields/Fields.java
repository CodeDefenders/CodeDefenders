import utils.Call;
import utils.MethodChain;

import static utils.Utils.doCatch;
import static utils.Utils.doThrow;

/**
 * <p>Fields
 * <p>JaCoCo coverage: Covers the first line of fields. Doesn't cover fields without initializer.
 * <p>Extended coverage: Covers all lines of fields based on the coverage of the field and the initializer expresison.
 *                       <p>Non-static fields without initializer are covered if the class is covered and no field
 *                       before them is not-covered. Static fields without initializer are covered if anything in the
 *                       class is covered (the class does not have to be initialized).
 */
public class Fields {
    @Call
    public void test() {
        doCatch(Fields.RegularFields::new);
        doCatch(Fields.ExceptionWithMultipleDelcarators::new);
        doCatch(Fields.ExceptionWithMultipleDelcaratorsAndCoveredExpr::new);
        doCatch(Fields.ExceptionWithMultipleDelcaratorsAndEmpty::new);
        doCatch(Fields.ExceptionWithMultipleDelcaratorsOnSameLine::new);
        new Fields.TrickyVariableDeclarators();
        Fields.StaticFieldWithoutInitializer.method();
        doCatch(Fields.FieldsInLocalClass::method);
        doCatch(Fields.FieldsInAnonymousClass::method);
    }

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

            k =
                    doThrow();

        int l;
    }

    static class ExceptionWithMultipleDelcaratorsAndCoveredExpr {
        int i = 0;

        int j = 0,

        k =
                MethodChain.create()
                        .doThrow()
                        .get(1);

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

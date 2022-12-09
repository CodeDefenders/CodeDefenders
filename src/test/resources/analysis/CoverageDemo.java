/**
 * <p>Code for testing extended coverage.
 *
 * <p>The JavaDoc comments also serve as an explanation of JaCoCo's coverage and extended coverage
 * (i.e. which lines are covered under which conditions).
 *
 * <p>CoverageDemo only tests code with normal formatting.
 * Challenging formatting that could lead to issues for coverage is tested in CoverageDemoFormat.
 */
public class CoverageDemo {

    public static void main(String[] args) {
        // Classes
        new ClassWithoutConstructor();
        new ClassWithConstructor();

        // Interfaces
        new InterfaceWithDefaultMethod(){}.method();
        new Interface(){
            @Override
            public void method() {

            }
        }.method();

        // Records
        new EmptyRecordWithoutConstructor();
        new EmptyRecordWithConstructor();
        new RecordWithoutConstructor(0);
        new RecordWithConstructor(0);

        // Fields
        doCatch(Fields::new);
        StaticFieldWithoutInitializer.method();

        // Constructors
        new Constructors();
        new Constructors(0);
        doCatch(() -> new Constructors(0, 0));
        new CompactConstructorsEmpty();
        new CompactConstructors(0);

        // Methods
        Methods.empty();
        Methods.explicitReturn();
        doCatch(Methods::throwsException);
        new InterfaceDefaultMethods(){}.empty();
        new InterfaceDefaultMethods(){}.explicitReturn();
        doCatch(new InterfaceDefaultMethods(){}::throwsException);


        // Local Variables
        doCatch(CoverageDemo::localVariables);

        // Blocks
        Blocks.coveredToEnd();
        Blocks.earlyReturn();
        doCatch(Blocks::earlyException);
        doCatch(Blocks::earlyIndirectException);
        Blocks.independentNodes(true);
        Blocks.nestedBlocks();

        // Ifs
        Ifs.normalIf(true);
        Ifs.returnFromIf(true);
        doCatch(() -> Ifs.exceptionFromIf(true));
    }

    // region Helpers

    static int doGet() {
        return 4;
    }

    static int doThrow() {
        throw new RuntimeException();
    }

    static void doCatch(Runnable r) {
       try {
            r.run();
       } catch (RuntimeException ignored) {

       }
    }

    public static class MethodChain {
        public MethodChain doNotThrow() {
            return this;
        }
        public MethodChain doThrow() {
            throw new RuntimeException();
        }
    }

    // endregion
    // region Classes

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

    // endregion
    // region Interfaces

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

    // endregion
    // region Records

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

    // endregion
    // region Fields

    /**
     * <p>fields
     * <p><b>JaCoCo coverage</b>: covers the first line of fields. doesn't cover fields without initializer, since they
     *                            don't correspond to a bytecode instruction
     * <p><b>extended coverage</b>: covers all lines of fields. non-static fields without initializer are covered if the
     *                              class is covered and no field before them is not-covered. static fields without
     *                              initializer are covered if anything in the class is covered (the class does not have
     *                              to be initialized)
     */
    static class Fields {
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
    static class StaticFieldWithoutInitializer {
        // static field without initializer
        static int i;

        // method, so we can cover something in the class without initializing it
        static void method() {

        }
    }

    // endregion
    // region Constructors

    static class Constructors {
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

    // endregion
    // region Methods

    static class Methods {
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

    // endregion
    // region Local Variables

    /**
     * <p>local variables declarations
     * <p><b>JaCoCo coverage</b>: either covers the first line of variable name, or a (sub-)expression of the
     *                            initializer if the expression is coverable (e.g. a method call expr).
     * <p><b>extended coverage</b>: covers all lines of local variable declarations. variables without initializer are
     *                              treated like empty lines and are covered by the surrounding block as necessary.
     */
    static void localVariables() {
        // variable with initializer
        int i = 0;

        // variable with coverable initializer expression
        int j = doGet();

        // variable without initializer
        int k;

        // not-covered variable
        int l = doThrow();

        // not-covered variable
        int m = 1;
    }

    // endregion
    // region Blocks

    /**
     * <p>code blocks
     * <p><b>JaCoCo coverage</b>: doesn't consider code blocks at all
     * <p><b>extended coverage</b>: covers lines without statements and lines with empty statements (i.e. stmts that
     *                              can never produce coverage) that are intuitively covered (i.e. control flow passes
     *                              through the lines)
     */
    static class Blocks {

        /**
         * <p>code blocks that are executed to the end
         * <p><b>JaCoCo coverage</b>: doesn't consider code blocks at all
         * <p><b>extended coverage</b>: covers the block up to (and including) the closing brace
         */
        static void coveredToEnd() {

        }

        /**
         * <p>code blocks with return/break/exception/etc.
         * <p><b>JaCoCo coverage</b>: doesn't consider code blocks at all
         * <p><b>extended coverage</b>: covers the block up to the jump
         */
        static void earlyReturn() {
            return;

        }
        static void earlyException() {
            throw new RuntimeException();

        }
        static void earlyIndirectException() {
            int i = 1;  // some statement to produce coverage, otherwise the method is not covered at all
            doThrow();

        }

        /**
         * <p>code blocks with local classes/anonymous classes/lambdas
         * <p><b>JaCoCo coverage</b>: doesn't consider code blocks at all
         * <p><b>extended coverage</b>: covers the block around the classes/methods (as appropriate), but not the
         *                              classes/methods themselves, as their coverage is not guaranteed even if the
         *                              surrounding code is covered
         */
        static void independentNodes(boolean cover) {
            // local class
            class LocalClass {

            }

            // anonymous class (not-covered anonymous class not possible?)
            new Runnable() {
                @Override
                public void run() {

                }
            };

            // lambda
            Runnable lambda = () -> {

            };

            if (cover) {
                new LocalClass();
                lambda.run();
            }
        }

        /**
         * <p>nested code blocks with return
         * <p><b>JaCoCo coverage</b>: doesn't consider code blocks at all
         * <p><b>extended coverage</b>: covers each nested block up to the return statement, but not past it
         */
        static void nestedBlocks() {
            {
                {
                    return;

                }
            }
        }
    }

    // endregion
    // region Ifs

    static class Ifs {

        /**
         * <p>block coverage after jumps out of then- or else-blocks
         * <p><b>JaCoCo coverage</b>: doesn't consider code blocks at all
         * <p><b>extended coverage</b>: covers the code block after the if-stmt depending on if the jump was taken.
         *                              however, we might not always be able to determine which branches were taken.
         *
         */
        static void normalIf(boolean takeThenBranch) {
            if (takeThenBranch) {
                int i = 1;
            } else {

            }

        }
        static void returnFromIf(boolean takeThenBranch) {
            if (takeThenBranch) {
                return;
            } else {

            }

        }
        static void exceptionFromIf(boolean takeThenBranch) {
            if (takeThenBranch) {
                doThrow();
            } else {

            }

        }

        // TODO: condition (not covered, partly covered, fully covered)
        // TODO: coverage of empty blocks?

    }

    // endregion
    // region Loops
    // endregion
    // region Switches
    // endregion
    // region Lambdas
    // endregion
    // region Try-Catch
    // endregion
    // region Statements

    // TODO: exceptions from method chains

    // endregion

    // we could use the fact that [int j = 1;] is not covered to determine that the class is not covered
    // however, the assignment could have thrown an exception (at least we'd need analysis to find out if it could've)
    // also who would do this?
    public static void gotcha1() {
        System.currentTimeMillis(); class NotCovered {
            int i;
            int j = 1;
        }
    }
    static class P {
        int i = doThrow();
    }

}

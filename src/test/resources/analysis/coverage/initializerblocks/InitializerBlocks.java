import static utils.Utils.doThrow;

public class InitializerBlocks {
    /**
     * <p>static initializer block
     * <p><b>JaCoCo coverage</b>: only covers the statement(s)
     * <p><b>extended coverage</b>: covers the block
     */
    static {
        int i = 1;
    }

    /**
     * <p>empty static initializer block
     * <p><b>JaCoCo coverage</b>: covers the line of the closing brace
     * <p><b>extended coverage</b>: covers the block
     */
    static {

    }

    // we don't consider static blocks throwing exceptions, because that leads to an ExceptionInInitializerError

    /**
     * <p>non-static initializer block with statement(s)
     * <p><b>JaCoCo coverage</b>: only covers the statement(s)
     * <p><b>extended coverage</b>: covers the block. if empty, the coverage depends on wheter the class has been
     *                              initialized, and whether non-static initializer blocks before it have been covered
     */
    {
        int i = 1;
    }

    // empty non-static block
    {

    }

    // non-static block throwing exception
    {
        int i = 1;
        doThrow();
    }

    // not-covered non-static block
    {

    }

    static class NoCoverageButStaticBlock {
        // something to access so we can execute the static block
        static int i;

        static {

        }
    }

    // fields in a local class
    // wrap in a not-covered class to check if the ObjectCreationExpression or the surrounding class is checked as the
    // declaring type
    static class BlocksInLocalClass {
        static void method() {
            class Local {
                {}
                { doThrow(); }
                {}
            }
            new Local();
        }
    }

    // fields in an anonymous class
    // wrap in a not-covered class to check if the local class or the surrounding class is checked as the declaring type
    static class BlocksInAnonymousClass {
        static void method() {
            new Runnable() {
                {}
                { doThrow(); }
                {}

                @Override
                public void run() {

                }
            };
        }
    }
}


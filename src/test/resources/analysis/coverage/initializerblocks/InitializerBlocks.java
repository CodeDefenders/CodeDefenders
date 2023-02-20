import utils.Call;

import static utils.Utils.doCatch;
import static utils.Utils.doThrow;
import static utils.Utils.consume;

public class InitializerBlocks {
    @Call
    public static void test() {
        doCatch(InitializerBlocks::new);
        consume(InitializerBlocks.NoCoverageButStaticBlock.i);
        doCatch(InitializerBlocks.BlocksInLocalClass::method);
        doCatch(InitializerBlocks.BlocksInAnonymousClass::method);
    }

    /**
     * <p>Static initializer block
     * <p>JaCoCo coverage: Only covers the statement(s).
     * <p>Extended coverage: Covers the block.
     */
    static {
        int i = 1;
    }

    /**
     * <p>Empty static initializer block
     * <p>JaCoCo coverage: Covers the line of the closing brace.
     * <p>Extended coverage: Covers the block.
     */
    static {

    }

    // We don't consider static blocks throwing exceptions, because that leads to an ExceptionInInitializerError.

    /**
     * <p>Non-static initializer block with statement(s)
     * <p>JaCoCo coverage: Only covers the statement(s).
     * <p>Extended coverage: Covers the block. If empty, the coverage depends on wheter the class has been initialized,
     *                       and whether non-static initializer blocks before it have been covered.
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

    // fields in a local class (wrap in a not-covered class to check if the ObjectCreationExpression or the surrounding
    // class is checked as the declaring type)
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

    // fields in an anonymous class (wrap in a not-covered class to check if the local class or the surrounding class is
    // checked as the declaring type)
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


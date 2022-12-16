import static utils.Utils.doThrow;

public class Blocks {

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

        // anonymous class (uninitialized anonymous class not possible I think)
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

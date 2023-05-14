import utils.Call;
import utils.TestRuntimeException;

import static utils.Utils.doThrow;
import static utils.Utils.doCall;

public class Blocks {

    /**
     * <p>Code blocks that are executed to the end
     * <p>JaCoCo coverage: Doesn't consider code blocks at all.
     * <p>Extended coverage: Covers the block up to (and including) the closing brace.
     */
    @Call
    public void coveredToEnd() {

    }

    /**
     * <p>Code blocks with return/break/exception/etc
     * <p>JaCoCo coverage: Doesn't consider code blocks at all.
     * <p>Extended coverage: Covers the block up to the jump.
     */
    @Call
    public void earlyReturn() {
        return;

    }
    @Call
    public void earlyException() {
        throw new TestRuntimeException();

    }
    @Call
    public void earlyIndirectException() {
        int i = 1;  // some statement to produce coverage, otherwise the method is not covered at all
        doThrow();

    }

    /**
     * <p>Code blocks with local classes/anonymous classes/lambdas
     * <p>JaCoCo coverage: Doesn't consider code blocks at all
     * <p>extended coverage: Covers the block around the classes/methods (as appropriate), but not the
     *                       classes/methods themselves, as their coverage is not guaranteed even if the
     *                       surrounding code is covered
     */
    @Call
    public void independentNodes() {
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
            return;

        };

        new LocalClass();
        lambda.run();
    }

    /**
     * <p>Nested code blocks with return
     * <p>JaCoCo coverage: Doesn't consider code blocks at all.
     * <p>Extended coverage: Covers each nested block up to the return statement, but not past it.
     */
    @Call
    public void nestedBlocks() {
        {
            {
                return;

            }
        }
    }

    @Call
    public void phases1() {
        int i = 0;
        // statusAfter is COVERED

        if (i == 0) {
            doThrow();
        }
        // statusAfter is MAYBE_COVERED (can't determine if then branch was skipped or taken + threw exception)
        // therefore, space is left EMPTY

        int j = 0;
        // statusAfter is NOT_COVERED

        return;
        // statusAfter is ALWAYS_JUMPS

    }

    @Call
    public void phases2() {
        int i = 0;
        // statusAfter is COVERED

        if (i != 0) {
            doCall();
        }
        // statusAfter is MAYBE_COVERED (can't determine if then branch was skipped or taken + threw exception)
        // but: space is COVERED, since a stmt after it is COVERED

        int j = 0;
        // statusAfter is COVERED

        return;
        // statusAfter is ALWAYS_JUMPS
    }
}

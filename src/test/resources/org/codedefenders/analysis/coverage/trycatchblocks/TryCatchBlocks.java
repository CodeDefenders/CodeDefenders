import utils.Call;
import utils.MethodChain;
import utils.ThrowingAutoCloseable;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

/**
 * <p>Try-catch blocks
 * <p>JaCoCo coverage: Covers the catch keywords based on whether the catch block was executed.
 *                     Covers the finally keyword if the finally-block is empty.
 *                     <p>Additionally, the line of the closing brace contains goto instructions from the end of the
 *                     try- and catch-blocks (except the last, since it doesn't need a goto instruction) and some
 *                     instructions from the finally block.
 *                     <p>The closing brace of a try-with-resources-block also contains instrtuctions and branches for
 *                     an ifnull check of the resource to close.
 * <p>Extended coverage: Covers empty space around the try-, catch- and finally blocks according to their coverage.
 */
public class TryCatchBlocks {
    @Call
    public void oneCatchBlockNoException() {
        try {
            doCall();
        }
        catch
        (RuntimeException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void oneCatchBlockWithCaughtException() {
        try {
            doThrow();
        }
        catch
        (RuntimeException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void oneCatchBlockWithUncaughtException() {
        try {
            doThrow();
        } catch (NullPointerException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void coveredTryBlockAndOneCatchBlockWithUncaughtException() {
        try {
            doCall();
            doThrow();
        } catch (NullPointerException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void twoCatchBlocksFirstCatchesException() {
        try {
            String s = null;
            s.toString();

        } catch (NullPointerException e) {

        } catch (RuntimeException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void twoCatchBlocksSecondCatchesException() {
        try {
            doThrow();
        } catch (NullPointerException e) {

        } catch (RuntimeException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void caughtExceptionWithFinallyBlock() {
        try {
            doThrow();
        } catch (RuntimeException e) {

        } finally {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void uncaughtExceptionWithFinallyBlock() {
        try {
            doThrow();
        } finally {

        }

        // block: ignore_end_status
    }

    @Call
    public void uncaughtExceptionWithNonEmptyFinallyBlock() {
        try {
            doThrow();
        } finally {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void coveredTryBlockAndUncaughtExceptionWithFinallyBlock() {
        try {
            doCall();
            doThrow();
        } finally {

        }

        // block: ignore_end_status
    }

    @Call
    public void uncaughtExceptionFromCatchBlock() {
        try {
            doThrow();
        } catch (RuntimeException e) {

            doThrow();
        }

        // block: ignore_end_status
    }

    @Call
    public void uncaughtExceptionFromCatchBlockWithFinallyBlock() {
        try {
            doThrow();
        } catch (RuntimeException e) {
            doThrow();
        } finally {

        }

        // block: ignore_end_status
    }

    @Call
    public void uncaughtExceptionFromFinallyBlock() {
        try {

        } finally {
            doThrow();
        }

        // block: ignore_end_status
    }

    @Call
    public void coveredTryBlockAndUncaughtExceptionFromFinallyBlock() {
        try {
            doCall();
        } finally {

            doThrow();
        }

        // block: ignore_end_status
    }

    @Call
    public void uncaughtExceptionFromCoveredFinallyBlock() {
        try {

        } finally {
            doCall();
            doThrow();
        }

        // block: ignore_end_status
    }

    @Call
    public void nonThrowingResource() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.dontThrow()) {

        }

        // block: ignore_end_status
    }

    @Call
    public void nullResource() {
        try (ThrowingAutoCloseable a = null) {

        }

        // block: ignore_end_status
    }

    @Call
    public void throwingOnInitResource() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnInit()) {

        }

        // block: ignore_end_status
    }

    @Call
    public void throwingOnInitResourceFromCoveredExpr() {
        try (ThrowingAutoCloseable a = MethodChain.create()
                .get(ThrowingAutoCloseable.throwOnInit())
        ) {

        }

        // block: ignore_end_status
    }

    @Call
    public void throwingOnInitResourceWithCatchBlock() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnInit()) {

        } catch (RuntimeException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void nonThrowingAndThrowingResourceOnInit() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.dontThrow();

             ThrowingAutoCloseable b = ThrowingAutoCloseable.throwOnInit()
        ) {

        }

        // block: ignore_end_status
    }

    @Call
    public void throwingOnCloseResource() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnClose()) {

        }

        // block: ignore_end_status
    }

    @Call
    public void throwingOnCloseResourceWithCatchBlock() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnClose()) {

        } catch (RuntimeException e) {

        }

        // block: ignore_end_status
    }

    @Call
    public void emptyTryBlocks() {
        try {

        } catch (RuntimeException e) {
            doCall();
        }

        try {

        } catch (NullPointerException e) {
            doCall();
        } catch (RuntimeException e) {
            doCall();
        }

        try {

        } catch (RuntimeException e) {
            doCall();
        } finally {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void emptyFinallyBlocks() {
        try {

        } finally {

        }

        try {
            doCall();
        } finally {

        }

        try {
            doCall();
        } catch (RuntimeException e) {

        } finally {

        }

        try {
            doThrow();
        } catch (RuntimeException e) {

        } finally {

        }
    }

    @Call
    public void spacingTest() {
        try

        {
            doCall();
        }

        catch

        (
                RuntimeException
                        e
        )

        {

        }

        finally

        {

        }

        // block: ignore_end_status
    }

}

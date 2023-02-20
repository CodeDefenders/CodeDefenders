import utils.Call;
import utils.MethodChain;

import static utils.Utils.doCall;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

/**
 * <p>Synchronized blocks
 * <p>JaCoCo coverage: Covers monitor expression. Covers the closing brace if it can be reached (i.e. not every path in
 *                     the block jumps).
 * <p>Extended coverage: Covers space before and after monitor expression as well.
 */
public class SynchronizedBlocks {
    @Call
    public void regularBlocks1() {
        synchronized (this) {

        }

        // block: ignore_end_status
    }

    @Call
    public void regularBlocks2() {
        synchronized (doGet(this)) {

        }

        // block: ignore_end_status
    }

    @Call
    public void regularBlocks3() {
        synchronized (this) {
            return;

        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInBlock1() {
        synchronized (this) {
            doThrow();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInBlock2() {
        synchronized (this) {
            doCall();
            doThrow();
        }

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionInMonitor1() {
        Object o = null;
        synchronized (o) {

        }

        // block: ignore_end_status
    }

    @Call(exception = NullPointerException.class)
    public void exceptionInMonitor2() {
        Object o = null;
        synchronized (
                doGet(o)
        ) {

        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInMonitor3() {
        synchronized (
                MethodChain.create()
                        .doThrow()
                        .get(this)
        ) {

        }

        // block: ignore_end_status
    }
}

import utils.Call;
import utils.MethodChain;

import static utils.Utils.doCall;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

public class SynchronizedBlocks {
    @Call
    public void regularBlocks() {
        synchronized (this) {

        }

        synchronized (doGet(this)) {

        }

        synchronized (this) {
            return;

        }
    }

    @Call
    public void exceptionInBlock1() {
        synchronized (this) {
            doThrow();
        }
    }

    @Call
    public void exceptionInBlock2() {
        synchronized (this) {
            doCall();
            doThrow();
        }
    }

    @Call(exception = NullPointerException.class)
    public void exceptionInMonitor1() {
        Object o = null;
        synchronized (o) {

        }
    }

    @Call(exception = NullPointerException.class)
    public void exceptionInMonitor2() {
        Object o = null;
        synchronized (
                doGet(o)
        ) {

        }
    }

    @Call
    public void exceptionInMonitor3() {
        synchronized (
                MethodChain.create()
                        .doThrow()
                        .get(this)
        ) {

        }
    }
}

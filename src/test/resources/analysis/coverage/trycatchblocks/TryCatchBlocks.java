import utils.Call;
import utils.MethodChain;
import utils.ThrowingAutoCloseable;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

public class TryCatchBlocks {
    @Call
    public void tryWithOneCatchBlock() {
        try {
            doCall();
        } catch (RuntimeException e) {

        }

        try {
            doCall();
            doThrow();
        } catch (RuntimeException e) {

        }

        try {
            doThrow();
        } catch (RuntimeException e) {

        }
    }

    @Call
    public void tryWithTwoCatchBlocks() {
        try {
            doCall();
        } catch (NullPointerException e) {

        } catch (RuntimeException e) {

        }

        try {
            doCall();
            doThrow();
        } catch (NullPointerException e) {

        } catch (RuntimeException e) {

        }

        try {
            String s = null;
            s.toString();
        } catch (NullPointerException e) {

        } catch (RuntimeException e) {

        }

        try {
            doThrow();
        } catch (NullPointerException e) {

        } catch (RuntimeException e) {

        }
    }

    @Call
    public void finallyBlocks() {
        try {
            doCall();
        } finally {
            doCall();
        }

        try {
            doCall();
        } catch (RuntimeException e) {

        } finally {
            doCall();
        }

        try {
            doThrow();
        } catch (RuntimeException e) {

        } finally {
            doCall();
        }
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
    }

    @Call
    public void emptyFinallyBlocks() {
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
    public void resources() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.dontThrow()) {

        }

        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnInit()) {

        } catch (RuntimeException e) {

        }

        // covered init that throws
        try (ThrowingAutoCloseable a = MethodChain.create()
                .get(ThrowingAutoCloseable.throwOnInit())) {

        } catch (RuntimeException e) {

        }
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.dontThrow();
             ThrowingAutoCloseable b = ThrowingAutoCloseable.throwOnInit()) {

        } catch (RuntimeException e) {

        }

        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnClose()) {

        } catch (RuntimeException e) {

        }
    }

    @Call
    public void uncaughtException1() {
        try {
            doThrow();
        } finally {

        }
    }

    @Call
    public void uncaughtException2() {
        try {
            doCall();
            doThrow();
        } finally {

        }
    }

    @Call
    public void uncaughtException3() {
        try {
            doThrow();
        } catch (NullPointerException e) {

        }
    }

    @Call
    public void uncaughtException4() {
        try {
            doCall();
            doThrow();
        } catch (NullPointerException e) {

        }
    }

    @Call
    public void uncaughtExceptionInCatchBlock1() {
        try {
            doCall();
            doThrow();
        } catch (RuntimeException e) {
            doThrow();
        }
    }

    @Call
    public void uncaughtExceptionInCatchBlock2() {
        try {
            doThrow();
        } catch (RuntimeException e) {
            doThrow();
        }
    }

    @Call
    public void uncaughtExceptionInCatchBlock3() {
        try {
            doThrow();
        } catch (RuntimeException e) {
            doCall();
            doThrow();
        }
    }

    @Call
    public void uncaughtExceptionInFinallyBlock1() {
        try {
            doCall();
        } finally {
            doThrow();
        }
    }

    @Call
    public void uncaughtExceptionInFinallyBlock2() {
        try {

        } finally {
            doThrow();
        }
    }

    @Call
    public void uncaughtExceptionInFinallyBlock3() {
        try {

        } finally {
            doCall();
            doThrow();
        }
    }

    @Call
    public void uncaughtExceptionInResources1() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnInit()) {

        }
    }

    @Call
    public void uncaughtExceptionInResources2() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.dontThrow();
             ThrowingAutoCloseable b = ThrowingAutoCloseable.throwOnInit()) {

        }
    }

    @Call
    public void uncaughtExceptionInResources3() {
        try (ThrowingAutoCloseable a = ThrowingAutoCloseable.throwOnClose()) {

        } // NOT_COVERED because 0/2 branch coverage. don't know why
    }
}

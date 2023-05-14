package utils;

import static utils.Utils.doThrow;

public class ThrowingAutoCloseable implements AutoCloseable {
    private boolean throwOnCLose;

    public static ThrowingAutoCloseable dontThrow() {
        return new ThrowingAutoCloseable(false, false);
    }

    public static ThrowingAutoCloseable throwOnInit() {
        return new ThrowingAutoCloseable(true, false);
    }

    public static ThrowingAutoCloseable throwOnClose() {
        return new ThrowingAutoCloseable(false, true);
    }

    public ThrowingAutoCloseable(boolean throwOnInit, boolean throwOnClose) {
        if (throwOnInit) {
            doThrow();
        }
        this.throwOnCLose = throwOnClose;
    }

    @Override
    public void close() {
        if (throwOnCLose) {
            doThrow();
        }
    }
}


package utils;

import utils.TestRuntimeException;

public class ThrowingClass {
    public ThrowingClass() {
        throw new TestRuntimeException();
    }

    public ThrowingClass(int i) {
        throw new TestRuntimeException();
    }

    public ThrowingClass(int i, int j) {
        throw new TestRuntimeException();
    }
}

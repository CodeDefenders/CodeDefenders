package utils;

import utils.TestRuntimeException;

public class ThrowingClass {
    public ThrowingClass() {
        throw new TestRuntimeException();
    }
}

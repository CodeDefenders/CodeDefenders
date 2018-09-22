package org.codedefenders.execution;

/**
 * This exception marks general errors during compiling.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public class CompileException extends Exception {
    public CompileException() {
    }

    public CompileException(String message) {
        super(message);
    }

    public CompileException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompileException(Throwable cause) {
        super(cause);
    }

    public CompileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

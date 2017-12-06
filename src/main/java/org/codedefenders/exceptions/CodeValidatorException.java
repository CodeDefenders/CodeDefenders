package org.codedefenders.exceptions;

public class CodeValidatorException extends Exception {

    private static final long serialVersionUID = -4526964014168540391L;

    public CodeValidatorException() {
        super();
    }

    public CodeValidatorException(String message) {
        super(message);
    }

    public CodeValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

}

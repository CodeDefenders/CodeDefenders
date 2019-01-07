package org.codedefenders.validation.code;

public enum CodeValidatorLevel {
    RELAXED,
    MODERATE,
    STRICT;

    /**
     * Similar to {@link #valueOf(String)} but returns {@code null} if
     * {@link #valueOf(String) valueOf()} does not match.
     *
     * @param name the name of the requested enum.
     * @return the enum for the given name, or {@code null} if none was found.
     */
    public static CodeValidatorLevel valueOrNull(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

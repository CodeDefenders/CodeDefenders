package org.codedefenders.model;

/**
 * This class represents key maps users can choose from.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
public enum KeyMap {
    DEFAULT("default"),
    SUBLIME("sublime"),
    VIM("vim"),
    EMACS("emacs");

    private final String codemirrorName;

    KeyMap(String codemirrorName) {
        this.codemirrorName = codemirrorName;
    }

    /**
     * Returns the internal name used by codemirror.
     * See <a href="https://codemirror.net/doc/manual.html#keymaps">the codemirror section on keymaps</a>.
     *
     * @return the internal name used by codemirror.
     */
    public String getCMName() {
        return codemirrorName;
    }

    /**
     * Similar to {@link #valueOf(String)} but returns {@link #DEFAULT} if
     * {@link #valueOf(String) valueOf()} does not match.
     *
     * @param name the name of the requested enum.
     * @return the enum for the given name, or {@link #DEFAULT} if none was found.
     */
    public static KeyMap valueOrDefault(String name) {
        KeyMap keyMap;
        try {
            keyMap = valueOf(name.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            keyMap = DEFAULT;
        }
        return keyMap;
    }
}

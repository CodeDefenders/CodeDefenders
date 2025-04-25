/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.model;

/**
 * This class represents key maps users can choose from.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
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

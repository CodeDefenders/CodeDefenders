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
package org.codedefenders.analysis.coverage.util;

import java.util.Map;

public class InMemoryClassLoader extends ClassLoader {
    private final Map<String, byte[]> definitions;

    public InMemoryClassLoader(Map<String, byte[]> definitions) {
        this.definitions = definitions;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
            throws ClassNotFoundException {
        if (definitions.containsKey(name)) {
            final byte[] bytes = definitions.get(name);
            return defineClass(name, bytes, 0, bytes.length);
        } else {
            return super.loadClass(name, resolve);
        }
    }
}

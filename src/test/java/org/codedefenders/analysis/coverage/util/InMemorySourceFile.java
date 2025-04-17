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

import java.io.IOException;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

// Adapted from https://github.com/trung/InMemoryJavaCompiler
public class InMemorySourceFile extends SimpleJavaFileObject {
    private final String contents;
    private final String className;

    public InMemorySourceFile(String className, String contents) throws Exception {
        super(URI.create("string:///" + className.replace('.', '/')
                + Kind.SOURCE.extension), Kind.SOURCE);
        this.contents = contents;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return contents;
    }
}

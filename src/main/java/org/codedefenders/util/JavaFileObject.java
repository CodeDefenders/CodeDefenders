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
package org.codedefenders.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.tools.SimpleJavaFileObject;

/**
 * {@link SimpleJavaFileObject} implementation, which allows for reading file content
 * from memory (by calling constructor with path <i>and</i> content) or reading
 * the file content from the hard-disk (by calling constructor just with path).
 *
 * <p>Inherited attributes {@code uri} and {@code kind}.
 *
 * <p>Useful methods: {@link #getName()}
 */
public class JavaFileObject extends SimpleJavaFileObject {
    private String path;
    private String content;

    /**
     * Constructor for reading file content.
     *
     * @param path File path.
     */
    public JavaFileObject(String path) {
        super(new File(path).toURI(), javax.tools.JavaFileObject.Kind.SOURCE);
        this.path = path;
        this.content = null;
    }

    /**
     * Constructor with file content already given.
     *
     * @param path    File path.
     * @param content File content.
     */
    public JavaFileObject(String path, String content) {
        super(new File(path).toURI(), javax.tools.JavaFileObject.Kind.SOURCE);
        this.path = path;
        this.content = content;
    }

    /**
     * Returns the content of the java file. If no content is specified yet, the
     * file content is read from the hard disk.
     *
     * @param ignoreEncodingErrors ignored to match parent method signature
     * @return the content of the java file.
     * @throws IOException when reading the file fails.
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        if (content == null) {
            content = new String(Files.readAllBytes(Paths.get(this.uri)));
        }
        return content;
    }

    /**
     * @return the path of the file as a String.
     */
    public String getPath() {
        return path;
    }

    /**
     * Return the content of the java file as a {@link String}. If no content
     * is specified, {@code null} is returned.
     *
     * @return the content of the file, or {@code null}.
     */
    public String getContent() {
        return content;
    }

    @Override
    public String getName() {
        return Paths.get(this.uri).getFileName().toString();
    }
}

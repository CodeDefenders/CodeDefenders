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

import java.nio.file.Path;

/**
 * Data class to store in-memory files.
 * Can be either in-memory representations of existing files or fully transient.
 *
 */
public class SimpleFile {
    private final Path path;
    private final byte[] content;

    private String stringContent;

    public SimpleFile(Path path, byte[] content) {
        this.content = content;
        this.path = path;
        this.stringContent = null;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentAsString() {
        if (stringContent == null) {
            stringContent = new String(content);
        }
        return stringContent;
    }

    /**
     * Returns the path of the file. The path may not lead to an existing file.
     */
    public Path getPath() {
        return path;
    }

    public String getFilename() {
        return path.getFileName().toString();
    }
}

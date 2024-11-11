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

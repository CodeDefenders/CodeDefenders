package org.codedefenders.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.tools.SimpleJavaFileObject;

/**
 * {@link SimpleJavaFileObject} implementation, which allows for reading file content
 * from memory (by calling constructor with path <i>and</i> content) or reading
 * the file content from the hard-disk (by calling constructor just with path).
 * <p>
 * Inherited attributes {@code uri} and {@code kind}.
 * <p>
 * Useful methods: {@link #getName()}
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
        super(URI.create("file:///" + path), javax.tools.JavaFileObject.Kind.SOURCE);
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
        super(URI.create("file:///" + path), javax.tools.JavaFileObject.Kind.SOURCE);
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
            content = String.join("\n", Files.readAllLines(Paths.get(uri)));
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
    };
}

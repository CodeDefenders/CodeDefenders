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

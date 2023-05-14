package org.codedefenders.analysis.coverage.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

// Adapted from https://github.com/trung/InMemoryJavaCompiler
public class InMemoryClassFile extends SimpleJavaFileObject {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final String className;

    public InMemoryClassFile(String className) throws Exception {
        super(new URI(className), Kind.CLASS);
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return outputStream;
    }

    public byte[] getByteCode() {
        return outputStream.toByteArray();
    }
}

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

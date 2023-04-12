package org.codedefenders.analysis.coverage.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

// Adapted from https://github.com/trung/InMemoryJavaCompiler
public class InMemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final List<InMemoryClassFile> classFiles;

    public InMemoryJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
        this.classFiles = new ArrayList<>();
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
                                               JavaFileObject.Kind kind, FileObject sibling) {
        try {
            InMemoryClassFile classFile = new InMemoryClassFile(className);
            classFiles.add(classFile);
            return classFile;
        } catch (Exception e) {
            throw new RuntimeException("Error while creating in-memory output file for " + className, e);
        }
    }

    public Map<String, byte[]> getClassFiles() {
        return classFiles.stream()
                .collect(Collectors.toMap(
                        InMemoryClassFile::getClassName,
                        InMemoryClassFile::getByteCode
                ));
    }
}

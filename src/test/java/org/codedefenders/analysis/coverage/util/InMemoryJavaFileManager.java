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

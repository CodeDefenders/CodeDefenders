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
package org.codedefenders.util.concurrent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.model.Dependency;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.util.FileUtils;

public class EditorUtils {
    public static HashMap<String, String> getDependencyHashMap(int clazzId, GameClassRepository gameClassRepo) {
        HashMap<String, String> dependencies = new HashMap<>();

        List<Dependency> dependencyList = gameClassRepo.getMappedDependenciesForClassId(clazzId);
        List<String> names = new ArrayList<>();
        boolean duplicate = false;
        for (Dependency dependency : dependencyList) {
            String name = FileUtils.extractFileNameNoExtension(Paths.get(dependency.getJavaFile()));
            if (names.contains(name)) {
                duplicate = true;
                break;
            }
            names.add(name);
        }

        for (Dependency dependency : dependencyList) {
            Path path = Paths.get(dependency.getJavaFile());
            String className = FileUtils.extractFileNameNoExtension(path);
            String classCode = StringEscapeUtils.escapeHtml4(FileUtils.readJavaFileWithDefault(path));

            if (duplicate) {
                String packageName = FileUtils.getPackageNameFromJavaFile(classCode);
                className = packageName.isEmpty() ? className : packageName + "." + className;
            }
            dependencies.put(className, classCode);
        }
        return dependencies;
    }
}

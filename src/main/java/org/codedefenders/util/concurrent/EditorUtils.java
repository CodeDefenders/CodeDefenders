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

package org.codedefenders.beans.game;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.model.Dependency;
import org.codedefenders.util.FileUtils;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Provides data for the class viewer game component.</p>
 * <p>Bean Name: {@code classViewer}</p>
 */
@ManagedBean
@RequestScoped
public class ClassViewerBean {
    String className;
    String classCode;
    Map<String, String> dependencies;

    public ClassViewerBean() {
        className = null;
        classCode = null;
        dependencies = new HashMap<>();
    }

    public void setGameClass(GameClass clazz) {
        className = clazz.getName();
        classCode = StringEscapeUtils.escapeHtml(clazz.getSourceCode());
    }

    public void setDependenciesForClass(GameClass clazz) {
        for (Dependency dependency : GameClassDAO.getMappedDependenciesForClassId(clazz.getId())) {
            Path path = Paths.get(dependency.getJavaFile());
            String className = FileUtils.extractFileNameNoExtension(path);
            String classCode = StringEscapeUtils.escapeHtml(FileUtils.readJavaFileWithDefault(path));
            dependencies.put(className, classCode);
        }
    }

    public String getClassName() {
        return className;
    }

    public String getClassCode() {
        return classCode;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }
}

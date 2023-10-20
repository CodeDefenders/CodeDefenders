package org.codedefenders.beans.game;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.model.Dependency;
import org.codedefenders.util.FileUtils;

/**
 * <p>Provides data for the class viewer game component.</p>
 * <p>Bean Name: {@code classViewer}</p>
 */
@ManagedBean
@RequestScoped
public class ClassViewerBean {
    /**
     * The name of the class to display.
     */
    private String className;

    /**
     * The source code of the class to display.
     */
    private String classCode;

    /**
     * The dependencies of the class to display, mapped by their names.
     * Can be empty, but must not be {@code null}.
     */
    private Map<String, String> dependencies;

    public ClassViewerBean() {
        className = null;
        classCode = null;
        dependencies = new HashMap<>();
    }

    /**
     * Sets the className and classCode. Since {@link GameClass#getName()} contains the fully qualified name, the
     * package information must be removed.
     * @param clazz The class under test
     */
    public void setClassCode(GameClass clazz) {
        String[] split = clazz.getName().split("\\.");
        className = split[split.length - 1];
        classCode = StringEscapeUtils.escapeHtml4(clazz.getSourceCode());
    }

    public void setDependenciesForClass(GameClass clazz) {
        for (Dependency dependency : GameClassDAO.getMappedDependenciesForClassId(clazz.getId())) {
            Path path = Paths.get(dependency.getJavaFile());
            String className = FileUtils.extractFileNameNoExtension(path);
            String classCode = StringEscapeUtils.escapeHtml4(FileUtils.readJavaFileWithDefault(path));
            dependencies.put(className, classCode);
        }
    }

    // --------------------------------------------------------------------------------

    public String getClassName() {
        return className;
    }

    /**
     * Returns the HTML-escaped code of the class.
     * @return The HTML-escaped code of the class.
     */
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

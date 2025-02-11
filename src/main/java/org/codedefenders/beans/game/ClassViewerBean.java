package org.codedefenders.beans.game;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.game.GameClass;
import org.codedefenders.model.Dependency;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.concurrent.EditorUtils;

/**
 * <p>Provides data for the class viewer game component.</p>
 * <p>Bean Name: {@code classViewer}</p>
 */
@Named("classViewer")
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

    private final GameClassRepository gameClassRepo;

    @Inject
    public ClassViewerBean(GameClassRepository gameClassRepo, GameProducer gameProducer) {
        this.gameClassRepo = gameClassRepo;
        var game = gameProducer.getGame();
        if (game != null) {
            init(game.getCUT());
        }
    }

    public void init(GameClass clazz) {
        setClassCode(clazz);
        setDependenciesForClass(clazz);
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
        dependencies = EditorUtils.getDependencyHashMap(clazz.getId(), gameClassRepo);
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

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
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.model.Dependency;
import org.codedefenders.util.FileUtils;

/**
 * <p>Provides data for the mutant editor game component.</p>
 * <p>Bean Name: {@code mutantEditor}</p>
 */
@ManagedBean
@RequestScoped
public class MutantEditorBean {
    /**
     * The class name of the mutant.
     */
    private String className;

    /**
     * The mutant code to display.
     */
    private String mutantCode;

    /**
     * The dependencies of the mutant to display, mapped by their names.
     * Can be empty, but must not be {@code null}.
     */
    private Map<String, String> dependencies;

    /**
     * Start of editable lines in the mutant.
     * If {@code null}, the code can be modified from the start.
     */
    private Integer editableLinesStart;

    /**
     * End of editable lines in the mutant.
     * If {@code null}, the code can be modified until the end.
     */
    private Integer editableLinesEnd;

    public MutantEditorBean() {
        className = null;
        mutantCode = null;
        dependencies = new HashMap<>();
        editableLinesStart = null;
        editableLinesEnd = null;
    }

    /**
     * Sets the className. This method is called upon the information of a {@link GameClass}. Since
     * {@link GameClass#getName()} contains the fully qualified name, the package information must be removed.
     * @param className The fully qualified name of the class
     */
    public void setClassName(String className) {
        String[] split = className.split("\\.");
        this.className = split[split.length - 1];
    }

    public void setMutantCodeForClass(GameClass clazz) {
        mutantCode = StringEscapeUtils.escapeHtml4(clazz.getSourceCode());
    }

    /**
     * Sets the code for the mutant editor from the previous submission of the player.
     * @param previousMutantCode The code from the previous submission, not HTML-escaped.
     */
    public void setPreviousMutantCode(String previousMutantCode) {
        mutantCode = StringEscapeUtils.escapeHtml4(previousMutantCode);
    }

    public void setDependenciesForClass(GameClass clazz) {
        for (Dependency dependency : GameClassDAO.getMappedDependenciesForClassId(clazz.getId())) {
            Path path = Paths.get(dependency.getJavaFile());
            String className = FileUtils.extractFileNameNoExtension(path);
            String classCode = StringEscapeUtils.escapeHtml4(FileUtils.readJavaFileWithDefault(path));
            dependencies.put(className, classCode);
        }
    }

    public void setEditableLinesForPuzzle(Puzzle puzzle) {
        this.editableLinesStart = puzzle.getEditableLinesStart();
        this.editableLinesEnd = puzzle.getEditableLinesEnd();
    }

    // --------------------------------------------------------------------------------

    public String getClassName() {
        return className;
    }

    /**
     * Returns the HTML-escaped code of the mutant.
     * @return The HTML-escaped code of the mutant.
     */
    public String getMutantCode() {
        return mutantCode;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public int getEditableLinesStart() {
        return editableLinesStart == null ? 1 : editableLinesStart;
    }

    public int getEditableLinesEnd() {
        return editableLinesEnd;
    }

    public boolean hasEditableLinesStart() {
        return editableLinesStart != null;
    }

    public boolean hasEditableLinesEnd() {
        return editableLinesEnd != null;
    }
}

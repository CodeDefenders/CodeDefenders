package org.codedefenders.beans.game;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.model.Dependency;
import org.codedefenders.util.FileUtils;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Provides data for the test editor game component.</p>
 * <p>Bean Name: {@code testEditor}</p>
 */
@ManagedBean
@RequestScoped
public class TestEditorBean {
    /**
     * The test code to display.
     */
    private String testCode;

    /**
     * Start of editable lines in the test.
     * If {@code null}, the code can be modified from the start.
     */
    private Integer editableLinesStart;

    /**
     * End of editable lines in the test.
     * If {@code null}, the code can be modified until the end.
     * Currently not used.
     */
    private Integer editableLinesEnd;

    /**
     * Enable autocompletion for mockito methods.
     */
    private Boolean mockingEnabled;

    public TestEditorBean() {
        testCode = null;
        editableLinesStart = null;
        editableLinesEnd = null;
        mockingEnabled = null;
    }

    public void setTestCodeForClass(GameClass clazz) {
        testCode = StringEscapeUtils.escapeHtml(clazz.getTestTemplate());
    }

    /**
     * Sets the code for the test editor from the previous submission of the player.
     * @param previousTestCode The code from the previous submission, not HTML-escaped.
     */
    public void setPreviousTestCode(String previousTestCode) {
        testCode = StringEscapeUtils.escapeHtml(previousTestCode);
    }

    public void setEditableLinesForClass(GameClass clazz) {
        this.editableLinesStart = clazz.getTestTemplateFirstEditLine();
    }

    public void setEditableLinesForPuzzle(Puzzle puzzle) {
        this.editableLinesStart = puzzle.getEditableLinesStart();
        this.editableLinesEnd = puzzle.getEditableLinesEnd();
    }

    public void setMockingEnabled(boolean mockingEnabled) {
        this.mockingEnabled = mockingEnabled;
    }

    // --------------------------------------------------------------------------------

    /**
     * Returns the HTML-escaped code of the test.
     * @return The HTML-escaped code of the test.
     */
    public String getTestCode() {
        return testCode;
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

    public boolean isMockingEnbaled() {
        return mockingEnabled;
    }
}

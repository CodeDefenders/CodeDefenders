package org.codedefenders.beans.game;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameState;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.servlets.games.GameProducer;

/**
 * <p>Provides data for the test editor game component.</p>
 * <p>Bean Name: {@code testEditor}</p>
 */
@RequestScoped
@Named("testEditor")
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

    /**
     * Change autocompletion based on the assertion library used.
     */
    private AssertionLibrary assertionLibrary;

    /**
     * Whether the editor is readonly.
     */
    private boolean readonly;

    @Inject
    public TestEditorBean(GameProducer gameProducer, PreviousSubmissionBean previousSubmission) {
        AbstractGame game = gameProducer.getGame();
        GameClass cut = game.getCUT();

        setEditableLinesForClass(cut);

        if (game instanceof PuzzleGame) {
            setMockingEnabled(false);
            setReadonly(game.getState() == GameState.SOLVED);
        } else {
            setMockingEnabled(cut.isMockingEnabled());
        }

        setAssertionLibrary(cut.getAssertionLibrary());

        if (previousSubmission.hasTest()) {
            // TODO (from player-view): don't display the wrong previous submission for equivalence duels
            setPreviousTestCode(previousSubmission.getTestCode());
        } else {
            setTestCodeForClass(cut);
        }
    }

    public void setTestCodeForClass(GameClass clazz) {
        testCode = StringEscapeUtils.escapeHtml4(clazz.getTestTemplate());
    }

    /**
     * Sets the code for the test editor from the previous submission of the player.
     *
     * @param previousTestCode The code from the previous submission, not HTML-escaped.
     */
    public void setPreviousTestCode(String previousTestCode) {
        testCode = StringEscapeUtils.escapeHtml4(previousTestCode);
    }

    public void setEditableLinesForClass(GameClass clazz) {
        this.editableLinesStart = clazz.getTestTemplateFirstEditLine();
    }

    public void setMockingEnabled(boolean mockingEnabled) {
        this.mockingEnabled = mockingEnabled;
    }

    public void setAssertionLibrary(AssertionLibrary assertionLibrary) {
        this.assertionLibrary = assertionLibrary;
    }

    // --------------------------------------------------------------------------------

    /**
     * Returns the HTML-escaped code of the test.
     *
     * @return The HTML-escaped code of the test.
     */
    public String getTestCode() {
        return testCode;
    }

    public int getEditableLinesStart() {
        return editableLinesStart;
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

    public boolean isMockingEnabled() {
        return mockingEnabled;
    }

    public AssertionLibrary getAssertionLibrary() {
        return assertionLibrary;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}

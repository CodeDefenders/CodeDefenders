package org.codedefenders.beans;

import org.codedefenders.util.Constants;

public class Message {
    private String text;
    private boolean fadeOut;

    public Message(String text) {
        this.text = text;
        this.fadeOut = true;

        /* Move fade-out logic from header.jsp here for now (until I change the fade-out to be added explicitly). */
        if (text.equals(Constants.MUTANT_UNCOMPILABLE_MESSAGE)
            || text.equals(Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE)
            || text.equals(Constants.TEST_DID_NOT_COMPILE_MESSAGE)
            || (text.contains("Congratulations, your") && text.contains("solved the puzzle!"))) {
            this.fadeOut = false;
        }
    }

    public String getText() {
        return text;
    }

    public boolean isFadeOut() {
        return fadeOut;
    }

    /* Builder-style setter methods. */
    public void fadeOut(boolean fadeOut) {
        this.fadeOut = fadeOut;
    }
}

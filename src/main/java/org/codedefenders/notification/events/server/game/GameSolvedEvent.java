package org.codedefenders.notification.events.server.game;

import com.google.gson.annotations.Expose;

public class GameSolvedEvent extends GameLifecycleEvent {
    @Expose
    private boolean isAttackPuzzle;

    public boolean isAttackPuzzle() {
        return isAttackPuzzle;
    }

    public void setAttackPuzzle(boolean attackPuzzle) {
        isAttackPuzzle = attackPuzzle;
    }
}
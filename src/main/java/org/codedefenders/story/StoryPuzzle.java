package org.codedefenders.story;

/**
 * Created by joe on 04/04/2017.
 */

import org.codedefenders.PuzzleMode;

public class StoryPuzzle {

    private int puzzleId;
    private int levelNum;
    private int puzzleNum;
    private String puzzleName;
    private String hint;
    private String desc;
    private PuzzleMode mode;

    public StoryPuzzle (int levelNum, int puzzleNum, String puzzleName, String hint, String desc, PuzzleMode mode) {

        this.levelNum = levelNum;
        this.puzzleNum = puzzleNum;
        this.puzzleName = puzzleName;
        this.hint = hint;
        this.desc = desc;
        this.mode = mode;

    }

    public StoryPuzzle (int puzzleId) {

        this.puzzleId = puzzleId;

    }

    public int getPuzzleId() { return puzzleId; }

    public int getLevelNum() { return levelNum; }

    public int getPuzzleNum() { return puzzleNum; }

    public String getPuzzleName() { return puzzleName; }

    public String getHint() { return hint; }

    public String getDesc() { return desc; }

    public PuzzleMode getMode() { return mode; }

    public void setMode(PuzzleMode newMode) { this.mode = newMode; }

}

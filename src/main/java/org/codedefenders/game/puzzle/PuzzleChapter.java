package org.codedefenders.game.puzzle;

/**
 * Represents a group of {@link Puzzle puzzles}, which belong together.
 * @see Puzzle
 */
public class PuzzleChapter {

    /**
     * ID of the chapter.
     */
    private int chapterId;

    /**
     * Position of the chapter in the chapter list. Can be null.
     */
    private Integer position;

    /**
     * Title of the chapter. Can be null.
     */
    private String title;

    /**
     * Description of the chapter. Can be null.
     */
    private String description;

    /**
     * @param chapterId ID of the chapter in the database.
     * @param position Position of the chapter in the chapter list. Can be null.
     * @param title Title of the chapter. Can be null.
     * @param description Description of the chapter. Can be null.
     */
    public PuzzleChapter(int chapterId,
                         Integer position,
                         String title,
                         String description) {
        this.chapterId = chapterId;
        this.position = position;
        this.title = title;
        this.description = description;
    }

    public int getChapterId() {
        return chapterId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

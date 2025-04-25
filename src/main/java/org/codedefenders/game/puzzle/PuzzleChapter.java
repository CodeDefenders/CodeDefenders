/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
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
     * Constructs a new puzzle chapter instance.
     *
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

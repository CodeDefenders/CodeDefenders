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

import java.time.LocalDate;

/**
 * Represents a group of {@link Puzzle puzzles}, which belong together.
 *
 * @see Puzzle
 */
public class PuzzleChapter {

    /**
     * ID of the chapter.
     */
    private int id;

    /**
     * Position of the chapter in the chapter list. Can be null.
     */
    private Integer position;

    private LocalDate calendarDate;


    public PuzzleChapter(int id, Integer position) {
        this(id, position, null);
    }

    /**
     * Constructs a new puzzle chapter instance.
     *
     * @param id           ID of the chapter in the database.
     * @param position     Position of the chapter in the chapter list. Can be null.
     * @param calendarDate The first day of a seasonal puzzle chapter (for example, December 1st, any year,
     *                     for an advent calendar). If null, the chapter is always available.
     *                     Always refers to the local Date of the server.
     */
    public PuzzleChapter(int id, Integer position, LocalDate calendarDate) {
        this.id = id;
        this.position = position;
        this.calendarDate = calendarDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int chapterId) {
        this.id = chapterId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public LocalDate getCalendarDate() {
        return calendarDate;
    }

    public void setCalendarDate(LocalDate calendarDate) {
        this.calendarDate = calendarDate;
    }

    public boolean isVisible() {
        if (calendarDate == null) {
            return true;
        } else {
            return calendarDate.withYear(2000).plusDays(-1).isBefore(LocalDate.now().withYear(2000))
                    && calendarDate.withYear(2000).plusDays(30).isAfter(LocalDate.now().withYear(2000));
        }
    }

    /**
     * Returns the seasonal style for this chapter. Returns an empty String for a regular puzzle chapter.
     * The style String should be identical to the css-class used for that season.
     */
    public String getSeasonalStyle() {
        if (calendarDate != null) {
            return "christmas-season";
        } else {
            return "";//TODO Configurable for other seasons
        }
    }
}

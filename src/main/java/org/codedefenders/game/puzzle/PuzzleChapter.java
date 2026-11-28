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
    private static final int DEFAULT_DAYS_AVAILABLE = 30;

    /**
     * ID of the chapter.
     */
    private int id;

    /**
     * Position of the chapter in the chapter list. Can be null.
     */
    private Integer position;

    private LocalDate calendarDate;
    private int daysAvailable;


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
        this(id, position, calendarDate, DEFAULT_DAYS_AVAILABLE);
    }

    /**
     * Constructs a new puzzle chapter instance.
     *
     * @param id           ID of the chapter in the database.
     * @param position     Position of the chapter in the chapter list. Can be null.
     * @param calendarDate The first day of a seasonal puzzle chapter. If null, the chapter is always available.
     * @param daysAvailable Number of days the seasonal chapter is available from its start date.
     */
    public PuzzleChapter(int id, Integer position, LocalDate calendarDate, int daysAvailable) {
        this.id = id;
        this.position = position;
        this.calendarDate = calendarDate;
        this.daysAvailable = daysAvailable;
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

    public boolean isSeasonalChapter() {
        return calendarDate != null;
    }

    public void setCalendarDate(LocalDate calendarDate) {
        this.calendarDate = calendarDate;
    }

    public int getDaysAvailable() {
        return daysAvailable;
    }

    public void setDaysAvailable(int daysAvailable) {
        this.daysAvailable = daysAvailable;
    }

    public boolean isVisible() {
        if (calendarDate == null) {
            return true;
        }
        var today = LocalDate.now();
        var seasonStart = getCurrentSeasonStartDate(today);
        return seasonStart.plusDays(-1).isBefore(today) && seasonStart.plusDays(daysAvailable).isAfter(today);
    }

    public LocalDate getCurrentSeasonStartDate(LocalDate today) {
        if (calendarDate == null) return null;
        var seasonStart = calendarDate.withYear(today.getYear());
        if (today.isBefore(seasonStart)) {
            var previousSeasonStart = calendarDate.withYear(today.getYear() - 1);
            if (!today.isBefore(previousSeasonStart.plusDays(-1))
                    && today.isBefore(previousSeasonStart.plusDays(daysAvailable))) {
                return previousSeasonStart;
            }
        }
        return seasonStart;
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

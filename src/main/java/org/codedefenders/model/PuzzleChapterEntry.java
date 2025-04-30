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
package org.codedefenders.model;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.servlets.games.puzzle.PuzzleOverview;

/**
 * This class is a wrapper around {@link PuzzleChapter} and a sorted set of
 * {@link PuzzleEntry}s. This class is used to display puzzles
 * or its games in the {@link PuzzleOverview}.
 *
 * <p>Implements {@link Comparable} by comparing the positions of the
 * puzzle chapters.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see PuzzleEntry
 */
public class PuzzleChapterEntry implements Comparable<PuzzleChapterEntry> {
    private PuzzleChapter chapter;
    private SortedSet<PuzzleEntry> puzzleEntries;

    public PuzzleChapterEntry(PuzzleChapter chapter, Collection<PuzzleEntry> puzzleEntries) {
        this.chapter = chapter;
        this.puzzleEntries = new TreeSet<>(puzzleEntries);
    }

    public PuzzleChapter getChapter() {
        return chapter;
    }

    /**
     * @return a non nullable sorted set of puzzle entries.
     */
    public SortedSet<PuzzleEntry> getPuzzleEntries() {
        return puzzleEntries;
    }

    @Override
    public int compareTo(PuzzleChapterEntry other) {
        final Integer pos1 = this.chapter.getPosition();
        final Integer pos2 = other.chapter.getPosition();
        if (pos1 == null) {
            return 1;
        }
        if (pos2 == null) {
            return -1;
        }
        return pos1 - pos2;
    }
}

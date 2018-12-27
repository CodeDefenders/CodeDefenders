package org.codedefenders.model;

import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.servlets.games.puzzle.PuzzleOverview;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class is a wrapper around {@link PuzzleChapter} and a sorted set of
 * {@link PuzzleEntry}s. This class is used to display puzzles
 * or its games in the {@link PuzzleOverview}.
 * <p>
 * Implements {@link Comparable} by comparing the positions of the
 * puzzle chapters.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see PuzzleEntry
 */
public class PuzzleChapterEntry implements Comparable {
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
    public int compareTo(Object o) {
        if (!(o instanceof PuzzleChapterEntry)) {
            return -1;
        }
        final PuzzleChapterEntry obj = (PuzzleChapterEntry) o;
        final Integer pos1 = this.chapter.getPosition();
        final Integer pos2 = obj.chapter.getPosition();
        if (pos1 == null) {
            return 1;
        }
        if (pos2 == null) {
            return -1;
        }
        return pos1 - pos2;
    }
}

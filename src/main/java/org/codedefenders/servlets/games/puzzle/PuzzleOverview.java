/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.servlets.games.puzzle;

import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.PuzzleChapterEntry;
import org.codedefenders.model.PuzzleEntry;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} handles to the overview page of {@link PuzzleGame}s.
 * <p>
 * {@code GET} requests redirect to the puzzle overview page.
 * <p>
 * Serves under {@code /puzzles}.
 *
 * @author <a href=https://github.com/werli>Phil Werli</a>
 * @see PuzzleGameSelectionManager
 * @see PuzzleGame
 * @see org.codedefenders.util.Paths#PUZZLE_OVERVIEW
 */
@WebServlet("/puzzles")
public class PuzzleOverview extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleOverview.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final int userId = ServletUtils.userId(request);

        final Set<PuzzleGame> activePuzzles = new HashSet<>(PuzzleDAO.getActivePuzzleGamesForUser(userId));

        final SortedSet<PuzzleChapterEntry> puzzles = PuzzleDAO.getPuzzleChapters()
                .stream()
                .map(toPuzzleChapterEntry(userId, activePuzzles))
                .collect(Collectors.toCollection(TreeSet::new));

        request.setAttribute("puzzleChapterEntries", puzzles);

        request.getRequestDispatcher(Constants.PUZZLE_OVERVIEW_VIEW_JSP).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("Unsupported POST request to /puzzles");
        // aborts request and sends 400
        super.doPost(req, resp);
    }

    /**
     * Helper function which converts a {@link PuzzleChapter} to a {@link PuzzleChapterEntry} for a
     * given userId and a set of active {@link PuzzleGame}s.
     * <p>
     * All {@link Puzzle}s for the chapter, which the given user has active games for, are replaced
     * with the active game.
     *
     * @param userId        the user who requested the puzzle.
     * @param activePuzzles a set of active puzzle games which replace puzzles.
     * @return a function which converts a puzzle chapter to a puzzle chapter entry.
     * @see PuzzleChapterEntry
     * @see #toPuzzleChapterEntry(int, Set)
     */
    private Function<PuzzleChapter, PuzzleChapterEntry> toPuzzleChapterEntry(int userId, Set<PuzzleGame> activePuzzles) {
        return puzzleChapter -> {
            final Set<PuzzleEntry> puzzleEntries = PuzzleDAO.getPuzzlesForChapterId(puzzleChapter.getChapterId())
                    .stream()
                    .map(toPuzzleEntry(userId, activePuzzles))
                    .collect(Collectors.toSet());
            return new PuzzleChapterEntry(puzzleChapter, puzzleEntries);
        };
    }

    /**
     * Helper function which converts a {@link Puzzle} to a {@link PuzzleEntry} for a given userId and a
     * set of active {@link PuzzleGame}s.
     *
     * If there exist an active game for this puzzle, the returned entry contains a {@link PuzzleGame} instance.
     * Otherwise, the returned entry contains a {@link Puzzle} instance.
     *
     * @param userId        the user who requested the puzzle. The puzzle may be locked for the user.
     * @param activePuzzles a set of active puzzle games which replace puzzles.
     * @return a function which converts a puzzle to a puzzle entry.
     * @see PuzzleEntry
     */
    private Function<Puzzle, PuzzleEntry> toPuzzleEntry(int userId, Set<PuzzleGame> activePuzzles) {
        return entry -> {
            for (PuzzleGame activePuzzle : activePuzzles) {
                if (activePuzzle.getPuzzleId() == entry.getPuzzleId()) {
                    return new PuzzleEntry(activePuzzle);
                }
            }
            boolean locked = isPuzzleLockedForUser(entry, userId);
            return new PuzzleEntry(entry, locked);
        };
    }

    /**
     * Returns whether a given puzzle is locked for a given user.
     *
     * @param puzzle the checked puzzle.
     * @param userId the user which the puzzle may be locked for.
     * @return {@link true} if the user can access the puzzle, {@link false} otherwise.
     */
    private static boolean isPuzzleLockedForUser(Puzzle puzzle, int userId) {
        return false; // TODO currently all puzzle are unlocked
    }
}

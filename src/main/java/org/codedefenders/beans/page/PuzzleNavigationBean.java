package org.codedefenders.beans.page;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.GameState;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.PuzzleChapterEntry;
import org.codedefenders.model.PuzzleEntry;
import org.codedefenders.persistence.database.PuzzleRepository;

/**
 * <p>Provides data for the puzzle navigation in the menu, or puzzle overview page.</p>
 * <p>Bean Name: {@code puzzleNavigation}</p>
 */
@RequestScoped
@Named("puzzleNavigation")
public class PuzzleNavigationBean {
    PuzzleRepository puzzleRepo;

    SortedSet<PuzzleChapterEntry> puzzleChapters;
    Optional<PuzzleEntry> nextPuzzle;

    @Inject
    public PuzzleNavigationBean(PuzzleRepository puzzleRepo, CodeDefendersAuth login) {
        this.puzzleRepo = puzzleRepo;

        final Set<PuzzleGame> activePuzzles = new HashSet<>(puzzleRepo.getActivePuzzleGamesForUser(login.getUserId()));

        puzzleChapters = puzzleRepo.getPuzzleChapters()
                .stream()
                .map(toPuzzleChapterEntry(login.getUserId(), activePuzzles))
                .collect(Collectors.toCollection(TreeSet::new));

        final List<PuzzleEntry> unsolvedPuzzles = puzzleChapters.stream()
                .flatMap(puzzleChapterEntry -> puzzleChapterEntry.getPuzzleEntries().stream())
                .filter(Predicate.not(PuzzleEntry::isSolved))
                .toList();
        nextPuzzle = unsolvedPuzzles.stream().findFirst();

        // lock puzzles if the user has not solved the previous puzzle
        unsolvedPuzzles.stream()
                .filter(puzzleEntry -> puzzleEntry.getType() == PuzzleEntry.Type.PUZZLE)
                .filter(puzzleEntry -> !nextPuzzle.map(puzzleEntry::equals).orElse(true))
                .forEach(PuzzleEntry::lock);
    }

    /**
     * A set of puzzle chapters, which contains chapter information and all associated puzzles.
     * The set is sorted ascending by puzzle chapters position.
     * Associated puzzles are sorted by the puzzle identifier.
     *
     * @return a set of puzzle chapters and their associated puzzles.
     */
    public SortedSet<PuzzleChapterEntry> getPuzzleChapters() {
        return puzzleChapters;
    }

    /**
     * Checks if there is a next puzzle to be solved by the user.
     *
     * @return {@code true} if there is a next puzzle to be solved by the user,
     * {@code false} if there are no unsolved puzzles left.
     */
    public boolean hasNextPuzzle() {
        return nextPuzzle.isPresent();
    }

    /**
     * The next puzzle to be solved by the user.
     * This puzzle is the first puzzle in the set of puzzles that was not solved yet.
     *
     * @return the next puzzle to be solved by the user.
     */
    public Optional<PuzzleEntry> getNextPuzzle() {
        return nextPuzzle;
    }

    /**
     * Helper function which converts a {@link PuzzleChapter} to a {@link PuzzleChapterEntry} for a
     * given userId and a set of active {@link PuzzleGame}s.
     *
     * <p>All {@link Puzzle}s for the chapter, which the given user has active games for, are replaced
     * with the active game.
     *
     * @param userId        the user who requested the puzzle.
     * @param activePuzzles a set of active puzzle games which replace puzzles.
     * @return a function which converts a puzzle chapter to a puzzle chapter entry.
     * @see PuzzleChapterEntry
     * @see #toPuzzleChapterEntry(int, Set)
     */
    private Function<PuzzleChapter, PuzzleChapterEntry> toPuzzleChapterEntry(int userId,
                                                                             Set<PuzzleGame> activePuzzles) {
        return puzzleChapter -> {
            final Set<PuzzleEntry> puzzleEntries = puzzleRepo.getPuzzlesForChapterId(puzzleChapter.getChapterId())
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
     * <p>If there exist an active game for this puzzle, the returned entry contains a {@link PuzzleGame} instance.
     * Otherwise, the returned entry contains a {@link Puzzle} instance.
     *
     * @param userId        the user who requested the puzzle. The puzzle may be locked for the user.
     * @param activePuzzles a set of active puzzle games which replace puzzles.
     * @return a function which converts a puzzle to a puzzle entry.
     * @see PuzzleEntry
     */
    private Function<Puzzle, PuzzleEntry> toPuzzleEntry(int userId, Set<PuzzleGame> activePuzzles) {
        return entry -> {
            int pid = entry.getPuzzleId();
            for (PuzzleGame activePuzzle : activePuzzles) {
                if (activePuzzle.getPuzzleId() == pid) {
                    boolean solved = activePuzzle.getState().equals(GameState.SOLVED);
                    return new PuzzleEntry(activePuzzle, solved);
                }
            }

            PuzzleGame puzzleGame = puzzleRepo.getLatestPuzzleGameForPuzzleAndUser(entry.getPuzzleId(), userId);
            boolean solved = puzzleGame != null && puzzleGame.getState().equals(GameState.SOLVED);
            int tries = puzzleGame != null ? puzzleGame.getCurrentRound() : 0;
            return new PuzzleEntry(entry, false, solved, tries);
        };
    }
}


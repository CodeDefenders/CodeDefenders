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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.game.puzzle.PuzzleType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.admin.api.AdminPuzzleAPI;
import org.codedefenders.servlets.admin.api.AdminPuzzleAPI.UpdatePuzzlePositionsData;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.QueryUtils.extractBatchParams;
import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * This class handles the database logic for puzzles.
 *
 * @see Puzzle
 * @see PuzzleChapter
 * @see PuzzleGame
 */
@Named("puzzleRepo")
@ApplicationScoped
public class PuzzleRepository {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public PuzzleRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Returns the {@link PuzzleChapter} for the given chapter ID.
     *
     * @param chapterId The chapter ID.
     * @return The {@link PuzzleChapter} for the given chapter ID.
     */
    public PuzzleChapter getPuzzleChapterForId(int chapterId) {
        @Language("SQL") String query = """
                SELECT *
                FROM puzzle_chapters
                WHERE Chapter_ID = ?;
        """;

        var chapter = queryRunner.query(query,
                oneFromRS(PuzzleRepository::puzzleChapterFromRS),
                chapterId
        );
        return chapter.orElse(null);
    }

    /**
     * Returns a {@link List} of all {@link PuzzleChapter PuzzleChapters}, sorted by the position in the chapter list.
     *
     * @return A {@link List} of all {@link PuzzleChapter PuzzleChapters}, sorted by the position in the chapter list.
     */
    public List<PuzzleChapter> getPuzzleChapters() {
        @Language("SQL") String query = """
                SELECT *
                FROM puzzle_chapters
                ORDER BY Position;
        """;

        return queryRunner.query(query,
                listFromRS(PuzzleRepository::puzzleChapterFromRS)
        );
    }

    /**
     * Returns the {@link Puzzle} for the given puzzle ID.
     *
     * @param puzzleId The puzzle ID.
     * @return The {@link Puzzle} for the given puzzle ID.
     */
    public Puzzle getPuzzleForId(int puzzleId) {
        @Language("SQL") String query = """
                SELECT *
                FROM puzzles
                WHERE Puzzle_ID = ?;
        """;

        var puzzle = queryRunner.query(query,
                oneFromRS(PuzzleRepository::puzzleFromRS),
                puzzleId
        );
        return puzzle.orElse(null);
    }

    /**
     * Returns a {@link List} of all {@link Puzzle Puzzles}, sorted by the chapter ID and position in the chapter.
     *
     * @return A {@link List} of all {@link Puzzle Puzzles}, sorted by the chapter ID and position in the chapter.
     */
    public List<Puzzle> getPuzzles() {
        @Language("SQL") String query = """
                SELECT *
                FROM view_active_puzzles as puzzles
                ORDER BY Chapter_ID, Position;
        """;

        return queryRunner.query(query,
                listFromRS(PuzzleRepository::puzzleFromRS)
        );
    }

    /**
     * Returns all unassigned {@link Puzzle Puzzles}, sorted by position.
     */
    public List<Puzzle> getUnassignedPuzzles() {
        @Language("SQL") String query = """
                SELECT *
                FROM puzzles
                WHERE Chapter_ID IS NULL
                ORDER BY Position;
        """;

        return queryRunner.query(query,
                listFromRS(PuzzleRepository::puzzleFromRS)
        );
    }

    /**
     * Returns all archived {@link Puzzle Puzzles}, sorted by position.
     */
    public List<Puzzle> getArchivedPuzzles() {
        @Language("SQL") String query = """
                SELECT *
                FROM puzzles
                WHERE Active = 0
                ORDER BY Position;
        """;

        return queryRunner.query(query,
                listFromRS(PuzzleRepository::puzzleFromRS)
        );
    }

    /**
     * Returns a {@link List} of all {@link Puzzle Puzzles} in the given {@link PuzzleChapter}, sorted by the position
     * in the chapter.
     *
     * @param chapterId The chapter ID.
     * @return A {@link List} of all {@link Puzzle Puzzles} in the given {@link PuzzleChapter}, sorted by the position
     *     in the chapter.
     */
    public List<Puzzle> getPuzzlesForChapterId(int chapterId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_active_puzzles as puzzles
                WHERE Chapter_ID = ?
                ORDER BY Position;
        """;

        return queryRunner.query(query,
                listFromRS(PuzzleRepository::puzzleFromRS),
                chapterId
        );
    }

    /**
     * Returns the {@link PuzzleGame} for the given game ID.
     *
     * @param gameId The game ID.
     * @return The {@link PuzzleGame} for the given game ID.
     */
    public PuzzleGame getPuzzleGameForId(int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_puzzle_games games
                WHERE ID = ?;
        """;

        var game = queryRunner.query(query,
                oneFromRS(PuzzleRepository::puzzleGameFromRS),
                gameId
        );
        return game.orElse(null);
    }

    /**
     * Returns the {@link PuzzleGame} that represents the latest try on the given puzzle by the given user.
     *
     * @param puzzleId The puzzle ID.
     * @param userId   The user ID.
     * @return The {@link PuzzleGame} that represents the latest try on the given puzzle by the given user.
     */
    public PuzzleGame getLatestPuzzleGameForPuzzleAndUser(int puzzleId, int userId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_puzzle_games games
                WHERE Puzzle_ID = ?
                  AND Creator_ID = ?
                ORDER BY Timestamp DESC;
        """;

        var game = queryRunner.query(query,
                nextFromRS(PuzzleRepository::puzzleGameFromRS),
                puzzleId,
                userId
        );
        return game.orElse(null);
    }

    /**
     * Returns a {@link List} of {@link PuzzleGame PuzzleGames} that represents the tries on the given puzzle by the
     * given user. The list is sorted by the timestamp of the games.
     *
     * @param puzzleId The puzzle ID.
     * @param userId   The user ID.
     * @return A {@link List} of {@link PuzzleGame PuzzleGames} that represents the tries on the given puzzle by the
     *     given user. The list is sorted by the timestamp of the games.
     */
    public List<PuzzleGame> getPuzzleGamesForPuzzleAndUser(int puzzleId, int userId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_puzzle_games games
                WHERE Puzzle_ID = ?
                  AND Creator_ID = ?
                ORDER BY Timestamp DESC;
        """;

        return queryRunner.query(query,
                listFromRS(PuzzleRepository::puzzleGameFromRS),
                puzzleId,
                userId
        );
    }

    /**
     * Returns a {@link List} of the active {@link PuzzleGame PuzzleGames} played by the given user.
     * The list is sorted by the timestamp of the games.
     *
     * @param userId The user ID.
     * @return A {@link List} of the active {@link PuzzleGame PuzzleGames} played by the given user.
     *     The list is sorted by the timestamp of the games.
     */
    public List<PuzzleGame> getActivePuzzleGamesForUser(int userId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_puzzle_games games
                WHERE State = 'ACTIVE'
                  AND Creator_ID = ?
                ORDER BY Timestamp DESC;
        """;

        return queryRunner.query(query,
                listFromRS(PuzzleRepository::puzzleGameFromRS),
                userId
        );
    }

    /**
     * Stores the given {@link Puzzle} in the database.
     *
     * @param puzzle The {@link Puzzle}.
     * @return The ID of the stored puzzle, or -1 if the insert failed.
     */
    public int storePuzzle(Puzzle puzzle) {
        @Language("SQL") String query = """
                INSERT INTO puzzles

                (Class_ID,
                Type,
                Level,
                Max_Assertions,
                Mutant_Validator_Level,
                Editable_Lines_Start,
                Editable_Lines_End,
                Chapter_ID,
                Position,
                Title,
                Description,
                IsEquivalent)

                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        return queryRunner.insert(query,
                generatedKeyFromRS(),
                puzzle.getClassId(),
                puzzle.getType().toString(),
                puzzle.getLevel().toString(),
                puzzle.getMaxAssertionsPerTest(),
                puzzle.getMutantValidatorLevel().toString(),
                puzzle.getEditableLinesStart(),
                puzzle.getEditableLinesEnd(),
                puzzle.getChapterId(),
                puzzle.getPosition(),
                puzzle.getTitle(),
                puzzle.getDescription(),
                puzzle.isEquivalent()
        ).orElseThrow(() -> new UncheckedSQLException("Couldn't store puzzle."));
    }


    /**
     * Stores the given {@link PuzzleChapter} in the database.
     *
     * @param chapter The {@link PuzzleChapter}.
     * @return The ID of the stored puzzle chapter, or -1 if the insert failed.
     */
    public int storePuzzleChapter(PuzzleChapter chapter) {
        @Language("SQL") String positionQuery = """
            SELECT MAX(position) AS max_position FROM puzzle_chapters;
        """;

        int maxPosition = queryRunner.query(positionQuery, oneFromRS(rs -> rs.getInt("max_position")))
                .orElse(0);

        @Language("SQL") String query = """
                INSERT INTO puzzle_chapters

                (Position,
                Title,
                Description)

                VALUES (?, ?, ?);
        """;

        return queryRunner.insert(query,
                generatedKeyFromRS(),
                maxPosition + 1,
                chapter.getTitle(),
                chapter.getDescription()
        ).orElseThrow(() -> new UncheckedSQLException("Couldn't store puzzle chapter."));
    }

    /**
     * Stores the given {@link PuzzleGame} in the database.
     *
     * @param game The {@link PuzzleGame}.
     * @return The game ID of the stored game, or -1 if the insert failed.
     */
    public int storePuzzleGame(PuzzleGame game) {
        @Language("SQL") String query = """
                INSERT INTO games

                (Class_ID,
                Level,
                Creator_ID,
                MaxAssertionsPerTest,
                MutantValidator,
                State,
                CurrentRound,
                ActiveRole,
                Mode,
                Puzzle_ID)

                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        return queryRunner.insert(query,
                generatedKeyFromRS(),
                game.getClassId(),
                game.getLevel().toString(),
                game.getCreatorId(),
                game.getMaxAssertionsPerTest(),
                game.getMutantValidatorLevel().toString(),
                game.getState().toString(),
                game.getCurrentRound(),
                game.getActiveRole().toString(),
                game.getMode().toString(),
                game.getPuzzleId()
        ).orElseThrow(() -> new UncheckedSQLException("Couldn't store puzzle game."));
    }


    /**
     * Updates the given {@link org.codedefenders.game.puzzle.Puzzle puzzle information} in the database.
     *
     * @param puzzle The {@link org.codedefenders.game.puzzle.Puzzle}.
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    public boolean updatePuzzle(Puzzle puzzle) {
        @Language("SQL") String query = """
                UPDATE puzzles
                SET Chapter_ID           = ?,
                    Position             = ?,
                    Title                = ?,
                    Description          = ?,
                    Max_Assertions       = ?,
                    Editable_Lines_Start = ?,
                    Editable_Lines_End   = ?,
                    Level                = ?,
                    IsEquivalent         = ?
                WHERE Puzzle_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                puzzle.getChapterId(),
                puzzle.getPosition(),
                puzzle.getTitle(),
                puzzle.getDescription(),
                puzzle.getMaxAssertionsPerTest(),
                puzzle.getEditableLinesStart(),
                puzzle.getEditableLinesEnd(),
                puzzle.getLevel().toString(),
                puzzle.isEquivalent(),
                puzzle.getPuzzleId()
        );
        return updatedRows > 0;
    }

    /**
     * Updates the given {@link PuzzleChapter}'s values in the database.
     *
     * @param chapter The {@link PuzzleChapter}.
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    public boolean updatePuzzleChapter(PuzzleChapter chapter) {
        @Language("SQL") String query = """
                UPDATE puzzle_chapters
                SET Position    = ?,
                    Title       = ?,
                    Description = ?
                WHERE Chapter_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                chapter.getPosition(),
                chapter.getTitle(),
                chapter.getDescription(),

                chapter.getChapterId()
        );
        return updatedRows > 0;
    }

    /**
     * Updates the given {@link PuzzleGame}'s values in the database.
     *
     * @param game The {@link PuzzleGame}.
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    public boolean updatePuzzleGame(PuzzleGame game) {
        @Language("SQL") String query = """
                UPDATE games

                SET Class_ID = ?,
                    Level = ?,
                    Creator_ID = ?,
                    MaxAssertionsPerTest = ?,
                    MutantValidator = ?,
                    State = ?,
                    CurrentRound = ?,
                    ActiveRole = ?,
                    Puzzle_ID = ?

                WHERE ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                game.getClassId(),
                game.getLevel().toString(),
                game.getCreatorId(),
                game.getMaxAssertionsPerTest(),
                game.getMutantValidatorLevel().toString(),
                game.getState().toString(),
                game.getCurrentRound(),
                game.getActiveRole().toString(),
                game.getPuzzleId(),
                game.getId()
        );
        return updatedRows > 0;
    }

    /**
     * Checks for a given puzzle whether at least one game
     * with this puzzle exists.
     *
     * @param puzzle the checked puzzle.
     * @return {@code true} if at least one game does exist, {@code false} otherwise.
     */
    public boolean gamesExistsForPuzzle(@Nonnull Puzzle puzzle) {
        @Language("SQL") String query = """
                    SELECT (COUNT(games.ID) > 0) AS games_exist
                    FROM games, puzzles
                    WHERE puzzles.Puzzle_ID = ?
                    AND games.Class_ID = puzzles.Class_ID
        """;

        var exists = queryRunner.query(query,
                oneFromRS(rs -> rs.getBoolean("games_exist")),
                puzzle.getPuzzleId()
        );
        return exists.orElseThrow();
    }

    /**
     * Sets the 'Active' column of a given puzzle to a given value.
     *
     * @param puzzle the given puzzle the active value is set for.
     * @param active whether the puzzle is active or not.
     * @return {@code true} if setting active was successful, {@code false} otherwise.
     */
    public boolean setPuzzleActive(@Nonnull Puzzle puzzle, boolean active) {
        @Language("SQL") String query = """
                UPDATE puzzles
                    SET ACTIVE = ?
                    WHERE Puzzle_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                active,
                puzzle.getPuzzleId()
        );
        return updatedRows > 0;
    }

    /**
     * Removes a given puzzle chapter from the database.
     * All puzzles, which reference this puzzle chapter, have their
     * puzzle chapter attribute set to {@code null} in the database.
     *
     * @param chapter the puzzle chapter to be removed.
     * @return {@code true} when the removal was successful, {@code false} otherwise.
     */
    public boolean removePuzzleChapter(@Nonnull PuzzleChapter chapter) {
        @Language("SQL") String query = "DELETE FROM puzzle_chapters WHERE Chapter_ID = ?;";
        int updatedRows = queryRunner.update(query,
                chapter.getChapterId()
        );
        return updatedRows > 0;
    }

    /**
     * Returns the parent class of a puzzle class.
     * The puzzle class is a copy of the parent class and still requires
     * the source files of its parent class.
     *
     * @param puzzleClassId the identifier of the puzzle child class.
     * @return the parent class of the given puzzle class.
     */
    public GameClass getParentGameClass(int puzzleClassId) {
        @Language("SQL") String query = """
                SELECT classes.*
                FROM classes,
                     view_puzzle_classes puzzle_classes
                WHERE puzzle_classes.Class_ID = ?
                  AND classes.Class_ID = puzzle_classes.Parent_Class;
        """;

        var clazz = queryRunner.query(query,
                oneFromRS(GameClassRepository::gameClassFromRS),
                puzzleClassId
        );
        return clazz.orElse(null);
    }

    /**
     * Checks whether the source files of a given class are used for puzzle classes.
     *
     * @param classId the identifier of the checked game class.
     * @return {@code true} if class files are used, {@code false} otherwise.
     */
    public boolean classSourceUsedForPuzzleClasses(int classId) {
        @Language("SQL") String query = """
                SELECT (COUNT(c1.Class_ID)) > 0 as class_used
                FROM classes c1,
                     view_puzzle_classes c2
                WHERE c1.Class_ID = ?
                      AND c1.JavaFile = c2.JavaFile
        """;

        var used = queryRunner.query(query,
                oneFromRS(rs -> rs.getBoolean("class_used")),
                classId
        );
        return used.orElseThrow();
    }

    public boolean checkPuzzlesEnabled() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.ALLOW_PUZZLE_SECTION).getBoolValue();
    }

    public boolean checkActivePuzzlesExist() {
        @Language("SQL") String query = """
                SELECT *
                FROM view_active_puzzles as puzzles
                LIMIT 1;
        """;

        var puzzle = queryRunner.query(query, nextFromRS(rs -> rs.getInt("Puzzle_ID")));
        return puzzle.isPresent();
    }

    public List<AdminPuzzleAPI.GetPuzzlesData.PuzzleData> getAdminPuzzleInfos() {
        @Language("SQL") String query = """
                SELECT puzzles.*, COUNT(games.ID) AS game_count
                FROM puzzles
                LEFT JOIN view_puzzle_games games ON puzzles.Puzzle_ID = games.Puzzle_ID
                GROUP BY puzzles.Puzzle_ID;
        """;

        return queryRunner.query(query, listFromRS(rs -> {
            Puzzle puzzle = puzzleFromRS(rs);
            int gameCount = rs.getInt("game_count");
            boolean active = rs.getBoolean("puzzles.Active");
            return new AdminPuzzleAPI.GetPuzzlesData.PuzzleData(puzzle, gameCount, active);
        }));
    }

    public void batchUpdatePuzzlePositions(UpdatePuzzlePositionsData positions) {
        @Language("SQL") String query = """
            UPDATE puzzles
            SET Position = ?,
                Chapter_ID = ?,
                Active = ?
            WHERE Puzzle_ID = ?;
        """;

        class Counter {
            int pos = 1;
            <T> Object getPos(T t) {
                return pos++;
            }
        }

        var batchParams = new ArrayList<>();

        // unassigned puzzles
        // -> Active = true, Chapter_ID = null, Position = <count>
        batchParams.addAll(Arrays.asList(
                extractBatchParams(positions.unassignedPuzzles(),
                        new Counter()::getPos,
                        id -> null,
                        id -> true,
                        id -> id)));

        // archived puzzles
        // -> Active = false, Chapter_ID = null, Position = <count>
        batchParams.addAll(Arrays.asList(
                extractBatchParams(positions.archivedPuzzles(),
                        new Counter()::getPos,
                        id -> null,
                        id -> false,
                        id -> id)));

        // regular puzzles
        // -> Active = true, Chapter_ID = id, Position = <count>
        for (var chapter : positions.chapters()) {
            batchParams.addAll(Arrays.asList(
                    extractBatchParams(chapter.puzzles(),
                            new Counter()::getPos,
                            id -> chapter.id(),
                            id -> true,
                            id -> id)));
        }

        queryRunner.batch(query, batchParams.toArray(Object[][]::new));

        // update chapter positions
        query = """
            UPDATE puzzle_chapters
            SET Position = ?
            WHERE Chapter_ID = ?;
        """;
        queryRunner.batch(query, extractBatchParams(positions.chapters(),
                new Counter()::getPos,
                chapter -> chapter.id()));
    }

    /**
     * Creates a {@link PuzzleChapter} from a {@link ResultSet}.
     *
     * @param rs The {@link ResultSet}.
     * @return The created {@link PuzzleChapter}.
     */
    private static PuzzleChapter puzzleChapterFromRS(ResultSet rs) throws SQLException {
        int chapterId = rs.getInt("puzzle_chapters.Chapter_ID");

        Integer position = rs.getInt("puzzle_chapters.Position");
        if (rs.wasNull()) {
            position = null;
        }

        String title = rs.getString("puzzle_chapters.Title");
        String description = rs.getString("puzzle_chapters.Description");

        return new PuzzleChapter(chapterId, position, title, description);
    }

    /**
     * Creates a {@link Puzzle} from a {@link ResultSet}.
     *
     * @param rs The {@link ResultSet}.
     * @return The created {@link Puzzle}.
     */
    private static Puzzle puzzleFromRS(ResultSet rs) throws SQLException {
        final int puzzleId = rs.getInt("puzzles.Puzzle_ID");
        final int classId = rs.getInt("puzzles.Class_ID");
        final PuzzleType type = PuzzleType.valueOf(rs.getString("puzzles.Type"));

        Integer chapterId = rs.getInt("puzzles.Chapter_ID");
        if (rs.wasNull()) {
            chapterId = null;
        }

        Integer position = rs.getInt("puzzles.Position");
        if (rs.wasNull()) {
            position = null;
        }

        String title = rs.getString("puzzles.Title");
        String description = rs.getString("puzzles.Description");

        GameLevel level = GameLevel.valueOf(rs.getString("puzzles.Level"));

        int maxAssertions = rs.getInt("puzzles.Max_Assertions");
        // TODO Pay attention that the name of the column here follows a different format

        CodeValidatorLevel mutantValidatorLevel =
                CodeValidatorLevel.valueOf(rs.getString("puzzles.Mutant_Validator_Level"));

        Integer editableLinesStart = rs.getInt("puzzles.Editable_Lines_Start");
        if (rs.wasNull()) {
            editableLinesStart = null;
        }

        Integer editableLinesEnd = rs.getInt("puzzles.Editable_Lines_End");
        if (rs.wasNull()) {
            editableLinesEnd = null;
        }

        boolean isEquivalent = rs.getBoolean("puzzles.IsEquivalent");
        return new Puzzle(puzzleId, classId, type, isEquivalent, level, maxAssertions,
                mutantValidatorLevel, editableLinesStart, editableLinesEnd, chapterId, position, title, description);
    }

    /**
     * Creates a {@link PuzzleGame} from a {@link ResultSet}.
     *
     * @param rs The {@link ResultSet}.
     * @return The created {@link PuzzleGame}.
     */
    private static PuzzleGame puzzleGameFromRS(ResultSet rs) throws SQLException {
        GameClass cut = GameClassRepository.gameClassFromRS(rs);
        int gameId = rs.getInt("games.ID");
        int classId = rs.getInt("games.Class_ID");
        GameLevel level = GameLevel.valueOf(rs.getString("games.Level"));
        int creatorId = rs.getInt("games.Creator_ID");
        int maxAssertionsPerTest = rs.getInt("games.MaxAssertionsPerTest");
        CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.valueOf(rs.getString("games.MutantValidator"));
        GameState state = GameState.valueOf(rs.getString("games.State"));
        int currentRound = rs.getInt("games.CurrentRound");

        int puzzleId = rs.getInt("games.Puzzle_ID");
        PuzzleType type = PuzzleType.valueOf(rs.getString("Puzzle_Type"));

        return new PuzzleGame(cut, puzzleId, gameId, classId, level, creatorId,
            maxAssertionsPerTest, mutantValidatorLevel, state, currentRound, type);
    }
}

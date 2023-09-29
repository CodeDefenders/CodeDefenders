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
package org.codedefenders.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.PuzzleInfo;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        try {
            var chapter = queryRunner.query(query,
                    oneFromRS(PuzzleRepository::puzzleChapterFromRS),
                    chapterId
            );
            return chapter.orElse(null);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            return queryRunner.query(query,
                    listFromRS(PuzzleRepository::puzzleChapterFromRS)
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            var puzzle = queryRunner.query(query,
                    oneFromRS(PuzzleRepository::puzzleFromRS),
                    puzzleId
            );
            return puzzle.orElse(null);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            return queryRunner.query(query,
                    listFromRS(PuzzleRepository::puzzleFromRS)
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            return queryRunner.query(query,
                    listFromRS(PuzzleRepository::puzzleFromRS),
                    chapterId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            var game = queryRunner.query(query,
                    oneFromRS(PuzzleRepository::puzzleGameFromRS),
                    gameId
            );
            return game.orElse(null);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            var game = queryRunner.query(query,
                    nextFromRS(PuzzleRepository::puzzleGameFromRS),
                    puzzleId,
                    userId
            );
            return game.orElse(null);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            return queryRunner.query(query,
                    listFromRS(PuzzleRepository::puzzleGameFromRS),
                    puzzleId,
                    userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            return queryRunner.query(query,
                    listFromRS(PuzzleRepository::puzzleGameFromRS),
                    userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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
                Active_Role,
                Level,
                Max_Assertions,
                Mutant_Validator_Level,
                Editable_Lines_Start,
                Editable_Lines_End,
                Chapter_ID,
                Position,
                Title,
                Description)

                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try {
            return queryRunner.insert(query,
                    oneFromRS(rs -> rs.getInt(1)),
                    puzzle.getClassId(),
                    puzzle.getActiveRole().toString(),
                    puzzle.getLevel().toString(),
                    puzzle.getMaxAssertionsPerTest(),
                    puzzle.getMutantValidatorLevel().toString(),
                    puzzle.getEditableLinesStart(),
                    puzzle.getEditableLinesEnd(),
                    puzzle.getChapterId(),
                    puzzle.getPosition(),
                    puzzle.getTitle(),
                    puzzle.getDescription()
            ).orElseThrow(() -> new UncheckedSQLException("Couldn't store puzzle."));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Stores the given {@link PuzzleChapter} in the database.
     *
     * @param chapter The {@link PuzzleChapter}.
     * @return The ID of the stored puzzle chapter, or -1 if the insert failed.
     */
    public int storePuzzleChapter(PuzzleChapter chapter) {
        @Language("SQL") String query = """
                INSERT INTO puzzle_chapters

                (Chapter_ID,
                Position,
                Title,
                Description)

                VALUES (?, ?, ?, ?);
        """;

        try {
            return queryRunner.insert(query,
                    oneFromRS(rs -> rs.getInt(1)),
                    chapter.getChapterId(),
                    chapter.getPosition(),
                    chapter.getTitle(),
                    chapter.getDescription()
            ).orElseThrow(() -> new UncheckedSQLException("Couldn't store puzzle chapter."));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            return queryRunner.insert(query,
                    oneFromRS(rs -> rs.getInt(1)),
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
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }


    /**
     * Updates the given {@link PuzzleInfo puzzle information} in the database.
     *
     * @param puzzle The {@link PuzzleInfo}.
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    public boolean updatePuzzle(PuzzleInfo puzzle) {
        @Language("SQL") String query = """
                UPDATE puzzles
                SET Chapter_ID           = ?,
                    Position             = ?,
                    Title                = ?,
                    Description          = ?,
                    Max_Assertions       = ?,
                    Editable_Lines_Start = ?,
                    Editable_Lines_End   = ?
                WHERE Puzzle_ID = ?;
        """;

        try {
            int updatedRows = queryRunner.update(query,
                    puzzle.getChapterId(),
                    puzzle.getPosition(),
                    puzzle.getTitle(),
                    puzzle.getDescription(),
                    puzzle.getMaxAssertionsPerTest(),
                    puzzle.getEditableLinesStart(),
                    puzzle.getEditableLinesEnd(),
                    puzzle.getPuzzleId()
            );
            return updatedRows > 0;
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            int updatedRows = queryRunner.update(query,
                    chapter.getPosition(),
                    chapter.getTitle(),
                    chapter.getDescription(),

                    chapter.getChapterId()
            );
            return updatedRows > 0;
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
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
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            var exists = queryRunner.query(query,
                    oneFromRS(rs -> rs.getBoolean("games_exist")),
                    puzzle.getPuzzleId()
            );
            return exists.orElseThrow();
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            int updatedRows = queryRunner.update(query,
                    active,
                    puzzle.getPuzzleId()
            );
            return updatedRows > 0;
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Removes a given puzzle chapter from the database.
     * All puzzles, which reference this puzzle chapter, have their
     * puzzle chapter attribute set to {@code null} in the database.
     *
     * @param chapter the puzzle chapter to be removed.
     * @return {@code true} when the removal was successful, {@code false} otherwise.
     */
    public boolean removePuzzleChapter(@Nonnull  PuzzleChapter chapter) {
        @Language("SQL") String query = "DELETE FROM puzzle_chapters WHERE Chapter_ID = ?;";
        try {
            int updatedRows = queryRunner.update(query,
                    chapter.getChapterId()
            );
            return updatedRows > 0;
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            var clazz = queryRunner.query(query,
                    oneFromRS(GameClassDAO::gameClassFromRS),
                    puzzleClassId
            );
            return clazz.orElse(null);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            var used = queryRunner.query(query,
                    oneFromRS(rs -> rs.getBoolean("class_used")),
                    classId
            );
            return used.orElseThrow();
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        try {
            var puzzle = queryRunner.query(query, nextFromRS(rs -> rs.getInt("Puzzle_ID")));
            return puzzle.isPresent();
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Creates a {@link PuzzleChapter} from a {@link ResultSet}.
     *
     * @param rs The {@link ResultSet}.
     * @return The created {@link PuzzleChapter}.
     */
    private static PuzzleChapter puzzleChapterFromRS(ResultSet rs) {
        try {
            int chapterId = rs.getInt("puzzle_chapters.Chapter_ID");

            Integer position = rs.getInt("puzzle_chapters.Position");
            if (rs.wasNull()) {
                position = null;
            }

            String title = rs.getString("puzzle_chapters.Title");
            String description = rs.getString("puzzle_chapters.Description");

            return new PuzzleChapter(chapterId, position, title, description);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while checking ResultSet.", e);
            return null;
        }
    }

    /**
     * Creates a {@link Puzzle} from a {@link ResultSet}.
     *
     * @param rs The {@link ResultSet}.
     * @return The created {@link Puzzle}.
     */
    private static Puzzle puzzleFromRS(ResultSet rs) {
        try {
            final int puzzleId = rs.getInt("puzzles.Puzzle_ID");
            final int classId = rs.getInt("puzzles.Class_ID");
            final Role activeRole = Role.valueOf(rs.getString("puzzles.Active_Role"));

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

            return new Puzzle(puzzleId, classId, activeRole, level, maxAssertions, mutantValidatorLevel,
                    editableLinesStart, editableLinesEnd, chapterId, position, title, description);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while checking ResultSet.", e);
            return null;
        }
    }

    /**
     * Creates a {@link PuzzleGame} from a {@link ResultSet}.
     *
     * @param rs The {@link ResultSet}.
     * @return The created {@link PuzzleGame}.
     */
    private static PuzzleGame puzzleGameFromRS(ResultSet rs) {
        try {
            GameClass cut = GameClassDAO.gameClassFromRS(rs);
            int gameId = rs.getInt("games.ID");
            int classId = rs.getInt("games.Class_ID");
            GameLevel level = GameLevel.valueOf(rs.getString("games.Level"));
            int creatorId = rs.getInt("games.Creator_ID");
            int maxAssertionsPerTest = rs.getInt("games.MaxAssertionsPerTest");
            CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.valueOf(rs.getString("games.MutantValidator"));
            GameState state = GameState.valueOf(rs.getString("games.State"));
            int currentRound = rs.getInt("games.CurrentRound");
            Role activeRole = Role.valueOf(rs.getString("games.ActiveRole"));
            int puzzleId = rs.getInt("games.Puzzle_ID");

            return new PuzzleGame(cut, puzzleId, gameId, classId, level, creatorId,
                maxAssertionsPerTest, mutantValidatorLevel, state, currentRound, activeRole);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while checking ResultSet.", e);
            return null;
        }
    }
}

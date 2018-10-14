package org.codedefenders.database;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * This class handles the database logic for puzzles.
 *
 * @see Puzzle
 * @see PuzzleChapter
 * @see PuzzleGame
 */
public class PuzzleDAO {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleDAO.class);

    /**
     * Returns the {@link PuzzleChapter} for the given chapter ID.
     * @param chapterId The chapter ID.
     * @return The {@link PuzzleChapter} for the given chapter ID.
     */
    public static PuzzleChapter getPuzzleChapterForId(int chapterId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzle_chapters",
                "WHERE Chapter_ID = ?;"
        );

        return executeQueryReturnValue(query,
                PuzzleDAO::getPuzzleChapterFromResultSet,
                DB.getDBV(chapterId));
    }

    /**
     * Returns a {@link List} of all {@link PuzzleChapter PuzzleChapters}.
     * @return A {@link List} of all {@link PuzzleChapter PuzzleChapters}.
     */
    public static List<PuzzleChapter> getPuzzleChapters() {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzle_chapters",
                "ORDER BY Position;"
        );

        return executeQueryReturnList(query,
                PuzzleDAO::getPuzzleChapterFromResultSet);
    }

    /**
     * Returns the {@link Puzzle} for the given puzzle ID.
     * @param puzzleId The puzzle ID.
     * @return The {@link Puzzle} for the given puzzle ID.
     */
    public static Puzzle getPuzzleForId(int puzzleId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzles",
                "WHERE Puzzle_ID = ?;"
        );

        return executeQueryReturnValue(query,
                PuzzleDAO::getPuzzleFromResultSet,
                DB.getDBV(puzzleId));
    }

    /**
     * Returns a {@link List} of all {@link Puzzle Puzzles}.
     * @return A {@link List} of all {@link Puzzle Puzzles}.
     */
    public static List<Puzzle> getPuzzles() {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzles",
                "ORDER BY Chapter_ID, Position;"
        );

        return executeQueryReturnList(query,
                PuzzleDAO::getPuzzleFromResultSet);
    }

    /**
     * Returns a {@link List} of all {@link Puzzle Puzzles} in the given {@link PuzzleChapter}.
     * @param chapterId The chapter ID.
     * @return A {@link List} of all {@link Puzzle Puzzles} in the given {@link PuzzleChapter}.
     */
    public static List<Puzzle> getPuzzlesForChapterId(int chapterId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzles",
                "WHERE Chapter_ID = ?",
                "ORDER BY Chapter_ID, Position;"
        );

        return executeQueryReturnList(query,
                PuzzleDAO::getPuzzleFromResultSet,
                DB.getDBV(chapterId));
    }

    /**
     * Returns the {@link PuzzleGame} for the given game ID.
     * @param gameId The game ID.
     * @return The {@link PuzzleGame} for the given game ID.
     */
    public static PuzzleGame getPuzzleGameForId(int gameId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM games",
                "WHERE Mode = 'PUZZLE'",
                "  AND ID = ?;"
        );

        return executeQueryReturnValue(query,
                PuzzleDAO::getPuzzleGameFromResultSet,
                DB.getDBV(gameId));
    }

    /**
     * Returns the {@link PuzzleGame} that represents the latest try on the given puzzle by the given user.
     * @param puzzleId The puzzle ID.
     * @param userId The user ID.
     * @return The {@link PuzzleGame} that represents the latest try on the given puzzle by the given user.
     */
    public static PuzzleGame getPuzzleGameForPuzzleAndUser(int puzzleId, int userId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM games",
                "WHERE Mode = 'PUZZLE'",
                "  AND Puzzle_ID = ?",
                "  AND Creator_ID = ?",
                "ORDER BY Timestamp DESC;"
        );

        return executeQueryReturnValue(query,
                PuzzleDAO::getPuzzleGameFromResultSet,
                DB.getDBV(puzzleId), DB.getDBV(userId));
    }

    /**
     * Stores the given {@link PuzzleGame} in the database.
     * @param game The {@link PuzzleGame}.
     * @return The game ID of the stored game, or -1 if the insert failed.
     */
    public static int storePuzzleGame(PuzzleGame game) {
        String query = String.join("\n",
                "INSERT INTO games",

                "(Class_ID,",
                "Level,",
                "Creator_ID,",
                "MaxAssertionsPerTest,",
                "MutantValidator,",
                "State,",
                "CurrentRound,",
                "ActiveRole,",
                "Mode,",
                "Puzzle_ID)",

                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
        );

        DatabaseValue[] valueList = new DatabaseValue[] {
                DB.getDBV(game.getClassId()),
                DB.getDBV(game.getLevel().toString()),
                DB.getDBV(game.getCreatorId()),
                DB.getDBV(game.getMaxAssertionsPerTest()),
                DB.getDBV(game.getMutantValidatorLevel().toString()),
                DB.getDBV(game.getState().toString()),
                DB.getDBV(game.getCurrentRound()),
                DB.getDBV(game.getActiveRole().toString()),
                DB.getDBV(game.getMode().toString()),
                DB.getDBV(game.getPuzzleId()),
        };

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdateGetKeys(stmt, conn);
    }

    /**
     * Updates the given {@link PuzzleGame}'s values in the database.
     * @param game The {@link PuzzleGame}.
     * @return {@code true} if the update was successful, {@code false}a otherwise.
     */
    public static boolean updatePuzzleGame(PuzzleGame game) {
        String query = String.join("\n",
                "UPDATE games",

                "SET Class_ID = ?,",
                "    Level = ?,",
                "    Creator_ID = ?,",
                "    MaxAssertionsPerTest = ?,",
                "    MutantValidator = ?,",
                "    State = ?,",
                "    CurrentRound = ?,",
                "    ActiveRole = ?,",
                "    Puzzle_ID = ?",

                "WHERE ID = ?;"
        );

        DatabaseValue[] valueList = new DatabaseValue[] {
                DB.getDBV(game.getClassId()),
                DB.getDBV(game.getLevel().toString()),
                DB.getDBV(game.getCreatorId()),
                DB.getDBV(game.getMaxAssertionsPerTest()),
                DB.getDBV(game.getMutantValidatorLevel().toString()),
                DB.getDBV(game.getState().toString()),
                DB.getDBV(game.getCurrentRound()),
                DB.getDBV(game.getActiveRole().toString()),
                DB.getDBV(game.getPuzzleId()),
                DB.getDBV(game.getId()),
        };

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdate(stmt, conn);
    }

    /**
     * Creates a {@link PuzzleChapter} from a {@link ResultSet}.
     * @param rs The {@link ResultSet}.
     * @return The created {@link PuzzleChapter}.
     */
    private static PuzzleChapter getPuzzleChapterFromResultSet(ResultSet rs) {
        try {
            int chapterId = rs.getInt("Chapter_ID");

            Integer position = rs.getInt("Position");
            if (rs.wasNull()) position = null;

            String title = rs.getString("Title");
            String description = rs.getString("Description");

            return new PuzzleChapter(chapterId, position, title, description);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while checking ResultSet.", e);
            return null;
        }
    }

    /**
     * Creates a {@link Puzzle} from a {@link ResultSet}.
     * @param rs The {@link ResultSet}.
     * @return The created {@link Puzzle}.
     */
    private static Puzzle getPuzzleFromResultSet(ResultSet rs) {
        try {
            int puzzleId = rs.getInt("Puzzle_ID");
            int classId = rs.getInt("Class_ID");
            Role activeRole = Role.valueOf(rs.getString("Active_Role"));

            Integer chapterId = rs.getInt("Chapter_ID");
            if (rs.wasNull()) chapterId = null;

            Integer position = rs.getInt("Position");
            if (rs.wasNull()) position = null;

            String title = rs.getString("Title");
            String description = rs.getString("Description");

            GameLevel level = GameLevel.valueOf(rs.getString("Level"));
            int maxAssertions = rs.getInt("Max_Assertions");
            CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.valueOf(rs.getString("Mutant_Validator_Level"));

            Integer editableLinesStart = rs.getInt("Editable_Lines_Start");
            if (rs.wasNull()) editableLinesStart = null;

            Integer editableLinesEnd = rs.getInt("Editable_Lines_End");
            if (rs.wasNull()) editableLinesEnd = null;

            return new Puzzle(puzzleId, classId, activeRole, level, maxAssertions, mutantValidatorLevel,
                    editableLinesStart, editableLinesEnd, chapterId, position, title, description);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while checking ResultSet.", e);
            return null;
        }
    }

    /**
     * Creates a {@link PuzzleGame} from a {@link ResultSet}.
     * @param rs The {@link ResultSet}.
     * @return The created {@link PuzzleGame}.
     */
    private static PuzzleGame getPuzzleGameFromResultSet(ResultSet rs) {
        try {
            int gameId = rs.getInt("ID");
            int classId = rs.getInt("Class_ID");

            GameLevel level = GameLevel.valueOf(rs.getString("Level"));
            int creatorId = rs.getInt("Creator_ID");
            int maxAssertionsPerTest = rs.getInt("MaxAssertionsPerTest");
            CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.valueOf(rs.getString("MutantValidator"));
            GameState state = GameState.valueOf(rs.getString("State"));
            int currentRound = rs.getInt("CurrentRound");
            Role activeRole = Role.valueOf(rs.getString("ActiveRole"));
            int puzzleId = rs.getInt("Puzzle_ID");

            return new PuzzleGame(puzzleId, gameId, classId, level, creatorId, maxAssertionsPerTest, mutantValidatorLevel,
                    state, currentRound, activeRole);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while checking ResultSet.", e);
            return null;
        }
    }

    /* TODO Add something like this to DB for the other DAOs to use? */
    /**
     * Executes the given query with the given parameters, then uses the given function to extract the first value from
     * the {@link ResultSet}.
     * @param query The query.
     * @param mapFunction A function, which takes the query's {@link ResultSet} as an argument, and uses it to construct
     *                    a return value. The function must not advance the {@link ResultSet}. If the function can't
     *                    construct the desired value from the {@link ResultSet}, it must return {@code null};
     * @param parameters The parameters for the query.
     * @param <T> The type of value to be queried.
     * @return The first result of the query, computed by the given function.
     */
    private static <T> T executeQueryReturnValue(String query, Function<ResultSet, T> mapFunction,
                                                 DatabaseValue... parameters) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, parameters);

        try {
            final ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                return mapFunction.apply(resultSet);
            }
            logger.warn("Query had no result.\n" + query);
            return null;

        } catch (SQLException e) {
            logger.error("Caught SQL exception while executing query.", e);
            return null;

        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /* TODO Add something like this to DB for the other DAOs to use? */
    /**
     * Executes the given query with the given parameters, then uses the given function to extract the values from
     * the {@link ResultSet}.
     * @param query The query.
     * @param mapFunction A function, which takes the query's {@link ResultSet} as an argument, and uses it to construct
     *                    a return value. The function must not advance the {@link ResultSet}. If the function can't
     *                    construct the desired value from the {@link ResultSet}, it must return {@code null};
     * @param parameters The parameters for the query.
     * @param <T> The type of value to be queried.
     * @return A list of the results of the query, computed by the given function.
     */
    private static <T> List<T> executeQueryReturnList(String query, Function<ResultSet, T> mapFunction,
                                                      DatabaseValue... parameters) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, parameters);

        try {
            final ResultSet resultSet = stmt.executeQuery();
            List<T> values = new ArrayList<>();

            while (resultSet.next()) {
                T value = mapFunction.apply(resultSet);
                if (value == null) return null;
                values.add(value);
            }
            return values;

        } catch (SQLException e) {
            logger.error("Caught SQL exception while executing query.", e);
            return null;

        } finally {
            DB.cleanup(conn, stmt);
        }
    }
}

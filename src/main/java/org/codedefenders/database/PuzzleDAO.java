package org.codedefenders.database;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

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
     * @param chapterId The chapter ID to get the {@link PuzzleChapter} for.
     * @return The {@link PuzzleChapter} for the given chapter ID.
     */
    public static PuzzleChapter getPuzzleChapter(int chapterId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzle_chapters",
                "WHERE Chapter_ID = ?",
                "ORDER BY Position;"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(chapterId));

        final ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        if (rs == null) {
            return null;
        }

        try {
            if (rs.next()) {
                return getPuzzleChapterFromResultSet(rs);
            }
            return null;
        } catch (SQLException e) {
            logger.error("Caught exception while querying puzzles.", e);
            return null;
        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /**
     * Returns a {@link List} of all {@link PuzzleChapter PuzzleChapters}.
     * @return a {@link List} of all {@link PuzzleChapter PuzzleChapters}.
     */
    public static List<PuzzleChapter> getPuzzleChapters() {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzle_chapters",
                "ORDER BY Position;"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query);

        final ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        if (rs == null) {
            return null;
        }

        final List<PuzzleChapter> puzzleChapters = new LinkedList<>();

        try {
            while (rs.next()) {
                puzzleChapters.add(getPuzzleChapterFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Caught exception while querying puzzles.", e);
            return null;
        } finally {
            DB.cleanup(conn, stmt);
        }

        return puzzleChapters;
    }


    /**
     * Returns the {@link Puzzle} for the given puzzle ID.
     * @param puzzleId The puzzle ID to get the {@link Puzzle} for.
     * @return The {@link Puzzle} for the given puzzle ID.
     */
    public static Puzzle getPuzzle(int puzzleId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzles",
                "WHERE Puzzle_ID = ?",
                "ORDER BY Chapter_ID, Position;"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(puzzleId));

        final ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        if (rs == null) {
            return null;
        }

        try {
            if (rs.next()) {
                return getPuzzleFromResultSet(rs);
            }
            return null;
        } catch (SQLException e) {
            logger.error("Caught exception while querying puzzles.", e);
            return null;
        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /**
     * Returns a {@link List} of all {@link Puzzle Puzzles}.
     * @return a {@link List} of all {@link Puzzle Puzzles}.
     */
    public static List<Puzzle> getPuzzles() {
        String query = String.join("\n",
                "SELECT *",
                "FROM puzzles",
                "ORDER BY Chapter_ID, Position;"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query);

        final ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        if (rs == null) {
            return null;
        }

        final List<Puzzle> puzzles = new LinkedList<>();

        try {
            while (rs.next()) {
                puzzles.add(getPuzzleFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Caught exception while querying puzzles.", e);
            return null;
        } finally {
            DB.cleanup(conn, stmt);
        }

        return puzzles;
    }

    public static int getFailedSubmissions(int gameId) {
        throw new Error("not implemented");
    }

    public static boolean storePuzzleGame(PuzzleGame game) {
        throw new Error("not implemented");
    }

    public static boolean updatePuzzleGame(PuzzleGame game) {
        throw new Error("not implemented");
    }

    public static boolean getPuzzleGame(int gameId) {
        throw new Error("not implemented");
    }

    public static boolean getPuzzleGame(int puzzleId, int uid) {
        throw new Error("not implemented");
    }

    /**
     * Creates a {@link PuzzleChapter} from a {@link ResultSet}.
     * @throws SQLException If a problem occurs with the {@link ResultSet}.
     */
    private static PuzzleChapter getPuzzleChapterFromResultSet(ResultSet rs) throws SQLException {
        int chapterId = rs.getInt("Chapter_ID");

        Integer position = rs.getInt("Position");
        if (rs.wasNull()) position = null;

        String title = rs.getString("Title");
        String description = rs.getString("Description");

        return new PuzzleChapter(chapterId, position, title, description);
    }

    /**
     * Creates a {@link Puzzle} from a {@link ResultSet}.
     * @throws SQLException If a problem occurs with the {@link ResultSet}.
     */
    private static Puzzle getPuzzleFromResultSet(ResultSet rs) throws SQLException {
        int puzzleId = rs.getInt("Puzzle_ID");
        int classId = rs.getInt("Class_ID");
        Role activeRole = Role.valueOf(rs.getString("Active_Role"));

        Integer chapterId = rs.getInt("Chapter_ID");
        if (rs.wasNull()) chapterId = null;

        Integer position = rs.getInt("Position");
        if (rs.wasNull()) position = null;

        String title = rs.getString("Title");
        String description = rs.getString("Description");

        GameLevel level = null;
        String levelString = rs.getString("Level");
        if (!rs.wasNull()) level = GameLevel.valueOf(levelString);

        Integer editableLinesStart = rs.getInt("Editable_Lines_Start");
        if (rs.wasNull()) editableLinesStart = null;

        Integer editableLinesEnd = rs.getInt("Editable_Lines_End");
        if (rs.wasNull()) editableLinesEnd = null;

        return new Puzzle(puzzleId, classId, activeRole, level,
                editableLinesStart, editableLinesEnd, chapterId, position, title, description);
    }
}

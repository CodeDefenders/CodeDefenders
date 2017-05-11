package org.codedefenders.story;

/**
 * Created by Joe Chung on 12/03/2017.
 */

import org.codedefenders.*;
import org.codedefenders.util.DatabaseAccess;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class StoryGame extends AbstractGame {

    public StoryGame (int userId, int puzzleId, int levelId, int puzzle, int classId, String puzzleName, String hint, String desc, PuzzleMode mode, int points, StoryState state) {

        this.id = userId;
        this.puzzleId = puzzleId;
        this.levelNum = levelId;
        this.puzzle = puzzle;
        this.classId = classId;
        this.puzzleName = puzzleName;
        this.hint = hint;
        this.desc = desc;
        this.storyMode = mode;
        this.points = points;
        this.storyState = state;

    }

    public StoryGame (int classId, String hint, String desc, PuzzleMode mode, int puzzle, int levelNum, String puzzleName, int points) {

        this.classId = classId;
        this.hint = hint;
        this.desc = desc;
        this.storyMode = mode;
        this.puzzle = puzzle;
        this.levelNum = levelNum;
        this.puzzleName = puzzleName;
        this.points = points;

    }

    // for DatabaseAccess.getPuzzles()
    public StoryGame (int userId, int puzzle, int level, int points, int puzzleId, StoryState state, String puzzleName, String puzzleDesc, PuzzleMode mode) {

        this.id = userId;
        this.puzzle = puzzle;
        this.levelNum = level;
        this.points = points;
        this.puzzleId = puzzleId;
        this.storyState = state;
        this.puzzleName = puzzleName;
        this.desc = puzzleDesc;
        this.storyMode = mode;

    }

    public StoryGame(int classId, String hint, String desc, PuzzleMode mode) {

        this.classId = classId;
        this.hint = hint;
        this.desc = desc;
        this.storyMode = mode;

    }

    public StoryGame (int levelNumber, PuzzleMode mode) {

        this.levelNum = levelNumber;
        this.storyMode = mode;

    }

    // getStoryClasses
    public StoryGame (int classId, String storyName, String alias, String puzzleName, String puzzleDesc, int level, int puzzle) {

        this.classId = classId;
        this.storyName = storyName;
        this.alias = alias;
        this.puzzleName = puzzleName;
        this.desc = puzzleDesc;
        this.levelId = level;
        this.puzzle = puzzle;

    }

    public StoryGame (int puzzleId) {

        this.puzzleId = puzzleId;

    }

    public StoryGame (int levelId, StoryState state, int puzzleNum) {

        this.levelId = levelId;
        this.storyState = state;
        this.puzzle = puzzleNum;
    }

    public StoryGame (int levelNum, int puzzleNum, String name, StoryState state) {

        this.levelNum = levelNum;
        this.puzzle = puzzleNum;
        this.puzzleName = name;
        this.storyState = state;

    }

    public boolean addPlayer(int userId, Role role) {
        return false;
    }

    // updating puzzle information (edit page)
    public boolean update() {

        logger.debug("Updating class (LevelNumber={}, PuzzleName={}, Hint={}, Description={})", levelNum, puzzleName, hint, desc);
        Connection conn = null;
        Statement stmt = null;

        String sql = String.format("UPDATE Puzzles INNER JOIN Levels ON Puzzles.Level_ID = Levels.Level_ID " +
                "SET Levels.LevelNumber='%s', PuzzleName='%s', Hint='%s', Description='%s' " +
                "WHERE Class_ID='%d';", levelNum, puzzleName, hint, desc, classId);

        try {
            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            DatabaseAccess.cleanup(conn, stmt);
        }

        return false;

    }

    // updating state (Unattempted, In progress, Completed)
    public boolean updateState(StoryState state) {

        logger.debug("Updating state");
        Connection conn = null;
        Statement stmt = null;

        String sql = String.format("UPDATE story SET State = '%s' WHERE Puzzle_ID = %d AND User_ID = %d", state, puzzleId, id);

        try {

            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            DatabaseAccess.cleanup(conn, stmt);
        }

        return false;
    }

    // updating score from puzzle submission
    public boolean updateScore(int score) {

        logger.debug("Updating score");
        Connection conn = null;
        Statement stmt = null;

        String sql = String.format("UPDATE story SET Points = %d WHERE Puzzle_ID = %d AND User_ID = %d", score, puzzleId, id);

        try {

            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            DatabaseAccess.cleanup(conn, stmt);
        }

        return false;

    }

    public boolean insert() {
        return false;
    }
}

package org.codedefenders;

/**
 * Created by joe on 29/03/2017.
 */

import org.apache.commons.io.FileUtils;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EditPuzzle {

    private static final Logger logger = LoggerFactory.getLogger(EditPuzzle.class);

    private int userId;
    private int levelNum;
    private int puzzleNum;
    private int classId;
    private int puzzleId;
    private String puzzleName;
    private String hint;
    private String desc;
    private String mode;

    public EditPuzzle(int userId, int levelNum, int puzzleNum, int classId, String puzzleName, String hint, String desc, String mode) {

        this.userId = userId;
        this.levelNum = levelNum;
        this.puzzleNum = puzzleNum;
        this.classId = classId;
        this.puzzleName = puzzleName;
        this.hint = hint;
        this.desc = desc;
        this.mode = mode;

    }

    public EditPuzzle(int classId, int puzzleId) {

        this.classId = classId;
        this.puzzleId = puzzleId;

    }

    // adding new puzzles
    public boolean insert() {

        Connection conn = null;
        Statement stmt = null;

        // Attempts to store initial user progress
        String sql = String.format("INSERT INTO story (User_ID, Puzzle_ID, Points, State) VALUES ('%d', '%d', 0, 'UNATTEMPTED')", userId, DatabaseAccess.getPuzzleId(classId).getPuzzleId());

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

    // editing puzzles
    public boolean update() {

        Connection conn = null;
        Statement stmt = null;

        // needing to update 2 tables to make sure data is up-to-date
        String sql = String.format("UPDATE puzzles " +
                "SET Level_ID='%d', Puzzle='%d', PuzzleName='%s', Hint='%s', Description='%s', Mode='%s' WHERE Class_ID='%d';", levelNum, puzzleNum, puzzleName, hint, desc, mode, classId);

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

    // delete from the tables
    // TODO: fix this
    public boolean delete() throws IOException {

        Connection conn = null;
        Statement stmt = null;

        PuzzleTest testId = DatabaseAccess.getTestIdByPuzzle(puzzleId);
        PuzzleMutant mutantId = DatabaseAccess.getMutantIdByPuzzle(puzzleId);
        StoryClass classFile = DatabaseAccess.getClassForPuzzle(puzzleId);

        int testIndex = testId.getJavaFile().lastIndexOf("/");
        String testDirString = testId.getJavaFile().substring(0,testIndex); // Test Directory
        File testDir = new File(testDirString);

        int mutantIndex = mutantId.getJavaFile().lastIndexOf("/");
        String mutantDirString = mutantId.getJavaFile().substring(0,mutantIndex); // Mutant Directory
        File mutantDir = new File(mutantDirString);

        int classIndex = classFile.getJavaFile().lastIndexOf("/");
        String classDirString = classFile.getJavaFile().substring(0,classIndex); // Source (class) directory
        File classDir = new File(classDirString);

        // Delete the directories
        FileUtils.deleteDirectory(testDir);
        FileUtils.deleteDirectory(mutantDir);
        FileUtils.deleteDirectory(classDir);

        // delete in this order to bypass all foreign key constraint errors
        String sql = String.format("DELETE FROM storytargetexecutions WHERE Test_ID = '%d'", testId.getTestId());
        String sql2 = String.format("DELETE FROM storytargetexecutions WHERE Mutant_ID = '%d'", mutantId.getMutantId());
        String sql3 = String.format("DELETE FROM puzzleTests WHERE Puzzle_ID='%d'", puzzleId);
        String sql4 = String.format("DELETE FROM puzzleMutants WHERE Puzzle_ID='%d'", puzzleId);
        String sql5 = String.format("DELETE FROM story WHERE Puzzle_ID='%d'", puzzleId);
        String sql6 = String.format("DELETE FROM puzzles WHERE Class_ID='%d'", classId);
        String sql7 = String.format("DELETE FROM classes WHERE Class_ID='%d'", classId);

        try {
            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.execute(sql2);
            stmt.execute(sql3);
            stmt.execute(sql4);
            stmt.execute(sql5);
            stmt.execute(sql6);
            stmt.execute(sql7);
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

    public int getLevelNum() { return levelNum; }

    public int getClassId() { return classId; }

    public String getPuzzleName() { return puzzleName; }

    public String getHint() { return hint; }

    public String getDesc() { return desc; }

}

package org.codedefenders;

/**
 * Created by joe on 04/04/2017.
 */

import javassist.ClassPool;
import javassist.CtClass;
import org.codedefenders.story.StoryGame;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.util.DatabaseAccess;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

public class PuzzleTest {

    private int testId;
    private int puzzleId;
    private String javaFile;
    private String classFile;

    private int mutantsKilled = 0;

    private int userId;

    public int getMutantsKilled() { return mutantsKilled; }

    private LineCoverage lineCoverage = new LineCoverage();

    private int score;

    public void setLineCoverage(LineCoverage lc) { lineCoverage = lc; }

    public LineCoverage getLineCoverage() { return lineCoverage; }

    public void setUserId(int id) { userId = id; }

    public int getUserId() { return userId; }

    public int getScore() { return score; }

    public void setScore(int s) { score += s; }

    private int aiMutantKilled = 0;

    public PuzzleTest(int puzzleId, String jFile, String cFile, int userId) {

        this.puzzleId = puzzleId;
        this.javaFile = jFile;
        this.classFile = cFile;
        this.userId = userId;
        score = 0;

    }

    public PuzzleTest(int tid, int pid, String jFile, String cFile, int mutantsKilled, int userId) {

        this(pid, jFile, cFile, userId);
        this.testId = tid;
        this.mutantsKilled = mutantsKilled;

    }

    // for getTestIdByPuzzle
    public PuzzleTest(int testId, String jFile) {

        this.testId = testId;
        this.javaFile = jFile;

    }

    public int getTestId() { return testId; }

    public int getPuzzleId() { return puzzleId; }

    public String getFolder() {

        int lio = javaFile.lastIndexOf("/");
        if (lio == -1) {
            lio = javaFile.lastIndexOf("\\");
        }
        return javaFile.substring(0,lio);
    }

    public void killMutant() {

        mutantsKilled++;
        update();

    }

    public List<String> getHTMLReadout() throws IOException {

        File testFile = new File(javaFile);
        List<String> testLines = new LinkedList<String>();

        String line = "";

        try {

            BufferedReader in = new BufferedReader(new FileReader(testFile));
            while ((line = in.readLine()) != null) {
                testLines.add(line);
            }

        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }

        return testLines;

    }

    public boolean insert() {

        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();

            String jFileDB = "'" + DatabaseAccess.addSlashes(javaFile) + "'";
            String cFileDB = classFile == null ? null : "'" + DatabaseAccess.addSlashes(classFile) + "'";

            sql = String.format("INSERT INTO puzzleTests (JavaFile, ClassFile, Puzzle_ID, User_ID, Points) " +
                    "VALUES (%s, %s, %d, %d, %d)", jFileDB, cFileDB, puzzleId, userId, score);

            stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                this.testId = rs.getInt(1);
                stmt.close();
                conn.close();
                return true;
            }

        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se3) {
                System.out.println(se3);
            }
        }

        return false;

    }

    public boolean update() {

        System.out.println("Updating Puzzle Test");
        Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {

            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();

            String linesCoveredString = "";
            String linesUncoveredString = "";
            if (lineCoverage != null) {
                for (int i : lineCoverage.getLinesCovered()) {
                    linesCoveredString += i + ",";
                }

                for (int i : lineCoverage.getLinesUncovered()) {
                    linesUncoveredString += i + ",";
                }

                if (linesCoveredString.length() > 0) {
                    linesCoveredString = linesCoveredString.substring(0, linesCoveredString.length() - 1);
                }

                if (linesUncoveredString.length() > 0) {
                    linesUncoveredString = linesUncoveredString.substring(0, linesUncoveredString.length() - 1);
                }

                // -1 for the leftover comma
            }

            sql = String.format("UPDATE puzzleTests SET mutantsKilled='%d', " +
                    "NumberAiMutantsKilled='%d', " +
                    "Lines_Covered='%s', " +
                    "Lines_Uncovered='%s', " +
                    "Points = %d " +
                    "WHERE Test_ID='%d'", mutantsKilled, aiMutantKilled, linesCoveredString, linesUncoveredString, score, testId);

            stmt.execute(sql);
            conn.close();
            stmt.close();
            return true;

        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {

            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se3) {
                System.out.println(se3);
            }
        }

        return false;

    }

    public String getFullyQualifiedClassName() {

        if (classFile == null)
            return null;

        ClassPool classPool = ClassPool.getDefault();
        CtClass cc = null;
        try {
            cc = classPool.makeClass(new FileInputStream(new File(classFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cc == null ? null : cc.getName();

    }

    public boolean isValid() { return classFile != null; }

    public void setAiMutantKilled(int count) { aiMutantKilled = count; }

    public int getAiMutantKilled() {

        if (aiMutantKilled == 0) {
            aiMutantKilled = DatabaseAccess.getNumAiMutantsKilledByPuzzleTest(getTestId());
        }

        return aiMutantKilled;

    }

    public void incrementAiMutantsKilled() { aiMutantKilled++; }

    public String getJavaFile() { return javaFile; }

    public void setJavaFile(String javaFile) { this.javaFile = javaFile; }

    public String getClassFile() { return classFile; }

    public void setClassFile(String classFile) { this.classFile = classFile; }

}

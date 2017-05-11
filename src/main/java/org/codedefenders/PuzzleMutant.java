package org.codedefenders;

/**
 * Created by joe on 04/04/2017.
 */

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codedefenders.story.StoryGame;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PuzzleMutant {

    private static Logger logger = LoggerFactory.getLogger(PuzzleMutant.class);

    private int mutantId;
    private int puzzleId;

    private String javaFile;
    private String md5;
    private String classFile;

    private boolean alive = true;

    private Equivalence equivalent;

    public enum Equivalence { ASSUMED_NO, PENDING_TEST, DECLARED_YES, ASSUMED_YES, PROVEN_NO }

    private int userId;
    private int killedByAITests = 0;
    private int score;

    private ArrayList<Integer> lines = null;
    private ArrayList<String> description = null;

    public PuzzleMutant(int puzzleId, String jFile, String cFile, boolean alive, int userId) {

        this.puzzleId = puzzleId;
        this.javaFile = jFile;
        this.classFile = cFile;
        this.alive = alive;
        this.equivalent = Equivalence.ASSUMED_NO;
        this.userId = userId;
        this.md5 = CodeValidator.getMD5FromFile(jFile);

    }

    public PuzzleMutant(int mid, int pid, String jFile, String cFile, boolean alive, Equivalence equiv, int userId) {

        this(pid, jFile, cFile, alive, userId);
        this.mutantId = mid;
        this.equivalent = equiv;
        score = 0;

    }

    // for getMutantIdByPuzzle
    public PuzzleMutant(int mutantId, String jFile) {

        this.mutantId = mutantId;
        this.javaFile = jFile;

    }

    public int getMutantId() { return mutantId; }

    public int getPuzzleId() { return puzzleId; }

    public Equivalence getEquivalent() { return equivalent; }

    public void setEquivalent(Equivalence e) {

        equivalent = e;
        if (e.equals(Equivalence.DECLARED_YES) || e.equals(Equivalence.ASSUMED_YES)) {
            score = 0;
        }
    }

    public String getSourceFile() { return javaFile; }

    public String getJavaFile() { return javaFile; }

    public String getClassFile() { return classFile; }

    public String getFolder() {

        int lio = javaFile.lastIndexOf("/");
        if (lio == -1) {
            lio = javaFile.lastIndexOf("\\");
        }

        return javaFile.substring(0, lio);

    }

    public boolean isAlive() { return alive; }

    public int sqlAlive() { return alive ? 1 : 0; }

    public int getUserId() { return userId; }

    public int getScore() { return score; }

    public void setScore(int score) { this.score += score; }

    public void kill() { kill(equivalent); }

    public void kill(Equivalence equivalent) {

        alive = false;
        setEquivalent(equivalent);

        update();

    }

    public Patch getDifferences() {

        int classId = DatabaseAccess.getPuzzleForUser(puzzleId).getClassId();
        StoryClass sut = DatabaseAccess.getStoryForKey(classId);

        File sourceFile = new File(sut.getJavaFile());
        File mutantFile = new File(javaFile);

        List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
        List<String> mutantLines = readLinesIfFileExist(mutantFile.toPath());

        return DiffUtils.diff(sutLines, mutantLines);

    }

    public String getPatchString() {

        int classId = DatabaseAccess.getPuzzleForUser(puzzleId).getClassId();
        StoryClass sut = DatabaseAccess.getStoryForKey(classId);

        File sourceFile = new File(sut.getJavaFile());
        File mutantFile = new File(javaFile);

        List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
        List<String> mutantLines = readLinesIfFileExist(mutantFile.toPath());

        Patch patch = DiffUtils.diff(sutLines, mutantLines);
        List<String> unifiedPatches = DiffUtils.generateUnifiedDiff(null, null, sutLines, patch, 3);
        StringBuilder unifiedPatch = new StringBuilder();

        for (String s : unifiedPatches) {

            if ("--- null".equals(s) || "+++ null".equals(s)) {
                continue;
            }
            unifiedPatch.append(s + System.getProperty("line.separator"));
        }

        return unifiedPatch.toString();

    }

    private List<String> readLinesIfFileExist(Path path) {

        List<String> lines = new ArrayList<>();
        try {
            if (Files.exists(path))
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            else
                logger.error("File not found {}", path);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return lines;
        }

    }

    public boolean insert() {

        Connection conn = null;
        Statement stmt = null;

        try {
            logger.info("Inserting mutant");

            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            System.out.println(puzzleId);

            String jFileDB = "'" + DatabaseAccess.addSlashes(javaFile) + "'";
            String cFileDB = classFile == null ? null : "'" + DatabaseAccess.addSlashes(classFile) + "'";
            String sql = String.format("INSERT INTO puzzleMutants (JavaFile, ClassFile, Puzzle_ID, Alive, User_ID, Points, MD5) " +
                    "VALUES (%s, %s, %d, %d, %d, %d, '%s');", jFileDB, cFileDB, puzzleId, sqlAlive(), userId, score, md5);

            stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                this.mutantId = rs.getInt(1);
                System.out.println("Setting Mutant ID to: " + this.mutantId);
                stmt.close();
                conn.close();
                return true;
            }
        } catch (SQLException se) {
            logger.error(se.getMessage());
            System.out.println(se);
        } catch (Exception e) {
            logger.error(e.getMessage());
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

        logger.info("Updating Mutant {}", getMutantId());

        Connection conn = null;
        Statement stmt = null;

        try {

            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            String sql = String.format("UPDATE puzzleMutants set Equivalent='%s', Alive='%d', NumberAiKillingTests='%d', Points=%d WHERE Mutant_ID='%d';",
                    equivalent.name(), sqlAlive(), killedByAITests, score, mutantId);
            stmt.execute(sql);

            conn.close();
            stmt.close();
            return true;
        } catch (SQLException se) {
            logger.error(se.getMessage());
            System.out.println(se);
        } catch (Exception e) {
            logger.error(e.getMessage());
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

    public void setTimesKilledAi(int count) { killedByAITests = count; }

    public int getTimesKilledAi() {

        if (killedByAITests == 0) {
            killedByAITests = DatabaseAccess.getNumTestsKillPuzzleMutant(getMutantId());
        }

        return killedByAITests;

    }

    public void incrementTimesKilledAi() { killedByAITests++; }

    public ArrayList<Integer> getLines() {

        if (lines != null) {
            return lines;
        }

        lines = new ArrayList<>();
        description = new ArrayList<>();

        Patch p = getDifferences();
        for (Delta d : p.getDeltas()) {

            Chunk c = d.getOriginal();
            Delta.TYPE t = d.getType();

            int firstLine = c.getPosition() + 1;
            String desc = "Line " + firstLine;
            lines.add(firstLine);

            int endLine = firstLine + c.getLines().size() - 1;
            if (endLine > firstLine) {
                for (int l = firstLine + 1; l <= endLine; l++) {
                    lines.add(l);
                }
                desc = String.format("lines %d-%d", firstLine, endLine);
            }

            if (t == Delta.TYPE.CHANGE) {
                description.add("Modified " + desc + "\n");
            } else if (t == Delta.TYPE.DELETE) {
                description.add("Removed " + desc + "\n");
            } else {
                description.add("Added " + desc + "\n");
            }
        }

        return lines;

    }

    public List<String> getHTMLReadout() throws IOException {

        if (description != null) {
            return description;
        }

        getLines();

        return description;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PuzzleMutant mutant = (PuzzleMutant) o;

        return new EqualsBuilder()
                .append(mutantId, mutant.mutantId)
                .append(puzzleId, mutant.puzzleId)
                .append(userId, mutant.userId)
                .append(javaFile, mutant.javaFile)
                .append(md5, mutant.md5)
                .append(classFile, mutant.classFile)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17,37)
                .append(mutantId)
                .append(puzzleId)
                .append(userId)
                .append(md5)
                .append(classFile)
                .toHashCode();
    }

}

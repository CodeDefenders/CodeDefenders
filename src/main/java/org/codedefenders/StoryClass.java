package org.codedefenders;

/**
 * Created by joe on 25/03/2017.
 */

import org.codedefenders.story.StoryGame;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class StoryClass {

    private static final Logger logger = LoggerFactory.getLogger(StoryClass.class);

    private int classId;
    private int puzzleId;
    private int creatorId;
    private String name;
    private String alias;
    private String javaFile;
    private String classFile;

    // getStoryForKey
    public StoryClass(int puzzleId, int classId, int creatorId, String name, String alias, String javaFile, String classFile) {

        this.puzzleId = puzzleId;
        this.classId = classId;
        this.creatorId = creatorId;
        this.name = name;
        this.alias = alias;
        this.javaFile = javaFile;
        this.classFile = classFile;

    }

    public StoryClass (int classId, String name, String alias, String jFile, String cFile) {

        this.classId = classId;
        this.name = name;
        this.alias = alias;
        this.javaFile = jFile;
        this.classFile = cFile;

    }

    public StoryClass(String name, String alias, String jFile, String cFile, int creatorId) {

        this.name = name;
        this.alias = alias;
        this.javaFile = jFile;
        this.classFile = cFile;
        this.creatorId = creatorId;

    }

    public StoryClass(String jFile) {

        this.javaFile = jFile;

    }

    public int getClassId() { return classId; }

    public int getPuzzleId() { return puzzleId; }

    public int getCreatorId() { return creatorId; }

    public String getClassName() { return name; }

    public String getBaseName() {
        String[] tokens = name.split("\\.");
        return tokens[tokens.length-1];
    }

    public void setClassName(String name) { this.name = name; }

    public String getAlias() { return alias; }

    public void setAlias(String alias) { this.alias = alias; }

    public String getPackage() {
        return (name.contains(".")) ? name.substring(0, name.lastIndexOf('.')) : "";
    }

    public String getAsString() {
        InputStream resourceContent = null;
        String result = "";
        try{
            resourceContent = new FileInputStream(javaFile);
            BufferedReader is = new BufferedReader((new InputStreamReader(resourceContent)));
            String line;
            while ((line = is.readLine()) != null) {
                result += line + "\n";
            }
        } catch (FileNotFoundException e) {
                result = "[File not found]";
                e.printStackTrace();
        } catch (IOException e) {
            result = "[File not readable]";
            e.printStackTrace();
        }
        return result;
    }

    // for defender view, initial load up
    public String getTestTemplate() {
        StringBuilder sb = new StringBuilder();
        if (! getPackage().isEmpty())
            sb.append(String.format("package %s;%n", getPackage()));
        else
            sb.append(String.format("/* no package name */%n"));
        sb.append(String.format("%n"));
        sb.append(String.format("import org.junit.*;%n"));
        sb.append(String.format("import static org.junit.Assert.*;%n%n"));
        sb.append(String.format("public class Test%s {%n", getBaseName()));
        sb.append(String.format("%c@Test(timeout = 4000)%n",'\t'));
        sb.append(String.format("%cpublic void test() throws Throwable {%n",'\t'));
        sb.append(String.format("%c%c// test here!%n",'\t','\t'));
        sb.append(String.format("%c}%n",'\t'));
        sb.append(String.format("}"));
        return sb.toString();
    }

    public boolean insert() {

        logger.debug("Insert class (Creator_ID={}, ClassName={}, Alias={}, JavaFile={}, ClassName={}", creatorId, name, alias, javaFile, classFile);
        Connection conn = null;
        Statement stmt = null;
        String sql = String.format("INSERT INTO classes (Creator_ID, Name, Alias, JavaFile, ClassFile) VALUES ('%d', '%s', '%s', '%s', '%s');", creatorId, name, alias, javaFile, classFile);

        try {
            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                this.classId = rs.getInt(1);
                System.out.println("Inserted CUT with ID: " + this.classId);
                stmt.close();
                conn.close();
                return true;
            }
        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            DatabaseAccess.cleanup(conn, stmt);
        }

        return false;

    }

    public boolean update() {

        logger.debug("Updating class (Name={}, Alias={}, JavaFile={}, ClassFile={})", name, alias, javaFile, classFile);
        Connection conn = null;
        Statement stmt = null;

        String sql = String.format("UPDATE classes SET Name='%s', Alias='%s', JavaFile='%s', ClassFile='%s' WHERE Class_ID='%d';", name, alias, javaFile, classFile, classId);
        String sql2 = String.format("INSERT INTO puzzles (Class_ID, Level_ID) VALUES ('%d', '%d');", classId, 1);
        // Attempt to update game info into database
        try {
            conn = DatabaseAccess.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.execute(sql2);
            puzzleId = DatabaseAccess.getPuzzleId(classId).getPuzzleId();
            String sql3 = String.format("INSERT INTO story (User_ID, Puzzle_ID) VALUES ('%d','%d');", creatorId, puzzleId);
            String sql4 = String.format("UPDATE puzzles SET Class_ID='%d' WHERE Puzzle_ID='%d'", classId, puzzleId);
            stmt.execute(sql3);
            stmt.execute(sql4);
            stmt.close();
            conn.close();
            return true;
        } catch (SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
        } catch (Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
        } finally {
            DatabaseAccess.cleanup(conn, stmt);
        } //end try
        return false;
    }

    public boolean updateMT() {

        logger.debug("Updating class (Name={}, Alias={}, JavaFile={}, ClassFile={}", name, alias, javaFile, classFile);
        Connection conn = null;
        Statement stmt = null;

        String sql = String.format("UPDATE classes SET Name='%s', Alias='%s', JavaFile='%s', ClassFile='%s' WHERE Class_ID='%d';", name, alias, javaFile, classFile, classId);

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

    public boolean deleteClass() {

        logger.debug("Deleting class(ID={})", classId);

        Connection conn = null;
        Statement stmt = null;

        String sql = String.format("DELETE FROM classes WHERE Class_ID = %d", classId);

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

    public boolean deleteMT() {
        logger.debug("Deleting class(ID={}", classId);
        Connection conn = null;
        Statement stmt = null;

        String sql = String.format("DELETE FROM classes WHERE Class_ID='%d';", classId);

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

    public String getJavaFile() { return javaFile; }

    public void setJavaFile(String javaFile) { this.javaFile = javaFile; }

    public String getClassFile() { return classFile; }

    public void setClassFile(String classFile) { this.classFile = classFile; }

    public void setName(String name) {
        this.name = name;
    }

}

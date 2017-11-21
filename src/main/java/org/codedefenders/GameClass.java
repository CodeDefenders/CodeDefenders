package org.codedefenders;

import org.codedefenders.duel.DuelGame;
import org.codedefenders.singleplayer.NoDummyGameException;
import org.codedefenders.util.DB;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;

public class GameClass {

    private static final Logger logger = LoggerFactory.getLogger(GameClass.class);

    private int id;
    private String name; // fully qualified name
    private String alias;
    private String javaFile;
    private String classFile;

    public GameClass(String name, String alias, String jFile, String cFile) {
        this.name = name;
        this.alias = alias;
        this.javaFile = jFile;
        this.classFile = cFile;
    }

    public GameClass(int id, String name, String alias, String jFile, String cFile) {
        this(name, alias, jFile, cFile);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseName() {
        String[] tokens = name.split("\\.");
        return tokens[tokens.length - 1];
    }

    public String getPackage() {
        return (name.contains(".")) ? name.substring(0, name.lastIndexOf('.')) : "";
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAsString() {
        InputStream resourceContent = null;
        String result = "";
        try {
            resourceContent = new FileInputStream(javaFile);
            BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
            String line;
            while ((line = is.readLine()) != null) {
                result += line + "\n";
            }

        } catch (FileNotFoundException e) {
            result = "[File Not Found]";
            logger.error("Could not find file " + javaFile);
        } catch (IOException e) {
            result = "[File Not Readable]";
            logger.error("Could not read file " + javaFile);
        }
        return result;

    }

    public boolean insert() {
        logger.debug("Inserting class (Name={}, Alias={}, JavaFile={}, ClassFile={})", name, alias, javaFile, classFile);
        // Attempt to insert game info into database
        Connection conn = DB.getConnection();
        String query = "INSERT INTO classes (Name, Alias, JavaFile, ClassFile) VALUES (?, ?, ?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(name), DB.getDBV(alias),
                DB.getDBV(javaFile), DB.getDBV(classFile)};
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        int res = DB.executeUpdateGetKeys(stmt, conn);
        if (res > -1) {
            this.id = res;
            logger.debug("Inserted CUT with ID: " + this.id);
            return true;
        }
        return false;
    }

    public boolean update() {
        logger.debug("Updating class (Name={}, Alias={}, JavaFile={}, ClassFile={})", name, alias, javaFile, classFile);
        // Attempt to update game info into database
        Connection conn = DB.getConnection();
        String query = "UPDATE classes SET Name=?, Alias=?, JavaFile=?, ClassFile=? WHERE Class_ID=?;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(name),
                DB.getDBV(alias),
                DB.getDBV(javaFile),
                DB.getDBV(classFile),
                DB.getDBV(id)};
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdate(stmt, conn);
    }

    public String getTestTemplate() {
        StringBuilder sb = new StringBuilder();
        if (!getPackage().isEmpty())
            sb.append(String.format("package %s;%n", getPackage()));
        else
            sb.append(String.format("/* no package name */%n"));
        sb.append(String.format("%n"));
        sb.append(String.format("import org.junit.*;%n"));
        sb.append(String.format("import static org.junit.Assert.*;%n%n"));
        sb.append(String.format("public class Test%s {%n", getBaseName()));
        sb.append(String.format("%c@Test(timeout = 4000)%n", '\t'));
        sb.append(String.format("%cpublic void test() throws Throwable {%n", '\t'));
        sb.append(String.format("%c%c// test here!%n", '\t', '\t'));
        sb.append(String.format("%c}%n", '\t'));
        sb.append(String.format("}"));
        return sb.toString();
    }

    public String getJavaFile() {
        return javaFile;
    }

    public void setJavaFile(String javaFile) {
        this.javaFile = javaFile;
    }

    public String getClassFile() {
        return classFile;
    }

    public void setClassFile(String classFile) {
        this.classFile = classFile;
    }

    public DuelGame getDummyGame() throws NoDummyGameException {
        DuelGame dg = DatabaseAccess.getAiDummyGameForClass(this.getId());
        return dg;
    }

    public boolean delete() {
        logger.debug("Deleting class (ID={})", id);
        // Attempt to update game info into database
        Connection conn = DB.getConnection();
        String query = "DELETE FROM classes WHERE Class_ID=?;";
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));
        return DB.executeUpdate(stmt, conn);
    }
}
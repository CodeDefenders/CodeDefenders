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
package org.codedefenders.game;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.*;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * This class represents a test case. These test cases are created by defenders
 * to find mutations in a game class.
 *
 * @see GameClass
 * @see Mutant
 */
public class Test {
    private static final Logger logger = LoggerFactory.getLogger(Test.class);

    private int id;
    private int playerId;
    private int gameId;
    private int classId;
    private String javaFile;
    private String classFile;

    private int roundCreated;
    private int mutantsKilled;
    private int score;
    private LineCoverage lineCoverage;

    /**
     * Creates a new Test with following attributes:
     * <ul>
     * <li><code>gameId -1</code></li>
     * <li><code>playerId -1</code></li>
     * <li><code>roundCreated -1</code></li>
     * <li><code>score 0</code></li>
     * </ul>
     */
    public Test(String javaFilePath, String classFilePath, int classId, LineCoverage lineCoverage) {
        this.javaFile = javaFilePath;
        this.classFile = classFilePath;
        this.gameId = Constants.DUMMY_GAME_ID;
        this.playerId = Constants.DUMMY_CREATOR_USER_ID;
        this.roundCreated = -1;
        this.score = 0;
        this.classId = classId;
        this.lineCoverage = lineCoverage;
    }

    public Test(int classId, int gameId, String javaFile, String classFile, int playerId) {
        this.classId = classId;
        this.gameId = gameId;
        this.roundCreated = GameDAO.getCurrentRound(gameId);
        this.javaFile = javaFile;
        this.classFile = classFile;
        this.playerId = playerId;
        this.score = 0;
        this.lineCoverage = new LineCoverage();
    }

    @Deprecated
    public Test(int testId, int classId, int gameId, String javaFile, String classFile, int roundCreated, int mutantsKilled, int playerId) {
        this(testId, classId, gameId, javaFile, classFile, roundCreated, mutantsKilled, playerId, Collections.emptyList(), Collections.emptyList(), 0);
    }

    public Test(int testId, int classId, int gameId, String javaFile, String classFile, int roundCreated, int mutantsKilled,
                int playerId, List<Integer> linesCovered, List<Integer> linesUncovered, int score) {
        this(classId, gameId, javaFile, classFile, playerId);

        this.id = testId;
        this.roundCreated = roundCreated;
        this.mutantsKilled = mutantsKilled;
        this.score = score;
        lineCoverage = new LineCoverage(linesCovered, linesUncovered);
    }

    /**
     * Creates a test from another test instance, but in a new game and for a new player.
     *
     * @param gameId   the game identifier of the new test.
     * @param playerId the players identifier of the new test.
     * @param other    the test instance the new is created for.
     * @return a test based on another test instance, gameId and playerId.
     */
    public static Test newTestForGameAndPlayerIds(int gameId, int playerId, Test other) {
        final Test test = new Test(other.classId, gameId, other.javaFile, other.classFile, playerId);
        test.lineCoverage = other.lineCoverage;
        return test;
    }

    // TODO Check that increment score does not consider mutants that were killed already
    public void incrementScore(int score) {
        if (score == 0) {
            // Why this is happening?
            // Phil: ^ because the calculated score for this test so far is zero (e.g. no mutants in a game yet)
            logger.warn("Do not increment score for test {} when score is zero", getId());
            return;
        }

        String query = "UPDATE tests SET Points = Points + ? WHERE Test_ID=?;";
        Connection conn = DB.getConnection();

        DatabaseValue[] valueList = new DatabaseValue[] { DatabaseValue.of(score), DatabaseValue.of(id) };

        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        boolean incremented = DB.executeUpdate(stmt, conn);

        logger.info("Increment score for {} by {}. Update? {} ", toString(), score, incremented);
    }

    @Deprecated()
    public void setScore(int store) {
        score = store;
    }

    public void updateScore(int score) {
        this.score += score;
    }

    public int getId() {
        return id;
    }

    public int getGameId() {
        return gameId;
    }

    public int getMutantsKilled() {
        return mutantsKilled;
    }

    public int getRoundCreated() {
        return roundCreated;
    }

    public String getDirectory() {
        File file = new File(javaFile);
        return file.getAbsoluteFile().getParent();
    }

    // Increment the number of mutant killed directly on the DB
    // And update the local object. But it requires several queries/connections
    //
    // TODO Check that this method is never called for tests that kill a mutant that was already dead...
    public void killMutant() {
        // TODO Phil 06/08/19: Why isn't the out-commented code called?
        // mutantsKilled++;
        // update();
        logger.info("Test {} killed a new mutant", getId());

        String query = "UPDATE tests SET MutantsKilled = MutantsKilled + ? WHERE Test_ID=?;";
        Connection conn = DB.getConnection();

        DatabaseValue[] valueList = new DatabaseValue[] { DatabaseValue.of(1), DatabaseValue.of(id) };

        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        boolean updated = DB.executeUpdate(stmt, conn);

        // Eventually update the kill count from the DB
        mutantsKilled = TestDAO.getTestById(getId()).getMutantsKilled();

        logger.info("Test {} new killcount is {}. Was updated ? {} ", toString(), mutantsKilled, updated);
    }

    public boolean isMutantCovered(Mutant mutant) {
        return CollectionUtils.containsAny(lineCoverage.getLinesCovered(), mutant.getLines());
    }

    public Set<Mutant> getCoveredMutants(List<Mutant> mutants) {
        List<Integer> coverage = lineCoverage.getLinesCovered();
        Set<Mutant> coveredMutants = new TreeSet<>(Mutant.orderByIdAscending());

        for(Mutant m : mutants) {
            if(CollectionUtils.containsAny(coverage, m.getLines())) {
                coveredMutants.add(m);
            }
        }

        return coveredMutants;
    }

    public Set<Mutant> getKilledMutants() {
        return DatabaseAccess.getKilledMutantsForTestId(id);
    }

    private String getAsString() {
        return FileUtils.readJavaFileWithDefault(Paths.get(javaFile));
    }

    @SuppressWarnings("Duplicates")
    public String getAsHTMLEscapedString() {
        return StringEscapeUtils.escapeHtml(getAsString());
    }

    public boolean insert() {
        try {
            this.id = TestDAO.storeTest(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store test to database.", e);
            return false;
        }
    }

    public boolean update() {
        try {
            return TestDAO.updateTest(this);
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store test to database.", e);
            return false;
        }
    }

    public String getFullyQualifiedClassName() {
        if (classFile == null)
            return null;

        ClassPool classPool = ClassPool.getDefault();
        CtClass cc = null;
        try {
            cc = classPool.makeClass(new FileInputStream(new File(classFile)));
        } catch (IOException e) {
            logger.error("IO exception caught", e);
        }
        return cc == null ? null : cc.getName();
    }

    public boolean isValid() {
        return classFile != null;
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

    public LineCoverage getLineCoverage() {
        return lineCoverage;
    }

    public void setPlayerId(int id) {
        playerId = id;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getScore() {
        return score;
    }

    public int getClassId() {
        return classId;
    }

    public void setLineCoverage(LineCoverage lineCoverage) {
        this.lineCoverage = lineCoverage;
    }

    // First created appears first
    public static Comparator<Test> orderByIdAscending() {
        return (o1, o2) -> o1.id - o2.id;
    }

    // Last created appears first
    public static Comparator<Test> orderByIdDescending() {
        return (o1, o2) -> o2.id - o1.id;
    }

    @Override
    public String toString() {
        return "[testId=" + id + ",classId="+ classId + ",mutantsKilled=" + mutantsKilled + ",score=" + score + "]";
    }
}

/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Creates a new Test with following attributes.
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

    public Test(int classId, int gameId, int roundCreated, String javaFile, String classFile, int playerId) {
        this.classId = classId;
        this.gameId = gameId;
        this.roundCreated = roundCreated;
        this.javaFile = javaFile;
        this.classFile = classFile;
        this.playerId = playerId;
        this.score = 0;
        this.lineCoverage = LineCoverage.empty();
    }

    public Test(int testId, int classId, int gameId, String javaFile, String classFile, int roundCreated,
                int mutantsKilled, int playerId, List<Integer> linesCovered, List<Integer> linesUncovered, int score) {
        this.classId = classId;
        this.gameId = gameId;
        this.javaFile = javaFile;
        this.classFile = classFile;
        this.playerId = playerId;
        this.id = testId;
        this.roundCreated = roundCreated;
        this.mutantsKilled = mutantsKilled;
        this.score = score;
        this.lineCoverage = new LineCoverage(linesCovered, linesUncovered);
    }

    @Deprecated
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

    public void setMutantsKilled(int mutantsKilled) {
        this.mutantsKilled = mutantsKilled;
    }

    public boolean isMutantCovered(Mutant mutant) {
        return CollectionUtils.containsAny(lineCoverage.getLinesCovered(), mutant.getLines());
    }

    public Set<Mutant> getCoveredMutants(List<Mutant> mutants) {
        List<Integer> coverage = lineCoverage.getLinesCovered();
        Set<Mutant> coveredMutants = new TreeSet<>(Comparator.comparing(Mutant::getId));

        for (Mutant m : mutants) {
            if (CollectionUtils.containsAny(coverage, m.getLines())) {
                coveredMutants.add(m);
            }
        }

        return coveredMutants;
    }

    public String getAsString() {
        return FileUtils.readJavaFileWithDefault(Paths.get(javaFile));
    }

    public String getAsHTMLEscapedString() {
        return StringEscapeUtils.escapeHtml4(getAsString());
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullyQualifiedClassName() {
        if (classFile == null) {
            return null;
        }

        ClassPool classPool = ClassPool.getDefault();
        CtClass cc = null;
        try {
            cc = classPool.makeClass(new FileInputStream(classFile));
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

    @Override
    public String toString() {
        return "[testId=" + id + ",classId=" + classId + ",mutantsKilled=" + mutantsKilled + ",score=" + score + "]";
    }

    // equals similar to Mutant::equals
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Test test = (Test) o;

        return new EqualsBuilder()
                .append(id, test.id)
                .append(gameId, test.gameId)
                .append(playerId, test.playerId)
                .append(javaFile, test.javaFile)
                .append(classFile, test.classFile)
                .isEquals();
    }

    // hashCode similar to Mutant::hashCode
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(gameId)
                .append(playerId)
                .append(javaFile)
                .append(classFile)
                .toHashCode();
    }
}

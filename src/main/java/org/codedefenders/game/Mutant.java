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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseValue;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

/**
 * This class represents a mutation in a game class. These mutations are created
 * by attackers in order to survive test cases.
 *
 * @see GameClass
 * @see Test
 */
public class Mutant implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Mutant.class);
    // https://stackoverflow.com/questions/9577930/regular-expression-to-select-all-whitespace-that-isnt-in-quotes
    public static String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";

    private int id;
    private int gameId;
    private int classId;

    private transient String javaFile;
    private transient String md5;
    private transient String classFile;

    private String creatorName;
    private int creatorId;

    private boolean alive;

    private Equivalence equivalent;

    // Derived and cached
    private String summaryString;

    /* Mutant Equivalence */
    public enum Equivalence {
        ASSUMED_NO, PENDING_TEST, DECLARED_YES, ASSUMED_YES, PROVEN_NO
    }

    private int roundCreated;
    private int roundKilled;

    private int playerId;

    /**
     * Indicates how many times this mutant is
     * killed by an AI test.
     */
    private transient int killedByAITests = 0;

    private int score; // multiplayer

    // Computed on the fly if not read in the db
    private List<Integer> lines = null;
    private transient List<String> description = null;
    private transient Patch difference = null;

    private String killMessage;

    /**
     * Creates a new Mutant with following attributes:
     * <ul>
     * <li><code>gameId -1</code></li>
     * <li><code>playerId -1</code></li>
     * <li><code>roundCreated -1</code></li>
     * <li><code>score 0</code></li>
     * </ul>
     */
    public Mutant(String javaFilePath, String classFilePath, String md5, int classId) {
        this.javaFile = javaFilePath;
        this.classFile = classFilePath;
        this.alive = false;
        this.gameId = Constants.DUMMY_GAME_ID;
        this.playerId = Constants.DUMMY_CREATOR_USER_ID;
        this.roundCreated = -1;
        this.score = 0;
        this.md5 = md5;
        this.classId = classId;
    }

    /**
     * Creates a mutant
     *
     * @param gameId
     * @param jFile
     * @param cFile
     * @param alive
     * @param playerId
     */
    public Mutant(int gameId, int classId,  String jFile, String cFile, boolean alive, int playerId) {
        this.gameId = gameId;
        this.classId = classId;
        this.roundCreated = GameDAO.getCurrentRound(gameId);
        this.javaFile = jFile;
        this.classFile = cFile;
        this.alive = alive;
        this.equivalent = Equivalence.ASSUMED_NO;
        this.playerId = playerId;
        this.md5 = CodeValidator.getMD5FromFile(jFile); // TODO: This may be null
    }

    public Mutant(int mid, int classId, int gid, String jFile, String cFile, boolean alive, Equivalence equiv, int rCreated, int rKilled, int playerId) {
        this(gid, classId, jFile, cFile, alive, playerId);
        this.id = mid;
        this.equivalent = equiv;
        this.roundCreated = rCreated;
        this.roundKilled = rKilled;

        score = 0;
    }

    public Mutant(int mid, int classId, int gid, String jFile, String cFile, boolean alive, Equivalence equiv, int rCreated, int rKilled, int playerId, String md5, String killMessage) {
        this(mid, classId, gid, jFile, cFile, alive, equiv, rCreated, rKilled, playerId);
        this.md5 = md5;
        this.killMessage = killMessage;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public int getId() {
        return id;
    }

    public int getGameId() {
        return gameId;
    }

    public Equivalence getEquivalent() {
        return equivalent;
    }

    public void setEquivalent(Equivalence e) {
        equivalent = e;
        if (e.equals(Equivalence.DECLARED_YES) || e.equals(Equivalence.ASSUMED_YES)) {
            score = 0;
        }
    }

    public String getClassFile() {
        return classFile;
    }

    public String getJavaFile() {
        return javaFile;
    }

    public int getRoundCreated() {
        return roundCreated;
    }

    public int getRoundKilled() {
        return roundKilled;
    }

    public String getMd5() {
        return md5;
    }

    public String getDirectory() {
        File file = new File(javaFile);
        return file.getAbsoluteFile().getParent();
    }

    public boolean isAlive() {
        return alive;
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

    // TODO why does incrementScore update the DB entry, shouldn't this be done with update()
    // TODO Phil 12/12/18: extract database logic to MutantDAO
    public void incrementScore(int score){
        if( score == 0 ){
            logger.debug("Do not update mutant {} score by 0", getId());
            return;
        }

        String query = "UPDATE mutants SET Points = Points + ? WHERE Mutant_ID=? AND Alive=1;";
        Connection conn = DB.getConnection();

        DatabaseValue[] valueList = new DatabaseValue[]{
                DatabaseValue.of(score), DatabaseValue.of(id)
        };

        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        DB.executeUpdate(stmt, conn);
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean kill() {
        return kill(equivalent);
    }

    // TODO Phil 12/12/18: extract database logic to MutantDAO
    public boolean kill(Equivalence equivalent) {
        alive = false;
        roundKilled = GameDAO.getCurrentRound(gameId);
        setEquivalent(equivalent);

        // This should be blocking
        Connection conn = DB.getConnection();

        String query;
        if (equivalent.equals(Equivalence.DECLARED_YES) || equivalent.equals(Equivalence.ASSUMED_YES)) {
            // if mutant is equivalent, we need to set score to 0
            query = "UPDATE mutants SET Equivalent=?, Alive=?, RoundKilled=?, Points=0 WHERE Mutant_ID=? AND Alive=1;";
        } else {
            // We cannot update killed mutants
            query = "UPDATE mutants SET Equivalent=?, Alive=?, RoundKilled=? WHERE Mutant_ID=? AND Alive=1;";
        }

        DatabaseValue[] values = new DatabaseValue[]{
            DatabaseValue.of(equivalent.name()),
            DatabaseValue.of(alive),
            DatabaseValue.of(roundKilled),
            DatabaseValue.of(id)
        };
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, values);
        return DB.executeUpdate(stmt, conn);
    }

    public boolean isCovered() {
        List<Test> tests = TestDAO.getValidTestsForGame(gameId, true);
        for (Test t : tests) {
            if (CollectionUtils.containsAny(t.getLineCoverage().getLinesCovered(), getLines()))
                return true;
        }
        return false;
    }

    // This might return several instances of the same test since Test does not implement hash and equalsTo
    public Set<Test> getCoveringTests() {
        Set<Test> coveringTests = new LinkedHashSet<>();

        for(Test t : TestDAO.getValidTestsForGame(gameId, false)) {
            if(t.isMutantCovered(this)) {
                coveringTests.add(t);
            }
        }

        return coveringTests;
    }

    public boolean doesRequireRecompilation() {
        // dummy game with id = -1 has null class, and this check cannot be implemented...
        GameClass cut = GameClassDAO.getClassForGameId(gameId);
        if(  cut == null ){
            cut = GameClassDAO.getClassForId(classId);
        }
        return CollectionUtils.containsAny(cut.getCompileTimeConstants(), getLines());
    }

    public Patch getDifferences() {
        if (difference == null) {
            computeDifferences();
        }
        return difference;
    }

    // Not sure
    private void computeDifferences() {
        GameClass sut = GameClassDAO.getClassForGameId(gameId);
        if( sut == null ){
            // in this case gameId might have been -1 (upload)
            // so we try to reload the sut
            sut = GameClassDAO.getClassForId(classId);
        }

        assert sut != null;

        File sourceFile = new File(sut.getJavaFile());
        File mutantFile = new File(javaFile);

        List<String> sutLines = FileUtils.readLines(sourceFile.toPath());
        List<String> mutantLines = FileUtils.readLines(mutantFile.toPath());

        for (int l = 0; l < sutLines.size(); l++) {
            sutLines.set(l, sutLines.get(l).replaceAll(regex, ""));
        }

        for (int l = 0; l < mutantLines.size(); l++) {
            mutantLines.set(l, mutantLines.get(l).replaceAll(regex, ""));
        }

        difference = DiffUtils.diff(sutLines, mutantLines);
    }

    public String getPatchString() {
        GameClass sut = GameClassDAO.getClassForGameId(gameId);
        if( sut == null ){
            // in this case gameId might have been -1 (upload)
            // so we try to reload the sut
            sut = GameClassDAO.getClassForId(classId);
        }

        Path sourceFile = Paths.get(sut.getJavaFile());
        Path mutantFile = Paths.get(javaFile);

        List<String> sutLines = FileUtils.readLines(sourceFile);
        List<String> mutantLines = FileUtils.readLines(mutantFile);

        Patch patch = DiffUtils.diff(sutLines, mutantLines);
        List<String> unifiedPatches = DiffUtils.generateUnifiedDiff(null, null, sutLines, patch, 3);
        StringBuilder unifiedPatch = new StringBuilder();
        for (String s : unifiedPatches) {
            if ("--- null".equals(s) || "+++ null".equals(s))
                continue;
            unifiedPatch.append(s).append(System.getProperty("line.separator"));
        }
        return unifiedPatch.toString();
    }

    public String getHTMLEscapedPatchString() {
        return StringEscapeUtils.escapeHtml(getPatchString());
    }

    public Test getKillingTest(){
        return DatabaseAccess.getKillingTestForMutantId(id);
    }

    public String getKillMessage() {
        if( killMessage != null ){
            return killMessage;
        } else {
            return Constants.DEFAULT_KILL_MESSAGE;
        }

    }
    public String getHTMLEscapedKillMessage() {
        return StringEscapeUtils.escapeHtml(getKillMessage());
    }


    public boolean insert() {
        try {
            this.id = MutantDAO.storeMutant(this);
            return true;
        } catch (Exception e) {
            logger.error("Inserting mutants resulted in error.", e);
            return false;
        }
    }

    public boolean update() {
        try {
            return MutantDAO.updateMutant(this);
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store mutant to database.", e);
            return false;
        }
    }

    // Does this every get called if mutant is not stored to DB ?
    public List<Integer> getLines() {
        if (lines == null) {
            computeLinesAndDescription();
        }
        return lines;
    }

    public String getSummaryString(){
        if (summaryString == null) {
            computeLinesAndDescription();
        }
        return summaryString;
    }

    /**
     * Identify lines in the original source code that have been modified
     * by a mutation.
     *
     * An insertion only modifies the line it was inserted in
     */
    private void computeLinesAndDescription() {
        // This workflow is not really nice...
        List<Integer> mutatedLines = new ArrayList<>();
        description = new ArrayList<>();

        List<String> fragementSummary = new ArrayList<>();

        Patch p = getDifferences();
        for (Delta d : p.getDeltas()) {
            Chunk chunk = d.getOriginal();
            // position starts at 0 but code readout starts at 1
            int firstLine = chunk.getPosition() + 1;
            String desc = "line " + firstLine;
            fragementSummary.add(String.format("%d", firstLine));
            // was it one single line or several?
            mutatedLines.add(firstLine);
            int endLine = firstLine + chunk.getLines().size() - 1;
            if (endLine > firstLine) {
                // if more than one line, report range of lines;
                // may not be 100% accurate, but is all we have in the delta
                // chunk
                for (int l = firstLine + 1; l <= endLine; l++) {
                    mutatedLines.add(l);
                }
                desc = String.format("lines %d-%d", firstLine, endLine);
                fragementSummary.remove( fragementSummary.size() - 1);
                fragementSummary.add( String.format("%d-%d", firstLine, endLine) );
            }
            // update mutant description
            String text;
            switch (d.getType()) {
            case CHANGE:
                text = "Modified ";
                break;
            case DELETE:
                text = "Removed ";
                break;
            case INSERT:
                text = "Added ";
                break;
            default:
                throw new IllegalStateException("Found unknown delta type " + d.getType());
            }
            description.add(StringEscapeUtils.escapeHtml(text + desc + "\n"));
        }

        setLines( mutatedLines );

        // Generate the summaryString
        summaryString = String.join(",", fragementSummary);
    }

    public synchronized List<String> getHTMLReadout() {
        if (description == null) {
            computeLinesAndDescription();
        }
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Mutant mutant = (Mutant) o;

        return new EqualsBuilder()
                .append(id, mutant.id)
                .append(gameId, mutant.gameId)
                .append(playerId, mutant.playerId)
                .append(javaFile, mutant.javaFile)
                .append(md5, mutant.md5)
                .append(classFile, mutant.classFile)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(gameId)
                .append(playerId)
                .append(javaFile)
                .append(md5)
                .append(classFile)
                .toHashCode();
    }

    public void prepareForSerialise(boolean showDifferences) {
        getHTMLReadout();
        getLines();
        if (showDifferences)
            getDifferences();
        else
            difference = new Patch();
    }

    /*
     * This comparators place first mutants that modify lines at the top of the file.
     */
    public static Comparator<Mutant> sortByLineNumberAscending() {
        return (o1, o2) -> {
            List<Integer> lines1 = o1.getLines();
            List<Integer> lines2 = o2.getLines();

            if (lines1.isEmpty()) {
                if (lines2.isEmpty()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (lines2.isEmpty()) {
                return 1;
            }

            return Collections.min(lines1) - Collections.min(lines2);
        };
    }

    // TODO Ideally this should have a timestamp ... we use the ID instead
    // First created appears first
    public static Comparator<Mutant> orderByIdAscending() {
        return (o1, o2) -> o1.id - o2.id;
    }

    // Last created appears first
    public static Comparator<Mutant> orderByIdDescending() {
        return (o1, o2) -> o2.id - o1.id;
    }

    public void setLines(List<Integer> mutatedLines) {
        this.lines = mutatedLines;
    }

    @Override
    public String toString() {
        return "[mutantId=" + getId() + ",alive="+ isAlive() + ",equivalent=" + getEquivalent() + ",score=" + getScore() + "]";
    }

    public void setKillMessage(String message) {
        this.killMessage = message;
    }
}

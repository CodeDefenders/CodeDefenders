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
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;

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
    public static String unquotedWhitespaceRegex =
            "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";

    private int id;
    private int gameId;
    private int classId;

    private transient String javaFile;
    private transient String md5;
    private transient String classFile;

    private String creatorName;
    private int creatorId;

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public void setRoundKilled(int roundKilled) {
        this.roundKilled = roundKilled;
    }

    private boolean alive;

    private Equivalence equivalent;

    // Derived and cached
    private String summaryString;

    /* Mutant Equivalence */
    public enum Equivalence {

        /**
         * Default state.
         */
        ASSUMED_NO,

        /**
         * The mutant was flagged as potentially equivalent to the original CUT by a defender/ other player.
         * The mutant's creator has not yet participated in the equivalence duel.
         */
        PENDING_TEST,

        /**
         * The mutant's creator has declared that the mutant is equivalent to the original CUT.
         * (Even though the mutant might not be equivalent.)
         * The creator will not receive any points for this mutant.
         */
        DECLARED_YES,

        /**
         * The mutant's creator failed to prove that the mutant is not equivalent to the original CUT.
         * (Even though the mutant might not be equivalent.)
         * The creator will not receive any points for this mutant.
         */
        ASSUMED_YES,

        /**
         * The mutant's creator wrote a test that kills the mutant, thereby proving that the mutant is not
         * equivalent to the original CUT.
         */
        PROVEN_NO
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
    private transient Patch<String> difference = null;

    private String killMessage;

    /**
     * Creates a new Mutant with following attributes.
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
     * Creates a mutant.
     *
     * @param gameId    The ID of the game for which we generate the mutant.
     * @param javaFile  The path to the java file of the mutant.
     * @param classFile The path to the class file with the compiled mutant.
     * @param alive     If the mutant is alive or not.
     * @param playerId  The ID of the player who submitted the mutant.
     */
    public Mutant(int gameId, int classId, String javaFile, String classFile, boolean alive, int playerId,
                  int roundCreated) {
        this.gameId = gameId;
        this.classId = classId;
        this.roundCreated = roundCreated;
        this.javaFile = javaFile;
        this.classFile = classFile;
        this.alive = alive;
        this.equivalent = Equivalence.ASSUMED_NO;
        this.playerId = playerId;
    }

    public Mutant(int mid, int classId, int gid, String javaFile, String classFile, boolean alive, Equivalence equiv,
                  int roundCreated, int roundKilled, int playerId) {
        this(gid, classId, javaFile, classFile, alive, playerId, roundCreated);
        this.id = mid;
        this.equivalent = equiv;
        this.roundCreated = roundCreated;
        this.roundKilled = roundKilled;

        score = 0;
    }

    public Mutant(int mid, int classId, int gid, String javaFile, String classFile, boolean alive, Equivalence equiv,
                  int roundCreated, int roundKilled, int playerId, String md5, String killMessage) {
        this(mid, classId, gid, javaFile, classFile, alive, equiv, roundCreated, roundKilled, playerId);
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

    public State getState() {
        if (equivalent == Equivalence.DECLARED_YES || equivalent == Equivalence.ASSUMED_YES) {
            return State.EQUIVALENT;
        } else if (equivalent == Equivalence.PENDING_TEST) {
            return State.FLAGGED;
        } else if (isAlive()) {
            return State.ALIVE;
        } else {
            return State.KILLED;
        }
    }

    public enum State {
        ALIVE,
        KILLED,
        FLAGGED,
        EQUIVALENT
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
        if (this.md5 == null) {
            this.md5 = CodeValidator.getMD5FromFile(getJavaFile()); // TODO: This may be null
        }
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

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isCovered(List<Test> tests) {
        return tests.stream()
                // Filter the tests that were created by the same user that created the mutant
                .filter(t -> t.getPlayerId() != this.getPlayerId())
                .anyMatch(t -> t.isMutantCovered(this));
    }

    public Set<Test> getCoveringTests(List<Test> tests) {
        return tests.stream()
                .filter(t -> t.isMutantCovered(this))
                .collect(Collectors.toSet());
    }

    public boolean doesRequireRecompilation() {
        return CollectionUtils.containsAny(getCUT().getCompileTimeConstants(), getLines());
    }

    public Patch<String> getDifferences() {
        if (difference == null) {
            computeDifferences();
        }
        return difference;
    }

    // Not sure
    private void computeDifferences() {
        GameClass sut = getCUT();

        File sourceFile = new File(sut.getJavaFile());
        File mutantFile = new File(javaFile);

        List<String> sutLines = FileUtils.readLines(sourceFile.toPath());
        List<String> mutantLines = FileUtils.readLines(mutantFile.toPath());

        sutLines.replaceAll(s -> s.replaceAll(unquotedWhitespaceRegex, ""));

        mutantLines.replaceAll(s -> s.replaceAll(unquotedWhitespaceRegex, ""));

        difference = DiffUtils.diff(sutLines, mutantLines);
    }

    /**
     * Removes single-line whitespace changes from a patch.
     * Whitespace inside of strings ("") is preserved.
     */
    public void removeSingleLineWhitespaceChanges(Patch<String> patch) {
        for (var delta : new ArrayList<>(patch.getDeltas())) {
            var source = delta.getSource().getLines();
            var target = delta.getTarget().getLines();
            int sourcePos = delta.getSource().getPosition();
            int targetPos = delta.getTarget().getPosition();

            boolean discardDelta = true;
            boolean updateDelta = false;

            for (int i = 0; i < Math.min(source.size(), target.size()); i++) {
                var sourceLine =  source.get(i).replaceAll(unquotedWhitespaceRegex, "");
                var targetLine =  target.get(i).replaceAll(unquotedWhitespaceRegex, "");
                if (!sourceLine.equals(targetLine)) {
                    discardDelta = false;
                } else {
                    updateDelta = true;
                    source.remove(i);
                    target.remove(i);
                    if (i == 0) {
                        // adjust the chunk's start line if the first lines contain only whitespace changes
                        sourcePos++;
                        targetPos++;
                    }
                    i--;
                }
            }

            if (discardDelta) {
                // discard the whole chunk if it only consists of whitespace changes
                patch.getDeltas().remove(delta);
            } else if (updateDelta) {
                // update the chunk if it contains lines with only whitespace changes
                patch.getDeltas().remove(delta);
                patch.getDeltas().add(delta.withChunks(
                    new Chunk<>(sourcePos, source),
                    new Chunk<>(targetPos, target)
                ));
            }
        }
    }

    public String getPatchString() {
        GameClass sut = getCUT();

        Path sourceFile = Paths.get(sut.getJavaFile());
        Path mutantFile = Paths.get(javaFile);

        List<String> sutLines = FileUtils.readLines(sourceFile);
        List<String> mutantLines = FileUtils.readLines(mutantFile);

        Patch<String> patch = DiffUtils.diff(sutLines, mutantLines);
        removeSingleLineWhitespaceChanges(patch);

        List<String> unifiedPatches = UnifiedDiffUtils.generateUnifiedDiff(null, null, sutLines, patch, 3);
        StringBuilder unifiedPatch = new StringBuilder();
        for (String s : unifiedPatches) {
            if ("--- null".equals(s) || "+++ null".equals(s)) {
                continue;
            }
            unifiedPatch.append(s).append(System.getProperty("line.separator"));
        }
        return unifiedPatch.toString();
    }

    public String getHTMLEscapedPatchString() {
        return StringEscapeUtils.escapeHtml4(getPatchString());
    }

    public String getKillMessage() {
        return Objects.requireNonNullElse(killMessage, Constants.DEFAULT_KILL_MESSAGE);
    }

    public String getHTMLEscapedKillMessage() {
        return StringEscapeUtils.escapeHtml4(getKillMessage());
    }

    public void setId(int id) {
        this.id = id;
    }

    // Does this every get called if mutant is not stored to DB ?
    public List<Integer> getLines() {
        if (lines == null) {
            computeLinesAndDescription();
        }
        return lines;
    }

    public String getSummaryString() {
        if (summaryString == null) {
            computeLinesAndDescription();
        }
        return summaryString;
    }

    /**
     * Identify lines in the original source code that have been modified
     * by a mutation.
     *
     * <p>An insertion only modifies the line it was inserted in
     */
    private void computeLinesAndDescription() {
        // This workflow is not really nice...
        List<Integer> mutatedLines = new ArrayList<>();
        description = new ArrayList<>();

        List<String> fragementSummary = new ArrayList<>();

        Patch<String> p = getDifferences();
        String modified = null;
        String deleted = null;
        String added = null;
        for (AbstractDelta<String> d : p.getDeltas()) {
            Chunk<String> chunk = d.getSource();
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
                fragementSummary.remove(fragementSummary.size() - 1);
                fragementSummary.add(String.format("%d-%d", firstLine, endLine));
            }
            // update mutant description
            switch (d.getType()) {
                case CHANGE:
                    modified = modified == null ? "Modified " + desc : modified + ", " + desc;
                    break;
                case DELETE:
                    deleted = deleted == null ? "Removed " + desc : deleted + ", " + desc;
                    break;
                case INSERT:
                    added = added == null ? "Added " + desc : added + ", " + desc;
                    break;
                default:
                    throw new IllegalStateException("Found unknown delta type " + d.getType());
            }
        }
        if (modified != null) {
            description.add(StringEscapeUtils.escapeHtml4(modified + "\n"));
        }
        if (deleted != null) {
            description.add(StringEscapeUtils.escapeHtml4(deleted + "\n"));
        }
        if (added != null) {
            description.add(StringEscapeUtils.escapeHtml4(added + "\n"));
        }

        setLines(mutatedLines);

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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Mutant mutant = (Mutant) o;

        return new EqualsBuilder()
                .append(id, mutant.id)
                .append(gameId, mutant.gameId)
                .append(playerId, mutant.playerId)
                .append(javaFile, mutant.javaFile)
                .append(getMd5(), mutant.getMd5())
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
                .append(getMd5())
                .append(classFile)
                .toHashCode();
    }

    public void setLines(List<Integer> mutatedLines) {
        this.lines = mutatedLines;
    }

    @Override
    public String toString() {
        return "[mutantId=" + getId() + ",alive=" + isAlive()
                + ",equivalent=" + getEquivalent() + ",score=" + getScore() + "]";
    }

    public void setKillMessage(String message) {
        this.killMessage = message;
    }

    protected GameClass getCUT() {
        GameClassRepository gameClassRepo = CDIUtil.getBeanFromCDI(GameClassRepository.class);

        var cut = gameClassRepo.getClassForGameId(gameId);
        if (cut.isEmpty()) {
            // in this case gameId might have been -1 (upload)
            // so we try to reload the sut
            cut = gameClassRepo.getClassForId(classId);
        }

        return cut.orElseThrow();
    }
}

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
package org.codedefenders.game.puzzle;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.validation.code.CodeValidatorLevel;

/**
 * Represents the blueprint for a puzzle, which can be instantiated into a {@link PuzzleGame}.
 * @see PuzzleChapter
 * @see PuzzleGame
 */
public class Puzzle {

    /**
     * ID of the chapter.
     */
    private int puzzleId;

    /**
     * Class ID of the class the puzzle uses.
     * The mutants and test for the puzzle come together with the class.
     */
    private int classId;

    /**
     * The {@link Role} the player takes in the puzzle.
     */
    private Role activeRole;

    /**
     * Whether the mutant is an equivalent mutant.
     */
    private boolean isEquivalent;

    /**
     * Whether the puzzle is an equivalence puzzle.
     * TODO: use subclass instead of flag
     */
    private boolean isEquivalencePuzzle;

    /**
     * The {@link GameLevel} the puzzle is played on.
     */
    private GameLevel level;

    /**
     * Maximum number of allowed assertions per submitted test.
     */
    private int maxAssertionsPerTest;

    /**
     * Validation level used to check submitted mutants.
     */
    private CodeValidatorLevel mutantValidatorLevel;

    /**
     * First editable line of the class or test. Can be null.
     */
    private Integer editableLinesStart;

    /**
     * Last editable line of the class or test. Can be null.
     */
    private Integer editableLinesEnd;

    /**
     * ID of the {@link PuzzleChapter chapter} the puzzle belongs to. Can be null.
     */
    private Integer chapterId;

    /**
     * Position of the puzzle inside the {@link PuzzleChapter chapter}. Can be null.
     */
    private Integer position;

    /**
     * Title of the puzzle. Can be null.
     */
    private String title;

    /**
     * Description of the puzzle. Can be null.
     */
    private String description;

    /**
     * The {@link PuzzleChapter} this puzzle belongs to.
     */
    private PuzzleChapter chapter;

    /**
     * Constructs a new Puzzle instance.
     *
     * @param puzzleId             ID of the chapter.
     * @param classId              Class ID of the class the puzzle uses.
     *                             The mutants and test for the puzzle come together with the class.
     * @param activeRole           The {@link Role} the player takes in the puzzle.
     * @param isEquivalent         Whether the mutant is an equivalent mutant.
     * @param isEquivalencePuzzle  Whether the puzzle is an equivalence puzzle.
     * @param level                The {@link GameLevel} the puzzle is played on.
     * @param maxAssertionsPerTest Maximum number of allowed assertions per submitted test.
     * @param mutantValidatorLevel Validation level used to check submitted mutants.
     * @param editableLinesStart   First editable line of the class or test. Can be null.
     * @param editableLinesEnd     Last editable line of the class or test. Can be null.
     * @param chapterId            ID of the chapter the puzzle belongs to. Can be null.
     * @param position             Position of the puzzle inside the chapter. Can be null.
     * @param title                Title of the puzzle. Can be null.
     * @param description          Description of the puzzle. Can be null.
     */
    public Puzzle(int puzzleId,
                  int classId,
                  Role activeRole,
                  boolean isEquivalent, boolean isEquivalencePuzzle, GameLevel level,
                  int maxAssertionsPerTest,
                  CodeValidatorLevel mutantValidatorLevel,
                  Integer editableLinesStart,
                  Integer editableLinesEnd,
                  Integer chapterId,
                  Integer position,
                  String title,
                  String description) {
        this.puzzleId = puzzleId;
        this.classId = classId;
        this.activeRole = activeRole;
        this.isEquivalent = isEquivalent;
        this.isEquivalencePuzzle = isEquivalencePuzzle;
        this.level = level;
        this.maxAssertionsPerTest = maxAssertionsPerTest;
        this.mutantValidatorLevel = mutantValidatorLevel;
        this.editableLinesStart = editableLinesStart;
        this.editableLinesEnd = editableLinesEnd;
        this.chapterId = chapterId;
        this.position = position;
        this.title = title;
        this.description = description;
        this.chapter = null;
    }

    public static Puzzle forPuzzleInfo(int puzzleId,
                                       Integer chapterId,
                                       Integer position,
                                       String title,
                                       String description,
                                       int maxAssertionsPerTest,
                                       Integer editableLinesStart,
                                       Integer editableLinesEnd) {
        return new Puzzle(puzzleId, -1, null, false, false, null, maxAssertionsPerTest,
            null, editableLinesStart, editableLinesEnd, chapterId, position, title, description);
    }

    public int getPuzzleId() {
        return puzzleId;
    }

    public void setPuzzleId(int puzzleId) {
        this.puzzleId = puzzleId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(Role activeRole) {
        this.activeRole = activeRole;
    }

    public GameLevel getLevel() {
        return level;
    }

    public void setLevel(GameLevel level) {
        this.level = level;
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public void setMaxAssertionsPerTest(int maxAssertionsPerTest) {
        this.maxAssertionsPerTest = maxAssertionsPerTest;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public void setMutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) {
        this.mutantValidatorLevel = mutantValidatorLevel;
    }

    public Integer getEditableLinesStart() {
        return editableLinesStart;
    }

    public void setEditableLinesStart(Integer editableLinesStart) {
        this.editableLinesStart = editableLinesStart;
    }

    public Integer getEditableLinesEnd() {
        return editableLinesEnd;
    }

    public void setEditableLinesEnd(Integer editableLinesEnd) {
        this.editableLinesEnd = editableLinesEnd;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the {@link PuzzleChapter} this puzzle belongs to. If the {@link PuzzleChapter} is requested for the first
     * time for this instance, it will be queried from the database.
     * @return The {@link PuzzleChapter} this puzzle belongs to.
     */
    public PuzzleChapter getChapter() {
        var puzzleRepo = CDIUtil.getBeanFromCDI(PuzzleRepository.class);
        if (chapterId != null && chapter == null) {
            chapter = puzzleRepo.getPuzzleChapterForId(chapterId);
        }
        return chapter;
    }

    public void setChapter(PuzzleChapter chapter) {
        this.chapter = chapter;
    }

    public boolean isEquivalent() {
        return isEquivalent;
    }

    public boolean isEquivalencePuzzle() {
        return isEquivalencePuzzle;
    }
}

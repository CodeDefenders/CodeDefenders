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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.Range;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.Dependency;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.analysis.ClassCodeAnalyser;
import org.codedefenders.util.analysis.CodeAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents a class under test. Games will be played with this class by
 * modifying it or creating test cases for it.
 */
public class GameClass {

    private static final Logger logger = LoggerFactory.getLogger(GameClass.class);

    private Integer id;
    private String name; // fully qualified name
    private String alias;
    private String javaFile;
    private String classFile;

    private boolean isMockingEnabled;
    private TestingFramework testingFramework;
    private AssertionLibrary assertionLibrary;

    private boolean isPuzzleClass;
    private Integer parentClassId; // @see getParentClassId() documentation
    private boolean isActive;

    private boolean visitedCode = false;
    private Set<String> additionalImports = new HashSet<>();
    // Store begin and end line which corresponds to uncoverable non-initialized fields
    private List<Integer> linesOfCompileTimeConstants = new ArrayList<>();
    private List<Integer> linesOfNonCoverableCode = new ArrayList<>();
    private List<Integer> nonInitializedFields = new ArrayList<>();
    private List<Integer> emptyLines = new ArrayList<>();
    private Map<Integer, Integer> linesCoveringEmptyLines = new HashMap<>();
    private List<Range<Integer>> linesOfMethods = new ArrayList<>();
    private List<Range<Integer>> linesOfMethodSignatures = new ArrayList<>();
    private List<Range<Integer>> linesOfClosingBrackets = new ArrayList<>();
    private List<MethodDescription> methodDescriptions = new ArrayList<>();

    private TestTemplate testTemplate;

    /**
     * The source code of this Java class. Used as an instance attribute so the file content only needs to be read once.
     */
    private String sourceCode;

    /**
     * Build a new GameClass instance.
     * <p></p>
     * Required values are:
     * <ul>
     *     <li>name</li>
     *     <li>alias</li>
     *     <li>javaFile</li>
     *     <li>classFile</li>
     * </ul>
     * <p></p>
     * Default values are:
     * <ul>
     *     <li>isMockingEnabled = false</li>
     *     <li>isPuzzleClass = false</li>
     *     <li>parentClass = null</li>
     *     <li>isActive = true</li>
     * </ul>
     * @return A GameClass builder.
     */
    public static Builder build() {
        return new Builder();
    }

    private GameClass(Builder builder) {
        Objects.requireNonNull(builder.name);
        Objects.requireNonNull(builder.alias);
        Objects.requireNonNull(builder.javaFile);
        Objects.requireNonNull(builder.classFile);
        this.id = builder.id;
        this.name = builder.name;
        this.alias = builder.alias;
        this.javaFile = builder.javaFile;
        this.classFile = builder.classFile;
        this.isMockingEnabled = builder.isMockingEnabled;
        this.testingFramework = builder.testingFramework;
        this.assertionLibrary = builder.assertionLibrary;
        this.isPuzzleClass = builder.isPuzzleClass;
        this.parentClassId = builder.parentClassId;
        this.isActive = builder.isActive;
    }

    /**
     * Creates a new puzzle class from an existing class.
     * @param other The other class.
     * @param newAlias A new alias for the puzzle class.
     * @return The new puzzle class.
     */
    public static GameClass ofPuzzle(GameClass other, String newAlias) {
        return GameClass.build()
                .name(other.getName())
                .javaFile(other.getJavaFile())
                .classFile(other.getClassFile())
                .mockingEnabled(other.isMockingEnabled())
                .testingFramework(other.getTestingFramework())
                .assertionLibrary(other.getAssertionLibrary())
                .alias(newAlias)
                .puzzleClass(true)
                .parentClassId(other.getId())
                .create();
    }

    private void visitCode() {
        if (!this.visitedCode) {
            final CodeAnalysisResult visit = ClassCodeAnalyser.visitCode(this.name, this.getSourceCode());
            this.additionalImports.addAll(visit.getAdditionalImports());
            this.linesOfCompileTimeConstants.addAll(visit.getCompileTimeConstants());
            this.linesOfNonCoverableCode.addAll(visit.getNonCoverableCode());
            this.nonInitializedFields.addAll(visit.getNonInitializedFields());
            this.linesOfMethods.addAll(visit.getMethods());
            this.linesOfMethodSignatures.addAll(visit.getMethodSignatures());
            this.linesOfClosingBrackets.addAll(visit.getClosingBrackets());
            this.emptyLines.addAll(visit.getEmptyLines());
            this.linesCoveringEmptyLines.putAll(visit.getLinesCoveringEmptyLines());
            this.methodDescriptions.addAll(visit.getMethodDescriptions());
            this.visitedCode = true;
        }
    }

    /**
     * Calls {@link GameClassDAO} to insert this {@link GameClass} instance into the database.
     * <p></p>
     * Updates the identifier of the called instance.
     *
     * @return {@code true} if insertion was successful, {@code false} otherwise.
     */
    public boolean insert() {
        try {
            this.id = GameClassDAO.storeClass(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store game class to database.", e);
            return false;
        }
    }

    /**
     * Calls {@link GameClassDAO} to update this {@link GameClass} instance in the database.
     *
     * @return {@code true} if updating was successful, {@code false} otherwise.
     */
    public boolean update() {
        try {
            return GameClassDAO.updateClass(this);
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store game class to database.", e);
            return false;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a copy of the additional import statements computed for this class.
     */
    public Set<String> getAdditionalImports() {
        visitCode();
        return new HashSet<>(additionalImports);
    }

    public String getJavaFile() {
        return javaFile;
    }

    public String getClassFile() {
        return classFile;
    }

    public boolean isMockingEnabled() {
        return isMockingEnabled;
    }

    public TestingFramework getTestingFramework() {
        return testingFramework;
    }

    public AssertionLibrary getAssertionLibrary() {
        return assertionLibrary;
    }

    public boolean isPuzzleClass() {
        return isPuzzleClass;
    }

    /**
     * Only applies when class is a puzzle class:
     * The parent class is the original uploaded class. Parent and child class share the same source files.
     * If a class is no puzzle class, the parent class is {@code null}.
     *
     */
    public Integer getParentClassId() {
        return parentClassId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
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

    public String getSourceCode() {
        if (sourceCode != null) {
            return sourceCode;
        }
        return sourceCode = FileUtils.readJavaFileWithDefault(Paths.get(javaFile));
    }

    public String getAsHTMLEscapedString() {
        return StringEscapeUtils.escapeHtml(getSourceCode());
    }

    /**
     * Returns a mapping of dependency class names and its HTML escaped class content.
     */
    public Map<String, String> getHTMLEscapedDependencyCode() {
        return GameClassDAO.getMappedDependenciesForClassId(id)
                .stream()
                .map(Dependency::getJavaFile)
                .map(Paths::get)
                .collect(Collectors.toMap(
                        FileUtils::extractFileNameNoExtension,
                        FileUtils::readJavaFileWithDefaultHTMLEscaped)
                );
    }

    private void createTestTemplate() {
        this.testTemplate = TestTemplate.build(this)
                .testingFramework(getTestingFramework())
                .assertionLibrary(getAssertionLibrary())
                .mockingEnabled(isMockingEnabled())
                .get();
    }

    /**
     * Generates and returns a template for a JUnit test.
     * <p></p>
     * Be aware that this template is not HTML escaped.
     * Please use {@link #getHTMLEscapedTestTemplate()}.
     * <p></p>
     * Apart from the additional imports, the template is
     * formatted based on the default IntelliJ formatting.
     *
     * @return template for a JUnit test as a {@link String}.
     * @see #getHTMLEscapedTestTemplate()
     */
    public String getTestTemplate() {
        if (testTemplate == null) {
            createTestTemplate();
        }
        return testTemplate.getCode();
    }

    /**
     * HTML escapes the test template.
     *
     * @return a HTML escaped test template for a Junit Test as a {@link String}.
     */
    public String getHTMLEscapedTestTemplate() {
        return StringEscapeUtils.escapeHtml(getTestTemplate());
    }

    /**
     * Returns the index of first editable line of this class
     * test template.
     *
     * <p>* Note that first index starts at 1.
     *
     * @return the first editable line of this class test template.
     * @see #getTestTemplate()
     */
    public int getTestTemplateFirstEditLine() {
        if (testTemplate == null) {
            createTestTemplate();
        }
        return testTemplate.getEditableLinesStart();
    }

    /**
     * Gathers and returns all lines which non initialized fields.
     *
     * @return All lines of not initialized fields as a {@link List} of {@link Integer Integers}.
     *     Can be empty, but never {@code null}.
     */
    public List<Integer> getNonInitializedFields() {
        visitCode();
        Collections.sort(this.nonInitializedFields);
        return Collections.unmodifiableList(this.nonInitializedFields);
    }

    /**
     * Gathers and returns all lines which contain a method signature.
     *
     * @return All lines of method signatures as a {@link List} of {@link Integer Integers}.
     *     Can be empty, but never {@code null}.
     */
    public List<Integer> getMethodSignatures() {
        visitCode();
        return this.linesOfMethodSignatures
                .stream()
                .flatMap(range -> IntStream.rangeClosed(range.getMinimum(), range.getMaximum()).boxed())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gathers and returns all non coverable lines.
     *
     * @return All lines which are not coverable as a {@link List} of {@link Integer Integers}.
     *     Can be empty, but never {@code null}.
     */
    public List<Integer> getNonCoverableCode() {
        visitCode();
        return linesOfNonCoverableCode;
    }

    /**
     * Return the lines which correspond to Compile Time Constants. Mutation of those lines requires tests
     * to be recompiled against the mutant.
     *
     * @return All lines of compile time constants as a {@link List} of {@link Integer Integers}.
     *     Can be empty, but never {@code null}.
     */
    public List<Integer> getCompileTimeConstants() {
        visitCode();
        return Collections.unmodifiableList(linesOfCompileTimeConstants);
    }

    /**
     * Return the lines of the method signature for the method which contains a given line.
     *
     * @param line the line the method signature is returned for.
     * @return All lines of the method signature a given line resides as a {@link List} of {@link Integer Integers}.
     *     Can be empty, but never {@code null}.
     */
    public List<Integer> getMethodSignaturesForLine(Integer line) {
        visitCode();
        final List<Integer> collect = linesOfMethods
                .stream()
                .filter(method -> method.contains(line))
                .flatMap(methodRange -> linesOfMethodSignatures.stream()
                        .filter(methodSignature -> methodSignature.contains(methodRange.getMinimum()))
                        .flatMap(msRange -> IntStream.rangeClosed(msRange.getMinimum(), msRange.getMaximum()).boxed()))
                .collect(Collectors.toList());
        return Collections.unmodifiableList(collect);
    }

    /**
     * Return the lines of closing brackets from the if-statements which contain a given line.
     *
     * @param line the line closing brackets are returned for.
     * @return All lines of closing brackets from if-statements a given line resides as a {@link List} of
     * {@link Integer Integers}. Can be empty, but never {@code null}.
     */
    public List<Integer> getClosingBracketForLine(Integer line) {
        visitCode();
        final List<Integer> collect = linesOfClosingBrackets
                .stream()
                .filter(integerRange -> integerRange.contains(line))
                .map(Range::getMaximum)
                .sorted()
                .collect(Collectors.toList());
        return Collections.unmodifiableList(collect);
    }

    /**
     * Returns the empty lines which are covered by any of the already covered lines. An empty line
     * can be covered if it belongs to a method and is followed by a covered line (either empty or not)
     */
    public List<Integer> getCoveredEmptyLines(List<Integer> alreadyCoveredLines) {
        visitCode();
        List<Integer> collect = new ArrayList<>();
        for (Range<Integer> linesOfMethod : linesOfMethods) {
            for (int line = linesOfMethod.getMinimum(); line < linesOfMethod.getMaximum(); line++) {
                if (emptyLines.contains(line)) {
                    if (alreadyCoveredLines.contains(linesCoveringEmptyLines.get(line))) {
                        collect.add(line);
                    }
                }
            }
        }
        return Collections.unmodifiableList(collect);
    }

    public List<MethodDescription> getMethodDescriptions() {
        visitCode();
        return Collections.unmodifiableList(methodDescriptions);
    }

    @Override
    public String toString() {
        return "[id=" + id + ",name=" + name + ",alias=" + alias + "]";
    }

    public static class Builder {
        private Integer id;
        private String name; // fully qualified name
        private String alias;
        private String javaFile;
        private String classFile;

        private boolean isMockingEnabled;
        private TestingFramework testingFramework;
        private AssertionLibrary assertionLibrary;

        private boolean isPuzzleClass;
        private Integer parentClassId;
        private boolean isActive;

        private Builder() {
            this.id = null;
            this.isMockingEnabled = false;
            this.isPuzzleClass = false;
            this.isActive = true;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder javaFile(String javaFile) {
            this.javaFile = javaFile;
            return this;
        }

        public Builder classFile(String classFile) {
            this.classFile = classFile;
            return this;
        }

        public Builder mockingEnabled(boolean isMockingEnabled) {
            this.isMockingEnabled = isMockingEnabled;
            return this;
        }

        public Builder testingFramework(TestingFramework testingFramework) {
            this.testingFramework = testingFramework;
            return this;
        }

        public Builder assertionLibrary(AssertionLibrary assertionLibrary) {
            this.assertionLibrary = assertionLibrary;
            return this;
        }

        public Builder puzzleClass(boolean isPuzzleClass) {
            this.isPuzzleClass = isPuzzleClass;
            return this;
        }

        public Builder parentClassId(Integer parentClassId) {
            this.parentClassId = parentClassId;
            return this;
        }

        public Builder active(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public GameClass create() {
            return new GameClass(this);
        }
    }

    public static class MethodDescription {
        private String description;
        private int startLine;
        private int endLine;

        public MethodDescription(String description, int startLine, int endLine) {
            this.description = description;
            this.startLine = startLine;
            this.endLine = endLine;
        }

        public String getDescription() {
            return description;
        }

        public int getStartLine() {
            return startLine;
        }

        public int getEndLine() {
            return endLine;
        }
    }
}

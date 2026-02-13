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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.analysis.gameclass.ClassCodeAnalyser;
import org.codedefenders.analysis.gameclass.ClassCodeAnalyser.ClassAnalysisResult;
import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.service.ClassAnalysisService;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private List<String> additionalImports = new ArrayList<>();
    private List<Integer> linesOfCompileTimeConstants = new ArrayList<>();
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
     *
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
     *
     * @param other    The other class.
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
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
        return StringEscapeUtils.escapeHtml4(getSourceCode());
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
     * @return an HTML escaped test template for a Junit Test as a {@link String}.
     */
    public String getHTMLEscapedTestTemplate() {
        return StringEscapeUtils.escapeHtml4(getTestTemplate());
    }

    /**
     * Returns the index of first editable line of this class
     * test template.
     *
     * <p>Note that first index starts at 1.
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

    private ClassAnalysisResult getClassAnalysis() {
        ClassAnalysisService analyser = CDIUtil.getBeanFromCDI(ClassAnalysisService.class);
        if (id != null) {
            return analyser.analyze(getId()).get();
        } else {
            return analyser.analyze(getSourceCode()).get();
        }
    }

    /**
     * Returns a copy of the additional import statements computed for this class.
     */
    public List<String> getAdditionalImports() {
        return Collections.unmodifiableList(getClassAnalysis().getAdditionalImports());
    }

    /**
     * Return the lines which correspond to compile-time constants. Mutation of those lines requires tests
     * to be recompiled against the mutant.
     *
     * @return All lines of compile time constants as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getCompileTimeConstantLines() {
        return Collections.unmodifiableList(getClassAnalysis().getCompileTimeConstantLines());
    }

    public List<ClassCodeAnalyser.CompileTimeConstant> getCompileTimeConstants() {
        return Collections.unmodifiableList(getClassAnalysis().getCompileTimeConstants());
    }

    public List<MethodDescription> getMethodDescriptions() {
        return Collections.unmodifiableList(getClassAnalysis().getMethodDescriptions());
    }

    public List<String> getDependencyCode() {
        return FileUtils.getCodeFromDependencies(id);
    }

    public List<String> getDependencyNames() {
        return FileUtils.getDependencyNames(id);
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
}

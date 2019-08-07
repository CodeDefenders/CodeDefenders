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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class represents a class under test. Games will be played with this class by
 * modifying it or creating test cases for it.
 */
public class GameClass {

    public final static List<String> BASIC_IMPORTS;
    public final static List<String> MOCKITO_IMPORTS;

    static {
        BASIC_IMPORTS = Collections.unmodifiableList(
                Arrays.asList(
                        // Enable JUnit
                        "import org.junit.*;",
                        // Additional empty line to match IntelliJ import formatting.
                        "",
                        // Enable easy Assertions
                        "import static org.junit.Assert.*;",
                        // Enable Hamcrest assertThat
                        "import static org.hamcrest.MatcherAssert.assertThat;",
                        // Enable Hamcrest basic Matchers
                        "import static org.hamcrest.Matchers.*;"
                )
        );
        MOCKITO_IMPORTS =
                Collections.unmodifiableList(
                        Arrays.asList(
                                // Enable Mockito
                                "import static org.mockito.Mockito.*;"
                        )
                );
    }

    private static final Logger logger = LoggerFactory.getLogger(GameClass.class);

    private int id;
    private String name; // fully qualified name
    private String alias;
    private String javaFile;
    private String classFile;

    private boolean isMockingEnabled;

    private boolean isPuzzleClass;

    private boolean isActive;

    private Set<String> additionalImports = new HashSet<>();
    // Store begin and end line which corresponds to uncoverable non-initializad fields
    private List<Integer> linesOfCompileTimeConstants = new ArrayList<>();
    private List<Integer> linesOfNonCoverableCode = new ArrayList<>();
    private List<Integer> nonInitializedFields = new ArrayList<>();
    //
    private List<Integer> emptyLines = new ArrayList<>();
    private Map<Integer, Integer> linesCoveringEmptyLines = new HashMap<>();

    private List<Range<Integer>> linesOfMethods = new ArrayList<>();
    private List<Range<Integer>> linesOfMethodSignatures = new ArrayList<>();
    private List<Range<Integer>> linesOfClosingBrackets = new ArrayList<>();

    /**
     * The source code of this Java class. Used as an instance attribute so the file content only needs to be read once.
     */
    private String sourceCode;

    public GameClass(String name, String alias, String jFile, String cFile) {
        this(name, alias, jFile, cFile, false);
    }

    public GameClass(int id, String name, String alias, String jFile, String cFile, boolean isMockingEnabled, boolean isActive) {
        this(name, alias, jFile, cFile, isMockingEnabled);
        this.id = id;
        this.isActive = isActive;
    }

    public GameClass(String name, String alias, String jFile, String cFile, boolean isMockingEnabled) {
        this.name = name;
        this.alias = alias;
        this.javaFile = jFile;
        this.classFile = cFile;
        this.isMockingEnabled = isMockingEnabled;
        this.isActive = true;

        visitCode();
    }

    public static GameClass ofPuzzleWithId(int id, String name, String alias, String javaFilePath, String classFilePath, boolean isMockingEnabled) {
        GameClass gameClass = ofPuzzle(name, alias, javaFilePath, classFilePath, isMockingEnabled);
        gameClass.id = id;
        return gameClass;
    }

    public static GameClass ofPuzzle(String name, String alias, String javaFilePath, String classFilePath, boolean isMockingEnabled) {
        GameClass gameClass = new GameClass(name, alias, javaFilePath, classFilePath, isMockingEnabled);
        gameClass.isPuzzleClass = true;
        gameClass.isActive = true;
        return gameClass;
    }

    private void visitCode() {
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
    }

    /**
     * Calls {@link GameClassDAO} to insert this {@link GameClass} instance into the database.
     * <p>
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
     * Returns a copy of the additional import statements computed for this class
     *
     * @return
     */
    public Set<String> getAdditionalImports() {
        return new HashSet<>(additionalImports);
    }

    public String getJavaFile() {
        return javaFile;
    }

    public String getClassFile() {
        return classFile;
    }

    public boolean isMockingEnabled() {
        return this.isMockingEnabled;
    }

    public boolean isPuzzleClass() {
        return this.isPuzzleClass;
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
                .collect(Collectors.toMap(FileUtils::extractFileNameNoExtension, FileUtils::readJavaFileWithDefaultHTMLEscaped));
    }

    /**
     * Generates and returns a template for a JUnit test.
     * <p>
     * Be aware that this template is not HTML escaped.
     * Please use {@link #getHTMLEscapedTestTemplate()}.
     * <p>
     * Apart from the additional imports, the template is
     * formatted based on the default IntelliJ formatting.
     *
     * @return template for a JUnit test as a {@link String}.
     * @see #getHTMLEscapedTestTemplate()
     */
    private String getTestTemplate() {
        final StringBuilder bob = new StringBuilder();
        final String classPackage = getPackage();
        if (!classPackage.isEmpty()) {
            bob.append(String.format("package %s;\n", classPackage));
            bob.append("\n");
        }

        for (String additionalImport : this.additionalImports) {
            // Additional import are already in the form of 'import X.Y.Z;\n'
            bob.append(additionalImport); // no \n required
        }
        bob.append("\n");

        for (String importEntry : BASIC_IMPORTS) {
            bob.append(importEntry).append("\n");
        }

        if (this.isMockingEnabled) {
            for (String importEntry : MOCKITO_IMPORTS) {
                bob.append(importEntry).append("\n");
            }
        }
        bob.append("\n");

        bob.append(String.format("public class Test%s {\n", getBaseName()))
                .append("    @Test(timeout = 4000)\n")
                .append("    public void test() throws Throwable {\n")
                .append("        // test here!\n")
                .append("    }\n")
                .append("}");
        return bob.toString();
    }

    /**
     * @return a HTML escaped test template for a Junit Test as a {@link String}.
     */
    public String getHTMLEscapedTestTemplate() {
        return StringEscapeUtils.escapeHtml(getTestTemplate());
    }

    private final static Pattern TEST_METHOD_PATTERN = Pattern.compile(".*public void test\\(\\) throws Throwable.*");

    /**
     * Returns the index of first editable line of this class
     * test template.
     * <p>
     * Note that first index starts at 1.
     *
     * @return the first editable line of this class test template.
     * @see #getTestTemplate()
     */
    public int getTestTemplateFirstEditLine() {
        String[] templateLines = getTestTemplate().split("\n");
        for (int i = 0; i < templateLines.length; i++) {
            Matcher matcher = TEST_METHOD_PATTERN.matcher(templateLines[i]);
            if (matcher.find()) {
                return i + 1;
            }
        }
        logger.warn("Test template for {} does not contain valid test method.", name);
        return -1;
    }

    /**
     * @return All lines of not initialized fields as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getNonInitializedFields() {
        Collections.sort(this.nonInitializedFields);
        return Collections.unmodifiableList(this.nonInitializedFields);
    }

    /**
     * @return All lines of method signatures as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getMethodSignatures() {
        return this.linesOfMethodSignatures
                .stream()
                .flatMap(range -> IntStream.rangeClosed(range.getMinimum(), range.getMaximum()).boxed())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * @return All lines which are not coverable as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getNonCoverableCode() {
        return linesOfNonCoverableCode;
    }

    /**
     * Return the lines which correspond to Compile Time Constants. Mutation of those lines requires tests
     * to be recompiled against the mutant.
     *
     * @return All lines of compile time constants as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getCompileTimeConstants() {
        return Collections.unmodifiableList(linesOfCompileTimeConstants);
    }

    /**
     * Return the lines of the method signature for the method which contains a given line.
     *
     * @param line the line the method signature is returned for.
     * @return All lines of the method signature a given line resides as a {@link List} of {@link Integer Integers}.
     * Can be empty, but never {@code null}.
     */
    public List<Integer> getMethodSignaturesForLine(Integer line) {
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

    @Override
    public String toString() {
        return "[id=" + id + ",name=" + name + ",alias=" + alias + "]";
    }

}

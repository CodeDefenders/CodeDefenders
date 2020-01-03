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
package org.codedefenders.util.analysis;

import org.apache.commons.lang3.Range;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameClass.MethodDescription;
import org.codedefenders.game.TestAccordionDTO.TestAccordionCategory;

import java.util.*;

/**
 * Container for lines or ranges of lines used for the {@link ClassCodeAnalyser}.
 * <p>
 * Setter methods can be chained.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see ClassCodeAnalyser
 */
public class CodeAnalysisResult {
    private final Set<String> additionalImports = new HashSet<>();
    private final Set<Integer> compileTimeConstants = new HashSet<>();
    private final Set<Integer> nonCoverableCode = new HashSet<>();
    private final Set<Integer> nonInitializedFields = new HashSet<>();
    private final Set<Range<Integer>> methodSignatures = new HashSet<>();
    private final Set<Range<Integer>> methods = new HashSet<>();
    private final Set<Range<Integer>> closingBrackets = new HashSet<>();
    private final Set<Integer> emptyLines = new HashSet<>();
    private final Map<Integer, Integer> linesCoveringEmptyLines = new HashMap<>();

    private final List<MethodDescription> methodDescriptions = new ArrayList<>();

    CodeAnalysisResult imported(String imported) {
        this.additionalImports.add(imported);
        return this;
    }

    CodeAnalysisResult compileTimeConstant(Integer line) {
        this.compileTimeConstants.add(line);
        return this;
    }

    CodeAnalysisResult nonCoverableCode(Integer line) {
        this.nonCoverableCode.add(line);
        return this;
    }

    CodeAnalysisResult nonInitializedField(Integer line) {
        this.nonInitializedFields.add(line);
        return this;
    }

    CodeAnalysisResult methodSignatures(Range<Integer> lines) {
        this.methodSignatures.add(lines);
        return this;
    }

    CodeAnalysisResult methods(Range<Integer> lines) {
        this.methods.add(lines);
        return this;
    }

    CodeAnalysisResult closingBracket(Range<Integer> lines) {
        this.closingBrackets.add(lines);
        return this;
    }

    CodeAnalysisResult emptyLine(Integer line) {
        this.emptyLines.add(line);
        return this;
    }

    CodeAnalysisResult lineCoversEmptyLine(Integer coveringLine, Integer emptyLine) {
        this.linesCoveringEmptyLines.put(emptyLine, coveringLine);
        return this;
    }

    CodeAnalysisResult methodDescription(String signature, int startLine, int endLine) {
        this.methodDescriptions.add(new MethodDescription(signature, startLine, endLine));
        return this;
    }

    public Set<String> getAdditionalImports() {
        return additionalImports;
    }

    public Set<Integer> getCompileTimeConstants() {
        return compileTimeConstants;
    }

    public Set<Integer> getNonCoverableCode() {
        return nonCoverableCode;
    }

    public Set<Integer> getNonInitializedFields() {
        return nonInitializedFields;
    }

    public Set<Range<Integer>> getMethodSignatures() {
        return methodSignatures;
    }

    public Set<Range<Integer>> getMethods() {
        return methods;
    }

    public Set<Range<Integer>> getClosingBrackets() {
        return closingBrackets;
    }

    public Set<Integer> getEmptyLines() {
        return emptyLines;
    }

    public Map<Integer, Integer> getLinesCoveringEmptyLines() {
        return linesCoveringEmptyLines;
    }

    public List<MethodDescription> getMethodDescriptions() {
        return methodDescriptions;
    }
}


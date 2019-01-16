package org.codedefenders.util.analysis;

import org.apache.commons.lang3.Range;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Container for lines or ranges of lines used for the {@link ClassCodeAnalyser}.
 * <p>
 * Setter methods can be chained.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
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
    
    CodeAnalysisResult imported(String imported) { this.additionalImports.add(imported); return this; }

    CodeAnalysisResult compileTimeConstant(Integer line) { this.compileTimeConstants.add(line); return this; }

    CodeAnalysisResult nonCoverableCode(Integer line) { this.nonCoverableCode.add(line); return this; }

    CodeAnalysisResult nonInitializedField(Integer line) { this.nonInitializedFields.add(line); return this; }

    CodeAnalysisResult methodSignatures(Range<Integer> lines) { this.methodSignatures.add(lines); return this; }

    CodeAnalysisResult methods(Range<Integer> lines) { this.methods.add(lines); return this; }

    CodeAnalysisResult closingBracket(Range<Integer> lines) { this.closingBrackets.add(lines); return this; }
    
    CodeAnalysisResult emptyLine(Integer line) { this.emptyLines.add(line); return this; }
    
    CodeAnalysisResult lineCoversEmptyLine(Integer coveringLine, Integer emptyLine) { this.linesCoveringEmptyLines.put( emptyLine,  coveringLine); return this; }


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
}

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
package org.codedefenders.validation.code;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.codedefenders.util.JavaParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;

public class ValidationUtils {
    private static Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    public static <T extends ValidationRule> List<List<T>> getTieredRules(List<T> rules) {
        List<List<T>> tieredResult = new ArrayList<>();
        outer:
        for (T r : rules) {
            if (r.isVisible()) {
                for (List<T> list : tieredResult) {
                    if (!list.isEmpty() && list.get(0).getGeneralDescription().equals(r.getGeneralDescription())) {
                        list.add(r);
                        continue outer;
                    }
                }
                List<T> newList = new ArrayList<>();
                newList.add(r);
                tieredResult.add(newList);
            }
        }
        tieredResult.removeIf(l -> l.size() < 2);
        return tieredResult;
    }

    public static <T extends ValidationRule> List<T> getSingleRules(List<T> rules) {
        List<T> result = new ArrayList<>(rules);
        for (int i = 0; i < result.size(); i++) {
            T r = result.get(i);
            if (!r.isVisible()) {
                result.remove(i);
                i--;
                continue;
            }
            boolean duplicate = false;
            for (int j = i + 1; j < result.size(); j++) {
                if (result.get(j).getGeneralDescription().equals(r.getGeneralDescription())) {
                    duplicate = true;
                    result.remove(j);
                    j--;
                }
            }
            if (duplicate) {
                result.remove(i);
                i--;
            }
        }
        return result;
    }

    static Optional<List<String>> anyHasBeenAdded(List<List<String>> orig, List<List<String>> muta,
                                                  String... forbiddenTerms) {
        return checkLineDiff(orig, muta, l -> containsAny(l.toString(), forbiddenTerms));
    }

    /**
     * Checks if a condition holds false in an original line diff, but true in a mutated one.
     *
     * @param orig          A list of line-diffs, which is itself a list of Strings, from the original CuT
     * @param muta          The equivalent list of line-diffs for the mutant
     * @param findPredicate A predicate that is checked against every line-diff of original and mutant
     * @return True if and only if there is at least one line-diff in which the predicate fails for the original
     * and succeeds for the mutant
     */
    static Optional<List<String>> checkLineDiff(List<List<String>> orig, List<List<String>> muta,
                                                Predicate<List<String>> findPredicate) {
        Iterator<List<String>> it1 = orig.iterator();
        Iterator<List<String>> it2 = muta.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            var originalPiece = it1.next();
            var mutatedPiece = it2.next();
            final boolean foundInOriginal = findPredicate.test(originalPiece);
            final boolean foundInMutant = findPredicate.test(mutatedPiece);
            if (!foundInOriginal && foundInMutant) {
                return Optional.of(mutatedPiece);
            }
        }
        return Optional.empty();
    }

    //FIXME this will not work if a string contains \"
    static boolean onlyLiteralsChanged(String orig, String muta) {
        final String originalWithout = removeQuoted(removeQuoted(orig, "\""), "'");
        final String mutantWithout = removeQuoted(removeQuoted(muta, "\""), "'");
        return originalWithout.equals(mutantWithout);
    }

    static Map<String, NodeList<Modifier>> extractTypeDeclaration(TypeDeclaration<?> td) {
        Map<String, NodeList<Modifier>> typeData = new HashMap<>();
        typeData.put(td.getNameAsString(), td.getModifiers());
        // Inspect if this type declares inner classes
        for (Object bd : td.getMembers()) {
            if (bd instanceof TypeDeclaration) {
                // Handle Inner classes - recursively
                typeData.putAll(extractTypeDeclaration((TypeDeclaration<?>) bd));
            }
        }
        return typeData;
    }

    static List<DiffMatchPatch.Diff> tokenDiff(String orig, String mutated) {
        final List<String> tokensOrig = getTokens(orig);
        final List<String> tokensMuta = getTokens(mutated);

        final Stream<DiffMatchPatch.Diff> origStream = tokensOrig
                .stream()
                .filter(token -> Collections.frequency(tokensMuta, token) < Collections.frequency(tokensOrig, token))
                .map(token -> new DiffMatchPatch.Diff(DiffMatchPatch.Operation.DELETE, token));

        final Stream<DiffMatchPatch.Diff> mutatedStream = tokensMuta
                .stream()
                .filter(token -> Collections.frequency(tokensMuta, token) > Collections.frequency(tokensOrig, token))
                .map(token -> new DiffMatchPatch.Diff(DiffMatchPatch.Operation.INSERT, token));
        return Stream.concat(origStream, mutatedStream).collect(Collectors.toList());
    }

    private static List<String> getTokens(String code) {
        return getTokens(new StreamTokenizer(new StringReader(code)));
    }

    // This removes " from Strings...
    private static List<String> getTokens(StreamTokenizer st) {
        final List<String> tokens = new LinkedList<>();
        try {
            while (st.nextToken() != StreamTokenizer.TT_EOF) {
                if (st.ttype == StreamTokenizer.TT_NUMBER) {
                    tokens.add(String.valueOf(st.nval));
                } else if (st.ttype == StreamTokenizer.TT_WORD) {
                    tokens.add(st.sval.trim());
                } else if (st.sval != null) {
                    if (((char) st.ttype) == '"' || ((char) st.ttype) == '\'') {
                        tokens.add('"' + st.sval + '"');
                    } else {
                        tokens.add(st.sval.trim());
                    }
                } else if ((char) st.ttype != ' ') {
                    tokens.add(Character.toString((char) st.ttype));
                }
            }
        } catch (IOException e) {
            logger.warn("Swallowing IOException", e); // TODO: Why are we swallowing this?
        }
        return tokens;
    }

    static String removeQuoted(String s, String quotationMark) {
        while (s.contains(quotationMark)) {
            int indexFirstOcc = s.indexOf(quotationMark);
            int indexSecondOcc = indexFirstOcc + s.substring(indexFirstOcc + 1).indexOf(quotationMark);
            s = s.substring(0, indexFirstOcc) + s.substring(indexSecondOcc + 2);
        }
        return s;
    }

    static Set<String> extractMethodSignaturesByType(TypeDeclaration<?> td) {
        Set<String> methodSignatures = new HashSet<>();
        // Method signatures in the class including constructors
        for (Object bd : td.getMembers()) {
            if (bd instanceof MethodDeclaration methodDecl) {
                methodSignatures.add(methodDecl.getDeclarationAsString());
            } else if (bd instanceof ConstructorDeclaration constructorDecl) {
                methodSignatures.add(constructorDecl.getDeclarationAsString());
            } else if (bd instanceof CompactConstructorDeclaration compactDecl) {
                methodSignatures.add(compactDecl.getDeclarationAsString(true, true, true));
            } else if (bd instanceof TypeDeclaration) {
                // Inner classes
                methodSignatures.addAll(extractMethodSignaturesByType((TypeDeclaration<?>) bd));
            }
        }
        return methodSignatures;
    }

    /**
     * Extracts only the names of methods, constructors and compact constructors in this class and inner classes,
     * no other signature information.
     */
    static Set<String> extractMethodNamesByType(TypeDeclaration<?> td) {
        Set<String> methodNames = new HashSet<>();
        // Method signatures in the class including constructors
        for (BodyDeclaration<?> bd : td.getMembers()) {
            if (bd instanceof TypeDeclaration<?> innerClass) {
                // Inner classes
                methodNames.addAll(extractMethodNamesByType(innerClass));
            } else if (bd instanceof NodeWithSimpleName<?> nameNode) {
                methodNames.add(nameNode.getNameAsString());
            }
        }
        return methodNames;
    }

    static Set<String> extractImportStatements(CompilationUnit cu) {
        return cu.getImports()
                .stream()
                .map(JavaParserUtils::unparse)
                .collect(Collectors.toSet());
    }

    // TODO Maybe we should replace this with a visitor instead ?
    static Set<String> extractFieldNamesByType(TypeDeclaration<?> td) {
        Set<String> fieldNames = new HashSet<>();

        // Method signatures in the class including constructors
        for (Object bd : td.getMembers()) {
            if (bd instanceof FieldDeclaration) {
                for (VariableDeclarator vd : ((FieldDeclaration) bd).getVariables()) {
                    fieldNames.add(vd.getNameAsString());
                }
            } else if (bd instanceof TypeDeclaration) {
                fieldNames.addAll(extractFieldNamesByType((TypeDeclaration<?>) bd));
            }
        }
        return fieldNames;
    }

    static boolean containsAny(String str, String[] tokens) {
        return Arrays.stream(tokens).anyMatch(str::contains);
    }

    //TODO maybe work directly with the deltas instead of extracting them could be easier?
    static List<AbstractDelta<String>> getDeltas(String original, String changed) {
        List<String> originalLines = Arrays
                .stream(original.split("\n"))
                .map(String::trim)
                .collect(Collectors.toList());
        List<String> changedLines = Arrays
                .stream(changed.split("\n"))
                .map(String::trim)
                .collect(Collectors.toList());

        return DiffUtils.diff(originalLines, changedLines).getDeltas();
    }

    static List<List<String>> getChangedLines(String original, String changed) {
        return getDeltas(original, changed)
                .stream()
                .map(AbstractDelta::getTarget)
                .map(Chunk::getLines)
                .collect(Collectors.toList());
    }

    static List<List<String>> getOriginalLines(String original, String changed) {
        return getDeltas(original, changed)
                .stream()
                .map(AbstractDelta::getSource)
                .map(Chunk::getLines)
                .collect(Collectors.toList());
    }
}

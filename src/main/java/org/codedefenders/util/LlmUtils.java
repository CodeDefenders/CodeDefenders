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
package org.codedefenders.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

/**
 * Utility class for static methods that can be used for llm players.
 */
public class LlmUtils {

    /**
     * Use heuristics to remove all lines from a reply that are not inside the test function.
     * This has to be used because some models will not respect the output format demanded by
     * the system prompt.
     * <p>
     * This doesn't have to be perfect, it only has to work in most cases, many llm generated tests
     * aren't going to compile anyway.
     * <p>
     * If the code is completely unparsable, the unmodified reply is returned. That way feedback can be added to the
     * llm conversation.
     */
    public static String extractTestContentFromReply(String reply) {
        reply = reply.replace("```java", "").replace("```", "");
        List<String> lines;
        var ast = JavaParserUtils.parse(reply);
        if (ast.isEmpty() || ast.get().getParsed() == Node.Parsedness.UNPARSABLE) {
            String newToParse = "class Foo {\n" + reply + "\n}";
            lines = newToParse.lines().toList();
            ast = JavaParserUtils.parse(newToParse);
        } else {
            lines = reply.lines().toList();
        }
        if (ast.isPresent() && ast.get().getParsed() == Node.Parsedness.PARSED) {
            String methodContent = ast.get().findAll(MethodDeclaration.class).stream()
                    .flatMap(method -> {
                        var range = method.getRange().orElseThrow();
                        if (range.begin.line < range.end.line - 1) {
                            return lines.subList(range.begin.line + 1, range.end.line - 1).stream();
                        } else if (range.begin.line <= range.end.line) {
                            return lines.subList(range.begin.line, range.end.line).stream();
                        } else return lines.stream();
                    })
                    .collect(Collectors.joining("\n"));
            if (!methodContent.isEmpty()) {
                return methodContent;
            } else {
                //If no method definition is present, take the entire reply as is
                return reply;
            }
        } else return reply;

    }

    public static String extractMutantFromReply(String reply, boolean removeDependencies, AbstractGame game) {
        String firstDependencyName = null;
        if (removeDependencies) {
            List<String> depNames = game.getCUT().getDependencyNames();
            firstDependencyName = depNames.isEmpty() ? null : depNames.get(0);
        }

        return extractMutantFromReply(reply, firstDependencyName);
    }

    public static String extractMutantFromReply(String reply, String firstDependencyName) {
        String formattedResult = reply.replace("```java\n", "")
                .replace("```java", "")
                .replace("```\n", "")
                .replace("\n```", "")
                .replace("```", "");
        if (firstDependencyName != null) {
            int classDeclaration = indexOfDependencyDeclaration(formattedResult, firstDependencyName);
            if (classDeclaration > 0) {
                formattedResult = formattedResult.substring(0, classDeclaration);
            }
        }
        return formattedResult;
    }

    public static String testTemplateFromReply(String reply, AbstractGame game) {
        reply = LlmUtils.extractTestContentFromReply(reply);
        return insertIntoTestTemplate(game, reply);
    }

    public static List<String> suiteOfTestTemplatesFromReply(String reply, AbstractGame game) {
        List<String> testContents = multipleTestsFromReply(reply);
        return testContents.stream().map(s -> insertIntoTestTemplate(game, s)).toList();
    }

    static List<String> multipleTestsFromReply(String reply) {
        List<String> testContents = new ArrayList<>();

        reply = reply.replace("```java", "").replace("```", "");
        var ast = JavaParserUtils.parse(reply);
        if (ast.isEmpty() || ast.get().getParsed() == Node.Parsedness.UNPARSABLE) {
            return testContents;
        } else {
            List<MethodDeclaration> methods = ast.get().findAll(MethodDeclaration.class);
            for (MethodDeclaration m : methods) {
                Optional<BlockStmt> body = m.getBody();
                if (body.isPresent()) {
                    String lines = body.get().getStatements().stream().map(Node::toString).collect(Collectors.joining("\n"));
                    testContents.add(lines);
                }
            }
        }
        return testContents;
    }

    private static String insertIntoTestTemplate(AbstractGame game, String toInsert) {
        return game.getCUT().getTestTemplate().replace(Constants.TEST_TEMPLATE_PLACEHOLDER, toInsert);
    }

    private static int indexOfDependencyDeclaration(String code, String dependencyName) {
        String regex = "\\n.*((class)|(enum)|(interface)|(record))\\s+" + dependencyName;
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(code);
        if (m.find()) {
            return m.start();
        } else {
            return -1;
        }
    }

    private static String removeLinesThatContainWords(String text, String... words) {
        for (String w : words) {
            text = text.replaceAll("(?m)^.*\\b" + w + "\\b.*$", "");
        }
        return text;
    }

    private static String removeLinesThatStartWith(String text, String... words) {
        for (String w : words) {
            text = text.replaceAll("(?m)^" + w + ".*$", "");
        }
        return text;
    }

    private static String removeSuperfluousClosingBrackets(String text) {
        int balance = 0;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '{') {
                balance++;
            } else if (c == '}') {
                if (balance > 0) {
                    balance--;
                } else {
                    chars[i] = ' ';
                }
            }
        }
        return new String(chars);
    }

    private static boolean containsWord(String line, String word) {
        return line.matches(".*\\b" + word + "\\b.*");
    }

    private static boolean containsAnyOf(String line, String... word) {
        for (String w : word) {
            if (containsWord(line, w)) {
                return true;
            }
        }
        return false;
    }

    public static String annotatedCut(AbstractGame game) {
        String[] lines = game.getCUT().getSourceCode().split("\n");
        int[] coverage = new int[lines.length];
        int[] killedLines = new int[lines.length];
        int[] livingLines = new int[lines.length];

        for (Test t : game.getTests()) {
            LineCoverage lc = t.getLineCoverage();
            for (int i : lc.getLinesCovered()) {
                i = Math.min(i, coverage.length - 1);
                coverage[i - 1]++;
            }
        }

        for (Mutant m : game.getKilledMutants()) {
            for (int i : m.getLines()) {
                i = Math.min(i, killedLines.length - 1);
                killedLines[i - 1]++;
            }
        }
        for (Mutant m : game.getAliveMutants()) {
            for (int i : m.getLines()) {
                i = Math.min(i, livingLines.length - 1);
                livingLines[i - 1]++;
            }
        }

        for (int i = 0; i < lines.length; i++) {
            lines[i] += "//coverage:%d, killed:%d, alive:%d".formatted(coverage[i], killedLines[i], livingLines[i]);
        }

        return String.join("\n", lines);
    }

    public static String annotatedMethodDescriptions(AbstractGame game, CompilationUnit cu) {
        List<MethodDeclaration> declarations = new ArrayList<>();
        cu.stream().forEach(node -> {
            if (node instanceof MethodDeclaration decl) {
                declarations.add(decl);
            }
        });

        List<String> resultList = new ArrayList<>();

        for (MethodDeclaration decl : declarations) {
            int alive = 0;
            int killed = 0;
            List<Mutant> mutants = game.getMutants();
            for (Mutant m : mutants) {
                if (isInMethod(decl, m.getLines())) {
                    if (m.isAlive()) {
                        alive++;
                    } else {
                        killed++;
                    }
                }
            }
            int coverage = 0;
            for (Test t : game.getTests()) {
                if (isInMethod(decl, t.getLineCoverage().getLinesCovered())) {
                    coverage++;
                }
            }
            resultList.add(decl.getDeclarationAsString());
            resultList.add("coverage: " + coverage);
            resultList.add("killed: " + killed);
            resultList.add("alive: " + alive);
        }
        return String.join("\n", resultList);
    }

    private static boolean isInMethod(MethodDeclaration decl, List<Integer> lines) {
        int begin = decl.getBegin().orElseThrow().line;
        int end = decl.getEnd().orElseThrow().line;
        for (int i : lines) {
            if (i >= begin && i <= end) {
                return true;
            }
        }
        return false;
    }

    public static String getMethodContent(String source, CallableDeclaration<?> methodDeclaration) {
        Range range =  methodDeclaration.getRange().orElseThrow();
        int begin = range.begin.line - 1;
        int end = range.end.line;
        String[] lines = source.split("\n");
        String [] methodLines = Arrays.copyOfRange(lines, begin, end);
        return String.join("\n", methodLines);
    }
}

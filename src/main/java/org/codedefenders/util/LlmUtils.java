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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codedefenders.game.AbstractGame;

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
     */
    public static String extractTestContentFromReply(String reply) {
        reply = reply.replace("```java", "").replace("```", "");
        reply = reply.replaceAll("(?m)^\\s*", "");
        reply = removeLinesThatContainWords(reply, "void", "public", "private");
        reply = removeLinesThatStartWith(reply, "import");
        reply = reply.replace("@Test", "");
        reply = removeSuperfluousClosingBrackets(reply);
        reply = reply.replaceAll("(?m)^\\s*" + System.lineSeparator(), "");
        return reply;
    }

    public static String extractMutantFromReply(String reply, boolean removeDependencies, AbstractGame game) {
        String firstDependencyName = null;
        if (removeDependencies) {
            List<String> depNames = game.getCUT().getDependencyNames();
            firstDependencyName = depNames.isEmpty() ? null : depNames.get(0);
        }

        return extractMutantFromReply(reply, firstDependencyName);
    }

    static String extractMutantFromReply(String reply, String firstDependencyName) {
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
}

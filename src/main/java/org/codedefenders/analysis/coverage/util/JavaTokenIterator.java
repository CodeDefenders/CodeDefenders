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
package org.codedefenders.analysis.coverage.util;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.Node;

import static org.codedefenders.util.JavaParserUtils.beginOf;
import static org.codedefenders.util.JavaParserUtils.beginToken;
import static org.codedefenders.util.JavaParserUtils.endOf;
import static org.codedefenders.util.JavaParserUtils.endToken;

/**
 * Utility class to make iterating through {@link JavaToken} tokens easier.
 */
public class JavaTokenIterator {
    /**
     * The next token to be checked in any find or skip operation. Could also be considered the current token.
     */
    private JavaToken next;

    /**
     * Operator to get the next token. For backwards traversal.
     */
    private UnaryOperator<JavaToken> getNext;

    private JavaTokenIterator(JavaToken token, UnaryOperator<JavaToken> getNext) {
        this.next = token;
        this.getNext = getNext;
    }

    /**
     * Constructs a new JavaTokenIterator pointing to the given token.
     */
    public static JavaTokenIterator of(JavaToken token) {
        return new JavaTokenIterator(token,
                next -> next.getNextToken().orElse(null));
    }

    /**
     * Constructs a new JavaTokenIterator pointing to the first token of the given token.
     */
    public static JavaTokenIterator ofBegin(Node node) {
        return JavaTokenIterator.of(beginToken(node));
    }

    /**
     * Constructs a new JavaTokenIterator pointing to the last token of the given token.
     */
    public static JavaTokenIterator ofEnd(Node node) {
        return JavaTokenIterator.of(endToken(node));
    }

    /**
     * Changes the direction of the iterator to go backward.
     */
    public JavaTokenIterator backward() {
        getNext = next -> next.getPreviousToken().orElse(null);
        return this;
    }

    private JavaToken findInternal(Predicate<JavaToken> predicate) {
        int numParens = 0;
        int numBrackets = 0;
        int numBraces = 0;

        while (next != null) {
            if (predicate.test(next)
                    && numParens == 0
                    && numBrackets == 0
                    && numBraces == 0) {
                JavaToken match = next;
                next = getNext.apply(next);
                return match;
            }

            // we don't have to worry about parens/brackets/braces in strings or comments,
            // since those are handled by string literal and comment tokens
            JavaToken.Kind kind = JavaToken.Kind.valueOf(next.getKind());
            switch (kind) {
                case LPAREN:
                    numParens++;
                    break;
                case RPAREN:
                    numParens--;
                    break;
                case LBRACKET:
                    numBrackets++;
                    break;
                case RBRACKET:
                    numBrackets--;
                    break;
                case LBRACE:
                    numBraces++;
                    break;
                case RBRACE:
                    numBraces--;
                    break;
            }

            next = getNext.apply(next);
        }

        return null;
    }

    /**
     * Finds the next token matching the given predicate.
     * Afterwards, the iterator points to the token after the found token.
     *
     * <p>Node: Only matches if the encountered parens/brackets/braces up to the match are balanced.
     */
    public JavaToken find(Predicate<JavaToken.Kind> predicate) {
        return findInternal(next -> predicate.test(JavaToken.Kind.valueOf(next.getKind())));
    }

    /**
     * Finds the next token of the given kind.
     * Afterwards, the iterator points to the token after the found token.
     *
     * <p>Node: Only matches if the encountered parens/brackets/braces up to the match are balanced.
     */
    public JavaToken find(JavaToken.Kind search) {
        return findInternal(next -> search == JavaToken.Kind.valueOf(next.getKind()));
    }

    /**
     * Finds the next token that is not whitespace or a comment.
     * Afterwards, the iterator points to the token after the found token.
     */
    public JavaToken findNextNonEmpty() {
        return findInternal(next -> !next.getCategory().isWhitespaceOrComment());
    }

    /**
     * Skips the next token matching the given predicate.
     * Afterwards, the iterator points to the token after the found token.
     *
     * <p>Node: Only matches if the encountered parens/brackets/braces up to the match are balanced.
     */
    public JavaTokenIterator skip(Predicate<JavaToken.Kind> predicate) {
        return find(predicate) != null ? this : null;
    }

    /**
     * Skips the next token of the given kind.
     * Afterwards, the iterator points to the token after the found token.
     *
     * <p>Node: Only matches if the encountered parens/brackets/braces up to the match are balanced.
     */
    public JavaTokenIterator skip(JavaToken.Kind search) {
        return find(search) != null ? this : null;
    }

    /**
     * Skips the current token.
     * Afterwards, the iterator points to the next token.
     */
    public JavaTokenIterator skipOne() {
        next = this.getNext.apply(next);
        return next != null ? this : null;
    }

    /**
     * Finds the first whitespace line before the token,
     * or the start line of the token itself if there is no whitespace line before the token.
     */
    public static int expandWhitespaceBefore(JavaToken token) {
        int originalLine = beginOf(token);
        JavaToken foundToken = JavaTokenIterator.of(token)
                .backward()
                .skipOne()
                .findNextNonEmpty();
        int foundLine = endOf(foundToken);
        return Math.min(originalLine, foundLine + 1);
    }

    /**
     * Finds the last whitespace line after the token,
     * or the end line of the token itself if there is no whitespace line after the token.
     */
    public static int expandWhitespaceAfter(JavaToken token) {
        int originalLine = endOf(token);
        JavaToken foundToken = JavaTokenIterator.of(token)
                .skipOne()
                .findNextNonEmpty();
        int foundLine = beginOf(foundToken);
        return Math.max(originalLine, foundLine - 1);
    }

    /**
     * Checks whether the line starts with the given node.
     */
    public static boolean lineStartsWith(Node node) {
        return lineStartsWith(beginToken(node));
    }

    /**
     * Checks whether the line starts with the given token.
     */
    public static boolean lineStartsWith(JavaToken token) {
        JavaToken foundToken = JavaTokenIterator.of(token)
                .backward()
                .skipOne()
                .findNextNonEmpty();
        return foundToken == null || endOf(foundToken) < beginOf(token);
    }

    /**
     * Checks whether the line ends with the given node.
     */
    public static boolean lineEndsWith(Node node) {
        return lineEndsWith(endToken(node));
    }

    /**
     * Checks whether the line ends with the given token.
     */
    public static boolean lineEndsWith(JavaToken token) {
        JavaToken foundToken = JavaTokenIterator.of(token)
                .skipOne()
                .findNextNonEmpty();
        return foundToken == null || beginOf(foundToken) > endOf(token);
    }
}



/*
 * Copyright (C) 2016-2023 Code Defenders contributors
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
package org.codedefenders.analysis.coverage;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.Node;

// TODO: rewrite so that find() stops with current == found token
// TODO: add a get() method to get the current token
// TODO: add a getLine method to get the line of the current token

public class JavaTokenIterator {
    private JavaToken next;
    private UnaryOperator<JavaToken> getNext;

    private JavaTokenIterator(JavaToken token, UnaryOperator<JavaToken> getNext) {
        this.next = token;
        this.getNext = getNext;
    }

    public static JavaTokenIterator of(JavaToken token) {
        return new JavaTokenIterator(token,
                next -> next.getNextToken().orElse(null));
    }

    public static JavaTokenIterator ofBegin(Node node) {
        return JavaTokenIterator.of(node.getTokenRange().get().getBegin());
    }

    public static JavaTokenIterator ofEnd(Node node) {
        return JavaTokenIterator.of(node.getTokenRange().get().getEnd());
    }

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

    public JavaToken find(Predicate<JavaToken.Kind> predicate) {
        return findInternal(next -> predicate.test(JavaToken.Kind.valueOf(next.getKind())));
    }

    public JavaToken find(JavaToken.Kind search) {
        return findInternal(next -> search == JavaToken.Kind.valueOf(next.getKind()));
    }

    public JavaToken findNext() {
        return findInternal(next -> !next.getCategory().isWhitespaceOrComment());
    }

    public JavaTokenIterator skip(Predicate<JavaToken.Kind> predicate) {
        return find(predicate) != null ? this : null;
    }

    public JavaTokenIterator skip(JavaToken.Kind search) {
        return find(search) != null ? this : null;
    }

    public JavaTokenIterator skipOne() {
        next = this.getNext.apply(next);
        return next != null ? this : null;
    }

    public static int expandWhitespaceBefore(JavaToken token) {
        int originalLine = token.getRange().get().begin.line;
        JavaToken find = JavaTokenIterator.of(token)
                .backward()
                .skipOne()
                .findNext();
        int foundLine = find.getRange().get().end.line;
        return Math.min(originalLine, foundLine + 1);
    }

    public static int expandWhitespaceAfter(JavaToken token) {
        int originalLine = token.getRange().get().begin.line;
        JavaToken find = JavaTokenIterator.of(token)
                .skipOne()
                .findNext();
        int foundLine = find.getRange().get().end.line;
        return Math.max(originalLine, foundLine - 1);
    }

    public static boolean lineStartsWith(Node node) {
        return lineStartsWith(node.getTokenRange().get().getBegin());
    }

    public static boolean lineStartsWith(JavaToken token) {
        int originalLine = token.getRange().get().begin.line;
        JavaToken find = JavaTokenIterator.of(token)
                .backward()
                .skipOne()
                .findInternal(next -> !next.getCategory().isWhitespaceOrComment()
                    || next.getRange().get().begin.line != originalLine);
        return find == null || find.getRange().get().begin.line != originalLine;
    }

    public static boolean lineEndsWith(Node node) {
        return lineEndsWith(node.getTokenRange().get().getEnd());
    }

    public static boolean lineEndsWith(JavaToken token) {
        int originalLine = token.getRange().get().begin.line;
        JavaToken find = JavaTokenIterator.of(token)
                .skipOne()
                .findInternal(next -> !next.getCategory().isWhitespaceOrComment()
                        || next.getRange().get().begin.line != originalLine);
        return find == null || find.getRange().get().begin.line != originalLine;
    }
}



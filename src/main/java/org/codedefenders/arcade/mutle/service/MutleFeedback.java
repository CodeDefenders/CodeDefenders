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
package org.codedefenders.arcade.mutle.service;

import java.util.Optional;
import java.util.Set;

import org.codedefenders.arcade.mutle.fixture.MutleMutant;
import org.codedefenders.util.JavaParserUtils;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;

public final class MutleFeedback {

    public enum Mark {
        NONE,
        METHOD,
        LINE,
        KILLED
    }

    private MutleFeedback() {
    }

    public static Mark classify(String source, MutleMutant mutant, Set<Integer> coveredLines, boolean killed) {
        if (killed) {
            return Mark.KILLED;
        }
        CallableRange callable = callableRange(source, mutant)
                .orElseThrow(() -> new IllegalStateException("Mutle mutant has no callable " + mutant.method()));
        if (coveredLines.stream().anyMatch(mutant.mutatedLines()::contains)) {
            return Mark.LINE;
        }
        if (coveredLines.stream().anyMatch(callable::contains)) {
            return Mark.METHOD;
        }
        return Mark.NONE;
    }

    public static Optional<CallableRange> callableRange(String source, String method) {
        return JavaParserUtils.parse(source)
                .flatMap(unit -> unit.findAll(CallableDeclaration.class).stream()
                        .filter(callable -> callable.getNameAsString().equals(method)
                                || method.equals("<init>") && callable instanceof ConstructorDeclaration)
                        .findFirst()
                        .flatMap(MutleFeedback::range));
    }

    private static Optional<CallableRange> callableRange(String source, MutleMutant mutant) {
        return JavaParserUtils.parse(source).flatMap(unit -> {
            var ranges = unit.findAll(CallableDeclaration.class).stream()
                        .filter(callable -> callable.getNameAsString().equals(mutant.method())
                                || mutant.method().equals("<init>") && callable instanceof ConstructorDeclaration)
                        .flatMap(callable -> range(callable).stream())
                        .filter(range -> mutant.mutatedLines().stream().anyMatch(range::contains))
                        .toList();
            return ranges.size() == 1 ? Optional.of(ranges.get(0)) : Optional.empty();
        });
    }

    private static Optional<CallableRange> range(CallableDeclaration<?> callable) {
        return callable.getRange().map(range -> new CallableRange(range.begin.line, range.end.line));
    }

    public record CallableRange(int firstLine, int lastLine) {
        public boolean contains(int line) {
            return firstLine <= line && line <= lastLine;
        }
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import com.github.javaparser.ast.CompilationUnit;

public class MutantRule {

    private final List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules;
    private final List<BiPredicate<List<List<String>>, List<List<String>>>> linediffRules;
    private final List<BiPredicate<String, String>> codeRules;
    private final List<String[]> insertionRules;

    protected final String generalDescription;
    protected final String detailedDescription;
    protected final ValidationMessage message;
    protected boolean hidden = false;

    private MutantRule(boolean hidden,
                       ValidationMessage message,
                       String detailedDescription,
                       String generalDescription,
                       List<String[]> insertionRules,
                       List<BiPredicate<String, String>> codeRules,
                       List<BiPredicate<List<List<String>>, List<List<String>>>> linediffRules,
                       List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules) {
        this.hidden = hidden;
        this.message = message;
        this.detailedDescription = detailedDescription;
        this.generalDescription = generalDescription;
        this.insertionRules = insertionRules;
        this.codeRules = codeRules;
        this.linediffRules = linediffRules;
        this.compilationUnitRules = compilationUnitRules;
    }

    public String getGeneralDescription() {
        return generalDescription;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public ValidationMessage getMessage() {
        return message;
    }


    public boolean fails(CompilationUnit original, CompilationUnit changed) {
        return compilationUnitRules.stream().anyMatch(r -> r.test(original, changed));
    }

    public boolean fails(List<List<String>> original, List<List<String>> changed) {
        return linediffRules.stream().anyMatch(r -> r.test(original, changed));
    }

    public boolean fails(String original, String changed) {
        return codeRules.stream().anyMatch(r -> r.test(original, changed));
    }

    public boolean fails(String diff) {
        return insertionRules.stream().anyMatch(r -> CodeValidator.containsAny(diff, r));
    }

    static class Builder {
        private final List<BiPredicate<CompilationUnit, CompilationUnit>> compilationUnitRules = new ArrayList<>();
        private final List<BiPredicate<List<List<String>>, List<List<String>>>> linediffRules = new ArrayList<>();
        private final List<BiPredicate<String, String>> codeRules = new ArrayList<>();
        private final List<String[]> insertionRules = new ArrayList<>();
        private boolean hidden = false;

        final String generalDescription;
        final String detailedDescription;
        final ValidationMessage message;

        Builder(String generalDescription, String detailedDescription, ValidationMessage message) {
            this.generalDescription = generalDescription;
            this.detailedDescription = detailedDescription;
            this.message = message;
        }

        Builder withCompilation(BiPredicate<CompilationUnit, CompilationUnit> rule) {
            compilationUnitRules.add(rule);
            return this;
        }

        Builder withLinediff(BiPredicate<List<List<String>>, List<List<String>>> rule) {
            linediffRules.add(rule);
            return this;
        }

        Builder withCode(BiPredicate<String, String> rule) {
            codeRules.add(rule);
            return this;
        }

        Builder withInsertion(String... rule) {
            insertionRules.add(rule);
            return this;
        }

        Builder hidden() {
            hidden = true;
            return this;
        }

        MutantRule build() {
            return new MutantRule(hidden,
                    message,
                    detailedDescription,
                    generalDescription,
                    Collections.unmodifiableList(insertionRules),
                    Collections.unmodifiableList(codeRules),
                    Collections.unmodifiableList(linediffRules),
                    Collections.unmodifiableList(compilationUnitRules));
        }
    }
}

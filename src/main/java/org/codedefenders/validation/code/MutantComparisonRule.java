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

import java.util.function.BiPredicate;

public class MutantComparisonRule<INPUT> extends MutantRule{
    private final BiPredicate<INPUT, INPUT> evaluator;

    public MutantComparisonRule(String generalDescription, String detailedDescription,
                                BiPredicate<INPUT, INPUT> evaluator, ValidationMessage message) {
        super(generalDescription, detailedDescription, message);
        this.evaluator = evaluator;
    }

    public MutantComparisonRule(String description, BiPredicate<INPUT, INPUT> evaluator, ValidationMessage message) {
        super(description, message);
        this.evaluator = evaluator;
    }

    public MutantComparisonRule(BiPredicate<INPUT, INPUT> evaluator, ValidationMessage message) {
        super(message);
        this.evaluator = evaluator;
    }

    boolean fails(INPUT original, INPUT changed) {
        return evaluator.test(original, changed);
    }
}

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
package org.codedefenders.validation.input;

import java.time.format.DateTimeFormatter;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckDateValidator implements ConstraintValidator<CheckDateFormat, Object> {

    // Those are checked with AT LEAST semantics, i.e., they are composed with
    // OR
    private String[] patterns;

    @Override
    public void initialize(CheckDateFormat constraintAnnotation) {
        this.patterns = constraintAnnotation.patterns();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintContext) {
        if (object == null) {
            return true;
        } else if (object instanceof String) {

            for (String pattern : patterns) {
                try {
                    DateTimeFormatter.ofPattern(pattern).parse(String.valueOf(object));
                    return true;
                } catch (Throwable e) {
                    // We do not care about this one, since there might be more than one format
                }
            }
            return false;
        } else {
            return object instanceof Long;
        }
    }
}

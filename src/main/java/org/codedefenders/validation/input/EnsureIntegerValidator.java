/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// https://stackoverflow.com/questions/19537664/how-to-validate-number-string-as-digit-with-hibernate
public class EnsureIntegerValidator implements ConstraintValidator<EnsureInteger, Object> {
    private EnsureInteger ensureInteger;

    @Override
    public void initialize(EnsureInteger constraintAnnotation) {
        this.ensureInteger = constraintAnnotation;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        } else if (value instanceof String) {
            // Initialize it.
            String regex = "^\\d+$";
            String data = String.valueOf(value);
            return data.matches(regex);

        } else if (value instanceof Integer) {
            return true;
        } else {
            return false;
        }


    }

}

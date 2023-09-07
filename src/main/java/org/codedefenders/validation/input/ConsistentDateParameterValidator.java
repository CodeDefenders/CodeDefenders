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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;

@SupportedValidationTarget(ValidationTarget.PARAMETERS)
public class ConsistentDateParameterValidator implements ConstraintValidator<ConsistentDateParameters, Object[]> {

    @Override
    public boolean isValid(Object[] value, ConstraintValidatorContext context) {

        if (value[12] == null || value[13] == null) {
            return false;
        }

        // FIXME
        // Parse to date if possible and then compare with before/after
        return true;

        // if (!(value[0] instanceof LocalDate)
        //   || !(value[1] instanceof LocalDate)) {
        //     throw new IllegalArgumentException(
        //       "Illegal method signature, expected two parameters of type LocalDate.");
        // }
        //
        // return ((LocalDate) value[0]).isAfter(LocalDate.now())
        //   && ((LocalDate) value[0]).isBefore((LocalDate) value[1]);
    }
}

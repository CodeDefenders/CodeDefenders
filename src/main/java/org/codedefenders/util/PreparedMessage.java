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

import java.text.MessageFormat;

import org.xnap.commons.i18n.I18n;

/**
 * A prepared message consisting of a {@link java.text.MessageFormat} pattern and
 * its corresponding argument values, ready to be resolved into a final string.
 * <br>
 * Instances of this class are typically created before the target locale or
 * translation is known, and resolved later during presentation.
 */
public record PreparedMessage(String pattern, Object... arguments) {

    public boolean isEmpty() {
        return pattern == null || pattern.isEmpty();
    }

    public boolean isPresent() {
        return !isEmpty();
    }

    public boolean hasArguments() {
        return arguments != null && arguments.length > 0;
    }

    public String resolve(I18n i18n) {
        if (isEmpty()) {
            return "";
        }
        if (hasArguments()) {
            return i18n.tr(pattern, arguments);
        } else {
            return i18n.tr(pattern);
        }
    }

    public String resolve() {
        if (isEmpty()) {
            return "";
        }
        if (hasArguments()) {
            return MessageFormat.format(pattern, arguments);
        } else {
            return pattern;
        }
    }
}

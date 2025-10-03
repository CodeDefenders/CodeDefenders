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

public class MutantInsertionRule extends MutantRule {
    private final String[] forbiddenTokens;

    public MutantInsertionRule(String generalDescription, String detailedDescription,
                               String[] forbiddenTokens, ValidationMessage message) {
        super(generalDescription, detailedDescription, message);
        this.forbiddenTokens = forbiddenTokens;
    }

    public MutantInsertionRule(String description, String[] forbiddenTokens,  ValidationMessage message) {
        super(description, message);
        this.forbiddenTokens = forbiddenTokens;
    }

    public MutantInsertionRule(String[] forbiddenTokens, ValidationMessage message) {
        super(message);
        this.forbiddenTokens = forbiddenTokens;
    }

    boolean fails(String diff) {
        return CodeValidator.containsAny(diff, forbiddenTokens);
    }
}

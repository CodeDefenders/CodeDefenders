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

public abstract class MutantRule {
    protected final String generalDescription;
    protected final String detailedDescription;
    protected final ValidationMessage message;
    protected boolean hidden = false;

    protected MutantRule(String generalDescription, String detailedDescription, ValidationMessage message) {
        this.generalDescription = generalDescription;
        this.detailedDescription = detailedDescription;
        this.message = message;
    }

    protected MutantRule(String description, ValidationMessage message) {
        this(description, description, message);
    }

    protected MutantRule(ValidationMessage message) {
        this("", message);
        hidden = true;
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
}

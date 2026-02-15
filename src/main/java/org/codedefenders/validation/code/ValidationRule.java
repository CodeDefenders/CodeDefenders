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

/**
 * Bundles some common functionality of {@link MutantRule} and {@link TestRule}, regarding their representation
 * in the UI.
 */
public abstract class ValidationRule {
    private final String generalDescription;
    private final String detailedDescription;
    private final String validationMessage;

    private final boolean visible;

    protected ValidationRule(String generalDescription, String detailedDescription, String validationMessage,
                             boolean visible) {
        this.generalDescription = generalDescription;
        this.detailedDescription = detailedDescription;
        this.validationMessage = validationMessage;
        this.visible = visible;
    }

    public String getGeneralDescription() {
        return generalDescription;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public boolean isVisible() {
        return visible;
    }
}

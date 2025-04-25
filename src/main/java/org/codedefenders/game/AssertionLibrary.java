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
package org.codedefenders.game;

public enum AssertionLibrary {
    JUNIT4("JUnit 4"),
    JUNIT5("JUnit 5"),
    HAMCREST("Hamcrest"),
    GOOGLE_TRUTH("Google Truth"),
    JUNIT4_HAMCREST("JUnit 4 + Hamcrest"),
    JUNIT5_HAMCREST("JUnit 5 + Hamcrest"),
    JUNIT4_GOOGLE_TRUTH("JUnit 4 + Google Truth"),
    JUNIT5_GOOGLE_TRUTH("JUnit 5 + Google Truth"),;

    private final String description;

    AssertionLibrary(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

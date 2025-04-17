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
package org.codedefenders.model;

import java.util.HashSet;
import java.util.Set;

public class DefenderIntention {

    private Set<Integer> lines;
    private Set<Integer> mutants;

    public DefenderIntention(Set<Integer> lines, Set<Integer> mutants) {
        this.lines = lines;
        this.mutants = mutants;
    }

    public Set<Integer> getLines() {
        return lines;
    }

    public Set<Integer> getMutants() {
        return mutants;
    }

    public static Set<Integer> parseIntentionFromCommaSeparatedValueString(String csvString) {
        Set<Integer> parsed = new HashSet<>();

        String[] numbers = csvString.split(",");
        for (String number : numbers) {
            if ("".equals(number) || (number != null && number.trim().equals(""))) {
                continue;
            }
            parsed.add(Integer.valueOf(number));
        }
        return parsed;
    }

    @Override
    public String toString() {
        return "DefenderIntention :" + "\n"
                + "\tLines: " + lines + "\n"
                + "\tMutants:" + mutants;
    }
}

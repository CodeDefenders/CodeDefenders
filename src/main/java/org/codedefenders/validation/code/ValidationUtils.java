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

import java.util.ArrayList;
import java.util.List;

public class ValidationUtils {
    public static <T extends ValidationRule> List<List<T>> getTieredRules(List<T> rules) {
        List<List<T>> tieredResult = new ArrayList<>();
        outer:
        for (T r : rules) {
            if (r.isVisible()) {
                for (List<T> list : tieredResult) {
                    if (!list.isEmpty() && list.get(0).getGeneralDescription().equals(r.getGeneralDescription())) {
                        list.add(r);
                        continue outer;
                    }
                }
                List<T> newList = new ArrayList<>();
                newList.add(r);
                tieredResult.add(newList);
            }
        }
        tieredResult.removeIf(l -> l.size() < 2);
        return tieredResult;
    }

    public static <T extends ValidationRule> List<T> getSingleRules(List<T> rules) {
        List<T> result = new ArrayList<>(rules);
        for (int i = 0; i < result.size(); i++) {
            T r = result.get(i);
            if (!r.isVisible()) {
                result.remove(i);
                i--;
                continue;
            }
            boolean duplicate = false;
            for (int j = i + 1; j < result.size(); j++) {
                if (result.get(j).getGeneralDescription().equals(r.getGeneralDescription())) {
                    duplicate = true;
                    result.remove(j);
                    j--;
                }
            }
            if (duplicate) {
                result.remove(i);
                i--;
            }
        }
        return result;
    }
}

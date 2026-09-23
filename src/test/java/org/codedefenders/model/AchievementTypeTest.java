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

import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementTypeTest {
    @Test
    void usesPngForExistingAchievementTypes() {
        assertEquals("codedefenders_achievements_4_lvl_2.png", AchievementType.WRITE_TESTS.getImageFileName(2));
    }

    @Test
    void usesJpegForMutleFirstSolve() {
        assertEquals("mutle_achievement_firstsolve.jpg", AchievementType.MUTLE_FIRST_SOLVE.getImageFileName(0));
    }

    @Test
    void usesJpegForMutleSolveInTwo() {
        assertEquals("mutle_achievement_solvedintwo.jpg", AchievementType.MUTLE_SOLVE_IN_TWO.getImageFileName(1));
    }

    @Test
    void exposesImageFilenameToAchievementNotifications() {
        Achievement achievement = new Achievement(AchievementType.MUTLE_FIRST_SOLVE, 1, 1);
        var json = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJsonTree(achievement);

        assertEquals("mutle_achievement_firstsolve.jpg", json.getAsJsonObject().get("imageFileName").getAsString());
    }
}

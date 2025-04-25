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
package org.codedefenders.game.tcs.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KillCountTestCaseSelector extends PrioritizedTestCaseSelector {

    private static final Logger logger = LoggerFactory.getLogger(KillCountTestCaseSelector.class);

    @Override
    public List<Test> prioritize(List<Test> allTests) {
        // Assume that all the tests target the same CUT
        // PrioritizedTestCaseSelector ensures that allTests.size > 1
        int classId = allTests.get(0).getClassId();

        try {
            // Get all the killmap entries for the class
            /*
             * TODO This might be huge !? Can we narrow this down without
             * calling KillMap.forClass(classId) which causes the re-computation
             * of the killMap
             */
            List<KillMapEntry> allKillMapEntrie = KillmapDAO.getKillMapEntriesForClass(classId);

            // Get only the kill entries that math the provided tests
            // TODO Make this a stream
            for (Test test : allTests) {
                List<KillMapEntry> killMapEntriesForTest = allKillMapEntrie.stream()
                        .filter(kme -> kme.test.getId() == test.getId()).collect(Collectors.toList());
                /*
                 * This should not make a difference since those tests are never
                 * stored to the DB... I Hope !
                 */
                test.setScore(0);
                for (KillMapEntry killMapEntry : killMapEntriesForTest) {
                    if (KillMapEntry.Status.KILL.equals(killMapEntry.status)) {
                        test.setScore(test.getScore() + 1);
                    }
                }
            }

            allTests.sort(Comparator.comparing(Test::getScore).reversed());
        } catch (Exception e) {
            logger.error("Cannot compute killmap:", e);
        }
        logger.debug("Prioritized test case {} ", allTests);
        return allTests;

    }

}

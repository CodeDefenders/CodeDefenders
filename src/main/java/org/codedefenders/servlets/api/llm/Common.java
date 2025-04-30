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
package org.codedefenders.servlets.api.llm;

import java.util.List;

import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.TestingFramework;
import org.codedefenders.model.EventType;

import com.github.javaparser.quality.Nullable;

public class Common {
    public record CutDTO(
            int classId,
            String alias,
            String name,

            String code,
            List<DependencyDTO> dependencies,
            TestingFramework testingFramework,
            AssertionLibrary assertionLibrary
    ) {
        public record DependencyDTO(
                String name,
                String code
        ) {
        }
    }

    public record PlayerDTO(
            int playerId,
            int userId,
            boolean isSystemPlayer,

            Role role,
            int points
    ) {
    }

    public record TestDTO(
            int testId,
            int playerId,

            boolean canView,
            @Nullable
            String code, // null if the requesting user is not allowed to see the test
            int score,
            List<Integer> linesCovered,

            List<Integer> coveredMutants,
            List<Integer> killedMutants
    ) {
        public static TestDTO fromTestDTO(org.codedefenders.dto.TestDTO test) {
            return new Common.TestDTO(
                    test.getId(),
                    test.getPlayerId(),
                    test.isCanView(),
                    test.getSource(),
                    test.getPoints(),
                    test.getLinesCovered(),
                    test.getCoveredMutantIds(),
                    test.getKilledMutantIds()
            );
        }
    }

    public record MutantDTO(
            int mutantId,
            int playerId,

            boolean canView,
            @Nullable
            String diff, // null if the requesting user is not allowed to see the mutant
            List<Integer> modifiedLines,
            int score,

            Mutant.State state,
            boolean covered,
            boolean canMarkEquivalent,

            @Nullable
            Integer killedByTestId, // null if alive
            @Nullable
            String killMessage // null if alive
    ) {
        public static MutantDTO fromMutantDTO(org.codedefenders.dto.MutantDTO mutant) {
             Integer killedByTestId = mutant.getKilledByTestId() != -1 ? mutant.getKilledByTestId() : null;
             return new Common.MutantDTO(
                    mutant.getId(),
                    mutant.getPlayerId(),
                    mutant.isCanView(),
                    mutant.getPatchString(),
                    mutant.getLines(),
                    mutant.getPoints(),
                    mutant.getState(),
                    mutant.getCovered(),
                    mutant.isCanMarkEquivalent(),
                    killedByTestId,
                    mutant.getKillMessage()
            );
        }

    }

    public record EventDTO(
            int eventId,
            int playerId,

            EventType type, // many of the EventType types are unused
            long timestamp
    ) {
    }

    public record ErrorResponseDTO(
            String message
    ) {
    }
}

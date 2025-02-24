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

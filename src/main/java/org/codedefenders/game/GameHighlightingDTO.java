package org.codedefenders.game;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.codedefenders.game.Mutant.Equivalence;

/**
 * Saves data for the game highlighting in order to convert it to JSON.
 */
public class GameHighlightingDTO {

    /**
     * Maps line numbers to the mutant ids of mutants that modify the line.
     */
    private Map<Integer, List<Integer>> mutantIdsPerLine;

    /**
     * Maps line numbers to the test ids of tests that cover the line.
     */
    private Map<Integer, List<Integer>> testIdsPerLine;

    /**
     * Maps test ids to mutants.
     */
    private Map<Integer, GHMutantDTO> mutants;

    /**
     * Maps test ids to tests..
     */
    private Map<Integer, GHTestDTO> tests;

    /**
     * Constructs the game highlighting data from the list of mutants and the list of tests in the game.
     * The object is ready to be converted to JSON after the constructor has been called.
     * @param mutants The mutants in the game.
     * @param tests The tests in the game.
     */
    public GameHighlightingDTO(List<Mutant> mutants, List<Test> tests) {
        this.mutantIdsPerLine = new TreeMap<>();
        this.testIdsPerLine = new TreeMap<>();
        this.mutants = new TreeMap<>();
        this.tests = new TreeMap<>();

        /* Construct the test maps. */
        for (Test test : tests) {
            this.tests.put(test.getId(), new GHTestDTO(test));
            List<Integer> linesCovered = test
                    .getLineCoverage()
                    .getLinesCovered()
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());
            for (Integer line : linesCovered) {
                List<Integer> list = testIdsPerLine.computeIfAbsent(line, key -> new LinkedList<>());
                list.add(test.getId());
            }
        }

        /* Construct the mutant maps. */
        for (Mutant mutant : mutants) {
            this.mutants.put(mutant.getId(), new GHMutantDTO(mutant));
            List<Integer> lines = mutant.getLines();
            for (Integer line : lines) {
                List<Integer> list = mutantIdsPerLine.computeIfAbsent(line, key -> new LinkedList<>());
                list.add(mutant.getId());
            }
        }
    }

    /**
     * Represents the status a mutant has in the game highlighting.
     */
    public enum GHMutantStatus {
        ALIVE,
        KILLED,
        FLAGGED,
        EQUIVALENT
    }

    /**
     * Represents a mutant for the game highlighting.
     */
    public static class GHMutantDTO {
        private int id;
        private int score;
        private String creatorName;
        private GHMutantStatus status;

        public GHMutantDTO(Mutant mutant) {
            this.id = mutant.getId();
            this.score = mutant.getScore();
            this.creatorName = mutant.getCreatorName();

            Equivalence eq = mutant.getEquivalent();
            if (eq == Equivalence.DECLARED_YES || eq == Equivalence.ASSUMED_YES) {
                this.status = GHMutantStatus.EQUIVALENT;
            } else if (eq == Equivalence.PENDING_TEST) {
                this.status = GHMutantStatus.FLAGGED;
            } else if (mutant.isAlive()) {
                this.status = GHMutantStatus.ALIVE;
            } else {
                this.status = GHMutantStatus.KILLED;
            }
        }
    }

    /**
     * Represents a test for the game highlighting.
     */
    public static class GHTestDTO {
        private int id;

        public GHTestDTO(Test test) {
            this.id = test.getId();
        }
    }

    /**
     * Serializes {@link Map Maps} as a list of lists, so that it can be used to construct an ES6 Map. <br>
     * Example: {@code var map = new Map(jsonString);}
     */
    public static class MapSerializer implements JsonSerializer<Map> {
        @Override
        public JsonElement serialize(Map map, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonArray outerArray = new JsonArray();
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                JsonArray innerArray = new JsonArray();
                innerArray.add(jsonSerializationContext.serialize(entry.getKey()));
                innerArray.add(jsonSerializationContext.serialize(entry.getValue()));
                outerArray.add(innerArray);
            }
            return outerArray;
        }
    }
}

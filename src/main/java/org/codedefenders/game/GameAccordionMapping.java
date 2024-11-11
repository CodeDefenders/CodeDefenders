package org.codedefenders.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.TestDTO;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

public class GameAccordionMapping {
    public static final String ALL_CATEGORY_ID = "all";
    public static final String OUTSIDE_METHODS_CATEGORY_ID = "noMethod";

    public final HashMap<MethodDescription, SortedSet<Integer>> elementsPerMethod;
    public final SortedSet<Integer> allElements;
    public final SortedSet<Integer> elementsOutsideMethods;

    private GameAccordionMapping() {
        this.elementsPerMethod = new HashMap<>();
        this.allElements = new TreeSet<>();
        this.elementsOutsideMethods = new TreeSet<>();
    }

    public static GameAccordionMapping computeForTests(Collection<MethodDescription> methodDescriptions,
                                                       Collection<TestDTO> tests) {
        return compute(methodDescriptions, tests, TestDTO::getId, TestDTO::getLinesCovered);
    }

    public static GameAccordionMapping computeForMutants(Collection<MethodDescription> methodDescriptions,
                                                         Collection<MutantDTO> mutants) {
        return compute(methodDescriptions, mutants, MutantDTO::getId, MutantDTO::getLines);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static <T> GameAccordionMapping compute(Collection<MethodDescription> methodDescriptions,
                                                   Collection<T> elements,
                                                   Function<T, Integer> getIdForElement,
                                                   Function<T, Collection<Integer>> getLinesForElement) {
        GameAccordionMapping mapping = new GameAccordionMapping();
        if (methodDescriptions.isEmpty()) {
            elements.stream()
                    .map(getIdForElement)
                    .forEach(id -> {
                        mapping.allElements.add(id);
                        mapping.elementsOutsideMethods.add(id);
                    });
            return mapping;
        }

        // Populate map of method descriptions to ids
        for (MethodDescription methodDescription : methodDescriptions) {
            mapping.elementsPerMethod.put(methodDescription, new TreeSet<>());
        }

        // Populate range map for method description ranges
        RangeMap<Integer, MethodDescription> methodRanges = TreeRangeMap.create();
        for (MethodDescription method : methodDescriptions) {
            methodRanges.put(Range.closed(method.getStartLine(), method.getEndLine()), method);
        }

        // Range before the first method
        Range<Integer> beforeFirst = Range.closedOpen(0, methodRanges.span().lowerEndpoint());

        // For every element, go through all lines and find the methods that include that line
        for (T element : elements) {
            int id = getIdForElement.apply(element);

            // Add to "all" set
            mapping.allElements.add(id);

            // Save the last range a line number fell into to avoid checking a line number in the same method twice
            Range<Integer> lastRange = null;

            for (Integer line : getLinesForElement.apply(element)) {

                // Skip if line falls into a method that was already considered
                if (lastRange != null && lastRange.contains(line)) {
                    continue;
                }

                Map.Entry<Range<Integer>, MethodDescription> entry = methodRanges.getEntry(line);

                // Line does not belong to any method
                if (entry == null) {
                    lastRange = beforeFirst;
                    mapping.elementsOutsideMethods.add(id);

                    // Line belongs to a method
                } else {
                    lastRange = entry.getKey();
                    MethodDescription methodDescription = entry.getValue();
                    mapping.elementsPerMethod.get(methodDescription).add(id);
                }
            }
        }

        return mapping;
    }
}

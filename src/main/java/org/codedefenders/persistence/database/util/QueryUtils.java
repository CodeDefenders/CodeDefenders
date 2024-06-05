package org.codedefenders.persistence.database.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryUtils {

    /**
     * Constructs SQL batch parameters from a list, using each value as single parameter.
     * This is only useful for queries with one parameter each.
     *
     * <p>Example:
     * <pre>
     * batchParamsFromList([1, 2, 3])
     *     == [[1], [2], [3]]
     * </pre>
     *
     * @param params A list of parameters, one for each query.
     * @return The parameters, in the correct format for {@link QueryRunner}.
     */
    public static <T> Object[][] batchParamsFromList(Collection<T> params) {
        return params.stream()
                .map(item -> new Object[]{item})
                .toArray(Object[][]::new);
    }

    /**
     * Constructs SQL batch parameters from a list, applying the giving extractors for each parameter.
     *
     * <p>Example:
     * <pre>
     * extractBatchParams([1, 2, 3], n -> n, n -> 10)
     *     == [[1, 10], [2, 10], [3, 10]]
     * </pre>
     *
     * @param params A list of parameters, one for each query.
     * @return The parameters, in the correct format for {@link QueryRunner}.
     */
    @SafeVarargs
    public static <T> Object[][] extractBatchParams(Collection<T> params, Function<T, Object>... extractors) {
        return params.stream()
                .map(item ->
                        Arrays.stream(extractors)
                            .map(extractor -> extractor.apply(item))
                            .toArray(Object[]::new)
                )
                .toArray(Object[][]::new);
    }

    /**
     * Creates comma-separated "?" placeholders for SQL queries.
     * @param number The number of placeholders to generate
     * @return A string containing the comma-separated placeholders.
     */
    public static String makePlaceholders(int number) {
        return Stream.generate(() -> "?")
                .limit(number)
                .collect(Collectors.joining(","));
    }
}

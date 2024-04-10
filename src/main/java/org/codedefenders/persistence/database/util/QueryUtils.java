package org.codedefenders.persistence.database.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryUtils {

    public static <T> Object[][] batchParamsFromList(Collection<T> params) {
        return params.stream()
                .map(item -> new Object[]{item})
                .toArray(Object[][]::new);
    }

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

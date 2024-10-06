package org.codedefenders.util;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

public class NamingUtils {
    private static final Pattern NUMBERED_REGEX = Pattern.compile("(.*)_(\\d+)$");

    public record Match(int index, String name){}

    /**
     * Formats a name with a number suffix. This should usually be used to dodge naming collisions.
     * @param bareName The name.
     * @param index The number to add to the name.
     * @return The name with the number suffix.
     */
    public static String formatNumberedName(String bareName, int index) {
        if (index == 1) {
            return bareName;
        } else {
            return "%s_%02d".formatted(bareName, index);
        }
    }

    /**
     * Parses a numbered name. Always returns a match, since any non-matching name that counts as index 1.
     * @param numberedName The numbered name to parse.
     * @return A match containing the index and the bare name.
     */
    public static Match parseNumberedName(String numberedName) {
        return parseNumberedName(numberedName, null)
                .orElseThrow(() -> new IllegalStateException("Unparsable numbered name: " + numberedName));
    }

    /**
     * Parses a numbered name. If the passed bare name is null, any input will result in a match.
     * If the bare name is non-null, a match is only returned in the bare name was matched.
     * @param numberedName The numbered name to parse.
     * @param bareName The bare name expected to be contained in the numbered name.
     * @return A match containing the index and the matched bare name.
     */
    public static Optional<Match> parseNumberedName(String numberedName, @Nullable String bareName) {
        if (numberedName.equals(bareName)) {
            return Optional.of(new Match(1, numberedName));
        }

        Matcher matcher = NUMBERED_REGEX.matcher(numberedName);
        if (matcher.find()) {
            String matchedName = matcher.group(1);
            int index = Integer.parseInt(matcher.group(2));
            if (bareName == null || matchedName.equals(bareName)) {
                return Optional.of(new Match(index, matchedName));
            }
        }

        if (bareName == null) {
            return Optional.of(new Match(1, numberedName));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Checks if the desired bare name already exists within the taken names.
     * If it exists, a number suffix is appended to the name, e.g. 'name_02'.
     * The number is chosen conservatively as the largest matching number in the taken names plus one.
     * @param bareName The bare name.
     * @param takenNames Names that are already taken. Can contain other names that don't match the bare name.
     *                   Needs to contain all names that match the bare name.
     * @return A numbered name that doesn't exist within the taken names. Can be the name without changes.
     */
    public static String nextFreeName(Collection<String> takenNames, String bareName) {
        int number = takenNames.stream()
                .filter(takenName -> takenName.startsWith(bareName))
                .map(takenName -> parseNumberedName(takenName, bareName))
                .flatMap(Optional::stream)
                .mapToInt(Match::index)
                .max()
                .orElse(0);
        return formatNumberedName(bareName, number + 1);
    }
}

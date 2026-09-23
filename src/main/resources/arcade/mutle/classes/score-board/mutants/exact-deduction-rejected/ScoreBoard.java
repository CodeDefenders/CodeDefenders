import java.util.LinkedHashMap;
import java.util.Map;

/** Records positive player scores while keeping the board within a fixed size. */
public final class ScoreBoard {
    private final int limit;
    private final Map<String, Integer> scores = new LinkedHashMap<>();

    public ScoreBoard(int limit) {
        if (limit < 1 || limit > 10) {
            throw new IllegalArgumentException("limit must be between 1 and 10");
        }
        this.limit = limit;
    }

    public boolean record(String player, int points) {
        requirePlayer(player);
        if (points <= 0) {
            return false;
        }
        if (!scores.containsKey(player) && scores.size() >= limit) {
            return false;
        }
        scores.merge(player, points, Integer::sum);
        return true;
    }

    public boolean deduct(String player, int points) {
        requirePlayer(player);
        if (points <= 0) {
            return false;
        }
        Integer current = scores.get(player);
        if (current == null || points >= current) {
            return false;
        }
        scores.put(player, current - points);
        return true;
    }

    public int score(String player) {
        requirePlayer(player);
        return scores.getOrDefault(player, 0);
    }

    public boolean hasPlayer(String player) {
        requirePlayer(player);
        return scores.containsKey(player);
    }

    public int size() {
        return scores.size();
    }

    private static void requirePlayer(String player) {
        if (player == null || player.trim().isEmpty()) {
            throw new IllegalArgumentException("player must not be blank");
        }
    }
}

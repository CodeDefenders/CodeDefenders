/** Stores bounded tokens that can be replenished and spent. */
public final class TokenBucket {
    private final int capacity;
    private int tokens;

    public TokenBucket(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public boolean refill(int amount) {
        if (amount <= 0) {
            return false;
        }
        tokens = (int) Math.min(capacity, (long) tokens + amount);
        return true;
    }

    public boolean spend(int amount) {
        if (amount <= 0 || amount > tokens) {
            return false;
        }
        tokens -= amount;
        return true;
    }

    public int tokens() {
        return tokens;
    }

    public boolean empty() {
        return tokens == 0;
    }
}

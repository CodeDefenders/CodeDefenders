/** Models a tank that accepts complete fills and drains within its bounds. */
public final class WaterTank {
    private final int capacity;
    private int level;

    public WaterTank(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public boolean fill(int amount) {
        if (amount <= 0) {
            return false;
        }
        if (level + amount > capacity) {
            return false;
        }
        level += amount;
        return true;
    }

    public boolean drain(int amount) {
        if (amount <= 0) {
            return false;
        }
        if (amount >= level) {
            return false;
        }
        level -= amount;
        return true;
    }

    public int level() {
        return level;
    }

    public int remainingCapacity() {
        return capacity - level;
    }

    public boolean isEmpty() {
        return level == 0;
    }

    public boolean isFull() {
        return level == capacity;
    }
}

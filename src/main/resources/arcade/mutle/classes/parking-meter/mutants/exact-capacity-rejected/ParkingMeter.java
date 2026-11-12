/** Tracks prepaid parking minutes within a meter's capacity. */
public final class ParkingMeter {
    private final int capacity;
    private int remaining;

    public ParkingMeter(int capacity) {
        if (capacity <= 0 || capacity > 240) {
            throw new IllegalArgumentException("capacity must be between 1 and 240 minutes");
        }
        this.capacity = capacity;
    }

    public boolean insert(int minutes) {
        if (minutes <= 0 || minutes >= capacity - remaining) {
            return false;
        }
        remaining += minutes;
        return true;
    }

    public boolean consume(int minutes) {
        if (minutes <= 0 || minutes > remaining) {
            return false;
        }
        remaining -= minutes;
        return true;
    }

    public int remaining() {
        return remaining;
    }

    public boolean expired() {
        return remaining == 0;
    }
}

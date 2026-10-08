import java.util.ArrayList;
import java.util.List;

/**
 * A bounded first-in-first-out queue of distinct, nonblank service requests.
 * Adding to a full queue or adding a duplicate changes nothing; serving an empty queue returns null.
 */
public class ServiceQueue {
    private final int capacity;
    private final List<String> requests = new ArrayList<>();

    public ServiceQueue(int capacity) {
        if (capacity < 1 || capacity > 20) {
            throw new IllegalArgumentException("capacity must be between 1 and 20");
        }
        this.capacity = capacity;
    }

    public boolean add(String request) {
        requireRequest(request);
        if (requests.size() == capacity || requests.contains(request)) {
            return false;
        }
        requests.add(request);
        return true;
    }

    public String serveNext() {
        if (requests.isEmpty()) {
            return null;
        }
        return requests.remove(requests.size() - 1);
    }

    public boolean cancel(String request) {
        requireRequest(request);
        if (!requests.contains(request)) {
            return false;
        }
        requests.remove(request);
        return true;
    }

    public int size() {
        return requests.size();
    }

    public boolean isFull() {
        return requests.size() == capacity;
    }

    private static void requireRequest(String request) {
        if (request == null || request.trim().isEmpty()) {
            throw new IllegalArgumentException("request must not be blank");
        }
    }
}

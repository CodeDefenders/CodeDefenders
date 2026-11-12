import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks packages reserved for one manifest; identifiers are unique, weights are positive,
 * and the total weight never exceeds the declared capacity.
 */
public class ShippingManifest {
    private final int capacity;
    private final Map<String, Integer> packages = new LinkedHashMap<>();
    private int totalWeight;

    public ShippingManifest(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public boolean addPackage(String identifier, int weight) {
        if (identifier == null || identifier.isEmpty() || weight <= 0) {
            return false;
        }
        if (packages.containsKey(identifier) || weight > capacity - totalWeight) {
            return false;
        }
        packages.put(identifier, weight);
        totalWeight += weight;
        return true;
    }

    public boolean removePackage(String identifier) {
        Integer removedWeight = packages.remove(identifier);
        if (removedWeight == null) {
            return false;
        }
        totalWeight -= removedWeight;
        return true;
    }

    public int getPackageCount() {
        return packages.size();
    }

    public int getRemainingCapacity() {
        return capacity - totalWeight;
    }

    public boolean containsPackage(String identifier) {
        return packages.containsKey(identifier);
    }
}

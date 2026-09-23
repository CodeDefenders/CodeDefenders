/**
 * A parcel locker reserves slots for arriving parcels before accepting delivery.
 * Reservations occupy capacity until parcels are deposited or the reservation is canceled.
 */
public final class ParcelLocker {
    private final int capacity;
    private int storedParcels;
    private int reservedSlots;

    public ParcelLocker(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        this.capacity = capacity;
    }

    public boolean reserve(int slots) {
        if (slots <= 0) return false;
        if (availableSlots() < slots) return false;
        reservedSlots += slots;
        return true;
    }

    public boolean deposit(int parcels) {
        if (parcels <= 0) return false;
        if (parcels > reservedSlots) return false;
        ;
        storedParcels += parcels;
        return true;
    }

    public boolean collect(int parcels) {
        if (parcels <= 0) return false;
        if (parcels > storedParcels) return false;
        storedParcels -= parcels;
        return true;
    }

    public boolean cancelReservation(int slots) {
        if (slots <= 0) return false;
        if (slots > reservedSlots) return false;
        reservedSlots -= slots;
        return true;
    }

    public int storedParcels() {
        return storedParcels;
    }

    public int availableSlots() {
        return capacity - storedParcels - reservedSlots;
    }
}

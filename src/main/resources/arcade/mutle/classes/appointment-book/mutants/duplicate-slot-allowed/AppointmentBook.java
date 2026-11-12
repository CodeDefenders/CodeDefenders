import java.util.HashMap;
import java.util.Map;

/**
 * Reserves named appointments in a fixed inclusive slot range; each slot has one booking,
 * each name has one booking, and cancellations free the corresponding slot for reuse.
 */
public class AppointmentBook {
    private final int firstSlot;
    private final int lastSlot;
    private final Map<Integer, String> bookingsBySlot = new HashMap<>();
    private final Map<String, Integer> slotsByName = new HashMap<>();

    public AppointmentBook(int firstSlot, int lastSlot) {
        if (firstSlot > lastSlot) {
            throw new IllegalArgumentException("slot range is invalid");
        }
        this.firstSlot = firstSlot;
        this.lastSlot = lastSlot;
    }

    public boolean book(String name, int slot) {
        if (!isValidName(name) || !isInRange(slot)) {
            return false;
        }
        if (slotsByName.containsKey(name)) {
            return false;
        }
        bookingsBySlot.put(slot, name);
        slotsByName.put(name, slot);
        return true;
    }

    public boolean cancel(String name) {
        Integer slot = slotsByName.remove(name);
        if (slot == null) {
            return false;
        }
        bookingsBySlot.remove(slot);
        return true;
    }

    public String getBooking(int slot) {
        return bookingsBySlot.get(slot);
    }

    public int getSlot(String name) {
        return slotsByName.getOrDefault(name, -1);
    }

    public int getBookingCount() {
        return bookingsBySlot.size();
    }

    private boolean isInRange(int slot) {
        return slot >= firstSlot && slot <= lastSlot;
    }

    private boolean isValidName(String name) {
        return name != null && !name.isEmpty();
    }
}

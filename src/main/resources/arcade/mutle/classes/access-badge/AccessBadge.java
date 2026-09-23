/**
 * A badge grants clearance from zero through five, can be suspended, and records denied checks.
 * Requested clearances outside that range are invalid; suspended badges deny every valid request.
 */
public class AccessBadge {
    private int clearance;
    private boolean suspended;
    private int deniedChecks;

    public AccessBadge(int clearance) {
        requireClearance(clearance);
        this.clearance = clearance;
    }

    public boolean permits(int requestedClearance) {
        requireClearance(requestedClearance);
        if (suspended) {
            deniedChecks++;
            return false;
        }
        boolean permitted = clearance >= requestedClearance;
        if (!permitted) {
            deniedChecks++;
        }
        return permitted;
    }

    public boolean changeClearance(int newClearance) {
        requireClearance(newClearance);
        if (clearance == newClearance) {
            return false;
        }
        clearance = newClearance;
        return true;
    }

    public boolean suspend() {
        if (suspended) {
            return false;
        }
        suspended = true;
        return true;
    }

    public boolean restore() {
        if (!suspended) {
            return false;
        }
        suspended = false;
        return true;
    }

    public int getDeniedChecks() {
        return deniedChecks;
    }

    private static void requireClearance(int value) {
        if (value < 0 || value > 5) {
            throw new IllegalArgumentException("clearance must be between 0 and 5");
        }
    }
}

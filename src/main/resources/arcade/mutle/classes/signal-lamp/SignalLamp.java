/** Models a three-colour signal whose phases advance in a fixed cycle. */
public final class SignalLamp {
    private static final String RED = "red";
    private static final String AMBER = "amber";
    private static final String GREEN = "green";
    private String colour;

    public SignalLamp(String initialColour) {
        requireColour(initialColour);
        colour = initialColour;
    }

    public boolean advance() {
        String next;
        if (RED.equals(colour)) {
            next = GREEN;
        } else if (GREEN.equals(colour)) {
            next = AMBER;
        } else {
            next = RED;
        }
        if (next.equals(colour)) {
            return false;
        }
        colour = next;
        return true;
    }

    public boolean safeToCross() {
        return GREEN.equals(colour);
    }

    public String colour() {
        return colour;
    }

    private static void requireColour(String value) {
        if (!RED.equals(value) && !AMBER.equals(value) && !GREEN.equals(value)) {
            throw new IllegalArgumentException("unknown signal colour");
        }
    }
}

/** Controls a temperature within inclusive bounds and reports heating demand. */
public final class Thermostat {
    private final int minimum;
    private final int maximum;
    private int temperature;

    public Thermostat(int minimum, int maximum, int initial) {
        if (minimum >= maximum) {
            throw new IllegalArgumentException("temperature range must be increasing");
        }
        if (initial < minimum || initial > maximum) {
            throw new IllegalArgumentException("initial temperature is outside the range");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.temperature = initial;
    }

    public boolean setTemperature(int value) {
        if (value <= minimum || value > maximum) {
            return false;
        }
        if (value == temperature) {
            return false;
        }
        temperature = value;
        return true;
    }

    public boolean heatingNeeded(int target) {
        if (target < minimum || target > maximum) {
            throw new IllegalArgumentException("target temperature is outside the range");
        }
        return temperature < target;
    }

    public int temperature() {
        return temperature;
    }
}

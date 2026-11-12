import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ThermostatInitialRangeTest {
  @Test
  public void constructorRejectsInitialValueOutsideRange() {
    assertThrows(IllegalArgumentException.class, () -> new Thermostat(18, 24, 17));
    assertThrows(IllegalArgumentException.class, () -> new Thermostat(18, 24, 25));
  }
}

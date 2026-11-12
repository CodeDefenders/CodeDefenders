import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ThermostatRangeOrderTest {
  @Test
  public void constructorRejectsReversedRange() {
    assertThrows(IllegalArgumentException.class, () -> new Thermostat(20, 20, 20));
    assertThrows(IllegalArgumentException.class, () -> new Thermostat(25, 20, 22));
  }
}

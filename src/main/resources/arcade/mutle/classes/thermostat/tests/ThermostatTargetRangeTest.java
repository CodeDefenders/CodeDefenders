import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ThermostatTargetRangeTest {
  @Test
  public void targetMustBeInRange() {
    Thermostat thermostat = new Thermostat(18, 24, 20);
    assertThrows(IllegalArgumentException.class, () -> thermostat.heatingNeeded(17));
    assertThrows(IllegalArgumentException.class, () -> thermostat.heatingNeeded(25));
  }
}

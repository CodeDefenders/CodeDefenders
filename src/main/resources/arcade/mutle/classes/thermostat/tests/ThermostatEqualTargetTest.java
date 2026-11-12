import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ThermostatEqualTargetTest {
  @Test
  public void equalTargetNeedsNoHeating() {
    assertFalse(new Thermostat(18, 24, 20).heatingNeeded(20));
  }
}

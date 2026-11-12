import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThermostatHigherTargetTest {
  @Test
  public void higherTargetNeedsHeating() {
    assertTrue(new Thermostat(18, 24, 20).heatingNeeded(21));
  }
}

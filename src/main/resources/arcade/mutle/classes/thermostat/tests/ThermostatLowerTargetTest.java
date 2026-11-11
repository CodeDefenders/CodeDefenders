import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ThermostatLowerTargetTest {
  @Test
  public void lowerTargetNeedsNoHeating() {
    assertFalse(new Thermostat(18, 24, 20).heatingNeeded(19));
  }
}

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThermostatLowerBoundTest {
  @Test
  public void lowerBoundCanBeSelected() {
    assertTrue(new Thermostat(18, 24, 20).setTemperature(18));
  }
}

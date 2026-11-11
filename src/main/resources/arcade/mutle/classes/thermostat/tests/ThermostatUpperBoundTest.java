import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThermostatUpperBoundTest {
  @Test
  public void upperBoundCanBeSelected() {
    assertTrue(new Thermostat(18, 24, 20).setTemperature(24));
  }
}

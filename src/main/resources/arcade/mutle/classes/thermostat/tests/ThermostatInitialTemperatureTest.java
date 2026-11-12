import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ThermostatInitialTemperatureTest {
  @Test
  public void initialTemperatureIsRetained() {
    assertEquals(20, new Thermostat(18, 24, 20).temperature());
  }
}

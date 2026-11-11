import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThermostatSettingChangesTest {
  @Test
  public void newSettingIsReported() {
    Thermostat thermostat = new Thermostat(18, 24, 20);
    assertTrue(thermostat.setTemperature(21));
    assertEquals(21, thermostat.temperature());
  }
}

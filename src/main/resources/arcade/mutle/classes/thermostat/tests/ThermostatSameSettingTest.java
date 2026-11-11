import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ThermostatSameSettingTest {
  @Test
  public void sameSettingReportsNoChange() {
    assertFalse(new Thermostat(18, 24, 20).setTemperature(20));
  }
}

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParkingMeterStartsExpiredTest {
  @Test
  public void newMeterHasNoMinutes() {
    ParkingMeter meter = new ParkingMeter(60);
    assertTrue(meter.expired());
  }
}

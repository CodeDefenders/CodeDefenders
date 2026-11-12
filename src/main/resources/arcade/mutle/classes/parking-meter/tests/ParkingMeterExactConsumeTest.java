import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParkingMeterExactConsumeTest {
  @Test
  public void consumesAllRemainingMinutes() {
    ParkingMeter meter = new ParkingMeter(60);
    meter.insert(20);
    assertTrue(meter.consume(20));
    assertEquals(0, meter.remaining());
  }
}

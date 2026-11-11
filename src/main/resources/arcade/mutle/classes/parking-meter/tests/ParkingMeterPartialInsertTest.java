import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParkingMeterPartialInsertTest {
  @Test
  public void retainsInsertedMinutes() {
    ParkingMeter meter = new ParkingMeter(60);
    assertTrue(meter.insert(15));
    assertEquals(15, meter.remaining());
  }
}

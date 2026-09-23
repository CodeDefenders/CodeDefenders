import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParkingMeterExactCapacityTest {
  @Test
  public void acceptsExactCapacity() {
    ParkingMeter meter = new ParkingMeter(60);
    assertTrue(meter.insert(60));
    assertEquals(60, meter.remaining());
  }
}

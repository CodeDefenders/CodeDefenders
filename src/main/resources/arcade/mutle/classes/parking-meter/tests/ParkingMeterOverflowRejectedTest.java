import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ParkingMeterOverflowRejectedTest {
  @Test
  public void rejectsMinutesBeyondCapacity() {
    ParkingMeter meter = new ParkingMeter(60);
    assertFalse(meter.insert(61));
    assertFalse(meter.insert(Integer.MAX_VALUE));
    assertEquals(0, meter.remaining());
  }
}

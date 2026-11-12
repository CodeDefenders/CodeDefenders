import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ParkingMeterNonpositiveRejectedTest {
  @Test
  public void rejectsZeroInsertAndConsume() {
    ParkingMeter meter = new ParkingMeter(60);
    assertFalse(meter.insert(0));
    assertFalse(meter.consume(0));
  }
}

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ParkingMeterUnderflowRejectedTest {
  @Test
  public void rejectsConsumptionBeyondRemaining() {
    ParkingMeter meter = new ParkingMeter(60);
    meter.insert(20);
    assertFalse(meter.consume(21));
    assertEquals(20, meter.remaining());
  }
}

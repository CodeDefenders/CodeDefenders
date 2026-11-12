import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ParkingMeterCapacityRangeTest {
  @Test
  public void rejectsNonpositiveCapacity() {
    assertThrows(IllegalArgumentException.class, () -> new ParkingMeter(0));
  }

  @Test
  public void rejectsCapacityAboveLimit() {
    assertThrows(IllegalArgumentException.class, () -> new ParkingMeter(241));
  }
}

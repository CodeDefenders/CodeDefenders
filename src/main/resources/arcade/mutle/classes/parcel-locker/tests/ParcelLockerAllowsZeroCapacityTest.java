import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ParcelLockerAllowsZeroCapacityTest {
  @Test
  public void allowsZeroCapacity() {
    ParcelLocker locker = new ParcelLocker(0);
    assertEquals(0, locker.availableSlots());
    assertFalse(locker.reserve(1));
  }
}

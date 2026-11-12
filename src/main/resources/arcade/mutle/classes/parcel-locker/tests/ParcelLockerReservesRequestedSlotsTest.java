import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerReservesRequestedSlotsTest {
  @Test
  public void reservesRequestedSlots() {
    ParcelLocker locker = new ParcelLocker(3);
    assertTrue(locker.reserve(2));
    assertEquals(1, locker.availableSlots());
  }
}

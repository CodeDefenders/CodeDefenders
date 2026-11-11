import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerRejectsReservationBeyondCapacityTest {
  @Test
  public void rejectsReservationBeyondCapacity() {
    ParcelLocker locker = new ParcelLocker(2);
    assertTrue(locker.reserve(2));
    assertFalse(locker.reserve(1));
    assertEquals(0, locker.availableSlots());
  }
}

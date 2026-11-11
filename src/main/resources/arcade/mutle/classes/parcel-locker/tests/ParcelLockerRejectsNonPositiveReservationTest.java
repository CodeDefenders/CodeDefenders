import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ParcelLockerRejectsNonPositiveReservationTest {
  @Test
  public void rejectsNonPositiveReservation() {
    ParcelLocker locker = new ParcelLocker(2);
    assertFalse(locker.reserve(0));
    assertEquals(2, locker.availableSlots());
  }
}

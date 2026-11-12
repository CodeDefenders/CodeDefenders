import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerCancellationFreesSlotsTest {
  @Test
  public void cancellationFreesSlots() {
    ParcelLocker locker = new ParcelLocker(2);
    locker.reserve(2);
    assertTrue(locker.cancelReservation(1));
    assertEquals(1, locker.availableSlots());
  }
}

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerDepositRequiresPositiveReservationTest {
  @Test
  public void depositRequiresPositiveReservation() {
    ParcelLocker locker = new ParcelLocker(2);
    assertTrue(locker.reserve(1));
    assertFalse(locker.deposit(0));
    assertEquals(0, locker.storedParcels());
    assertEquals(1, locker.availableSlots());
  }
}

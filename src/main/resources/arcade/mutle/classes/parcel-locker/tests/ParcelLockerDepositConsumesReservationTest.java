import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerDepositConsumesReservationTest {
  @Test
  public void depositConsumesReservation() {
    ParcelLocker locker = new ParcelLocker(1);
    locker.reserve(1);
    assertTrue(locker.deposit(1));
    assertFalse(locker.deposit(1));
    assertFalse(locker.cancelReservation(1));
  }
}

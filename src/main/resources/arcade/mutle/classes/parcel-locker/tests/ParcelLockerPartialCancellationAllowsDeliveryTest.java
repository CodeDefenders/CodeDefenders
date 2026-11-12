import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerPartialCancellationAllowsDeliveryTest {
  @Test
  public void partialCancellationAllowsDelivery() {
    ParcelLocker locker = new ParcelLocker(3);
    assertTrue(locker.reserve(2));
    assertTrue(locker.cancelReservation(1));
    assertTrue(locker.deposit(1));
  }
}

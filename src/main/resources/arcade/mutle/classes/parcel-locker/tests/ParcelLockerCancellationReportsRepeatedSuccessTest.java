import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerCancellationReportsRepeatedSuccessTest {
  @Test
  public void cancellationReportsRepeatedSuccess() {
    ParcelLocker locker = new ParcelLocker(2);
    assertFalse(locker.cancelReservation(0));
    locker.reserve(2);
    assertTrue(locker.cancelReservation(1));
    assertTrue(locker.cancelReservation(1));
  }
}

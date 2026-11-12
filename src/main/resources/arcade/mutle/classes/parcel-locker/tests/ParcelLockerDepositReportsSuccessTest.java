import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerDepositReportsSuccessTest {
  @Test
  public void depositReportsSuccess() {
    ParcelLocker locker = new ParcelLocker(2);
    locker.reserve(2);
    assertTrue(locker.deposit(1));
  }
}

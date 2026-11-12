import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerCollectionReportsRepeatedSuccessTest {
  @Test
  public void collectionReportsRepeatedSuccess() {
    ParcelLocker locker = new ParcelLocker(2);
    locker.reserve(2);
    locker.deposit(2);
    assertTrue(locker.collect(1));
    assertTrue(locker.collect(1));
  }
}

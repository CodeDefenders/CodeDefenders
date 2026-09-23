import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerRejectsCollectionOutsideRangeTest {
  @Test
  public void rejectsCollectionOutsideRange() {
    ParcelLocker locker = new ParcelLocker(2);
    assertTrue(locker.reserve(1));
    assertTrue(locker.deposit(1));
    assertFalse(locker.collect(0));
    assertFalse(locker.collect(2));
    assertEquals(1, locker.storedParcels());
  }
}

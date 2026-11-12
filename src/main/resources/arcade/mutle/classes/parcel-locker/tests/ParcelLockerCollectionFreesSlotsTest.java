import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParcelLockerCollectionFreesSlotsTest {
  @Test
  public void collectionFreesSlots() {
    ParcelLocker locker = new ParcelLocker(2);
    locker.reserve(2);
    locker.deposit(2);
    assertTrue(locker.collect(1));
    assertEquals(1, locker.storedParcels());
    assertEquals(1, locker.availableSlots());
  }
}

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ParcelLockerReportsInitialSlotsTest {
  @Test
  public void reportsInitialSlots() {
    assertEquals(3, new ParcelLocker(3).availableSlots());
  }
}

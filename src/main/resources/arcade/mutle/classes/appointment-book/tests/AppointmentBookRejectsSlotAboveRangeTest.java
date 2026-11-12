import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppointmentBookRejectsSlotAboveRangeTest {
  @Test
  public void rejectsSlotAboveRange() {
    assertFalse(new AppointmentBook(1, 2).book("A", 3));
  }
}

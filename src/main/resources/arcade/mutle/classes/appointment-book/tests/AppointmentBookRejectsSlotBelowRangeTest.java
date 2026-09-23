import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppointmentBookRejectsSlotBelowRangeTest {
  @Test
  public void rejectsSlotBelowRange() {
    assertFalse(new AppointmentBook(1, 2).book("A", 0));
  }
}

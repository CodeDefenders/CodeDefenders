import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppointmentBookRejectsOccupiedSlotTest {
  @Test
  public void rejectsOccupiedSlot() {
    AppointmentBook book = new AppointmentBook(1, 2);
    book.book("A", 1);
    assertFalse(book.book("B", 1));
  }
}

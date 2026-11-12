import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppointmentBookBooksLastSlotTest {
  @Test
  public void booksLastSlot() {
    assertTrue(new AppointmentBook(1, 2).book("A", 2));
  }
}

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppointmentBookBooksFirstSlotTest {
  @Test
  public void booksFirstSlot() {
    assertTrue(new AppointmentBook(1, 2).book("A", 1));
  }
}

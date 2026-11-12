import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppointmentBookCancellationFreesSlotTest {
  @Test
  public void cancellationFreesSlot() {
    AppointmentBook book = new AppointmentBook(1, 2);
    book.book("A", 1);
    book.cancel("A");
    assertTrue(book.book("B", 1));
  }
}

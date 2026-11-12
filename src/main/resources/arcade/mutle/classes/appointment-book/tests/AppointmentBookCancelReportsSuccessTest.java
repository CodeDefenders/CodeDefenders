import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppointmentBookCancelReportsSuccessTest {
  @Test
  public void cancellationReportsSuccess() {
    AppointmentBook book = new AppointmentBook(1, 2);
    book.book("A", 1);
    assertTrue(book.cancel("A"));
  }
}

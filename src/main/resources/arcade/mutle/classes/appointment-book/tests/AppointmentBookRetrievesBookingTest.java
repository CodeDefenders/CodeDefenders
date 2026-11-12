import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppointmentBookRetrievesBookingTest {
  @Test
  public void retrievesBookedName() {
    AppointmentBook book = new AppointmentBook(1, 2);
    book.book("A", 1);
    assertEquals("A", book.getBooking(1));
  }
}

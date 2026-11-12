import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppointmentBookCancelMissingBookingTest {
  @Test
  public void cancelMissingBooking() {
    assertFalse(new AppointmentBook(1, 2).cancel("A"));
  }
}

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppointmentBookRejectsDuplicateNameTest {
  @Test
  public void rejectsDuplicateName() {
    AppointmentBook book = new AppointmentBook(1, 2);
    book.book("A", 1);
    assertFalse(book.book("A", 2));
  }
}

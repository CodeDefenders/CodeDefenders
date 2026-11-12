import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppointmentBookRejectsEmptyNameTest {
  @Test
  public void rejectsEmptyName() {
    assertFalse(new AppointmentBook(1, 2).book("", 1));
  }
}

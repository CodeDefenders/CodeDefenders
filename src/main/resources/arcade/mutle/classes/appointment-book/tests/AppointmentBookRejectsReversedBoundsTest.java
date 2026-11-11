import org.junit.Test;

public class AppointmentBookRejectsReversedBoundsTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsReversedBounds() {
    new AppointmentBook(5, 4);
  }
}

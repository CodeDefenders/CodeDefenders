import org.junit.Test;

public class AccessBadgeConstructorRangeTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsClearanceAboveMaximum() {
    new AccessBadge(6);
  }
}

import org.junit.Test;

public class AccessBadgeRequestRangeTest {
  @Test(expected = IllegalArgumentException.class)
  public void rejectsNegativeRequestedClearance() {
    new AccessBadge(0).permits(-1);
  }
}

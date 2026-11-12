import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgePermitsEqualTest {
  @Test
  public void permitsMatchingClearance() {
    assertTrue(new AccessBadge(3).permits(3));
  }
}

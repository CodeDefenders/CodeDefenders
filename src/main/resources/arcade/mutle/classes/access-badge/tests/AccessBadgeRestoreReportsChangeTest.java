import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgeRestoreReportsChangeTest {
  @Test
  public void restoreReportsChange() {
    AccessBadge badge = new AccessBadge(1);
    badge.suspend();
    assertTrue(badge.restore());
  }
}

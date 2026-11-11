import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgeRestoreTest {
  @Test
  public void restoreChangesSuspendedBadge() {
    AccessBadge badge = new AccessBadge(1);
    badge.suspend();
    assertTrue(badge.restore());
    assertTrue(badge.permits(1));
    assertFalse(badge.restore());
  }
}

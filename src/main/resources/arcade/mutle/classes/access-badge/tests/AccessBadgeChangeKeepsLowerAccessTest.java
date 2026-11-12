import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgeChangeKeepsLowerAccessTest {
  @Test
  public void changedClearanceKeepsLowerAccess() {
    AccessBadge badge = new AccessBadge(1);
    badge.changeClearance(3);
    assertTrue(badge.permits(1));
  }
}

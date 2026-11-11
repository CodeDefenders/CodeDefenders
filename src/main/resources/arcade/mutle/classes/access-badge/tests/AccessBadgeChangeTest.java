import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgeChangeTest {
  @Test
  public void changedClearanceAffectsLaterChecks() {
    AccessBadge badge = new AccessBadge(1);
    assertTrue(badge.changeClearance(4));
    assertTrue(badge.permits(4));
    assertFalse(badge.permits(5));
  }
}

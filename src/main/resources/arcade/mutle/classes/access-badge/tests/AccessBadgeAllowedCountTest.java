import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgeAllowedCountTest {
  @Test
  public void allowedCheckDoesNotIncreaseDeniedCount() {
    AccessBadge badge = new AccessBadge(4);
    assertTrue(badge.permits(2));
    assertEquals(0, badge.getDeniedChecks());
  }
}

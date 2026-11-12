import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AccessBadgeDenialTest {
  @Test
  public void recordsDeniedHigherClearance() {
    AccessBadge badge = new AccessBadge(2);
    assertFalse(badge.permits(3));
    assertEquals(1, badge.getDeniedChecks());
  }
}

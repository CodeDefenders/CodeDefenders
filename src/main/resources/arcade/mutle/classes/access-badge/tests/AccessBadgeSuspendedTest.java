import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgeSuspendedTest {
  @Test
  public void suspensionDeniesAndCountsValidRequest() {
    AccessBadge badge = new AccessBadge(5);
    assertTrue(badge.suspend());
    assertFalse(badge.permits(0));
    assertEquals(1, badge.getDeniedChecks());
  }
}

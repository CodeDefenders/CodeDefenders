import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessBadgeSuspendRepeatTest {
  @Test
  public void secondSuspensionReportsNoChange() {
    AccessBadge badge = new AccessBadge(1);
    assertTrue(badge.suspend());
    assertFalse(badge.suspend());
  }
}

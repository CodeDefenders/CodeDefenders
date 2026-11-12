import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AccessBadgeRestoreUnsuspendedTest {
  @Test
  public void unsuspendedBadgeCannotBeRestored() {
    assertFalse(new AccessBadge(1).restore());
  }
}
